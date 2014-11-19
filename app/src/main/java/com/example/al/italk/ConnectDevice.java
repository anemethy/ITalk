package com.example.al.italk;

/***
 * ITalk App for Android Version 0.5 Beta
 * Author: Albert N. Nemethy
 * Last UpDate: 11-10-2014
 * Copyright (c) Autonomous Engineering of VT
 * All Rights Reserved
 *
 * ConnectDevice.java
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.widget.TextView;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class ConnectDevice extends Thread implements Runnable
{ public static enum ConnectAs {None, Client, Server}      // Make available to all classes
  public static enum LogStat {Disabled, Enabled, Paired, Connected, closed}   // Make available to all classes

  private ConnectAs ourConnectionMethod = ConnectAs.Client;     // Initialize our connection method
  private final String ourName = "ITalk";

  private LogStat ourLogicalStatus = LogStat.Disabled;     // Start off disabled
  private int ourProgressCode = 0;
  private TextView ourStatusTF = null;
  private BluetoothAdapter ourBTAdapter = BluetoothAdapter.getDefaultAdapter();
  private static int ourDevCntr = 0;
  private BluetoothServerSocket ourServerSocket;           // This probably will not be used
  private BluetoothSocket ourSocket = null;
  private DeviceType ourDevice = null;
  private Handler ourResHandler = null;

  private UUID ourUUID = null;
  private ArrayList<DeviceType> ourDevices = new ArrayList();

  ConnectDevice (DeviceType ldev, TextView lv, Handler hdlr)     // Assign our connection methodology 11/06/2014 only client is supported
  { this.ourDevice = ldev;                         // Assign our device
    // this.ourUUID = UUID.randomUUID();
    this.ourUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    this.ourStatusTF = lv;
    this.ourResHandler = hdlr;
  }

  @Override
  public void run()
  { EnableBT();                             // Enable the adapter if it is not already
    if ((getLogicalStatus() == ConnectDevice.LogStat.Enabled) ||
       (getLogicalStatus() == ConnectDevice.LogStat.Paired))
    { ConnectMe (this.ourDevice);
    }
  }

  public boolean EnableBT ()
  { try               // ourUUID is the app's UUID string, also used by the server code
    { if (this.ourConnectionMethod == ConnectAs.Client)
      { if (!ourBTAdapter.isEnabled())           // Is adapter enabled ???
        { ourBTAdapter.enable();                 // This doesn't ask for permission
        }
                                                 // Probably should set a timer ...
        while (!ourBTAdapter.isEnabled())        // Spin here until we're enabled
          ;
        ourLogicalStatus = LogStat.Enabled;                                         // Are any devices paired  getBondedDevices ()
        Set<BluetoothDevice> pairedDevices = ourBTAdapter.getBondedDevices();
        if (pairedDevices.size() > 0)
        { showThreadMsg("Paired Devs Found");
          ourLogicalStatus = LogStat.Paired;
        }
        else if (this.ourDevice != null)
        { showThreadMsg("Using !"+ this.ourDevice.getSysDevice().getName());
        }
        else   // if not scan for devices
        { showThreadMsg("Must Scan ...");
        }
      }
    }
    catch (final Exception e)
    { showThreadMsg("CD Enable Exception ! "+e.getMessage());
      ourResHandler.sendEmptyMessage(1);
      return false;
    }
    return true;
  }

  public boolean ConnectMe (DeviceType ldev)
  { try
    { showThreadMsg("Connecting To: " + ldev.getSysDevice().getName());
           //this.ourUUID = this.ourDevice.getSysDevice().getUuids()[0].getUuid();
      ourBTAdapter.cancelDiscovery();    // Make sure this is done first

      try
      { BluetoothDevice remDevice = ourBTAdapter.getRemoteDevice(ldev.getMACAddress());
        Method m = remDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
        this.ourSocket = (BluetoothSocket) m.invoke(remDevice, 1);
      }
      catch (NoSuchMethodException e) { showThreadMsg("No Such Method !"); }
      catch (IllegalArgumentException e) { showThreadMsg("Illegal Argument !"); }
      catch (IllegalAccessException e) { showThreadMsg("Illegal Access !"); }
      catch (InvocationTargetException e) { showThreadMsg("Invocation Target Err !"); }

      showThreadMsg("Pairing With " + this.ourSocket.getRemoteDevice().getName());
      this.ourSocket.connect();

      ourLogicalStatus = LogStat.Connected;
      ourResHandler.sendEmptyMessage(0);       // Send our results to the UI
    }
    catch (IOException e)
    { showThreadMsg("CD IO Exception 1: " + e.getMessage());   // !!! Must Inform User !!!
      try { this.ourSocket.close (); }
      catch (Exception e2) { }
      ourResHandler.sendEmptyMessage(1);
      return false;
    }
    return true;
  }

  public void CloseConnection ()
  { try
    { ourSocket.close();
      ourLogicalStatus = LogStat.closed;
    }
    catch (IOException ioe)
    { showThreadMsg("Exception Closing Connection: " + ioe.getMessage());   // !!! Must Inform User !!!
    }
  }

  private void showThreadMsg (final String msg)
  { ourStatusTF.post (new Runnable()           // Must run this on the UI thread
    { public void run() { ourStatusTF.setText(msg); }
    });
    return;
  }

  public LogStat getLogicalStatus ()
  { return (this.ourLogicalStatus);
  }

  public BluetoothSocket getConnectedSocket ()    // Provide access to the connected socket
  { if (ourLogicalStatus == LogStat.Connected)    // Make sure we're connected
      return this.ourSocket;
    else
      return (null);
  }
}




