package com.example.al.italk;

/***
 * ITalk App for Android Version 0.5 Beta
 * Author: Albert N. Nemethy
 * Last UpDate: 11-12-2014
 * Copyright (c) Autonomous Engineering of VT
 * All Rights Reserved
 *
 * ITalkHostService.java
 * Cobbled from TruvoloWebServices
 */

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import android.util.Log;

public class ITalkHostService extends Service
{ private static String response ;
  private static int count=0;
  private static final String APPID = "30141e7252b54d789aee536dd86a680b";  // CHANGE THIS !!!

  public interface ITalkHostUploadDataSetListener
  { public void responseReceived(String respcode,String inputString);
  }

  public interface ITalkHostListener
  { public void responseReceived(String resp);
  }

  @Override
  public IBinder onBind(Intent intent)
  { // TODO: Return the communication channel to the service.
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /***
   *           Call Like this ...
   *  logAPI = getString(R.string.URl) + getString(R.string.driverlogin) + "?username=" + username + "&password=" + password + "&provider=phone";
   *  URL loginUrl = null;
   *  loginUrl = new URI(getString(R.string.protocol), logAPI, null).toURL();
   *  utils.trvLog(log, Level.INFO, "Logging in " + username );
   *  TruvoloWebService.truvoloService(new LoginListener(),loginUrl.toString() , "POST" ,"" );
   *
   *  See truvolo main for additional examples
   *
   ***/

  public static void ITalkHostService (final ITalkHostListener listner ,final String URL,final String requestType,final String secureToken)
  {
    new Thread(new Runnable()
    { @Override
      public void run()
      { try
        { if(requestType.equals("GET"))
            response = executeGETURL(URL,secureToken);
          if(requestType.equals("POST"))
            response = executePOSTURL(URL,secureToken);
          if(requestType.equals("POST1"))
            response = executePOSTURLcode(URL,secureToken);
          if(requestType.equals("PUT"))
            response = executePUTURL(URL,secureToken);
          if(requestType.equals("DELETE"))
            response = executeDELETEURL(URL,secureToken);
          if(response != null)
          { listner.responseReceived(response);
          }
        }
        catch (Exception e)
        { Log.i("Webservice", "error occured "+URL+""+e.getMessage()+"======mymessage");
          response = "timeout";
          listner.responseReceived(response);
        }
      }
    }).start();
  }

  protected static String executeDELETEURL(final String url, String secureToken) throws MalformedURLException, IOException,Exception
  { Log.i("URL is ", url);
    try
    { HttpClient client = new DefaultHttpClient();
      HttpDelete delete = new HttpDelete(url);
      delete.addHeader("Content-Type","application/json");
      delete.addHeader("Authorization",secureToken);
      delete.addHeader("X-APP-ID",APPID);
      HttpResponse response = client.execute(delete);
      InputStream inputStream = response.getEntity().getContent();
      String str = read(inputStream);
      client.getConnectionManager().shutdown();
      return str;
    }
    catch (MalformedURLException me)
    { Log.d ("Util", "MalformedURLException found: "+me.getMessage()+ " " + me.getClass().getName());
      throw me;
    }
    catch (IOException e)
    { Log.d ("Util", "Exception found: "+e.getMessage()+" "+e.getClass().getName());
      throw e;
    }
    catch (Exception e)
    { Log.d ("Util", "Exception found: "+e.getMessage()+" "+e.getClass().getName());
      throw e;
    }
  }

  public static String executePOSTURL(final String url,String secureToken) throws MalformedURLException, IOException, Exception
  { Log.i("URL is ", url);
    try
    { HttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost(url);
      post.addHeader("Content-Type","application/json");
      post.addHeader("Authorization",secureToken);
      post.addHeader("X-APP-ID",APPID);
      HttpResponse response = client.execute(post);
      InputStream inputStream = response.getEntity().getContent();
      String str = read(inputStream);
      client.getConnectionManager().shutdown();
      return str;
    }
    catch (MalformedURLException me)
    { Log.d ("Util", "MalformedURLException found: "+me.getMessage()+" "+me.getClass().getName());
      me.printStackTrace();
      throw me;
    }
    catch (IOException e)
    { Log.d ("Util", "Exception found: "+e.getMessage()+" "+e.getClass().getName());
      e.printStackTrace();
      throw e;
    }
    catch (Exception e)
    { Log.d ("Util", "Exception found: "+e.getMessage()+" "+e.getClass().getName());
      e.printStackTrace();
      throw e;
    }
  }

  public static String executePOSTURLcode(final String url,String secureToken) throws MalformedURLException, IOException, Exception
  { Log.i ("URL is ", url);
    try
    { HttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost(url);
      post.addHeader("Content-Type","application/json");
      post.addHeader("Authorization",secureToken);
      post.addHeader("X-APP-ID",APPID);
      HttpResponse response = client.execute(post);
      return String.valueOf(response.getStatusLine().getStatusCode());
    }
    catch (MalformedURLException me)
    { Log.d ("Util", "MalformedURLException found: "+me.getMessage()+" "+me.getClass().getName());
      me.printStackTrace();
      throw me;
    }
    catch (IOException e)
    { Log.d ("Util", "Exception found: "+e.getMessage()+" "+e.getClass().getName());
      e.printStackTrace();
      throw e;
    }
    catch (Exception e)
    { Log.d ("Util", "Exception found: "+e.getMessage()+" "+e.getClass().getName());
      e.printStackTrace();
      throw e;
    }
  }

  public static String executeGETURL(final String url,String secureToken) throws MalformedURLException, IOException, Exception
  { Log.i ("URL is ", url);
    try
    { HttpClient client = new DefaultHttpClient();
      HttpGet get = new HttpGet(url);
      Log.i("count",String.valueOf(++count));
      get.addHeader("Content-Type","application/json");
      get.addHeader("Authorization",secureToken);
      get.addHeader("X-APP-ID",APPID);
      HttpResponse response = client.execute(get);
      InputStream inputStream = response.getEntity().getContent();
      String str = read(inputStream);
      return str;
    }
    catch (MalformedURLException me)
    { Log.d ("Util", "MalformedURLException found: "+me.getMessage()+" "+me.getClass().getName());
      throw me;
    }
    catch (IOException e)
    { Log.d ("Util", "Exception found: "+e.getMessage()+" "+e.getClass().getName());
      throw e;
    }
    catch (Exception e)
    { Log.d ("Util", "Exception found: "+e.getMessage()+" "+e.getClass().getName());
      throw e;
    }
  }

  public static String executePUTURL(final String url,String secureToken) throws MalformedURLException, IOException, Exception
  { Log.i ("URL is ", url);
    try
    { HttpClient client = new DefaultHttpClient();
      HttpPut put = new HttpPut(url);
      put.addHeader("Content-Type","application/json");
      put.addHeader("Authorization", secureToken);
      put.addHeader("X-APP-ID",APPID);
      HttpResponse response = client.execute(put);
      InputStream inputStream = response.getEntity().getContent();
      String str = read(inputStream);
      client.getConnectionManager().shutdown();
      return str;
    }
    catch (MalformedURLException me)
    { Log.d ("Util", "MalformedURLException found: "+me.getMessage()+" "+me.getClass().getName());
      throw me;
    }
    catch (IOException e)
    { Log.d ("Util", "Exception found: "+e.getMessage()+" "+e.getClass().getName());
      throw e;
    }
    catch (Exception e)
    { Log.d ("Util", "Exception found: "+e.getMessage()+" "+e.getClass().getName());
      throw e;
    }
  }

  private static String read(InputStream in) throws IOException
  { StringBuilder sb = new StringBuilder();
    BufferedReader r = new BufferedReader(new InputStreamReader(in), 1000);
    //String line = r.readLine();
    //while (line != null) { sb.append(line); line = r.readLine(); }

    for (String line = r.readLine(); line != null; line = r.readLine())
    { sb.append(line); }
    in.close();
    return sb.toString();
  }

  /**
   * Method to post method to cloud
   *
   */

   public static void uploadDataSet(final ITalkHostUploadDataSetListener listner ,final String URL, final String jsonString,final String secureToken)
   {
     new Thread(new Runnable()
     { @Override
       public void run()
       { DefaultHttpClient httpClient = new DefaultHttpClient();
         HttpPost postRequest = new HttpPost(URL);
         postRequest.addHeader("Authorization",secureToken);
         postRequest.addHeader("X-APP-ID",APPID);
         StringEntity input;
         try
         { input = new StringEntity("["+jsonString+"]");
           input.setContentType("application/json");
           postRequest.setEntity(input);
           HttpResponse response = httpClient.execute(postRequest);
           Log.e ("response.getStatusLine().getStatusCode()", String.valueOf(response.getStatusLine().getStatusCode()));
           httpClient.getConnectionManager().shutdown();
           listner.responseReceived(String.valueOf(response.getStatusLine().getStatusCode()),jsonString);
         }
         catch (Exception e)
         { listner.responseReceived("",jsonString);
         }
       }
     }).start();
   }

   /**
    * Method to post method to cloud
    * @param hostname
    * @param jsonString

        public static void tripReport(final TruvoloUploadDataSetListener listner ,final String URL, final String jsonString,final String secureToken) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    DefaultHttpClient httpClient = new DefaultHttpClient();
                    HttpPost postRequest = new HttpPost(URL);
                    postRequest.addHeader("Authorization",secureToken);
                    postRequest.addHeader("X-APP-ID",APPID);
                    StringEntity input;
                    try {
                        input = new StringEntity(jsonString);
                        input.setContentType("application/json");
                        postRequest.setEntity(input);
                        HttpResponse response = httpClient.execute(postRequest);
                        Log.e("response.getStatusLine().getStatusCode()", String.valueOf(response.getStatusLine().getStatusCode()));
                        httpClient.getConnectionManager().shutdown();
                        listner.responseReceived(String.valueOf(response.getStatusLine().getStatusCode()),jsonString);
                    } catch (Exception e) {
                        listner.responseReceived("",jsonString);
                    }
                }
            }).start();
        }
    }
*************/

}
