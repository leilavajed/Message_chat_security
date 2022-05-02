package com.rymon.exampel.wifi_encrypted_messaging.Server;

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
import android.widget.TextView;
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
import com.rymon.exampel.wifi_encrypted_messaging.Utils.NetworkUtils;
import com.rymon.exampel.wifi_encrypted_messaging.Utils.SecurityUtils;


public class ServerActivity extends AppCompatActivity {
    Button img_send_server;
    EditText editText_server;
    TextView chatspace;
    final Context context = this;
    String ip, name;
    static Socket arr[] = new Socket[100];
    static int num = 0;
    Handler handler = new Handler();
    private Chat_Server_Adapter chatServerAdapter;
    private ListView listView;
    private boolean side = false;
    ServerSocket ss;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_server);

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
        editText_server = (EditText) findViewById(R.id.editText_server);
        img_send_server = (Button) findViewById(R.id.send_but);

        listView = (ListView) findViewById(R.id.ListView_server);
    }

    private void Setup_View()
    {
        //get ip MAC address from NetworkUtils
        ip = NetworkUtils.getIPAddress(true);


        chatServerAdapter = new Chat_Server_Adapter(getApplicationContext(), R.layout.right);
        listView.setAdapter(chatServerAdapter);

        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatServerAdapter);

        //to scroll the list view to bottom on data change
        chatServerAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatServerAdapter.getCount() - 1);
            }
        });

        //Creat Dialog in first time

        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.prompt_server, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptsView);

        final EditText uname = (EditText) promptsView.findViewById(R.id.uname);

        alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        //get name user
                        name = uname.getText().toString();
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

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        for (int i = 0; i < num; i++) {
            Socket temp = arr[i];
            SendToAll thread = new SendToAll(temp, "exit");
            thread.start();
        }
        try {
            ss.close();
        } catch (Exception e) {
        }
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
                chatServerAdapter.add(new ChatMessage(false, greenFont + str + "***</font>"));
            else
                chatServerAdapter.add(new ChatMessage(false, redFont + str + "***</font>"));
        } else
        {
            if (arr.length == 5) {
                if( arr[4].contains("Not the Same SHA (message has changed)"))
                    shaCheck = redFont;
                else shaCheck = greenFont;
                if (!arr[0].equals(name))
                    chatServerAdapter.add(
                            new ChatMessage(
                                    false,
                                    "<font color='#0077CC'>SENDER -> " + arr[0] + "</font><br/>" + "\n" +
                                            "<font color='#741178'>" + arr[1] + "\n" +"</font><br/>" + "\n" + "\n" +
                                            "<font color='#741178'>" + "\n" + "</font><br/>" +  "\n" +
                                            "<font color='#0900CC'>" + arr[2] + "</font><br/>" + "\n"+
                                            "<font color='#222222'>"+ arr[3] + "\n"+ "</font><br/>" +
                                            shaCheck + arr[4] +"\n" + "*** </font>" + "\n"));
                else
                    chatServerAdapter.add(
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
                    chatServerAdapter.add(new ChatMessage(false, "<font color='#0077CC'>" + arr[0] + "</font><br/>" + arr[1]));
                else
                    chatServerAdapter.add(new ChatMessage(true, "<font color='#0077CC'> ME(" + arr[0] + ")</font><br/>" + arr[1]));

            }

        }
        return true;
    }

    class StartCommunication implements Runnable {
        @Override
        public void run() {
            try
            {
                //find empty port and creat ServerSocket
                ss = new ServerSocket(55555);

                //show this message to list view " Server started at 192.168.1.100 "
                sendChatMessage("Server started at " + ip + " !!\n");

                img_send_server.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        String message = editText_server.getText().toString();
                        if (message.length() > 138)
                            Toast.makeText(ServerActivity.this, getString(R.string.too_much_input), Toast.LENGTH_LONG).show();
                        else if (message.length()==0)
                            Toast.makeText(ServerActivity.this, getString(R.string.empty_message), Toast.LENGTH_LONG).show();
                        else {

                            editText_server.setText("");
                            message = name + ": " + message;
                            final String mes = message;
                            sendChatMessage(mes + "\n");
                            for (int i = 0; i < num; i++)
                            {
                                Socket socket = arr[i];
                                SendToAll thread = new SendToAll(socket, message);
                                thread.start();
                            }
                        }
                    }
                });

                while (true) {
                    Socket clientSocket = ss.accept();
                    ServerThread thread = new ServerThread(clientSocket);
                    arr[num++] = clientSocket;
                    thread.start();
                }

            } catch (final Exception e) {

                sendChatMessage(e.toString());

            }
        }
    }

    class SendToAll extends Thread {

        Socket s;
        String msg;

        SendToAll(Socket s, String msg) {
            this.s = s;
            this.msg = msg;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        public void run() {
            try {
                PrintStream ps = new PrintStream(s.getOutputStream());
                String str2="nothing";
                if (msg.substring(0, 6).equals("j01ne6"))
                {
                    str2 = msg.substring(7, msg.length()) + " Joined";
                    str2 = (str2+ "\n(#Server)PublicKey->" + Server.getInstance().getPublicKey());
                }
                else  if (msg.equalsIgnoreCase("exit"))
                {
                    str2 = msg;
                }
                else{
                   try {
                       String encryptedString = Base64.getEncoder().encodeToString(SecurityUtils.encrypt(msg, Client.getInstance().getPublicKey()));
                       str2 = encryptedString +  "#SHA-256->"+ SecurityUtils.SHA_256(msg);
                   }catch (Exception e){
                        String ea = e.toString();
                       Log.e("TAG", "run: ", e);
                   }
                }

                ps.println(str2);
                if (msg.equalsIgnoreCase("exit"))
                    for (int i = 0; i < num; i++) {
                        if (arr[i] == s) {
                            s.close();
                            break;
                        }
                    }

                ps.flush();
            } catch (final Exception e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        chatspace.append(e.toString());
                    }
                });
            }
        }
    }

    class ServerThread extends Thread {

        Socket clientSocket;

        ServerThread(Socket cs) {
            clientSocket = cs;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        public void run() {
            try {
                boolean isSentToClient = true;
                String str;
                BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                while (true)
                {
                    str = br.readLine();
                    if (str.startsWith("Ex1+:")) {
                        str = str.substring(5, str.length()) + " Left";
                        for (int i = 0; i < num; i++) {
                            if (arr[i] == clientSocket)
                                for (int j = i; j < num - 1; j++)
                                    arr[j] = arr[j + 1];
                            num--;

                        }
                        clientSocket.close();
                        for (int i = 0; i < num; i++) {
                            Socket temp = arr[i];
                            SendToAll thread = new SendToAll(temp, str);
                            thread.start();
                        }
                        sendChatMessage(str + "\n");
                        break;
                    }
                    String str2="nothing";
                    if (str.substring(0, 6).equals("j01ne6"))
                    {
                        if(str.contains("(#Client)PublicKey->")){
                            String clientPublicKey = str.substring(str.indexOf("(#Client)PublicKey->")+20);
                            Client.getInstance().setProperty(clientPublicKey,"NOT Found",true);
                        }

                        else
                        {
                            // TODO: 4/18/2022 must handel
                        }
                        str2 = str.substring(7, str.length()) + " Joined";
                    }
                    else if(str.contains("#T0_Server_Private->"))
                    {
                        String privateMsg = str.substring(str.indexOf("#T0_Server_Private->")+20,str.indexOf("#SHA-256->"));
                        String sha256 = str.substring(str.indexOf("#SHA-256->")+10);
                        String decryptedString = SecurityUtils.decrypt(privateMsg, Server.getInstance().getPrivateKey());
                        String calculatedSHA = SecurityUtils .SHA_256(decryptedString);
                        String isAuthorized = "Not the Same SHA (message has changed)";
                        if(calculatedSHA.equals(sha256))
                            isAuthorized = "the Same SHA (message has not changed)";
                        str2 = decryptedString + ": Encrypted Message : " + privateMsg + ":" + isAuthorized;
                        isSentToClient =false;

                    }
                    else
                        str2 = str;

                    sendChatMessage(str2 + "\n");

                    if(isSentToClient){
                        for (int i = 0; i < num; i++) {
                            Socket temp = arr[i];
                            SendToAll thread = new SendToAll(temp, str);
                            thread.start();
                        }
                    }
                }
            }
            catch (final Exception e) {
                try
                {

                    for (int i = 0; i < num; i++)
                    {
                        if (arr[i] == clientSocket)
                            for (int j = i; j < num - 1; j++)
                                arr[j] = arr[j + 1];
                    }
                    num--;
                    clientSocket.close();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
