package com.example.al.italk;

/***
 * ITalk App for Android Version 0.5 Beta
 * Author: Albert N. Nemethy
 * Last UpDate: 11-10-2014
 * Copyright (c) Autonomous Engineering of VT
 * All Rights Reserved
 *
 * DeviceTalk.java
 */

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;

public class DeviceTalk extends Thread implements Runnable
{ public static enum DevTlkResMsgTyp          // Message type definitions
  { MSG_DTBCREAD (0xFFFF),
    MSG_DTSSTATUS (0xFFFE),
    MSG_DTEXCEPTION (0xFFFD),
    MSG_DTDEVBATDATA (0xFFFC);

    public final int msgKey;

    DevTlkResMsgTyp(int msgKey) { this.msgKey = msgKey; }
    public int getMsgKey () { return this.msgKey; }
  }

  private final BluetoothSocket ourSocket;
  private final InputStream ourInStream;
  private final OutputStream ourOutStream;
  private TextView ourStatusTF = null;
  private byte[] dataBuffer = null;              // Temporary data repository for device data
  private Handler ourResHandler = null;
  private DataPipe ourDataPipe = null;

  public final static int PACKET_DEBUG = 1;
  public final static int PACKET_QUAT = 2;
  public final static int PACKET_DATA = 3;
  public final static int PACKET_OTHER_SENSORS = 4;
  public final static int PACKET_ROT_MAT = 5;
  public final static int PACKET_EULER = 6;
  public final static int PACKET_HEADING = 7;

  public final static int PACKET_DATA_ACCEL = 0;
  public final static int PACKET_DATA_GYRO = 1;
  public final static int PACKET_DATA_COMPASS = 2;
  public final static int PACKET_DATA_QUAT = 3;
  public final static int PACKET_DATA_EULER = 4;
  public final static int PACKET_DATA_ROT = 5;
  public final static int PACKET_DATA_HEADING = 6;
  public final static int PACKET_DATA_OTHER_SENSORS = 7;
  public final static int PACKET_DATA_LOG = 8;

  Semaphore mutexLock = new Semaphore(1);

  public DeviceTalk(BluetoothSocket socket, Handler hdlr, DataPipe dp, TextView lv)
  { ourSocket = socket;
    ourResHandler = hdlr;
    ourDataPipe = dp;
    ourStatusTF = lv;
    InputStream tmpIn = null;
    OutputStream tmpOut = null;

    try                                          // Get the input and output streams, using
    { // dataBuffer = new byte[1024];
      tmpIn = socket.getInputStream();           //  temp objects because member streams are final
      tmpOut = socket.getOutputStream();
    }
    catch (IOException ioe)
    { Log.e("DTX", "IO Exception 1", ioe);
      showThreadMsg("DT IO Exception 1: " + ioe.getMessage());   // Must Inform User !

      try { this.ourSocket.close (); }
      catch (Exception e2) { }
    }
    catch (Exception e)                          // Catch potential memory exception
    { Log.e("DTX", "IO Exception 2", e);
      showThreadMsg("DT Exception 2: " + e.getMessage());        // Must Inform User !
    }
    ourInStream = tmpIn;
    ourOutStream = tmpOut;
  }

  public void run()
  {
    showThreadMsg("Reading Device Data ...");
    if (!synRead(ourInStream))
    { Log.e("DTX", "Unable to Syncronize Device Data");
      showThreadMsg("Error Reading Device Data");
      return;
    }
    while (true)      // Keep listening to the InputStream until an
    {                 //  exception occurs or the host data pipe is full
      if (ourDataPipe.getSize() < ourDataPipe.getMaxSize())
      { dataBuffer = new byte[23];      // All packets should be 23 bytes long
                      // Read from the InputStream then append to the data pipe if applicable
        if (!runRead(ourInStream, dataBuffer, dataBuffer.length))
        { Log.e("DTX", "Error Reading Device Data");
          showThreadMsg("Error Reading Device Data");
          return;
        }
      }
    }
  }

  // Call this to send data to the remote device
  public void devWrite(byte[] binData)
  { try
    { ourOutStream.write(binData);
    }
    catch (IOException ioe)
    { Log.e("DTX", "IO Exception 5", ioe);
      showThreadMsg("DT IO Exception 5: " + ioe.getMessage());   // Must Inform User !
    }
  }
                     // This can be used to send all messages to the UI
  private Message bundleMsg (String msgid, DevTlkResMsgTyp mt, Object msgobj)
  { Bundle bndl = new Bundle ();
    if (mt.getMsgKey() == DevTlkResMsgTyp.MSG_DTBCREAD.getMsgKey())
      bndl.putInt(msgid, (Integer)msgobj);
    else if (mt.getMsgKey() == DevTlkResMsgTyp.MSG_DTSSTATUS.getMsgKey())
      bndl.putString(msgid, (String)msgobj);
    else if (mt.getMsgKey() == DevTlkResMsgTyp.MSG_DTEXCEPTION.getMsgKey())
      bndl.putString(msgid, (String)msgobj);
    else if (mt.getMsgKey() == DevTlkResMsgTyp.MSG_DTDEVBATDATA.getMsgKey())
      bndl.putString(msgid, (String)msgobj);
    Message dmsgobj = ourResHandler.obtainMessage(mt.getMsgKey());
    dmsgobj.setData(bndl);
    return dmsgobj;
  }
                     // Cheap way to send a status message to the UI can be replaced with the above
  private void showThreadMsg (final String msg)
  { ourStatusTF.post (new Runnable()           // Must run this on the UI thread
    { public void run() { ourStatusTF.setText(msg); }
    });
    return;
  }

  /** Synchronizes the first read operation between the wearable device and the
   *  application to ensure the synchronization for rest of the packets.
   *
   * @param inStream input stream to read from
   * @return true if successful false otherwise
   */
  public boolean synRead(InputStream inStream)
  { byte[] pData = new byte[23];
    int curRead = 0;
    Log.i("DebugApp", "sync read reached");
    pData[0] = 0;
    while (pData[0] != '$')
    { Log.i("DebugApp", " NOT $$$$ " + pData[0]);
      try
      { curRead = inStream.read(pData, 0, 1);
      }
      catch (IOException e)
      { e.printStackTrace();
      }
      if (pData[0] != '$')
        Log.i("DebugApp", "Wrong Ping");
      if (curRead < 0)
      { return false;
      }
    }
    int m = 1;
    int shiftIndex = m;
    int pLength = 23;
    while (m != pLength)
    { try
      { curRead = inStream.read(pData, shiftIndex, pLength - shiftIndex);
      }
      catch (IOException e)
      { e.printStackTrace();
      }
      if (curRead < 0)
      { return false;
      }
      m += curRead;
     shiftIndex = m;
    }
    return true;
  }

  /*** Reads data from input stream and stores them to a buffer
   * @param inStream input stream to read from
   * @param pData    buffer where the read data will be stored
   * @param pLength  maximum bytes to store into pData
   * @return true if successful false otherwise
   */
  public boolean readData(InputStream inStream, byte[] pData, int pLength)
  { try
    { int m = 0;
      int shiftIndex = m;
      while (m != pLength)
      { int curRead = inStream.read(pData, shiftIndex, pLength - shiftIndex);
        if (curRead < 0)
        { return false;
        }
        m += curRead;
        shiftIndex = m;
      }
      return true;
    }
    catch (IOException e)
    { // running = false;
      // e.printStackTrace();
      return false;
    }
  }

  // This method is called continuously and updates the Sensors values by reading the
  // Bluetooth input stream from wearable SDK. There are optional filters can be applied.
  protected boolean runRead (InputStream is, byte[] dbuf, int dbsize)
  { boolean returntrf = false;
    try
    { mutexLock.acquire();
      if (readData(is, dbuf, dbsize))
      { if (dbuf[1] == PACKET_DATA)
        { switch (dbuf[2])
          { case PACKET_DATA_QUAT:
                 onPacketDataQuat (dbuf);           // Are we using this ???
                 break;
            case PACKET_DATA_OTHER_SENSORS:
                 onPacketDataOtherSensors(dbuf);    // How bout this ???
                 break;
            case PACKET_DATA_ACCEL:
                 onPacketDataAccel(dbuf);           // Any of these ???
                 break;
            case PACKET_DATA_COMPASS:
                 onPacketDataCompass(dbuf);
                 break;
            case PACKET_DATA_EULER:
                 onPacketDataEuler(dbuf);
                 break;
            case PACKET_DATA_GYRO:
                 onPacketDataGyro(dbuf);
                 break;
            case PACKET_DATA_HEADING:
                 onPacketDataHeading(dbuf);
                 break;
            case PACKET_DATA_ROT:
                 onPacketDataRotMat(dbuf);
                 break;
          }                         // All Of the above data type records get sent to the host
          ourDataPipe.add (dbuf);   // Append to data pipe !
          ourResHandler.sendMessage(bundleMsg ("bytecount", DevTlkResMsgTyp.MSG_DTBCREAD, dbuf.length));
        }
        else if (dbuf[1] == PACKET_QUAT)
        { if (dbuf[2] == PACKET_DATA_QUAT)
            onPacketQuat(dbuf);
        }
        else if (dbuf[1] == PACKET_OTHER_SENSORS)
        { if (dbuf[2] == PACKET_DATA_OTHER_SENSORS)
            onPacketOtherSensors(dbuf);
        }
        else if (dbuf[1] == PACKET_ROT_MAT)
        { if (dbuf[2] == PACKET_DATA_ROT)
            onPacketRotMat(dbuf);
        }
        else if (dbuf[1] == PACKET_EULER)
        { if (dbuf[2] == PACKET_DATA_EULER)
            onPacketEuler(dbuf);
        }
        else if (dbuf[1] == PACKET_HEADING)
        { if (dbuf[2] == PACKET_DATA_HEADING)
          { onPacketHeading(dbuf);
            ourDataPipe.add (dbuf);   // Append to data pipe !
            ourResHandler.sendMessage(bundleMsg ("bytecount", DevTlkResMsgTyp.MSG_DTBCREAD, dbuf.length));
          }
        }
        else if (dbuf[1] == PACKET_DEBUG)
        { onPacketDebugLog(dbuf);            // This has the battery life in it ... I Think
        }
      }
      returntrf = true;
    }
    catch (InterruptedException e)
    { Log.i("BluetoothDataReader", "intterupted exception");
      showThreadMsg("DT IO Exception 3: "+e.getMessage());   // Must Inform User !
      ourResHandler.sendEmptyMessage(DevTlkResMsgTyp.MSG_DTEXCEPTION.getMsgKey());
      returntrf = false;
    }
    catch (Exception ioe)
    { Log.e("DTX", "IO Exception 4", ioe);
      showThreadMsg("DT IO Exception 4: " + ioe.getMessage());   // Must Inform User !
      try { this.ourSocket.close (); }
      catch (Exception e2) {  }
      ourResHandler.sendEmptyMessage(DevTlkResMsgTyp.MSG_DTEXCEPTION.getMsgKey());
      returntrf = false;
    }
    finally
    { mutexLock.release();
    }
    return (returntrf);
  }

  /*** Combine two individual bytes into a float without overflow check.
   * @param d1 First Byte
   * @param d2 Second Byte
   * @return Combined float value.
   */
  float twoBytes_nocheck(byte d1, byte d2)
  { // """ unmarshal two bytes into int16 """
    float d = ord(d1) * 256 + ord(d2);
    return d;
  }

  /*** Combine two individual bytes into a float with overflow check.
   * @param d1 First Byte
   * @param d2 Second Byte
   * @return Combined float value.
   */
  float twoBytes(byte d1, byte d2)
  { // """ unmarshal two bytes into int16 """
    float d = ord(d1) * 256 + ord(d2);
    if (d > 32767)
      d -= 65536;
    return d;
  }

  int ord(byte d)
  { return d & 0xff;
  }

  /*** Combine four individual bytes into a float with overflow check.
   * @param d1 First Byte
   * @param d2 Second Byte
   * @param d3 Third Byte
   * @param d4 Fourth Byte
   * @return Combined Float value.
   */
   // For 32-bit signed integers.
  float four_bytes(byte d1, byte d2, byte d3, byte d4)
  { float d = ord(d1) * (1 << 24) + ord(d2) * (1 << 16) + ord(d3) * (1 << 8) + ord(d4);
    if (d > 2147483648l)
      d -= 4294967296l;
    return d;
  }

  private void onPacketDataHeading(byte[] l) { }
  private void onPacketHeading(byte[] l) { }
  private void onPacketDataQuat(byte[] buffer) { }
  private void onPacketQuat(byte[] buffer) { }
  private void onPacketOtherSensors(byte[] buffer) { }
  private void onPacketDataOtherSensors(byte[] buffer) { }
  private void onPacketDataAccel(byte[] l) { }
  private void onPacketDataGyro(byte[] l) { }
  private void onPacketDataCompass(byte[] l) { }
  private void onPacketDataEuler(byte[] l) { }
  private void onPacketEuler(byte[] l) { }
  private void onPacketDataRotMat(byte[] l) { }
  private void onPacketRotMat(byte[] l) { }

  private boolean onPacketDebugLog(byte[] buffer)
  { StringBuilder strBuilder = new StringBuilder(23);
    for (int j = 2; j < 22; j++)
    { if (buffer[j] == '\r' && buffer[j + 1] == '\n')
        break;
      strBuilder.append((char) buffer[j]);
    }
    String msgStr = strBuilder.toString();
    if(msgStr.contains("Battery"))
    { ourResHandler.sendMessage(bundleMsg("devbatlvl",DevTlkResMsgTyp.MSG_DTDEVBATDATA, msgStr));
      return (true);
    }
    return (false);
  }

}     // End of class


/*********** CASDK Utility Code ***********
 package com.invensense.casdk.client;

 import java.io.IOException;
 import java.io.InputStream;
 import java.util.LinkedList;
 import java.util.concurrent.Semaphore;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;

 import android.app.ActionBar.Tab;
 import android.os.AsyncTask;
 import android.os.Message;
 import android.util.Log;

 import com.invensense.cubequat.Global;

 **
 * This class is responsible for handling communication between the wearable
 * sensor and the Android application. Data is read here and stored in a Global
 * variable for other classes to access.
 *
 * @author Invensense
 *
 *
public final class BluetoothDataReader extends AsyncTask<InputStream, Integer, Integer> {
    public final static int PACKET_DEBUG = 1;
    public final static int PACKET_QUAT = 2;
    public final static int PACKET_DATA = 3;
    public final static int PACKET_OTHER_SENSORS = 4;
    public final static int PACKET_ROT_MAT = 5;
    public final static int PACKET_EULER = 6;
    public final static int PACKET_HEADING = 7;

    public final static int PACKET_DATA_ACCEL = 0;
    public final static int PACKET_DATA_GYRO = 1;
    public final static int PACKET_DATA_COMPASS = 2;
    public final static int PACKET_DATA_QUAT = 3;
    public final static int PACKET_DATA_EULER = 4;
    public final static int PACKET_DATA_ROT = 5;
    public final static int PACKET_DATA_HEADING = 6;
    public final static int PACKET_DATA_OTHER_SENSORS = 7;
    public final static int PACKET_DATA_LOG = 8;

    Semaphore mutexLock = new Semaphore(1);

    // true if the bluetooth connection is on
    public boolean running = false;

    **
     * Synchronizes the first read operation between the wearable device and the
     * application to ensure the synchronization for rest of the packets.
     *
     * @param inStream
     *            input stream to read from
     * @return true if successful false otherwise
     *
    public boolean synRead(InputStream inStream) {
        byte[] pData = new byte[23];
        int curRead = 0;
        Log.i("DebugApp", "sync read reached");
        pData[0] = 0;
        while (pData[0] != '$') {
            Log.i("DebugApp", " NOT $$$$ " + pData[0]);
            try {
                curRead = inStream.read(pData, 0, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (pData[0] != '$')
                Log.i("DebugApp", "Wrong Ping");
            if (curRead < 0) {
                return false;
            }
        }

        int m = 1;
        int shiftIndex = m;
        int pLength = 23;
        while (m != pLength) {
            try {
                curRead = inStream
                        .read(pData, shiftIndex, pLength - shiftIndex);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (curRead < 0) {
                return false;
            }
            m += curRead;
            shiftIndex = m;
        }

        return true;
    }

    **
     * Reads data from input stream and stores them to a buffer
     *
     * param inStream
     *            input stream to read from
     * param pData
     *            buffer where the read data will be stored
     * param pLength
     *            maximum bytes to store into pData
     * return true if successful false otherwise
     *
    public boolean readData(InputStream inStream, byte[] pData, int pLength) {

        try {
            int m = 0;
            int shiftIndex = m;
            while (m != pLength) {

                int curRead = inStream.read(pData, shiftIndex, pLength
                        - shiftIndex);
                if (curRead < 0) {
                    return false;
                }
                m += curRead;
                shiftIndex = m;
            }

            return true;

        } catch (IOException e) {

            running = false;
            // e.printStackTrace();
            return false;
        }
    }

    protected void onPreExecute() {
        super.onPreExecute();
        running = true;
    }

    **
     * Combine two individual bytes into a float without overflow check.
     *
     * param d1
     *            First Byte
     * param d2
     *            Second Byte
     * return Combined float value.
     *
    float twoBytes_nocheck(byte d1, byte d2) {
        // """ unmarshal two bytes into int16 """
        float d = ord(d1) * 256 + ord(d2);
        return d;
    }

    **
     * Combine two individual bytes into a float with overflow check.
     *
     * param d1
     *            First Byte
     * param d2
     *            Second Byte
     * return Combined float value.
     *

    float twoBytes(byte d1, byte d2) {
        // """ unmarshal two bytes into int16 """
        float d = ord(d1) * 256 + ord(d2);
        if (d > 32767)
            d -= 65536;
        return d;
    }

    int ord(byte d) {
        return d & 0xff;
    }

    **
     * Combine four individual bytes into a float with overflow check.
     *
     * param d1
     *            First Byte
     * param d2
     *            Second Byte
     * param d3
     *            Third Byte
     * param d4
     *            Fourth Byte
     * return Combined Float value.
     *

    // For 32-bit signed integers.
    float four_bytes(byte d1, byte d2, byte d3, byte d4) {
        float d = ord(d1) * (1 << 24) + ord(d2) * (1 << 16) + ord(d3)
                * (1 << 8) + ord(d4);
        if (d > 2147483648l)
            d -= 4294967296l;
        return d;
    }

    private void onPacketDataQuat(byte[] buffer) {
        if (Global.CASDKUtilityActivityHandler != null) {
            Message msg = new Message();
            msg.arg1 = 1;
            msg.arg2 = PACKET_DATA_QUAT;
            Global.SensorLogStatus |= Global.PRINT_QUAT;
            Global.CASDKUtilityActivityHandler.sendMessage(msg);
        }
    }

    private void onPacketQuat(byte[] buffer) {
        Global.q[0] = four_bytes(buffer[3], buffer[4], buffer[5], buffer[6])
                * 1.0f / (1 << 30);
        Global.q[1] = four_bytes(buffer[7], buffer[8], buffer[9], buffer[10])
                * 1.0f / (1 << 30);
        Global.q[2] = four_bytes(buffer[11], buffer[12], buffer[13], buffer[14])
                * 1.0f / (1 << 30);
        Global.q[3] = four_bytes(buffer[15], buffer[16], buffer[17], buffer[18])
                * 1.0f / (1 << 30);

        double mag = Math
                .sqrt(((Global.q[0] * Global.q[0])
                        + (Global.q[1] * Global.q[1])
                        + (Global.q[2] * Global.q[2]) + (Global.q[3] * Global.q[3])));

        for (int i = 0; i < 4; i++) {
            Global.q[i] = (float) (Global.q[i] / mag);
        }

        Global.eular[0] = (float) (Math
                .atan2(2 * (Global.q[0] * Global.q[1] + Global.q[2]
                                * Global.q[3]),
                        1 - 2 * (Global.q[1] * Global.q[1] + Global.q[2]
                                * Global.q[2])) * 180 / Math.PI);
        Global.eular[1] = (float) (Math
                .asin(2 * (Global.q[0] * Global.q[2] - Global.q[3]
                        * Global.q[1])) * 180 / Math.PI);

        Global.eular[2] = (float) (Math
                .atan2(2 * (Global.q[0] * Global.q[3] + Global.q[1]
                                * Global.q[2]),
                        1 - 2 * (Global.q[2] * Global.q[2] + Global.q[3]
                                * Global.q[3])) * 180 / Math.PI);

    }

    private void onPacketOtherSensors(byte[] buffer) {
        Global.Pressure = twoBytes_nocheck(buffer[3], buffer[4]);

        Global.Pressure = Global.Pressure * 60 / 65535 + 50;
        Global.Pressure = Global.Pressure * 10;

        Global.Humidity = twoBytes_nocheck(buffer[5], buffer[6]);
        Global.Tempreture = twoBytes_nocheck(buffer[7], buffer[8]);

        float hex = Integer.parseInt("FFF8", 16);
        Global.Humidity = Float.intBitsToFloat(Float
                .floatToRawIntBits(Global.Humidity)
                & Float.floatToRawIntBits(hex));

        Global.Humidity = (float) (-6 + 125 * (Global.Humidity / Math
                .pow(2, 16)));

        Global.Tempreture = (float) (-46.85 + 175.72 * (Global.Tempreture / Math
                .pow(2.0, 16.0)));

        float curr_light;
        curr_light = twoBytes_nocheck(buffer[9], buffer[10]);
        // curr_light = curr_light * 0.1f; // for CM36681
        curr_light = curr_light * 0.06103f; // for CM3232

        Global.Light = (curr_light * .8f) + (Global.Light * .2f);
        Global.Light = Math.round(Global.Light);
        Global.UV = twoBytes_nocheck(buffer[11], buffer[12]);
        Global.UV = (Global.UV * 0.022f) * 10;

        **
         * Optional lowpass filters can be applied here lpfUVIndex();
         * movingAvgHumidity(); movingAvgLight(); lpfPressure();
         * lpfTemperature();
         *
    }

    private void onPacketDataOtherSensors(byte[] buffer) {
        if (Global.CASDKUtilityActivityHandler != null) {
            Message msg = new Message();
            msg.arg1 = 1;
            msg.arg2 = PACKET_DATA_OTHER_SENSORS;
            Global.SensorLogStatus |= Global.PRINT_OTHER_SENSORS;
            Global.CASDKUtilityActivityHandler.sendMessage(msg);
        }
    }

    private void onPacketDebugLog(byte[] buffer) {
        StringBuilder strBuilder = new StringBuilder(23);
        for (int j = 2; j < 22; j++) {
            if (buffer[j] == '\r' && buffer[j + 1] == '\n')
                break;
            strBuilder.append((char) buffer[j]);
        }
        String msgStr = strBuilder.toString();

        if (Global.CASDKUtilityActivityHandler != null) {
            Message mssg = new Message();
            mssg.arg1 = 0;
            mssg.obj = msgStr;
            Global.CASDKUtilityActivityHandler.sendMessage(mssg);
            if(mssg.obj!=null)
            {
                String stat = mssg.obj.toString();

                if(stat.contains("Battery"))
                {
                    Log.i("MODEL",stat);
                    Pattern p = Pattern.compile("(?!=\\d\\.\\d\\.)([\\d.]+)");
                    Matcher m = p.matcher(stat);
                    if(m.find()) {
                        Global.status = Double.parseDouble(m.group(1));
                        Log.i("MODEL", Global.status+"");
                    }
                    Log.i("MODEL", Global.status+" d");


                }
            }
        }
    }

    private void onPacketDataAccel(byte[] l) {

        Global.Accel[0] = four_bytes(l[3], l[4], l[5], l[6]) * 1.0f / (1 << 16);

        Global.Accel[1] = four_bytes(l[7], l[8], l[9], l[10]) * 1.0f
                / (1 << 16);

        Global.Accel[2] = four_bytes(l[11], l[12], l[13], l[14]) * 1.0f
                / (1 << 16);

        if (Global.CASDKUtilityActivityHandler != null) {
            Message msg = new Message();
            msg.arg1 = 1;
            msg.arg2 = PACKET_DATA_ACCEL;
            Global.SensorLogStatus |= Global.PRINT_ACCEL;
            Global.CASDKUtilityActivityHandler.sendMessage(msg);
        }

    }

    private void onPacketDataGyro(byte[] l) {
        Global.Gyro[0] = four_bytes(l[3], l[4], l[5], l[6]) * 1.0f / (1 << 16);

        Global.Gyro[1] = four_bytes(l[7], l[8], l[9], l[10]) * 1.0f / (1 << 16);

        Global.Gyro[2] = four_bytes(l[11], l[12], l[13], l[14]) * 1.0f
                / (1 << 16);

        if (Global.CASDKUtilityActivityHandler != null) {
            Message msg = new Message();
            msg.arg1 = 1;
            msg.arg2 = PACKET_DATA_GYRO;
            Global.SensorLogStatus |= Global.PRINT_GYRO;
            Global.CASDKUtilityActivityHandler.sendMessage(msg);
        }
    }

    private void onPacketDataCompass(byte[] l) {
        Global.Compass[0] = four_bytes(l[3], l[4], l[5], l[6]) * 1.0f
                / (1 << 16);

        Global.Compass[1] = four_bytes(l[7], l[8], l[9], l[10]) * 1.0f
                / (1 << 16);

        Global.Compass[2] = four_bytes(l[11], l[12], l[13], l[14]) * 1.0f
                / (1 << 16);

        if (Global.CASDKUtilityActivityHandler != null) {
            Message msg = new Message();
            msg.arg1 = 1;
            msg.arg2 = PACKET_DATA_COMPASS;
            Global.SensorLogStatus |= Global.PRINT_COMPASS;
            Global.CASDKUtilityActivityHandler.sendMessage(msg);
        }
    }

    private void onPacketDataEuler(byte[] l) {
        Global.eular[0] = four_bytes(l[3], l[4], l[5], l[6]) * 1.0f / (1 << 16);

        Global.eular[1] = four_bytes(l[7], l[8], l[9], l[10]) * 1.0f
                / (1 << 16);

        Global.eular[2] = four_bytes(l[11], l[12], l[13], l[14]) * 1.0f
                / (1 << 16);

        if (Global.CASDKUtilityActivityHandler != null) {
            Message msg = new Message();
            msg.arg1 = 1;
            msg.arg2 = PACKET_DATA_EULER;
            Global.SensorLogStatus |= Global.PRINT_EULER;
            Global.CASDKUtilityActivityHandler.sendMessage(msg);
        }
    }

    private void onPacketEuler(byte[] l) {
        Global.eular[0] = four_bytes(l[3], l[4], l[5], l[6]) * 1.0f / (1 << 16);

        Global.eular[1] = four_bytes(l[7], l[8], l[9], l[10]) * 1.0f
                / (1 << 16);

        Global.eular[2] = four_bytes(l[11], l[12], l[13], l[14]) * 1.0f
                / (1 << 16);

        if (Global.CASDKUtilityActivityHandler != null) {
            Message msg = new Message();
            msg.arg1 = 3;
            msg.arg2 = PACKET_EULER;
            Global.CASDKUtilityActivityHandler.sendMessage(msg);
        }
    }

    private void onPacketDataHeading(byte[] l) {
        Global.Heading = four_bytes(l[3], l[4], l[5], l[6]) * 1.0f / (1 << 16);

        if (Global.CASDKUtilityActivityHandler != null) {
            Message msg = new Message();
            msg.arg1 = 1;
            msg.arg2 = PACKET_DATA_HEADING;
            Global.SensorLogStatus |= Global.PRINT_HEADING;
            Global.CASDKUtilityActivityHandler.sendMessage(msg);
        }
    }

    private void onPacketHeading(byte[] l) {
        Global.Heading = four_bytes(l[3], l[4], l[5], l[6]) * 1.0f / (1 << 16);

        if (Global.CASDKUtilityActivityHandler != null) {
            Message msg = new Message();
            msg.arg1 = 3;
            msg.arg2 = PACKET_HEADING;
            Global.CASDKUtilityActivityHandler.sendMessage(msg);
        }
    }

    private void onPacketDataRotMat(byte[] l) {
        Global.RM[0] = twoBytes(l[3], l[4]) * 1.0f / (1 << 14);

        Global.RM[1] = twoBytes(l[5], l[6]) * 1.0f / (1 << 14);

        Global.RM[2] = twoBytes(l[7], l[8]) * 1.0f / (1 << 14);

        Global.RM[3] = twoBytes(l[9], l[10]) * 1.0f / (1 << 14);

        Global.RM[4] = twoBytes(l[11], l[12]) * 1.0f / (1 << 14);

        Global.RM[5] = twoBytes(l[13], l[14]) * 1.0f / (1 << 14);

        Global.RM[6] = twoBytes(l[15], l[16]) * 1.0f / (1 << 14);

        Global.RM[7] = twoBytes(l[17], l[18]) * 1.0f / (1 << 14);

        Global.RM[8] = twoBytes(l[19], l[20]) * 1.0f / (1 << 14);

        if (Global.CASDKUtilityActivityHandler != null) {
            Message msg = new Message();
            msg.arg1 = 1;
            msg.arg2 = PACKET_DATA_ROT;
            Global.SensorLogStatus |= Global.PRINT_ROT_MAT;
            Global.CASDKUtilityActivityHandler.sendMessage(msg);
        }
    }

    private void onPacketRotMat(byte[] l) {
        Global.RM[0] = twoBytes(l[3], l[4]) * 1.0f / (1 << 14);

        Global.RM[1] = twoBytes(l[5], l[6]) * 1.0f / (1 << 14);

        Global.RM[2] = twoBytes(l[7], l[8]) * 1.0f / (1 << 14);

        Global.RM[3] = twoBytes(l[9], l[10]) * 1.0f / (1 << 14);

        Global.RM[4] = twoBytes(l[11], l[12]) * 1.0f / (1 << 14);

        Global.RM[5] = twoBytes(l[13], l[14]) * 1.0f / (1 << 14);

        Global.RM[6] = twoBytes(l[15], l[16]) * 1.0f / (1 << 14);

        Global.RM[7] = twoBytes(l[17], l[18]) * 1.0f / (1 << 14);

        Global.RM[8] = twoBytes(l[19], l[20]) * 1.0f / (1 << 14);

        if (Global.CASDKUtilityActivityHandler != null) {
            Message msg = new Message();
            msg.arg1 = 3;
            msg.arg2 = PACKET_ROT_MAT;
            Global.CASDKUtilityActivityHandler.sendMessage(msg);
        }
    }


      // This method runs in the background and continuously updates the Sensors
      //  values by reading the Bluetooth input stream from wearable SDK. There are
      //  optional filters can be applied in the code

    protected Integer doInBackground (InputStream... params)
    {
        synRead(params[0]);

      while (running)
      { try
        { mutexLock.acquire();
          byte buffer[] = new byte[23];
          if (readData(params[0], buffer, 23))
          { if (buffer[1] == PACKET_DATA)
            { switch (buffer[2])
              { case PACKET_DATA_QUAT:
                     onPacketDataQuat(buffer);    // Are we using this ???
                     break;
                case PACKET_DATA_OTHER_SENSORS:
                     onPacketDataOtherSensors(buffer);   // How bout this ???
                     break;
                case PACKET_DATA_ACCEL:
                     onPacketDataAccel(buffer);
                     break;
                case PACKET_DATA_COMPASS:
                     onPacketDataCompass(buffer);
                     break;
                case PACKET_DATA_EULER:
                     onPacketDataEuler(buffer);
                     break;
                case PACKET_DATA_GYRO:
                     onPacketDataGyro(buffer);
                     break;
                case PACKET_DATA_HEADING:
                     onPacketDataHeading(buffer);
                     break;
                case PACKET_DATA_ROT:
                     onPacketDataRotMat(buffer);
                     break;
              }
            }
            else if (buffer[1] == PACKET_QUAT)
            { if (buffer[2] == PACKET_DATA_QUAT)
                onPacketQuat(buffer);
            }
            else if (buffer[1] == PACKET_OTHER_SENSORS)
            { if (buffer[2] == PACKET_DATA_OTHER_SENSORS)
              { onPacketOtherSensors(buffer);
              }
            }
            else if (buffer[1] == PACKET_ROT_MAT)
            { if (buffer[2] == PACKET_DATA_ROT)
              { onPacketRotMat(buffer);
              }
            }
            else if (buffer[1] == PACKET_EULER)
            { if (buffer[2] == PACKET_DATA_EULER)
              { onPacketEuler(buffer);
              }
            }
            else if (buffer[1] == PACKET_HEADING)
            { if (buffer[2] == PACKET_DATA_HEADING)
              { onPacketHeading(buffer);
              }
            }
            else if (buffer[1] == PACKET_DEBUG)
            { onPacketDebugLog(buffer);
            }
          }
        }
        catch (InterruptedException e)
        { Log.i("BluetoothDataReader", "intterupted exception");
        }
        finally
        { mutexLock.release();
        }
      }
      return 1;
    }

    // Previous uv value. Used for filtering
    protected float prevUVIndex = 0;
    // Previous humidity value. Used for filtering
    protected float prevHumidity = 0;
    // Previous light value. Used for filtering
    protected float prevLight = 0;
    // Previous temperature value. Used for filtering
    protected float prevTemperature = 0;
    // Previous pressure value. Used for filtering
    protected float prevPressure = 0;
    // Filter value for low pass filter.
    protected static final float alpha = 0.8f;

    **
     * Applies a low pass filter to the UV value. Values are store in
     * Global.java
     *
    protected void lpfUVIndex() {
        Global.UV = Global.UV * (1 - alpha) + alpha * prevUVIndex;
        prevUVIndex = Global.UV;
    }

    **
     * Applies a low pass filter to Humidity. Values are stored in Global.java
     *
    protected void lpfHumiditiy() {
        Global.Humidity = Global.Humidity * (1 - alpha) + alpha * prevHumidity;
        prevHumidity = Global.Humidity;
    }

    // humidity values are stored to calculate the average
    protected LinkedList<Float> humiditySamples = new LinkedList<Float>();

    **
     * Applies a moving average to Humidity
     *
    protected void movingAvgHumidity() {
        if (humiditySamples.size() < mov_avg_size) {
            humiditySamples.push(Global.Humidity);
        } else {
            float temp = 0;
            for (Float sample : humiditySamples) {
                temp += sample;
            }
            temp = temp / mov_avg_size;
            humiditySamples.removeFirst();
            humiditySamples.addLast(Global.Humidity);

            Global.Humidity = temp;
        }
    }

    **
     * Applies a low pass filter to light.
     *
    protected void lpfLight() {
        Global.Light = Global.Light * (1 - alpha) + alpha * prevLight;
        prevLight = Global.Light;
    }

    // used for calculating moving average
    static protected final int mov_avg_size = 23;
    // stored light samples for the moving average
    protected LinkedList<Float> lightSamples = new LinkedList<Float>();

    **
     * Applies a moving average to light values.
     *
    protected void movingAvgLight() {
        if (lightSamples.size() < mov_avg_size) {
            lightSamples.push(Global.Light);
        } else {
            float temp = 0;
            for (Float sample : lightSamples) {
                temp += sample;
            }
            temp = temp / mov_avg_size;
            lightSamples.removeFirst();
            lightSamples.addLast(Global.Light);

            Global.Light = temp;
        }
    }

    **
     * Applies a low pass filter to temperature.
     *
    protected void lpfTemperature() {
        Global.Tempreture = Global.Tempreture * (1 - alpha) + alpha
                * prevTemperature;
        prevTemperature = Global.Tempreture;
    }

    **
     * applies a low pass filter to pressure
     *
    protected void lpfPressure() {
        Global.Pressure = Global.Pressure * (1 - alpha) + alpha * prevPressure;
        prevPressure = Global.Pressure;
    }
}



*******/