package com.example.al.italk;

/***
 * ITalk App for Android Version 0.5 Beta
 * Author: Albert N. Nemethy
 * Last UpDate: 11-11-2014
 * Copyright (c) Autonomous Engineering of VT
 * All Rights Reserved
 *
 * ConnectHost.java
 */

import android.os.Handler;
import java.lang.Enum;

public class ConnectHost extends Thread implements Runnable
{ public static enum HostConResMsgTyp         // Message type definitions
  { MSG_CHWRITEDATA(0xEFFF),
    MSG_CHSSTATUS(0xEFFE),
    MSG_CHEXCEPTION(0xEFFD);

    private final int msgKey;
          // For an implementation example see MainActivity.devResultHndlr()
    HostConResMsgTyp(int msgKey) { this.msgKey = msgKey; }
    public int getMsgKey () { return this.msgKey; }
  }

  private String ourHostName = null;
  private Handler ourResHandler = null;
  private DataPipe ourDataPipe = null;

  ConnectHost (String hostname, Handler hdlr, DataPipe ldp)
  { ourHostName = hostname;
    ourResHandler = hdlr;
    ourDataPipe = ldp;
  }

  @Override
  public void run ()
  { //int a = 0;
    //if (a == HostConResMsgTyp.MSG_CHSSTATUS.msgKey) {}
    //if (a == HostConResMsgTyp.MSG_CHEXCEPTION.getMsgKey()) {}  // Use this in the UI

  }

  public void CloseHostConnection ()
  {

  }

}
