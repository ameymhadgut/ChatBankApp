package com.android.chatbank;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {

    static  String ACCOUNT_NUMBER = "";
    static  String CPIN = "";
    private Button mloginBtn ;
    private static int attempts=3;
    private EditText mAccNumber,mCPin;
    static final String NETWORK_IP_ADDRESS = "192.168.0.104";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        mloginBtn = (Button) findViewById(R.id.loginButton);
        mloginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAccNumber = (EditText) findViewById(R.id.loginNumber);
                mCPin = (EditText) findViewById(R.id.loginPin);
                String accNumber = mAccNumber.getText().toString();
                String cPin = mCPin.getText().toString();

                if(accNumber.equals("") || cPin.equals(""))
                    showEmptyAlert();
                else
                    new LoginCheck().execute(accNumber, cPin);

            }
        });
    }

    private void showEmptyAlert() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        alertDialogBuilder.setTitle("Invalid Login");
        alertDialogBuilder.setMessage("Required field(s) missing !");
        // set dialog message
        alertDialogBuilder
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //logout to login page
                                //ChatActivity.ACCOUNT_NUMBER = "";
                                //ChatActivity.CPIN="";


                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    //2.0 and above
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    // Before 2.0
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

   /* @Override
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/


    class LoginCheck extends AsyncTask<String, String, JSONObject> {
        JSONParser jsonParser = new JSONParser();

        private ProgressDialog pDialog;
        private String acc_no,cpin;
        private static final String LOGIN_URL = "http://"+LoginActivity.NETWORK_IP_ADDRESS+"/bank_login_check.php";
        private static final String TAG_SUCCESS = "success";
        private static final String TAG_MESSAGE = "message";


        @Override
        protected void onPreExecute() {
            pDialog = new ProgressDialog(LoginActivity.this);
            pDialog.setMessage("Authenticating...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected JSONObject doInBackground(String... args) {

            try {

                HashMap<String, String> params = new HashMap<>();

                params.put("acc_no",args[0]);
                params.put("cpin",args[1]);
                acc_no = args[0];
                cpin = args[1];
                Log.d("request", "starting");
                Log.d("request1",params.toString());

                JSONObject json = jsonParser.makeHttpRequest(LOGIN_URL, "POST", params);

                if (json != null) {
                    Log.d("JSON result", json.toString());

                    return json;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(JSONObject json) {

            int success = 0;
            String message = "";

            if (pDialog != null && pDialog.isShowing()) {
                pDialog.dismiss();
            }

            if (json != null) {
               // Toast.makeText(LoginActivity.this, json.toString(),
                 //       Toast.LENGTH_LONG).show();

                try {
                    success = json.getInt(TAG_SUCCESS);
                    message = json.getString(TAG_MESSAGE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (success == 1) {
                ACCOUNT_NUMBER =acc_no;
                CPIN = cpin;
                Intent iToChat = new Intent(LoginActivity.this,ChatActivity.class);
                startActivity(iToChat);

            }else{
                if(message.equals("Invalid Cpin!") || message.equals("Problem in fetching customer id !")) {
                    showAlert(attempts);
                    attempts--;
                }
                 Log.d("failure",message);
            }
        }

    }

    private void showAlert(int at) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        alertDialogBuilder.setTitle("Invalid Login");
        if(at>0)
            alertDialogBuilder.setMessage("You have "+at+" more attempts! After that your cPin will be blocked.");
        else
            alertDialogBuilder.setMessage("Your cPin is blocked! Please consult the bank executive.");
        // set dialog message
        alertDialogBuilder
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //logout to login page
                                //ChatActivity.ACCOUNT_NUMBER = "";
                                //ChatActivity.CPIN="";


                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

}
