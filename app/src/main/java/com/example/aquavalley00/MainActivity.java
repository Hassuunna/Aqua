package com.example.aquavalley00;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

public class MainActivity extends Activity {

    String device_name;
    String serverIP;

    Button send_btn;
    TextView device_name_txt;
    TextView message_txt;
    private ServerSocket serverSocket;

    Thread serverThread = null;
    BufferedReader in;
    int checker = 3;
    public static final int ServerPort = 8081;

    //Button getContactbtn;
    Dictionary dictionary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //getContact();
        dictionary = new Hashtable();
        //getContactbtn = findViewById(R.id.getContactbtn);
        //getContact();

        send_btn = findViewById(R.id.send_device_name_btn);
        device_name_txt = findViewById(R.id.device_name_txt);
        message_txt = findViewById(R.id.message_txt);
        device_name = String.format("%s %s", Build.BRAND, Build.DEVICE);
        device_name_txt.setText(device_name);

        startServer();

        send_btn.setOnClickListener(v -> new Thread(() -> connectSocket(device_name)).start());
    }
    /*private void getContact(Socket socket){

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_CONTACTS},0);
        }
        ContentResolver contentResolver = getContentResolver();
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        Cursor cursor = contentResolver.query(uri,null,null,null,null);
        if (cursor.getCount() > 0 ){
            while(cursor.moveToNext()){
                @SuppressLint("Range") String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                @SuppressLint("Range") String contactNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                /*dictionary.put(contactName,contactNumber);
                for (Enumeration i = dictionary.elements(); i.hasMoreElements();)
                {
                    Log.i("Value in Dictionary : ",String.valueOf(i.nextElement()));
                    //System.out.println("Value in Dictionary : " + i.nextElement());

                }
                try {
                    BufferedWriter buffer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    buffer.write(contactName + ":" + contactNumber + ",");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }*/

    private void startServer() {
        try {
            message_txt.setText(R.string.starting_server_info);
            this.serverThread = new Thread(new ServerThread());
            this.serverThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connectSocket(String message) {
        try {
            InetAddress serverAddress = InetAddress.getByName(serverIP);
            Log.d("TCP", "C: Connecting...");

            try (Socket socket = new Socket(serverAddress, 4444)) {
                PrintWriter out;
                BufferedReader in;
                Log.d("TCP", "C: Sending: '" + message + "'");
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println(message);
                String text;
                StringBuilder finalText = new StringBuilder();
                while ((text = in.readLine()) != null) {
                    finalText.append(URLDecoder.decode(text, "UTF-8"));
                }
                message_txt.setText(R.string.receiving_info);
                message_txt.setText(finalText.toString());

                Log.d("TCP", "C: Sent.");
                Log.d("TCP", "C: Done.");

            } catch (Exception e) {
                Log.e("TCP", "S: Error", e);
            }

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            Log.e("TCP", "C: UnknownHostException", e);
            e.printStackTrace();
        }
    }



    class ServerThread implements Runnable {

        public void run() {
            /* Called when the activity is first created. */
            String TAG = "ServerSocketTest";

            try {
                serverSocket = new ServerSocket(ServerPort);

                while (true) {
                    Socket socket = serverSocket.accept();

                     in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String str = in.readLine();
                    //message_txt.setText(str.split("[:]")[1]);
                    Log.i("received response: ", str);

                    Response(socket, in, str);

                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }

        private void Response(Socket socket, BufferedReader in, String str) throws IOException {
            final BufferedWriter out = new BufferedWriter(new OutputStreamWriter((socket.getOutputStream())));
            String value = str.split(":")[1];

            switch(str.split(":")[0]) {
                case "handshake":
                    out.write(device_name);
                    break;
                case "pair":
                    try {
                        //out.write("waitingtoconfirm");
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setCancelable(true);
                        builder.setTitle("Pair");
                        builder.setMessage(String.format("Pair Device with \nPasskey: %s", value));
                        new Thread() {
                            private void confirmationDialog() {
                                builder.setPositiveButton("Confirm", (dialog, which) -> {
                                    try {
                                        message_txt.setText("Confirmed");
                                        out.write("Ok");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }).setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                                    try {
                                        out.write("Cancel");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                                builder.create();
                                builder.show();
                            }

                            public void run() {
                                MainActivity.this.runOnUiThread(this::confirmationDialog);
                            }
                        }.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case "contacts":
                    if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_CONTACTS},0);
                    }
                    ContentResolver contentResolver = getContentResolver();
                    Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
                    Cursor cursor = contentResolver.query(uri,null,null,null,null);
                    int cnt = 0;
                    if (cursor.getCount() > 0 ) {
                        while (cursor.moveToNext() && cnt < 10) {
                            cnt++;
                            @SuppressLint("Range") String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                            @SuppressLint("Range") String contactNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            dictionary.put(contactName,contactNumber);
                            for (Enumeration i = dictionary.elements(); i.hasMoreElements();)
                            {
                                Log.i("Value in Dictionary : ",String.valueOf(i.nextElement()));
                                //System.out.println("Value in Dictionary : " + i.nextElement());

                            }
                            try {
                                if(cnt==10)
                                    out.write(contactName + ":" + contactNumber);
                                else
                                out.write(contactName + ":" + contactNumber + ",");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                        break;

                default:
                    throw new IllegalStateException("Unexpected value: " + str.split("[:]")[0]);
            }


            //out.close();
            out.flush();
            in.close();
            socket.close();

        }

    }
}