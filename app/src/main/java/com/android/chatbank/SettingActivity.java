package com.android.chatbank;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

public class SettingActivity extends AppCompatActivity {

    private static final String FIRST_STRING = "";
    static final String DEFAULT_STRING = "N/A";
    private SeekBar transactionLimitBar ;
    private EditText transactionLimit;
    private EditText fav_trans_1,fav_trans_2,fav_trans_3,fav_trans_4,fav_trans_5;
    private Button saveBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences firstTimeSettings = getSharedPreferences(ChatActivity.PREFS_NAME, Context.MODE_PRIVATE);
        if(firstTimeSettings.getBoolean("my_first_time",true)){

            Log.d("first_time1", "Setting activity first tym!");

            transactionLimit = (EditText) findViewById(R.id.finalLimit);
            transactionLimit.setText("0");

            SharedPreferences savingSettingData = getSharedPreferences("Settings Data", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = savingSettingData.edit();


            editor.putInt("transaction_limit", 0);
            editor.putString("fav_trans_1", FIRST_STRING);
            editor.putString("fav_trans_2", FIRST_STRING);
            editor.putString("fav_trans_3",FIRST_STRING);
            editor.putString("fav_trans_4",FIRST_STRING);
            editor.putString("fav_trans_5", FIRST_STRING);

            editor.commit();

            SharedPreferences.Editor editor_first = firstTimeSettings.edit();
            Log.d("check_value1", "Setting activity :" + firstTimeSettings.getBoolean("my_first_time", true));
            editor_first.putBoolean("my_first_time", false);
            editor_first.commit();
            Log.d("check_value2", "Setting activity :" + firstTimeSettings.getBoolean("my_first_time", true));
        }
        else
        {
            Log.d("load_history", "Setting activity load history tym!");
            loadHistory();
        }
        dailyTransactionLimit();
        saveButtonAction();

    }

    private void loadHistory() {

        transactionLimitBar = (SeekBar) findViewById(R.id.limitBar);
        transactionLimit = (EditText) findViewById(R.id.finalLimit);
        fav_trans_1 = (EditText) findViewById(R.id.fav_t_1);
        fav_trans_2 = (EditText) findViewById(R.id.fav_t_2);
        fav_trans_3 = (EditText) findViewById(R.id.fav_t_3);
        fav_trans_4 = (EditText) findViewById(R.id.fav_t_4);
        fav_trans_5 = (EditText) findViewById(R.id.fav_t_5);


        SharedPreferences loadingHistory = getSharedPreferences("Settings Data",Context.MODE_PRIVATE);

        int savedLimit = loadingHistory.getInt("transaction_limit", 0);
        transactionLimitBar.setProgress(savedLimit);
        transactionLimit.setText("" +savedLimit);
        fav_trans_1.setText(loadingHistory.getString("fav_trans_1", DEFAULT_STRING));
        fav_trans_2.setText(loadingHistory.getString("fav_trans_2", DEFAULT_STRING));
        fav_trans_3.setText(loadingHistory.getString("fav_trans_3", DEFAULT_STRING));
        fav_trans_4.setText(loadingHistory.getString("fav_trans_4", DEFAULT_STRING));
        fav_trans_5.setText(loadingHistory.getString("fav_trans_5", DEFAULT_STRING));


    }

    private void saveButtonAction() {
            saveBtn = (Button) findViewById(R.id.saveBtn);

            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fav_trans_1 = (EditText) findViewById(R.id.fav_t_1);
                    fav_trans_2 = (EditText) findViewById(R.id.fav_t_2);
                    fav_trans_3 = (EditText) findViewById(R.id.fav_t_3);
                    fav_trans_4 = (EditText) findViewById(R.id.fav_t_4);
                    fav_trans_5 = (EditText) findViewById(R.id.fav_t_5);

                    transactionLimit = (EditText) findViewById(R.id.finalLimit);
                    int tLimit = Integer.parseInt(transactionLimit.getText().toString());

                    SharedPreferences savingSettingData = getSharedPreferences("Settings Data", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = savingSettingData.edit();

                    editor.putInt("transaction_limit",tLimit);
                    editor.putString("fav_trans_1", fav_trans_1.getText().toString());
                    editor.putString("fav_trans_2",fav_trans_2.getText().toString());
                    editor.putString("fav_trans_3",fav_trans_3.getText().toString());
                    editor.putString("fav_trans_4",fav_trans_4.getText().toString());
                    editor.putString("fav_trans_5",fav_trans_5.getText().toString());

                    editor.commit();

                    Toast.makeText(SettingActivity.this,"Settings saved successfully!",Toast.LENGTH_SHORT).show();
                }
            });


    }

    private void dailyTransactionLimit() {
        transactionLimitBar = (SeekBar) findViewById(R.id.limitBar);
        transactionLimit = (EditText) findViewById(R.id.finalLimit);

        SharedPreferences extractingData = getSharedPreferences("Settings Data",Context.MODE_PRIVATE);
        int tLimit = extractingData.getInt("transaction_limit",0);

        transactionLimit.setText(""+tLimit);

        transactionLimitBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int progressValue=0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressValue = progress;
                transactionLimit.setText(""+progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                transactionLimit.setText(""+progressValue);
            }
        });
    }


}
