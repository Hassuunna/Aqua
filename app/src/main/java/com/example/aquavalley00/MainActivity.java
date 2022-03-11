package com.example.aquavalley00;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
    ServerSocket serverSocket;

    Thread serverThread = null;
    BufferedReader in;
    public static final int ServerPort = 8081;
    EditText numberEdit,messageEdit;
    Button sendBtnSMS;
    String no,mess;
    Dictionary dictionary;

    //Socket Deceration
    PrintWriter out2;
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

        //decleration to send sms
        messageEdit = findViewById(R.id.message);
        numberEdit = findViewById(R.id.number);
        sendBtnSMS = findViewById(R.id.sendBtn);
        sendBtnSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED){
                    no = numberEdit.getText().toString();
                    mess = messageEdit.getText().toString();
                    sendSMS(no,mess);
                }
                else{
                    ActivityCompat.requestPermissions(MainActivity.this,new String[] {Manifest.permission.SEND_SMS},100);
                }
            }
        });


        startServer();

        send_btn.setOnClickListener(v -> new Thread(() -> connectSocket(device_name)).start());
    }

    //Send SMS Method
    public void sendSMS(String phoneNumber, String message)
    {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
    }
    /*public void getContact(){

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
                dictionary.put(contactName,contactNumber);
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
            this.serverThread = new Thread(new Helper(this));
            this.serverThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connectSocket(String serverIP) {
        try {
            InetAddress serverAddress = InetAddress.getByName(serverIP);
            Log.d("TCP", "C: Connecting...");

            try (Socket socket = new Socket(serverAddress, 8080)) {

                BufferedReader in;
                Log.d("TCP", "C: Sending: ");
                out2 = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.d("TCP", "C: Sending: FROM IN OUT ");
                //out.println(message);
                String text;
                StringBuilder finalText = new StringBuilder();
                Log.d("TCP", "C: Sending: BEFORE WHILE");
                /*while ((text = in.readLine()) != null) {
                    finalText.append(URLDecoder.decode(text, "UTF-8"));
                    Log.d("TCP", "C: Sending: IN WHILE");
                }*/
                Log.d("TCP", "C: Sending: AFTER WHILE");
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
}