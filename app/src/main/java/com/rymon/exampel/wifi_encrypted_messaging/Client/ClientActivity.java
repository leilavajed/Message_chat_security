package com.rymon.exampel.wifi_encrypted_messaging.Client;

import android.database.DataSetObserver;
import android.os.Build;

import android.os.Bundle;

import java.io.*;
import java.net.*;
import java.util.Base64;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.rymon.exampel.wifi_encrypted_messaging.Models.ChatMessage;
import com.rymon.exampel.wifi_encrypted_messaging.Models.Client;
import com.rymon.exampel.wifi_encrypted_messaging.Models.Server;
import com.rymon.exampel.wifi_encrypted_messaging.R;
import com.rymon.exampel.wifi_encrypted_messaging.Utils.SecurityUtils;


public class ClientActivity extends AppCompatActivity {
    Button imageView_send;
    EditText editText_client;
    Handler handler = new Handler();
    String ip, name;
    final Context context = this;
    Socket clientSocket;
    PrintStream printStream = null;
    private Chat_Client_Adapter chatClientAdapter;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_client);


        Initilize();


    }


    private void Initilize()
    {
        //FindView
       FindViewById();

       Setup_View();



    }
    private void FindViewById()
    {
        editText_client = (EditText) findViewById(R.id.editText_client);

        imageView_send = (Button) findViewById(R.id.send_but);


        listView = (ListView) findViewById(R.id.ListView_client);
    }

    private void Setup_View()
    {

        chatClientAdapter = new Chat_Client_Adapter(getApplicationContext(), R.layout.right);
        listView.setAdapter(chatClientAdapter);

        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatClientAdapter);

        //to scroll the list view to bottom on data change
        chatClientAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatClientAdapter.getCount() - 1);
            }
        });


        //Creat Dialog in first time
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.prompt_client, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptsView);

        final EditText uname = (EditText) promptsView.findViewById(R.id.uname);
        final EditText sip = (EditText) promptsView.findViewById(R.id.sip);

        alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        //get name user and ip server
                        name = uname.getText().toString();
                        ip = sip.getText().toString();
                        //Start Communication in Thread
                        Thread sc = new Thread(new StartCommunication());
                        sc.start();
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }





    //get String and show message to ListView
    private boolean sendChatMessage(String str)
    {
        //use code html for change color
        String greenFont = "<font color='#00AA00'> *** ";
        String redFont =  "<font color='#AA0000'>*** ";
        String shaCheck= "";
        String arr[] = str.split(":");
        if (arr.length == 1)
        {
            //if Server started or joined >color change to greenFont >>  set adapter and show message to List view
            if (str.contains("Server started") || str.contains("Joined"))
                chatClientAdapter.add(new ChatMessage(false, greenFont + str + "***</font>"));
            else
                chatClientAdapter.add(new ChatMessage(false, redFont + str + "***</font>"));
        } else {
            if (arr.length == 5) {
                if( arr[4].contains("Not the Same SHA (message has changed)"))
                    shaCheck = redFont;
                else shaCheck = greenFont;
                if (!arr[0].equals(name))
                    chatClientAdapter.add(
                            new ChatMessage(
                                    false,
                                    "<font color='#0077CC'>SENDER -> " + arr[0] + "</font><br/>" + "\n" +
                                            "<font color='#741178'>" + arr[1] + "\n" +"</font><br/>" + "\n" + "\n" +
                                            "<font color='#741178'>" + "\n" + "</font><br/>" +  "\n" +
                                            "<font color='#0900CC'>" + arr[2] + "</font><br/>" + "\n"+
                                            "<font color='#222222'>"+ arr[3] + "\n"+ "</font><br/>" +
                                            shaCheck + arr[4] +"\n" + "*** </font>" + "\n"));
                else
                    chatClientAdapter.add(
                            new ChatMessage(
                                    true,
                                    "<font color='#0077CC'>SENDER -> " + arr[0] + "</font><br/>" + "\n" +
                                            "<font color='#741178'>" + arr[1] + "\n" +"</font><br/>" + "\n" + "\n" +
                                            "<font color='#741178'>" + "\n" + "</font><br/>" +  "\n" +
                                            "<font color='#0900CC'>" + arr[2] + "</font><br/>" + "\n"+
                                            "<font color='#222222'>"+ arr[3] + "\n"+ "</font><br/>" +
                                            shaCheck + arr[4] +"\n" + "*** </font>" + "\n"));
            } else {
                if (!arr[0].equals(name))
                    chatClientAdapter.add(new ChatMessage(false, "<font color='#0077CC'>" + arr[0] + "</font><br/>" + arr[1]));
                else
                    chatClientAdapter.add(new ChatMessage(true, "<font color='#0077CC'> ME(" + arr[0] + ")</font><br/>" + arr[1]));

            }

        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            if (printStream != null)
            {
                printStream.println("Ex1+:" + name);
                printStream.close();
            }
        }catch (Exception e){
            // TODO: 4/19/2022 cant sent exit msg
        }
    }

    class StartCommunication implements Runnable {

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            try {
                InetSocketAddress inetAddress = new InetSocketAddress(ip, 55555);
                clientSocket = new Socket();
                clientSocket.connect(inetAddress, 7000);
                printStream = new PrintStream(clientSocket.getOutputStream());
                sendChatMessage("Connected to " + ip + " !!\n");
                printStream.println("j01ne6:" + name + "(#Client)PublicKey->" + Client.getInstance().getPublicKey());
                imageView_send.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(editText_client.getText().length()>130)
                            Toast.makeText(ClientActivity.this, getString(R.string.too_much_input), Toast.LENGTH_LONG).show();
                        else if (editText_client.getText().length()==0)
                            Toast.makeText(ClientActivity.this, getString(R.string.empty_message), Toast.LENGTH_LONG).show();
                        else {
                            Thread st = new Thread(new SendThread());
                            st.start();
                        }
                    }
                });

                BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                while (true) {
                    final String str = br.readLine();
                    if (str.equalsIgnoreCase("exit"))
                    {
                        sendChatMessage("Server Closed the Connection!");
                        Thread.sleep(2000);
                        finish();
                        break;
                    }
                    else if (str.contains("(#Server)PublicKey->"))
                    {


                        String serverPublicKey = str.substring(str.indexOf("(#Server)PublicKey->") + 20);
                        Log.i("TAG", "serverPublicKey: " + serverPublicKey);
                        Server.getInstance().setProperty(serverPublicKey, "NOT found", true);

                        String ServerPublicKey = Server.getInstance().getPublicKey();
                        String ServerPrivateKey = Server.getInstance().getPrivateKey();
                        String ClientPublicKey = Client.getInstance().getPublicKey();
                        String ClientPrivateKey = Client.getInstance().getPrivateKey();
                        int a = 0;


                        sendChatMessage(str);
                    }
                    else if (str.contains("(#Client)PublicKey->"))
                    {


                        int a = 0;


                        sendChatMessage(str);
                    } else
                    {

                        String privateMsg = str.substring(0,str.indexOf("#SHA-256->"));
                        int a = 0;
                        String sha256 = str.substring(str.indexOf("#SHA-256->")+10);
                        String decryptedString = SecurityUtils.decrypt(privateMsg, Client.getInstance().getPrivateKey());
                        String calculatedSHA = SecurityUtils .SHA_256(decryptedString);
                        String isAuthorized = "Not the Same SHA (message has changed)";
                        if(calculatedSHA.equals(sha256))
                            isAuthorized = "the Same SHA (message has not changed)";
                         String str2 = decryptedString + ": Encrypted Message : " + privateMsg + ":" + isAuthorized;

                        sendChatMessage(str2);

                    }
                }
            } catch (final Exception e) {
                sendChatMessage("Not able to connect!");
                try {
                    Thread.sleep(2000);
                } catch (Exception exx) {
                }
                finish();
            }
        }
    }

    //encrypted Message and put to server
    class SendThread implements Runnable {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            try {
                String message = editText_client.getText().toString();
                handler.post(new Runnable()
                {
                    @Override
                    public void run() {
                        editText_client.setText("");
                    }
                });
                message = name + ": " + message;
                String encryptedString = Base64.getEncoder().encodeToString(SecurityUtils.encrypt(message, Server.getInstance().getPublicKey()));

                String serverMessage = "#T0_Server_Private->" + encryptedString + "#SHA-256->"+ SecurityUtils.SHA_256(message);

                sendChatMessage(message);
                printStream.println(serverMessage);
                printStream.flush();
            } catch (Exception e) {

            }
        }
    }
}
