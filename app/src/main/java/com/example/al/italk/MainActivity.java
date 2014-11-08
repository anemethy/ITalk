package com.example.al.italk;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity
{ private static EditText ourPatientID = null;
  private static String ourPatientIDVal = null;      // THIS NEEDS TO PERSIST !
  private static TextView ourConStatus = null;
  private static boolean ourContinueFlag = false;
  private static boolean bcastAFReceiverIsRegistered = false;
  private static boolean bcastADFReceiverIsRegistered = false;

  private boolean ourScanInvoked = false;
  private Context ourContext = null;
  private BluetoothAdapter BTAdapter = null;
  private ConnectDevice ourConn = null;             // Our Connection
  private DeviceType ourDevice = null;
  private ArrayList<DeviceType> ourDevices = new ArrayList();

  public static final String PREFS_NAME = "ITalkPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    { super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      ourContext = this.getApplicationContext();

      if (ourPatientIDVal == null)    // Restore preferences if necessary
      { SharedPreferences appPrefs = getSharedPreferences(PREFS_NAME, 0);
        ourPatientIDVal = appPrefs.getString("ourPatientIDVal", null);
      }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    { // Inflate the menu; this adds items to the action bar if it is present.
      getMenuInflater().inflate(R.menu.menu_main, menu);

      if (ourConStatus == null)
      { ourConStatus = ((TextView)findViewById(R.id.ds_tf)); }

      ourPatientID = ((EditText)findViewById(R.id.pd_tf));
      if (ourPatientIDVal == null || ourPatientIDVal.length() < 2)
      { ourPatientID.setText("");
        ourPatientID.setInputType(EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        ourPatientID.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN |
                                   EditorInfo.IME_FLAG_NO_EXTRACT_UI |
                                   EditorInfo.IME_FLAG_NO_ACCESSORY_ACTION |
                                   EditorInfo.IME_ACTION_DONE);

        ourPatientID.setOnEditorActionListener(new TextView.OnEditorActionListener()
        { @Override
          public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
          { if (actionId == EditorInfo.IME_ACTION_DONE)
            { hideKeyboard();                 // Hide the keyboard
              if(ourPatientID.getText().length() > 2)
              { ourContinueFlag = true;       // Set a continue connection process flag
                ourPatientIDVal = ourPatientID.getText().toString();
                ourPatientID.setVisibility(View.VISIBLE);
              }
            }
            return true;
          }
        });

        AlertDialog.Builder adb = new AlertDialog.Builder (this);
        adb.setMessage(R.string.enter_patient_id);
        adb.setPositiveButton("Ok",new DialogInterface.OnClickListener()
        { @Override
          public void onClick(DialogInterface arg0, int arg1)
          {  // Do Nothing
          }
        });
        AlertDialog patIDReminder = adb.create();
        patIDReminder.show();
      }
      else
      { ourContinueFlag = true;
        ourPatientID.setText(ourPatientIDVal);
        ourPatientID.setVisibility(View.VISIBLE);
      }

      if (ourContinueFlag)
      { ourConStatus.setText("Patient ID Done Continuing ...");
        if (this.ourDevice == null && !this.ourScanInvoked) // Must scan for devices but only once !
        { ourContinueFlag = false;  // Don't continue until we have a device to connect to
          this.ourScanInvoked = true;
          DeviceScan();             // Start scan
        }
      }
      return true;
    }

    public boolean runConnectProcess()
    {
      if ((ourContinueFlag) && this.ourDevice != null)  // OurDevice must be ! null and valid
      { if (ourConn == null)
          ourConn = new ConnectDevice(this.ourDevice, (TextView) findViewById(R.id.ds_tf));
        ourConStatus.setText("Connecting to "+this.ourDevice.getDeviceName());
        ourConn.start();          // Start connection thread
      }
      return true;
    }

    public boolean DeviceScan ()
    { try
      { ourConStatus.setText("Scanning ...");

        // Register for broadcasts of ACTION_FOUND on BluetoothAdapter state change
        IntentFilter fndfilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bcastAFReceiver, fndfilter);
        bcastAFReceiverIsRegistered = true;

        // Register for broadcasts of ACTION_DISCOVERY_FINISHED on BluetoothAdapter state change
        IntentFilter adffilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bcastADFReceiver, adffilter);
        bcastADFReceiverIsRegistered = true;

        BTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!BTAdapter.isEnabled())
        { BTAdapter.enable(); }

        BTAdapter.startDiscovery();           // Start discovery process
      }
      catch (final Exception ioe)
      { ourConStatus.setText("Scan Exception ! "+ioe.getMessage());
        return false;
      }
      return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {   // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.abm_action_settings)
        { return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void hideKeyboard()
    { InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
      // check if no view has focus:
      View view = this.getCurrentFocus();
      if (view != null)
      { inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
      }
    }

    // Todo Add Exception handling
    private final BroadcastReceiver bcastAFReceiver = new BroadcastReceiver()
    { @Override
      public void onReceive(Context context, Intent intent)
      { final String action = intent.getAction();          // Get the action from the intent so we can verify it

        if (action.equals(BluetoothDevice.ACTION_FOUND))   // Verify action then get the device data
        { BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

          DeviceType odev = new DeviceType (device);       // Instantiate our device type
          ourDevice = odev;                                // Use the first device found
          ourDevices.add (odev);                           // Add the device data to it
          ourConStatus.setText("Scanning ... "+ourDevices.size()+"Found");
        }
      }
    };

    // Todo Add Exception handling
    private final BroadcastReceiver bcastADFReceiver = new BroadcastReceiver()
    { @Override
      public void onReceive(Context context, Intent intent)
      { final String action = intent.getAction();          // Get the action from the intent

        if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))   // Scan finished so collect the data
        { ourConStatus.setText("Scan Finished");                         // Tell them we're finished
          BTAdapter.cancelDiscovery();                                   // Cancel discovery mode !!!!!!! Make sure we do this !!!!!
          ourContinueFlag = true;       // Allow the process to continue
          runConnectProcess();          // Run the connection process
        }
      }
    };

    @Override
    protected void onStop()
    { super.onStop();
      // We need an Editor object to make preference changes.
      // All objects are from android.context.Context
      SharedPreferences appPrefs = getSharedPreferences(PREFS_NAME, 0);
      SharedPreferences.Editor editor = appPrefs.edit();
      editor.putString("ourPatientIDVal", ourPatientIDVal);
      editor.commit();          // Commit the edits!
    }

    @Override
    public void onDestroy()     // Upon destruction
    { super.onDestroy();
      //  this.unregisterReceiver(bcastASCReceiver);    // Unregister all broadcast listeners
      if (bcastAFReceiverIsRegistered)
        unregisterReceiver(bcastAFReceiver);
      if (bcastADFReceiverIsRegistered)
        unregisterReceiver(bcastADFReceiver);
    }
}
