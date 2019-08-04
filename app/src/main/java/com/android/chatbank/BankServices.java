package com.android.chatbank;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Created by satishc on 03/06/16.
 */
public class BankServices extends ChatActivity{

    private ListView msgContainer;
    private ChatAdapter cAdapter;
    private Activity context;
    private CountDownLatch latch;
    private static final String CPIN = "123456";
    private static final String ACCOUNT_NUMBER="ABC123";
    private BankDbHelper dbHelper;
   // private EditText cpin_input;
    private boolean cpinConfirmFlag = false,loop=true;

    BankServices(ChatAdapter cAdapter,ListView msgContainer,Activity context){
        this.cAdapter = cAdapter;
        this.msgContainer = msgContainer;
        this.context = context;
    }

    public void dispAccountStatements(String no,String what)  {

        showConfirmDialogBox("as",no,what);

    }

    public void dispAccountBalance()  {

        showConfirmDialogBox("ab");

    }

    public void billPayment() {

        //showConfirmDialogBox();
       // new RechargeAndBill().execute();
        chatResponseDisplay("Bill paid successfully!", 125, false);


    }



    public void recharge(String mobileNo, String amt) {
            if(mobileNo.length()!=10)
                chatResponseDisplay("I think you entered wrong mobile number! Please check", 124, false);
            else
                showConfirmDialogBox("r",amt);


    }

    public void recharge(String amt)  {

                showConfirmDialogBox("r",amt);

    }

    public void transfer(String ben_acc_no, String amt) {
                showConfirmDialogBox("tr",ben_acc_no,amt);
    }

    public void creditCardInfo() {
        showConfirmDialogBox("cri");
    }


    public void chatResponseDisplay(String s, int i,boolean isMe) {
        ChatMessage chatResponse = new ChatMessage();
        chatResponse.setId(i);//dummy
        chatResponse.setMessage(s);
        chatResponse.setDate(sdf.format(new Date()));
        chatResponse.setMe(isMe);

        displayMessageAndInsert(chatResponse);
    }



   // public void displayMessage(ChatMessage message) {
    public void displayMessageAndInsert(ChatMessage message) {
        cAdapter.add(message);
        cAdapter.notifyDataSetChanged();
        scroll();
        dbHelper = new BankDbHelper(context);
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

    private void scroll() {
        msgContainer.setSelection(msgContainer.getCount() - 1);
    }


    private void showConfirmDialogBox(final String... par)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Confirm Transaction ");
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.dialog_box_prompt, null);

        // Set up the input
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text

        builder.setView(promptsView);

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    cpinConfirmFlag = false;
                    loop = false;
                    dialog.cancel();
                }
            });

            builder.setPositiveButton("OK", null);
            // Set up the buttons
            final AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            EditText cpin_input = (EditText) dialog.findViewById(R.id.cpin_pass);
                            String inputPass = cpin_input.getText().toString();
                            if (inputPass.equals(CPIN)) {

                                //cpinConfirmFlag = true;
                                switch (par[0]) {
                                    case "ab":
                                        new AccountBalance().execute();
                                        break;
                                    case "as":
                                        new AccountStatements().execute(par[1],par[2]);
                                        break;
                                    case "r":
                                                 int val = Integer.parseInt(par[1]);
                                                 if (checkTransactionValue(val)) {

                                                     new RechargeAndBill().execute(par[1]);
                                                 } else
                                                     chatResponseDisplay("Sorry sir, transaction limit for the day exceeded! Please increase the daily transaction limit in settings.", 125, false);
                                                 break;
                                    case "tr":
                                                int valu = Integer.parseInt(par[2]);
                                                if (checkTransactionValue(valu)) {
                                                    new MoneyTransfer().execute(par[1], par[2]);
                                                } else
                                                    chatResponseDisplay("Sorry sir, transaction limit for the day exceeded! Please increase the daily transaction limit in settings.", 125, false);
                                                break;
                                    case "cri": new CreditCardInfo().execute();
                                                break;
                                }
                                dialog.dismiss();


                            } else {
                                Toast.makeText(context, "Invalid cPIN !", Toast.LENGTH_SHORT).show();
                                //cpinConfirmFlag = false;

                                return;

                            }
                        }
                    });

    }

    void incrementTransactionValue(int value){
        CURRENT_TRANSACTION_VALUE = CURRENT_TRANSACTION_VALUE + value;
        Log.d("t_val", "Value is :" + CURRENT_TRANSACTION_VALUE);
        SharedPreferences trChange = context.getSharedPreferences("TransactionChange", Context.MODE_PRIVATE);
        SharedPreferences.Editor trChangeEitor = trChange.edit();
        trChangeEitor.putInt("CurrentTransactionValue", CURRENT_TRANSACTION_VALUE);
        trChangeEitor.commit();
    }

    boolean checkTransactionValue(int value){
        boolean check = true;
        CURRENT_TRANSACTION_VALUE+=value;
        if(CURRENT_TRANSACTION_VALUE>TRANSACTION_LIMIT ){

            check = false;
        }
        CURRENT_TRANSACTION_VALUE-=value;
        return check;
    }

    public void viewCustDetails() {
        new CustomerDetails().execute();
    }

    class AccountStatements extends AsyncTask<String, String, JSONObject> {
        JSONParser jsonParser = new JSONParser();

        private ProgressDialog pDialog;

        private static final String LOGIN_URL = "http://192.168.0.104/bank_account_statements.php";
        private  String current_balance,amount;
        private static final String TAG_SUCCESS = "success";
        private int count=0;
        JSONArray myListsAll;
        JSONObject jsonobject;

        @Override
        protected void onPreExecute() {
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Fetching");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected JSONObject doInBackground(String... args) {

            try {

                HashMap<String, String> params = new HashMap<>();

                params.put("acc_no",ACCOUNT_NUMBER);
                params.put("no_of",args[0]);
                params.put("what",args[1]);
                Log.d("request", "starting");
                Log.d("request1",params.toString());

                JSONObject json = jsonParser.makeHttpRequest(LOGIN_URL, "POST", params);

                if (json != null) {
                    Log.d("JSON result", json.toString());

                    return json;
                }

            } catch (Exception e) {
                Log.d("ee1", "exception error");
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

            //if (success == 1) {
                String statement="Date\tDescription\t\tRef\t\tWithdrawals\tDeposits\tBalance\n";
try {
    myListsAll = json.getJSONArray("jsonData");
    Log.d("result123",myListsAll.toString());
}
catch (JSONException e){
    e.printStackTrace();
}
                for(int i=0;i<myListsAll.length();i++){
                    try {
                        jsonobject = (JSONObject) myListsAll.get(i);

                        statement=statement.concat(jsonobject.optString("date") + "\t" + jsonobject.optString("description") +
                                "\t\t" + jsonobject.optString("ref") + "\t" + jsonobject.optString("withdrawals") + "\t"
                                + jsonobject.optString("deposits") + "\t" + jsonobject.optString("balance") + "\n");
                    }catch (JSONException e){
                        e.printStackTrace();
                    }


                }

                chatResponseDisplay(statement, 125, false);
            //else{
              //  Log.d("Failure", message);
            //}
        }

    }
    class AccountBalance extends AsyncTask<String, String, JSONObject> {
        JSONParser jsonParser = new JSONParser();

        private ProgressDialog pDialog;

        private static final String LOGIN_URL = "http://192.168.0.104/bank_check_balance.php";
        private  String current_balance;
        private static final String TAG_SUCCESS = "success";
        private static final String TAG_MESSAGE = "message";


        @Override
        protected void onPreExecute() {
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Fetching...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected JSONObject doInBackground(String... args) {

            try {

                HashMap<String, String> params = new HashMap<>();

                params.put("acc_no",ACCOUNT_NUMBER);
                Log.d("request", "starting");
                Log.d("request1",params.toString());

                JSONObject json = jsonParser.makeHttpRequest(LOGIN_URL, "POST", params);
                Log.d("request2","here1");
                if (json != null) {

                    Log.d("JSON result", json.toString());

                    return json;
                }

            } catch (Exception e) {
                Log.d("ee1", "exception error");
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
                //Toast.makeText(context, json.toString(),
                //      Toast.LENGTH_LONG).show();

                try {
                    success = json.getInt(TAG_SUCCESS);
                    message = json.getString(TAG_MESSAGE);
                    current_balance = json.getString("balance");
                } catch (JSONException e) {
                    Log.d("err1","here2");
                    e.printStackTrace();
                }
            }

            if (success == 1) {
                Date date = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
                String fDate = sdf.format(date);// 12/01/2011 4:48:16 PM
                chatResponseDisplay("Your account balance at "+fDate+" is Rs."+current_balance, 125, false);
            }else{
                Log.d("Failure", message);
            }
        }

    }
    class RechargeAndBill extends AsyncTask<String, String, JSONObject> {
        JSONParser jsonParser = new JSONParser();

        private ProgressDialog pDialog;

        private static final String LOGIN_URL = "http://"+LoginActivity.NETWORK_IP_ADDRESS+"/bank_recharge_bill.php";
        private  String current_balance,amount;
        private static final String TAG_SUCCESS = "success";
        private static final String TAG_MESSAGE = "message";


        @Override
        protected void onPreExecute() {
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Attempting transaction...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected JSONObject doInBackground(String... args) {

            try {

                HashMap<String, String> params = new HashMap<>();

                params.put("acc_no",LoginActivity.ACCOUNT_NUMBER);
                params.put("amt", args[0]);
                amount = args[0];
                Log.d("request", "starting");
                Log.d("request1",params.toString());

                JSONObject json = jsonParser.makeHttpRequest(LOGIN_URL, "POST", params);

                if (json != null) {
                    Log.d("JSON result", json.toString());

                    return json;
                }

            } catch (Exception e) {
                Log.d("ee1", "exception error");
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
                //Toast.makeText(context, json.toString(),
                  //      Toast.LENGTH_LONG).show();

                try {
                    success = json.getInt(TAG_SUCCESS);
                    message = json.getString(TAG_MESSAGE);
                    current_balance = json.getString("balance");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (success == 1) {
                chatResponseDisplay("Recharge done successfully!", 125, false);
                chatResponseDisplay("Dear user, Rs."+amount+" has been debited from your account "+LoginActivity.ACCOUNT_NUMBER+" and your current balance is Rs."+current_balance, 126, false);
                incrementTransactionValue(Integer.parseInt(amount));

            }else{
                Log.d("Failure", message);
            }
        }

    }
    class MoneyTransfer extends AsyncTask<String, String, JSONObject> {
        JSONParser jsonParser = new JSONParser();

        private ProgressDialog pDialog;

        private static final String LOGIN_URL = "http://"+LoginActivity.NETWORK_IP_ADDRESS+"/bank_money_transfer.php";
        private  String current_balance,amount,ben_acc_no;
        private static final String TAG_SUCCESS = "success";
        private static final String TAG_MESSAGE = "message";


        @Override
        protected void onPreExecute() {
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Attempting transaction...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected JSONObject doInBackground(String... args) {

            try {

                HashMap<String, String> params = new HashMap<>();

                params.put("acc_no",LoginActivity.ACCOUNT_NUMBER);
                params.put("ben_acc_no", args[0]);
                params.put("amt", args[1]);
                amount = args[1];
                ben_acc_no = args[0];
                Log.d("request", "starting");
                Log.d("request1",params.toString());

                JSONObject json = jsonParser.makeHttpRequest(LOGIN_URL, "POST", params);

                if (json != null) {
                    Log.d("JSON result", json.toString());

                    return json;
                }

            } catch (Exception e) {
                Log.d("ee1", "exception error");
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
                //Toast.makeText(context, json.toString(),
                  //      Toast.LENGTH_LONG).show();

                try {
                    success = json.getInt(TAG_SUCCESS);
                    message = json.getString(TAG_MESSAGE);
                    current_balance = json.getString("balance");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (success == 1) {
                chatResponseDisplay("Rs. "+amount+" transfered to a/c. no.: "+ben_acc_no+" successfully!", 126, false);
                chatResponseDisplay("Dear user, Rs." + amount + " has been debited from your account " + LoginActivity.ACCOUNT_NUMBER + " and your current balance is Rs." + current_balance, 126, false);

            }else{

                Log.d("Failure", message);
                if(message.equals("Insufficient balance !"))
                    chatResponseDisplay("Dear user, your account "+LoginActivity.ACCOUNT_NUMBER+" don't have sufficient amount to be transfered.Sorry the transfer cannot be completed !", 127, false);
                else if(message.equals("Invalid account number !"))
                    chatResponseDisplay("Sorry, the transfer cannot be completed due to invalid  account no.: "+ben_acc_no+". Please check the beneficiary account number !", 128, false);
                else
                    chatResponseDisplay("Sorry, the transfer cannot be completed due to some internal error! Try again later.", 129, false);
            }
        }

    }


    class CreditCardInfo extends AsyncTask<String, String, JSONObject> {
        JSONParser jsonParser = new JSONParser();

        private ProgressDialog pDialog;
        private String credit_card_no,expiry_date,limit,payment_due;
        private static final String LOGIN_URL = "http://"+LoginActivity.NETWORK_IP_ADDRESS+"/bank_card_info.php";
        private static final String TAG_SUCCESS = "success";
        private static final String TAG_MESSAGE = "message";


        @Override
        protected void onPreExecute() {
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Fetching data...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected JSONObject doInBackground(String... args) {

            try {

                HashMap<String, String> params = new HashMap<>();

                params.put("acc_no",LoginActivity.ACCOUNT_NUMBER);

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
                Toast.makeText(context, json.toString(),
                      Toast.LENGTH_LONG).show();

                try {
                    success = json.getInt(TAG_SUCCESS);
                    message = json.getString(TAG_MESSAGE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (success == 1) {
                try {
                    credit_card_no= json.getString("credit_card_no");
                    expiry_date = json.getString("expiry_date");
                    limit = json.getString("credit_limit");
                    payment_due = json.getString("paymen_left");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                chatResponseDisplay("Dear user,your Credit Card Details are as follows :\nCredit Card No.: "+credit_card_no+"\nExpiry Date : "+expiry_date+"\nCard Limit : Rs."+limit+"\nPayment Due : Rs."+payment_due, 125, false);

            }else{
                chatResponseDisplay("Sorry sir,action cannot be completed due to some internal error! Please try again later.", 125, false);

            }
        }

    }

    class CustomerDetails extends AsyncTask<String, String, JSONObject> {
        JSONParser jsonParser = new JSONParser();

        private ProgressDialog pDialog;

        private static final String LOGIN_URL = "http://"+LoginActivity.NETWORK_IP_ADDRESS+"/bank_cust_info.php";
        private  String cust_name,cust_add,cust_phone;
        private static final String TAG_SUCCESS = "success";
        private static final String TAG_MESSAGE = "message";


        @Override
        protected void onPreExecute() {
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Attempting transaction...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected JSONObject doInBackground(String... args) {

            try {

                HashMap<String, String> params = new HashMap<>();

                params.put("acc_no", LoginActivity.ACCOUNT_NUMBER);

                Log.d("request", "starting");

                JSONObject json = jsonParser.makeHttpRequest(LOGIN_URL, "POST", params);

                if (json != null) {
                    Log.d("JSON result", json.toString());

                    return json;
                }

            } catch (Exception e) {
                Log.d("ee1", "exception error");
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
                Toast.makeText(context, json.toString(),
                      Toast.LENGTH_LONG).show();

                try {
                    success = json.getInt(TAG_SUCCESS);
                    message = json.getString(TAG_MESSAGE);
                    cust_name= json.getString("cust_name");
                    cust_add= json.getString("cust_address");
                    cust_phone= json.getString("cust_phone");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (success == 1) {
                chatResponseDisplay("Yes sir,your name is :"+cust_name+"\nYou live at : "+cust_add+"\nContact number : "+cust_phone+"\nAccount no. : "+LoginActivity.ACCOUNT_NUMBER, 125, false);


            }else{
                Log.d("Failure", message);
                chatResponseDisplay("Your Account no. : " + LoginActivity.ACCOUNT_NUMBER+"\nThat's all i can tell you due to some technical issue.", 125, false);

            }
        }

    }

}
