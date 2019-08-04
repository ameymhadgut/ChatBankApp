package com.android.chatbank;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

public class ChatActivity extends AppCompatActivity {

    private BroadcastReceiver m_dateChangedReceiver;
    static int TRANSACTION_LIMIT;
    static int CURRENT_TRANSACTION_VALUE;


    private EditText messageET;
    private ListView messagesContainer;
    private FloatingActionButton fab;
    private ChatAdapter adapter;
    private ArrayList<ChatMessage> chatHistory;
    private Scanner sc ;
    private BankServices bks;
    private static final String BOTNAME = "chatbank";
    private String path,clientResponse,response;

    private BankDbHelper dbHelper;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    final static String PREFS_NAME = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
//------------Detecting date change-------------------------------------------------------------------------------------
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        m_dateChangedReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (Intent.ACTION_DATE_CHANGED.equals(action) || Intent.ACTION_TIME_CHANGED.equals(action)){
                    CURRENT_TRANSACTION_VALUE =0;
                    SharedPreferences trChange = getSharedPreferences("TransactionChange", Context.MODE_PRIVATE);
                    SharedPreferences.Editor trChangeEitor = trChange.edit();
                    trChangeEitor.putInt("CurrentTransactionValue",CURRENT_TRANSACTION_VALUE);
                    trChangeEitor.commit();
                }

            }
        };
        registerReceiver(m_dateChangedReceiver, filter);
//----------------------------------------------------------------------------------------------------------------------

        firstTimeSettings();
        initializingTransactionValue();

        initialControls();
    }

    private void initializingTransactionValue() {
        SharedPreferences initTransaction = getSharedPreferences("Settings Data", Context.MODE_PRIVATE);
        TRANSACTION_LIMIT = initTransaction.getInt("transaction_limit",0);
        Log.d("ttt_val","initializing val in chat act :"+TRANSACTION_LIMIT);

        SharedPreferences initCurrentTransaction = getSharedPreferences("TransactionChange", Context.MODE_PRIVATE);
        CURRENT_TRANSACTION_VALUE = initCurrentTransaction.getInt("CurrentTransactionValue",0);
        Log.d("tt_val","initializing curr val in chat act :"+CURRENT_TRANSACTION_VALUE);
    }

    private void firstTimeSettings() {

        SharedPreferences firstTimeSettings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if(!firstTimeSettings.contains("my_first_time")){
            Log.d("first_time","chat activity first tym!");
            SharedPreferences.Editor editorFirst = firstTimeSettings.edit();
            editorFirst.putBoolean("my_first_time",true);
            editorFirst.commit();

            SharedPreferences trChange = getSharedPreferences("TransactionChange", Context.MODE_PRIVATE);
            SharedPreferences.Editor trChangeEitor = trChange.edit();
            trChangeEitor.putInt("CurrentTransactionValue",0);
            trChangeEitor.commit();
        }




    }
    //2.0 and above
    @Override
    public void onBackPressed() {
       // moveTaskToBack(true);
        logout();
    }

    // Before 2.0
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //moveTaskToBack(true);
            logout();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(ChatActivity.this,SettingActivity.class));
            return true;
        }


        if (id == R.id.logout) {

            logout();
            return true;
        }
        if (id == R.id.help_section) {

            startActivity(new Intent(ChatActivity.this,HelpActivity.class));
            return true;
        }





        return super.onOptionsItemSelected(item);
    }

    private void logout()
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        alertDialogBuilder.setTitle("Logout");
        alertDialogBuilder.setMessage(R.string.logout_msg);
        // set dialog message
        alertDialogBuilder
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                //logout to login page
                                LoginActivity.ACCOUNT_NUMBER = "";
                                LoginActivity.CPIN="";
                                startActivity(new Intent(ChatActivity.this,LoginActivity.class));

                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }
    private void initialControls() {
        messagesContainer = (ListView) findViewById(R.id.messageContainer);
        messageET = (EditText) findViewById(R.id.chatMessage);



        RelativeLayout container = (RelativeLayout) findViewById(R.id.container);
        loadDummyHistory();


        fab = (FloatingActionButton) findViewById(R.id.chatSendButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageText = messageET.getText().toString();
                if (TextUtils.isEmpty(messageText)) {
                    return;
                }

                ChatMessage chatMessage = new ChatMessage();
                //chatMessage.setId(122);//dummy
                chatMessage.setMessage(messageText);
                chatMessage.setDate(sdf.format(new Date()));
                chatMessage.setMe(true);
                messageET.setText("");

                displayMessageAndInsert(chatMessage);

                File fileExt = new File(getExternalFilesDir(null).getAbsolutePath() + "/bots");

                if (!fileExt.exists()) {
                    ZipFileExtraction extract = new ZipFileExtraction();

                    try {
                        extract.unZipIt(getAssets().open("bots.zip"), getExternalFilesDir(null).getAbsolutePath() + "/");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                //Checking from favourite list-----------------------------------------------------------------
                String request = messageText;

                int favNumber = 0;
                if(messageText.equalsIgnoreCase("Do 1"))
                    favNumber=1;
                else if(messageText.equalsIgnoreCase("Do 2"))
                    favNumber=2;
                else if(messageText.equalsIgnoreCase("Do 3"))
                    favNumber=3;
                else if(messageText.equalsIgnoreCase("Do 4"))
                    favNumber=4;
                else if(messageText.equalsIgnoreCase("Do 5"))
                    favNumber=5;
                else
                    favNumber=6;

                SharedPreferences mappingRequest;
                ChatMessage messageRequest;
                boolean flag_map = true;
                switch(favNumber)
                {
                    case 1:  mappingRequest = getSharedPreferences("Settings Data", Context.MODE_PRIVATE);
                            request = mappingRequest.getString("fav_trans_1", SettingActivity.DEFAULT_STRING);
                            if(request.equals("N/A") || request.equals("")) {
                                flag_map = false;
                                messageRequest = new ChatMessage();
                                messageRequest.setMe(true);
                                messageRequest.setMessage("Sorry, but you haven't defined any action! Please check the settings.");
                            }
                            else {
                                flag_map = true;
                                messageRequest = new ChatMessage();
                                messageRequest.setMe(true);
                                messageRequest.setMessage("Performing your favourite action : " + request);
                            }
                            displayMessageAndInsert(messageRequest);
                            break;
                    case 2:  mappingRequest = getSharedPreferences("Settings Data",Context.MODE_PRIVATE);
                            request = mappingRequest.getString("fav_trans_2", SettingActivity.DEFAULT_STRING);
                            if(request.equals("N/A") || request.equals("")) {
                            flag_map = false;
                            messageRequest = new ChatMessage();
                            messageRequest.setMe(true);
                            messageRequest.setMessage("Sorry, but you haven't defined any action! Please check the settings.");
                            }
                            else {
                                flag_map = true;
                            messageRequest = new ChatMessage();
                            messageRequest.setMe(true);
                            messageRequest.setMessage("Performing your favourite action : " + request);
                            }
                            displayMessageAndInsert(messageRequest);
                            break;
                    case 3:  mappingRequest = getSharedPreferences("Settings Data",Context.MODE_PRIVATE);
                            request = mappingRequest.getString("fav_trans_3", SettingActivity.DEFAULT_STRING);
                            if(request.equals("N/A") || request.equals("")) {
                            flag_map = false;
                            messageRequest = new ChatMessage();
                            messageRequest.setMe(true);
                            messageRequest.setMessage("Sorry, but you haven't defined any action! Please check the settings.");
                            }
                            else {
                                flag_map = true;
                            messageRequest = new ChatMessage();
                            messageRequest.setMe(true);
                            messageRequest.setMessage("Performing your favourite action : " + request);
                            }
                            displayMessageAndInsert(messageRequest);
                            break;
                    case 4:  mappingRequest = getSharedPreferences("Settings Data",Context.MODE_PRIVATE);
                            request = mappingRequest.getString("fav_trans_4", SettingActivity.DEFAULT_STRING);
                            if(request.equals("N/A") || request.equals("")) {
                            flag_map = false;
                            messageRequest = new ChatMessage();
                            messageRequest.setMe(true);
                            messageRequest.setMessage("Sorry, but you haven't defined any action! Please check the settings.");
                            }
                            else {
                                flag_map = true;
                            messageRequest = new ChatMessage();
                            messageRequest.setMe(true);
                            messageRequest.setMessage("Performing your favourite action : " + request);
                            }
                            displayMessageAndInsert(messageRequest);
                            break;
                    case 5:  mappingRequest = getSharedPreferences("Settings Data",Context.MODE_PRIVATE);
                            request = mappingRequest.getString("fav_trans_5", SettingActivity.DEFAULT_STRING);
                            if(request.equals("N/A") || request.equals("")) {
                            flag_map = false;
                            messageRequest = new ChatMessage();
                            messageRequest.setMe(true);
                            messageRequest.setMessage("Sorry, but you haven't defined any action! Please check the settings.");
                            }
                            else {
                                flag_map = true;
                            messageRequest = new ChatMessage();
                            messageRequest.setMe(true);
                            messageRequest.setMessage("Performing your favourite action : " + request);
                            }
                            displayMessageAndInsert(messageRequest);
                            break;
                    case 6: request = messageText;
                            flag_map = true;
                            break;
                    default:Log.d("error_map","Error while mapping requests!");

                }
                //------------------------------------------------------------------------------------------------

                path = getExternalFilesDir(null).getAbsolutePath();
                Bot bot = new Bot(BOTNAME, path);
                Chat chatSession = new Chat(bot);
                if(flag_map) {
                    response = chatSession.multisentenceRespond(request);

                    Log.d("response", response);

                    sc = new Scanner(response);
                    sc.useDelimiter("\\|");


                    //Response to be given to the client
                    clientResponse = sc.next();
                    Log.d("res", clientResponse);

                    //Remaining part of string to detect the appropriate function
                    if (sc.hasNext()) {
                        String functionSelection = sc.next();
                        Log.d("fres", functionSelection);
                        functionCall(functionSelection);
                    }

                    ChatMessage chatResponse = new ChatMessage();
                    chatResponse.setId(125);//dummy
                    chatResponse.setMessage(clientResponse);
                    chatResponse.setDate(sdf.format(new Date()));
                    chatResponse.setMe(false);


                    displayMessageAndInsert(chatResponse);
                }
            }
        });


    }

    private void functionCall(String functionSelection) {

        String mobileNo,amt;
        BankServices bks;
        sc = new Scanner(functionSelection);
        sc.useDelimiter("\\s");
        Log.d("rem", functionSelection);
        String functionPattern = sc.next();//Function Alphabet
        Log.d("f1res", functionPattern);

        bks = new BankServices(adapter,messagesContainer,ChatActivity.this);
        switch(functionPattern)
        {
            case "j": bks.billPayment();
                      break;
            case "k": mobileNo = sc.next();
                      amt = sc.next();
                      bks.recharge(mobileNo, amt);
                      break;
            case "l": amt = sc.next();
                      bks.recharge(amt);
                      break;
            case "tr":  String ben_acc_no = sc.next();
                        amt = sc.next();
                        bks.transfer(ben_acc_no,amt);
                        break;
            case "cri": bks.creditCardInfo();
                        break;
            case "cd":  bks.viewCustDetails();
                        break;
            case "ab":
                bks.dispAccountBalance();
                break;
            case "as":
                String no=sc.next();
                String what=sc.next();
                bks.dispAccountStatements(no,what);
                break;

        }

    }

    public void displayMessageAndInsert(ChatMessage message) {


        adapter.add(message);
        adapter.notifyDataSetChanged();
        scroll();
        dbHelper = new BankDbHelper(ChatActivity.this);
        SQLiteDatabase bankDb = dbHelper.getWritableDatabase();
        long result = dbHelper.addChat(message,bankDb);
        if(result<0)
        {
            Log.d("Error1","Error in inserting..");
        }
        else
        {
            Log.d("Success1","Chat message inserted successfully..");
        }
    }

    public void displayMessage(ChatMessage message) {

        Log.d("d1","display only");
        adapter.add(message);
        adapter.notifyDataSetChanged();
        scroll();

    }


    private void scroll() {
        messagesContainer.setSelection(messagesContainer.getCount() - 1);
    }

    private void loadDummyHistory(){

        boolean isMe;
        int isMeInt;
        String date;
        dbHelper = new BankDbHelper(ChatActivity.this);
        SQLiteDatabase bankDb = dbHelper.getWritableDatabase();
        Cursor getDataCursor = dbHelper.getAllChats(bankDb);




        chatHistory = new ArrayList<ChatMessage>();

        ChatMessage historyChat ;
        while(getDataCursor.moveToNext()){
            historyChat = new ChatMessage();
            String msg = getDataCursor.getString(1);
            String dateSt = getDataCursor.getString(2);
            isMeInt = getDataCursor.getInt(0);
            isMe = (isMeInt==1)?true:false;


            historyChat.setMe(isMe);
            historyChat.setMessage(msg);
            historyChat.setDate(dateSt);

            chatHistory.add(historyChat);


        }


        adapter = new ChatAdapter(ChatActivity.this, new ArrayList<ChatMessage>());
        messagesContainer.setAdapter(adapter);
        Log.d("trial","Trying....");
        for(int i=0; i<chatHistory.size(); i++) {
            ChatMessage message = chatHistory.get(i);
            displayMessage(message);
        }
    }



}






