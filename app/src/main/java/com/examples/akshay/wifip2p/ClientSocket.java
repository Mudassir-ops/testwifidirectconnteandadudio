package com.examples.akshay.wifip2p;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by ash on 16/2/18.
 */

public class ClientSocket extends AsyncTask {
    private static String data;
    private static ByteArrayOutputStream outputStream1;
    private static final String TAG = "===ClientSocket";
    private Socket socket;

    public ClientSocket(Context context, MainActivity activity, String data1, ByteArrayOutputStream outputStream) {
        //this.context = context;
        if (data1 != null) {
            data = data1;
        } else data = "null data";


        if (outputStream != null) {
            outputStream1 = outputStream;
        } else outputStream1 = null;


    }

    @Override
    protected Object doInBackground(Object[] objects) {
        sendData();
        return null;
    }


    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        Log.d(ClientSocket.TAG, "SendDataTask Completed");
    }


    public void sendData() {
        String host = MainActivity.IP;
        int port = 8888;
        int len;
        socket = new Socket();
        byte buf[] = new byte[1024];

        try {

            socket.bind(null);
            Log.d(ClientSocket.TAG, "Trying to connect...");

            socket.connect((new InetSocketAddress(host, port)), 500);
            Log.d(ClientSocket.TAG, "Connected...");

            OutputStream outputStream = socket.getOutputStream();

            //ContentResolver cr = context.getContentResolver();
            InputStream inputStream = null;
            inputStream = new ByteArrayInputStream(outputStream1.toByteArray());
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
        } catch (FileNotFoundException e) {
            //catch logic
            Log.d(ClientSocket.TAG, e.toString());
        } catch (IOException e) {
            //catch logic
            //activity.makeToast(ClientSocket.TAG + " " +e.toString());
            Log.d(ClientSocket.TAG, e.toString());
        }

        /**
         * Clean up any open sockets when done
         * transferring or if an exception occurred.
         */ finally {
            if (socket != null) {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        //catch logic
                    }
                }
            }
        }
    }


    public class sendDataTask extends AsyncTask {

        private String toSend;

        public sendDataTask(String data) {
            toSend = data;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            sendString();
            return null;
        }

        private void sendString() {

        }
    }

}