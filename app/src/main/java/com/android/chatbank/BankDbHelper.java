package com.android.chatbank;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by satishc on 06/06/16.
 */
public class BankDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "IndiaBank";
    private static final String TABLE_NAME = "ChatData";
    private static final String CHAT_ID = "_id";
    private static final String IS_ME = "me";
    private static final String MESSAGE = "message";
    private static final String CHAT_DATE = "chat_date";
    private static final int DATABASE_VERSION = 1;

    private static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+" ("+CHAT_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+IS_ME+" INTEGER, "+MESSAGE+" TEXT, "+CHAT_DATE+" DATETIME);";



    public BankDbHelper(Context context){
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
       try {
           db.execSQL(CREATE_TABLE);
           Log.d("first_db","Database created");
       }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public long addChat(ChatMessage message,SQLiteDatabase bankDb) {
        long res=0;
        int isMe = message.getIsMe()?1:0;
        ContentValues ct = new ContentValues();
        ct.put(IS_ME, isMe);
        ct.put(MESSAGE,message.getMessage());
        ct.put(CHAT_DATE,message.getDate());
        res = bankDb.insert(TABLE_NAME,null,ct);
        return res;
    }

    public Cursor getAllChats(SQLiteDatabase bankDb) {
        Cursor cr ;

        String[] col = {IS_ME,MESSAGE,CHAT_DATE};
        cr = bankDb.query(TABLE_NAME,col,null,null,null,null,null);
        return cr;
    }
}
