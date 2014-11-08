package com.example.al.italk;

/**
 * Created by Al Nemethy on 11/5/14.
*/

import android.bluetooth.BluetoothDevice;

public class DeviceType
{ private String ourDeviceName;    // Our name
  private String ourMACAddress;    // Our MAC address if we have one
  private boolean ourConnFlg;      // Our connection flag; true = connected
  private boolean ourPairedState;  // if bluetooth connection, are we paired or not. true if yes
  private BluetoothDevice ourSysDevice;  // The device data returned by system of type BluetoothDevice
    //  this is necessary when creating a connection so we must save it

  public DeviceType ()    // Object definition for a device with a potential connection
  { this.ourDeviceName = null;           // Initialize all values
    this.ourMACAddress = null;
    this.ourConnFlg = false;
    this.ourPairedState = false;
    this.ourSysDevice = null;
  }

  public DeviceType (String devicename)
  { this.ourDeviceName = devicename;     // Assign our name
    this.ourMACAddress = null;           // Initialize all other values
    this.ourConnFlg = false;
    this.ourPairedState = false;
    this.ourSysDevice = null;
  }

  public DeviceType (String devicename, final String MACaddress)
  { this.ourDeviceName = devicename;     // Assign our name
    this.ourMACAddress = MACaddress;     // Assign our MAC address
    this.ourConnFlg = false;             // Initialize all other values
    this.ourPairedState = false;
    this.ourSysDevice = null;
  }

  public DeviceType (BluetoothDevice ldev)
  { if (ldev != null)
    { this.ourDeviceName = ldev.getName();     // Assign our name
      this.ourMACAddress = ldev.getAddress();  // Assign our MAC address
      this.ourConnFlg = false;             // Initialize all other values
      this.ourPairedState = false;
      this.ourSysDevice = ldev;
    }
  }

  // Define all accessor methods
  public String getDeviceName ()  { return (this.ourDeviceName);  }
  public String getMACAddress ()  { return (this.ourMACAddress);  }
  public boolean getConnFlg ()  { return (this.ourConnFlg);  }
  public boolean getPairedState ()  { return (this.ourPairedState);  }
  public BluetoothDevice getSysDevice ()  { return (this.ourSysDevice);  }

  public void setDeviceName (String dname)
  { this.ourDeviceName = dname;
    return;
  }

  // Define all getter methods
  public void setMACAddress (String MACadr)
  { this.ourMACAddress = MACadr;
    return;
  }

  public boolean setConnFlg (boolean cf)
  { this.ourConnFlg = cf;
    return true;
  }

  public boolean setPairedState (boolean ps)
  { this.ourPairedState = ps;
    return true;
  }

  public boolean setSysDevice (BluetoothDevice sdev)
  { this.ourSysDevice = sdev;
    return true;
  }

}

