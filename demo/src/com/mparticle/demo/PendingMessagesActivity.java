package com.mparticle.demo;

import android.app.ListActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;

import com.mparticle.MessageDatabase;
import com.mparticle.MessageDatabase.MessageTable;

public class PendingMessagesActivity extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MessageDatabase mmDB = new MessageDatabase(this);
        SQLiteDatabase db = mmDB.getReadableDatabase();

        Cursor selectCursor = db.query("messages", null, null, null, null, null, MessageTable.MESSAGE_TIME
                + " desc, _id desc");

        String[] from = new String[] { MessageTable.SESSION_ID, MessageTable.MESSAGE_TIME, MessageTable.MESSAGE_TYPE,
                MessageTable.MESSAGE, MessageTable.UUID, MessageTable.UPLOAD_STATUS };
        int[] to = { R.id.sessionId, R.id.msgTime, R.id.msgType, R.id.msgMsg, R.id.msgId, R.id.msgStatus };

        // NOTE: this Activity is doing SQL directly on the main UI thread,
        // which you would never do in production code
        @SuppressWarnings("deprecation")
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.message_list_entry, selectCursor,
                from, to);

        setListAdapter(adapter);
        db.close();

    }

}