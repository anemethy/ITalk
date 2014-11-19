package com.example.al.italk;

/***
 * ITalk App for Android Version 0.5 Beta
 * Author: Albert N. Nemethy
 * Last UpDate: 11-10-2014
 * Copyright (c) Autonomous Engineering of VT
 * All Rights Reserved
 *
 * DataPipe.java
 */
import java.util.concurrent.ConcurrentLinkedQueue;

public class DataPipe extends ConcurrentLinkedQueue<byte[]>
{ private ConcurrentLinkedQueue ourQue = null;
  private int ourMaxSize = 1024 * 5;  // Start out with a default 5K buffer
  private int ourCurSize = 0;

  DataPipe (int size)
  { if (size > 1024) ourMaxSize = size;
    ourQue = new ConcurrentLinkedQueue();   // Instantiate our queue
    ourQue.clear();                         // Start out empty
  }

  public boolean isFull ()             // Determines if we're full
  { if (ourQue.size() >= ourCurSize)
      return true;
    return false;
  }

  public int getSize ()                // Returns our size
  { return ourCurSize;
  }

  public int getMaxSize ()             // Returns the maximum size that the Queue can have
  { return ourMaxSize;
  }

  public int getPercentFull ()         // Returns the percentage of the queue that has been used
  { int percent = (int)(((double)ourCurSize / (double)ourMaxSize) * 100.0);
    if (percent > 100) { percent = 100; }
    return ((int)percent);
  }

  @Override                            // Override the Add methods so we can keep track of our size
  public boolean add (byte[] dat)
  { if ((dat.length + ourCurSize) >= ourMaxSize)
      return false;
    super.add(dat);
    return true;
  }

  @Override                            // Override the poll methods so we can keep track of our size
  public byte[] poll ()
  { byte[] lba = super.poll();
    if (lba != null)
    { ourCurSize -= lba.length;
      if (ourCurSize < 0)
        ourCurSize = 0;
      return (lba);
    }
    return null;
  }
}
