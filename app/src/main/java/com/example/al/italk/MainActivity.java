package com.example.al.italk;

/***
 * ITalk App for Android Version 0.5 Beta
 * Author: Albert N. Nemethy
 * Last UpDate: 11-10-2014
 * Copyright (c) Autonomous Engineering of VT
 * All Rights Reserved
 *
 * MainActivity.java
 */

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity
{ private static String sysStatMsg = "";            // System Status message copy for persistence
  private static String ourPatientIDVal = null;     // Patient ID value THIS NEEDS TO PERSIST !
  private static String storedConStatMsg = null;    // Not use as of yet. Can be used to for persistance
  private static String storedSysStatMsg = null;
  private static TextView ourConStatus = null;      // TextView View object for Connection text field
  private static TextView ourDevBatLvl = null;      // TextView View object for batter level text field
  private static TextView ourSysStatus = null;      // TextView View object for System Status text field
  private final String ourHName = "http://www.whatnicWoodi.com";

  public static final String PREFS_NAME = "ITalkPrefsFile";   // App preferences file name

  private EditText ourPatientID = null;             // EditText View object for patient ID
  private SharedPreferences appPrefs = null;        // App preferences object for persistent data
  private InputMethodManager ourImm;                // Our input method manager for dynamic keyboard
  private Context ourContext = null;                // Our content definition not use as of yet
  private BluetoothAdapter BTAdapter = null;        // Systems Bluetooth adapter
  private ConnectDevice ourConn = null;             // Object representing a connection
  private DeviceTalk ourTalk = null;                // Object representing a device conversation
  private DeviceType ourDevice = null;              // Our device object
  private ArrayList<DeviceType> ourDevices = null;  // Array for the devices found
  private DataPipe ourDataPipe = new DataPipe(0);   // Define our host FIFO data pipe
  private ProgressBar ourProgBar = null;            // Define the progress bar
  private ConnectHost ourHostConnection = null;     // Our host connection object to access a host socket from

  private bcastASCReceiver ourASCReceiver = null;   // Broadcast receiver definitions BT Adapter ACTION_STATE_CHANGED
  private bcastADFReceiver ourADFReceiver = null;   // Broadcast receiver for scanner ACTION_DISCOVERY_FINISHED
  private bcastAFReceiver ourAFReceiver = null;     // Broadcast receiver for device found ACTION_FOUND

  @Override
  protected void onCreate(Bundle savedInstanceState)
  { super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ourContext = this.getApplicationContext();
                                    // Turn screen rotation OFF ! 0 means off
    Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

    if (ourSysStatus == null)       // Get all of our status widgets by their xml ID values
    { ourSysStatus = ((TextView)findViewById(R.id.sstat_tf)); }    // System status text field

    if (ourPatientID == null)
    { ourPatientID = ((EditText)findViewById(R.id.pd_tf)); }       // Patient ID text field

    if (ourConStatus == null)
    { ourConStatus = ((TextView)findViewById(R.id.ds_tf)); }       // Device status text field

    if (ourDevBatLvl == null)
    { ourDevBatLvl = ((TextView)findViewById(R.id.bl_tf)); }       // Device Battery level text field

    if (ourPatientIDVal == null)                     // Access stored preferences if necessary
    { appPrefs = getSharedPreferences(PREFS_NAME, 0);
      ourPatientIDVal = appPrefs.getString("ourPatientIDVal", null);
    }

    if (ourProgBar == null)                          // Get access to the progress bar
    { ourProgBar = ((ProgressBar)findViewById(R.id.databuffer_progbar)); }

    ourDevices = new ArrayList<DeviceType>();

    if (ourDataPipe == null)                         // If necessary
      ourDataPipe = new DataPipe(0);                 //  instantiate the datapipe Use the default size

    ourASCReceiver = new bcastASCReceiver ();        // Instantiate all broadcast receivers
    ourAFReceiver  = new bcastAFReceiver  ();
    ourADFReceiver = new bcastADFReceiver ();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)      // In conjunction with onCreate
  {                                 // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  protected void onStart ()         // Happens right after onCreate
  { super.onStart();                                 // Make sure we call the super class
                // Register for broadcasts of ACTION_STATE_CHANGED on BluetoothAdapter state change
    IntentFilter btefilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    registerReceiver(ourASCReceiver, btefilter);
                // Register for broadcasts of ACTION_FOUND on BluetoothAdapter Device found
    IntentFilter fndfilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    registerReceiver(ourAFReceiver, fndfilter);
                // Register for broadcasts of ACTION_DISCOVERY_FINISHED on BluetoothAdapter state change
    IntentFilter adffilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
    registerReceiver(ourADFReceiver, adffilter);

    setSysStatMsg ("System Starting ...");
    if (ourPatientIDVal == null || ourPatientIDVal.length() < 2)  // Verify the validity of the patient ID
    { showIDAlert();                             // If no good make them enter it.
    }
    else
    { ourPatientID.setText(ourPatientIDVal);     // Set the patient ID into its text field
      ourPatientID.setVisibility(View.VISIBLE);  // Make sure its visible
      EnableBluetooth();    // Need to synchronously start  EnableBluetooth() here
    }
  }

  @Override
  protected void onResume ()            // When returning after being eclipsed by another app
  { super.onResume();
    setConStatMsg (storedConStatMsg);   // Redisplay all status
    setSysStatMsg (storedSysStatMsg);
  }

  @Override
  protected void onPause ()             // Happens when were eclipsed by another app
  { super.onPause();
  }

  @Override
  public void onBackPressed ()          // Happens when someone presses the Back Button
  {
     // Do nothing !
  }


  private void showIDAlert ()           // If patient data needs to entered by the user alert them to that
  { final AlertDialog.Builder adb = new AlertDialog.Builder (MainActivity.this);
    adb.setMessage(R.string.enter_patient_id);
    adb.setPositiveButton("Ok",new DialogInterface.OnClickListener()      // Just an OK button
    { @Override
      public void onClick(DialogInterface arg0, int arg1)  // When they hit the OK button
      { showEditor();                                      //  show the keyboard editor so they
      }                                                    //  can enter the patient ID data
    });
    AlertDialog patIDReminder = adb.create();              // Create the dialog
    patIDReminder.show();                                  //  and show it to them
  }

  private void showEditor ()           // Must set the device settings to use the ANDROID keyboard
  { ourPatientID.setText("");
    ourPatientID.setAutoLinkMask(0);                       // No auto http link suggestions
    ourPatientID.setImeOptions(0);                         // May not need this ???
    ourPatientID.setImeOptions(EditorInfo.IME_ACTION_DONE);       // Enter key is labeled Done and input type is basic text
    ourPatientID.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    ourPatientID.setVisibility(View.VISIBLE);              // Make sure its visable

    if (ourImm == null)                                    // If necessary get the systems Input Method Mgr
      ourImm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    ourImm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);     //  so we can force it to open

    ourPatientID.setOnEditorActionListener(new TextView.OnEditorActionListener()
    { @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
      { if (actionId == EditorInfo.IME_ACTION_DONE)
        { setSysStatMsg ("Patient ID Entered Continuing ...");
          if(ourPatientID.getText().length() > 2)          // If they didn't enter a valid length this where we end !
          { ourPatientIDVal = ourPatientID.getText().toString();   // Get what they entered
            ourPatientID.setVisibility(View.VISIBLE);              // Make sure its visible

            ourImm.hideSoftInputFromWindow(v.getWindowToken(),0);  // Hide the keyboard and save the preference data !
            appPrefs = getSharedPreferences(PREFS_NAME, 0);        // Access our preferences
            SharedPreferences.Editor editor = appPrefs.edit();     // Put preferences in edit mode
            editor.putString("ourPatientIDVal", ourPatientIDVal);  // Key value pair for PatientID Val
            editor.commit();                                       // Commit the edits!
            setSysStatMsg ("Patient ID Stored ...");
            EnableBluetooth();        // Enable the Bluetooth adapter and start the device scan
          }
        }
        return true;
      }
    });
  }

  public boolean EnableBluetooth ()   // Enable the devices bluetooth adapter
  { try
    { setConStatMsg("Bluetooth Enable Requested");
      BTAdapter = BluetoothAdapter.getDefaultAdapter();
      if (!BTAdapter.isEnabled())
      { Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBTIntent, 0);
      }
      else
      { DeviceScan ();
      }
      return true;
    }
    catch (Exception e)
    { setSysStatMsg("Exception Enabling Bluetooth ! "+e.getMessage());
    }
    return false;
  }

  public boolean DeviceScan ()
  { if (this.ourDevice != null)
      return (true);   // If we already have a valid device don't scan again
    try
    { setConStatMsg ("Scan Requested");
      if (BTAdapter.isEnabled())            // Make us discoverable by the other device
      { Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        BTAdapter.startDiscovery();           // Start discovery process
        setSysStatMsg ("Bluetooth Enabled, Discoverable & Scanning...");
      }
      else
      { setSysStatMsg ("Bluetooth Not Enabled !");
        return false;
      }
    }
    catch (final Exception ioe)
    { setSysStatMsg("Scan Exception ! "+ioe.getMessage());
      return false;
    }
    return true;
  }
                              // Handle the results of the device data reading process
  private Handler devResultHndlr = new Handler()
  { @Override
    public void handleMessage(Message msg)
    { if (msg.what == DeviceTalk.DevTlkResMsgTyp.MSG_DTBCREAD.getMsgKey())
      { Integer bytecnt = msg.getData().getInt("bytecount");
        setSysStatMsg(bytecnt+ " Bytes Received");
        ourProgBar.setProgress(ourDataPipe.getPercentFull()); // Indicate how full the data pipe is
      }
      else if (msg.what == DeviceTalk.DevTlkResMsgTyp.MSG_DTEXCEPTION.getMsgKey())
      { ourConn.CloseConnection();
        setConStatMsg("Socket Is Closed");
      }
      else if (msg.what == DeviceTalk.DevTlkResMsgTyp.MSG_DTDEVBATDATA.getMsgKey())
      { Integer devbatlvl = msg.getData().getInt("devbatlvl");
        ourDevBatLvl.setText (devbatlvl+" %");
      }

      // else unknown message type
    }
  };
                               // Handle the results of the device connection process
  private Handler conResultHndlr = new Handler()
  { @Override
    public void handleMessage(Message msg)
    { if (msg.what == 0)
      { setConStatMsg("Socket Is Connected");
        BluetoothSocket ourSocket = ourConn.getConnectedSocket();
        DeviceTalk devTlkObj = new DeviceTalk(ourSocket, devResultHndlr, ourDataPipe, ourSysStatus); // Spin read thread
        devTlkObj.start();       // Start device data reading thread
      }
      // else if 'what' is non 0 handle the connection exception
    }
  };

  public boolean runConnectProcess()
  { if (this.ourDevice != null)  // OurDevice must be ! null and valid
    { if (ourConn == null)
        ourConn = new ConnectDevice(this.ourDevice, ourSysStatus, this.conResultHndlr);
      setSysStatMsg ("Connecting to "+this.ourDevice.getDeviceName());
      ourConn.start();           // Start connection thread

      // If we have a valid device connection spin off the host connection thread in a similar way
      ConnectHost hconn = new  ConnectHost(ourHName, hostConnResHndlr, ourDataPipe);
      if (hconn == null)
      { setSysStatMsg ("Unable to establish Host Connection to "+ourHName);
        return false;
      }
      ourHostConnection = hconn;

    }
    else
      setSysStatMsg ("No Devices Found");
    return true;
  }

  private Handler hostConnResHndlr = new Handler()
  { @Override
    public void handleMessage(Message msg)
    { if (msg.what == 0)
      { setConStatMsg("Host Socket Is Connected");

        // ITalkHostService ourHostSock = ourHostConn.getConnectedSocket();
        // DeviceTalk devTlkObj = new DeviceTalk(ourSocket, devResultHndlr, ourDataPipe, ourSysStatus); // Spin read thread
        // devTlkObj.start();       // Start device data reading thread
      }
            // else if 'what' is non 0 handle the connection exception
    }
  };



  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  { // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    switch (id)
    { case R.id.abm_action_settings:
           break;
      case R.id.abm_action_exit:
           System.exit(0);
           break;
    }
    return super.onOptionsItemSelected(item);
  }

  public static void setSysStatMsg (String msg)
  { if (ourSysStatus != null)
    { ourSysStatus.setText ("Status: "+msg);
      ourSysStatus.setVisibility(View.VISIBLE);
      storedSysStatMsg = msg;
    }
  }

  public static void setConStatMsg (String msg)
  { if (ourConStatus != null)
    { ourConStatus.setText (msg);
      ourConStatus.setVisibility(View.VISIBLE);
      storedConStatMsg = msg;
    }
  }

  @Override
  protected void onStop()
  { super.onStop();
    unregisterReceiver(ourASCReceiver);    // Unregister all broadcast listeners
    unregisterReceiver(ourAFReceiver);
    unregisterReceiver(ourADFReceiver);
  }

  @Override
  public void onDestroy()                  // Upon destruction
  { super.onDestroy();
  }

  // INNER CLASSES !

  public class bcastAFReceiver extends BroadcastReceiver
  { @Override
    public void onReceive (Context rcontext, Intent rintent)
    { String action = rintent.getAction();
      if (action.equals(BluetoothDevice.ACTION_FOUND))   // Scan finished so collect the data
      { BluetoothDevice device = rintent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        DeviceType odev = new DeviceType (device);       // Instantiate our device type
        ourDevice = odev;                                // Use the first device found
        ourDevices.add (odev);                           // Add the device data to it
        setSysStatMsg("Scanning ... "+ourDevices.size()+" Found");
      }
    }
  }
                                // Build a broadcast receiver for the ACTION_DISCOVERY_FINISHED message
  public class bcastADFReceiver extends BroadcastReceiver
  { @Override
    public void onReceive (Context rcontext, Intent rintent)
    { String action = rintent.getAction();
      if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))   // Scan finished so collect the data
      { setConStatMsg("Scan Finished");                         // Tell them we're finished
        BTAdapter.cancelDiscovery();                            // Cancel discovery mode !!!!!!! Make sure we do this !!!!!
        runConnectProcess();          // Run the connection process
      }
    }
  }

  public class bcastASCReceiver extends BroadcastReceiver
  { @Override
    public void onReceive(Context context, Intent intent)
    { String action = intent.getAction();

      if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
      { final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
        switch (state)
        { case BluetoothAdapter.STATE_OFF:
               setConStatMsg("Bluetooth Disabled !");
               break;
          case BluetoothAdapter.STATE_TURNING_OFF:
               setSysStatMsg("Disabling Bluetooth ...");
               break;
          case BluetoothAdapter.STATE_ON:
               setConStatMsg("Bluetooth Enabled");

               DeviceScan();                        // Call the scanner
               break;
          case BluetoothAdapter.STATE_TURNING_ON:
               setSysStatMsg("Enabling Bluetooth ...");
               break;
        }
      }
    }
  };
}      // The Bitter End ...
