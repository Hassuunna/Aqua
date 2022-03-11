package com.example.aquavalley00;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;

class Helper implements Runnable {
    MainActivity main;
    public Helper(MainActivity ob) {
        main = ob;
    }
    public void run() {
        /* Called when the activity is first created. */
        String TAG = "ServerSocketTest";

        try {
            main.serverSocket= new ServerSocket(main.ServerPort);

            while (true) {
                Socket socket = main.serverSocket.accept();

                main.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String str = main.in.readLine();
                //message_txt.setText(str.split("[:]")[1]);
                Log.i("received response: ", str);

                Response(socket, main.in, str);

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
                out.write(main.device_name);
                Log.i("IP", value);
                //main.connectSocket(value);
                break;
            case "pair":
                try {
                    //out.write("waitingtoconfirm");
                    //main.out2.write("OK");
                    AlertDialog.Builder builder = new AlertDialog.Builder(main);
                    builder.setCancelable(true);
                    builder.setTitle("Pair");
                    builder.setMessage(String.format("Pair Device with \nPasskey: %s", value));
                    new Thread() {
                        private void confirmationDialog() {
                            builder.setPositiveButton("Confirm", (dialog, which) -> {
                                try {
                                    main.message_txt.setText("Confirmed");
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
                            main.runOnUiThread(this::confirmationDialog);
                        }
                    }.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "contacts":
                if(ContextCompat.checkSelfPermission(main, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(main, new String[]{Manifest.permission.READ_CONTACTS},0);
                }
                ContentResolver contentResolver = main.getContentResolver();
                Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
                Cursor cursor = contentResolver.query(uri,null,null,null,null);
                int cnt = 0;
                if (cursor.getCount() > 0 ) {
                    while (cursor.moveToNext() && cnt < 5) {
                        cnt++;
                        @SuppressLint("Range") String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        @SuppressLint("Range") String contactNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        main.dictionary.put(contactName,contactNumber);
                        for (Enumeration i = main.dictionary.elements(); i.hasMoreElements();)
                        {
                            Log.i("Value in Dictionary : ",String.valueOf(i.nextElement()));
                            //System.out.println("Value in Dictionary : " + i.nextElement());

                        }
                        try {
                            if(cnt==5)
                                out.write(contactName + ":" + contactNumber);
                            else
                                out.write(contactName + ":" + contactNumber + ",");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case "SMS":
                if(ContextCompat.checkSelfPermission(main, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED){
                    String straf =  str.split(":")[1];
                    main.no = straf.split(",")[0];
                    main.mess = straf.split(",")[1];
                    main.sendSMS(main.no,main.mess);
                }
                else{
                    ActivityCompat.requestPermissions(main,new String[] {Manifest.permission.SEND_SMS},100);
                }

                break;
            case"call":
                    if (ContextCompat.checkSelfPermission(main, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED){
                        Intent callIntent = new Intent(Intent.ACTION_CALL);
                        callIntent.setData(Uri.parse("tel:" + value));//change the number
                        main.startActivity(callIntent);
                    }
                    else {
                        ActivityCompat.requestPermissions(main, new String[] {Manifest.permission.CALL_PHONE},100);
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