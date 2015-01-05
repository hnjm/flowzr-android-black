package com.flowzr.export.flowzr;


import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.flowzr.activity.FlowzrSyncActivity;
import com.flowzr.export.flowzr.FlowzrSyncEngine;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.util.prefs.Preferences;

/**
* IntentService responsible for handling GCM messages.
*/

	public class GCMIntentService extends IntentService {

		static String TAG="flowzr";
		
		public static final int NOTIFICATION_ID = 1;
	    NotificationCompat.Builder builder;

	    public GCMIntentService() {
	        super("GCMIntentService");
	    }

        private void handleMessage(Intent intent) {
            String b = intent.getStringExtra("force");
            if (b.equals("true")) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                if (editor!=null) {
                    editor.putLong("PROPERTY_LAST_SYNC_TIMESTAMP", 0);
                    editor.commit();
                }
            }
        }

	    @Override
	    protected void onHandleIntent(Intent intent) {
	        Bundle extras = intent.getExtras();
	        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
	        String messageType = gcm.getMessageType(intent);
	        if (!extras.isEmpty()) {
                String action=intent.getAction();
                if (action.equals("com.google.android.c2dm.intent.RECEIVE")) {
                    handleMessage(intent);
                }


	        	if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {	    		
	        		if (FlowzrSyncEngine.isRunning) {
		        		Log.i(TAG,"sync already in progess");
	        			return;
	        		}
	        		Log.i(TAG,"starting sync from GCM");
	        		new FlowzrSyncTask(getApplicationContext()).execute();
	            }
	        }
	    }
	}
	
	
