package com.rymon.exampel.wifi_encrypted_messaging.Client;


import android.content.Context;
import android.os.*;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.rymon.exampel.wifi_encrypted_messaging.Models.ChatMessage;
import com.rymon.exampel.wifi_encrypted_messaging.R;

import java.util.ArrayList;
import java.util.List;

//ListView Adapter for show message in client activity
class Chat_Client_Adapter extends ArrayAdapter<ChatMessage>
{
    private TextView chatText;
    private List<ChatMessage> chatMessageList = new ArrayList<ChatMessage>();
    private Context context;
    Handler handler = new Handler();

    @Override
    public void add(final ChatMessage object)
    {
        handler.post(new Runnable() {
            @Override
            public void run() {
                chatMessageList.add(object);
                Chat_Client_Adapter.super.add(object);
            }
        });
    }

    public Chat_Client_Adapter(Context context, int textViewResourceId)
    {
        super(context, textViewResourceId);
        this.context = context;
    }

    public int getCount() {
        return this.chatMessageList.size();
    }

    public ChatMessage getItem(int index) {
        return this.chatMessageList.get(index);
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        ChatMessage chatMessageObj = getItem(position);
        View row = convertView;
        LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (chatMessageObj.left) {
            row = inflater.inflate(R.layout.right, parent, false);
        }else{
            row = inflater.inflate(R.layout.left, parent, false);
        }
        chatText = (TextView) row.findViewById(R.id.msgr);
        chatText.setText(Html.fromHtml(chatMessageObj.plainText),TextView.BufferType.SPANNABLE);

        return row;
    }
}