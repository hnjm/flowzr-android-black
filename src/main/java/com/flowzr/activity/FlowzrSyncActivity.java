/*
 * Copyright (c) 2012 Emmanuel Florent.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.flowzr.activity;


import android.accounts.Account;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.flowzr.R;
import com.flowzr.export.flowzr.FlowzrBillTask;
import com.flowzr.export.flowzr.FlowzrSyncEngine;
import com.flowzr.export.flowzr.FlowzrSyncOptions;
import com.flowzr.export.flowzr.FlowzrSyncTask;
import com.flowzr.utils.MyPreferences;
import com.flowzr.utils.PinProtection;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.util.Date;

import static com.flowzr.service.FlowzrAutoSyncScheduler.scheduleNextAutoSync;
import static com.flowzr.utils.NetworkUtils.isOnline;


public class FlowzrSyncActivity extends AppCompatActivity {
	
	public static final String PROPERTY_REG_ID = "registration_id";
    public Account useCredential;
	DefaultHttpClient http_client ;
	public String TAG="flowzr";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private static final int FLOWZR_PREFERENCES = 9001;
	public String regid="";
	GoogleCloudMessaging gcm; 
	  
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);

    	if (requestCode == MainActivity.CHANGE_PREFERENCES) {
            scheduleNextAutoSync(this);
        }
    }

    public void setIsFinished() {
        setReady();
        if (MainActivity.activity !=null) {
            runOnUiThread(new Runnable() {
				public void run() {
					((MainActivity) MainActivity.activity).refreshCurrentTab();
				}
			});

        }
    }

    public void setReady() {
        runOnUiThread(new Runnable() {
			public void run() {
				TextView tv = (TextView) findViewById(R.id.sync_was);
				long lastSyncLocalTimestamp = MyPreferences.getFlowzrLastSync(FlowzrSyncActivity.this);
				tv.setText(getString(R.string.flowzr_sync_was) + " " + new Date(lastSyncLocalTimestamp).toLocaleString());
				CheckBox chk = (CheckBox) findViewById(R.id.chk_sync_from_zero);
				chk.setChecked(false);
				setProgressBarIndeterminateVisibility(false);
			}
		});
    }


    public void renderLastTime() {
		long lastTime=MyPreferences.getFlowzrLastSync(getApplicationContext());
	    TextView tv = (TextView) findViewById(R.id.sync_was);
	    tv.setText(getString(R.string.flowzr_sync_was) + " " + new Date(lastTime).toLocaleString());			
	}	
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.flowzr_actions, menu);
        return true;
    }
    
    public void startSync () {
    	if (FlowzrSyncEngine.isRunning) {
    		Toast.makeText(this, R.string.flowzr_sync_inprogress, Toast.LENGTH_LONG).show();
    		return;
    	}
	    String accountName=MyPreferences.getFlowzrAccount(getApplicationContext());
        if (accountName == null) {     	
        	startActivity(new Intent(this,FlowzrPreferencesActivity.class));
        	Toast.makeText(this, R.string.flowzr_choose_account, Toast.LENGTH_SHORT).show();         	
        	return;
        }
		if (isOnline(FlowzrSyncActivity.this)) {
	        checkPlayServices();    
		} else {         			
			showErrorPopup(FlowzrSyncActivity.this, R.string.flowzr_sync_error_no_network);            			           			
			return;
		}   
  	
		if (FlowzrSyncEngine.isRunning) {
			showErrorPopup(FlowzrSyncActivity.this, R.string.flowzr_sync_auth_inprogress);  				       
			return;
		}
		
    	Thread myThread = new Thread(new Runnable(){
    	    @Override
    	    public void run()
    	    {
    	    	new FlowzrSyncTask(FlowzrSyncActivity.this).execute();
    	    }
    	});
    	myThread.start();
        Toast.makeText(this, R.string.flowzr_sync_inprogress, Toast.LENGTH_SHORT).show();    	
    	finish();
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
			case android.R.id.home:
	        {
	        	if (this.isTaskRoot()) {
	        		startActivity(new Intent (this,MainActivity.class));
	        		finish();
	        	} else {
	        		onBackPressed();
	        	}
	        	return true;
			}

	    	case R.id.action_sync: 
	        	startSync();            	
	            return true;
	        case R.id.action_cancel: 
            	FlowzrSyncEngine.isCanceled=true;
    			FlowzrSyncEngine.isRunning=false;            	
                setResult(RESULT_CANCELED);    
                
    			new AlertDialog.Builder(FlowzrSyncActivity.this)
    			.setMessage(R.string.cancel)
    			.setTitle(R.string.info)
    			.setPositiveButton(R.string.ok, 
    					new DialogInterface.OnClickListener() {
                    		public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(getApplicationContext(), MainActivity.class));                
                                finish();
                    		}
    					})
    			.setCancelable(true)
    			.create().show();
	            return true;
	        case R.id.action_settings:  
				Intent intent = new Intent(this.getApplicationContext(), FlowzrPreferencesActivity.class);
			    startActivityForResult(intent, FLOWZR_PREFERENCES);		        
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

    @Override
    public void onDestroy(){
        super.onDestroy();

    }

	protected void initToolbar() {
		final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		final ActionBar actionBar = getSupportActionBar();

		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}


	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	setContentView(R.layout.flowzr_sync);
		//setupDrawer();
		initToolbar();
    	renderLastTime();
    	CheckBox chkForce= (CheckBox) findViewById(R.id.chk_sync_from_zero);
        chkForce.setOnClickListener(new View.OnClickListener() {        		
        	public void onClick(View v) {
        			resetLastTime();
        			renderLastTime();
			}
			});
        Button syncButton = (Button) findViewById(R.id.sync);
        syncButton.setOnClickListener(new View.OnClickListener() {        		
        	public void onClick(View v) {
        			startSync();     			
        		}
		});
        Button textViewAbout = (Button) findViewById(R.id.buySubscription);
        textViewAbout.setOnClickListener(new View.OnClickListener() {        		
        	public void onClick(View v) {
	        	    String accountName=MyPreferences.getFlowzrAccount(getApplicationContext());
	                if (accountName == null) {
	                    Toast.makeText(FlowzrSyncActivity.this, R.string.flowzr_choose_account, Toast.LENGTH_SHORT).show(); 	         
	                	return;
	                }
	        		if (isOnline(FlowzrSyncActivity.this)) {
	        	        checkPlayServices();
	        		} else {         			
	        			showErrorPopup(FlowzrSyncActivity.this, R.string.flowzr_sync_error_no_network);            			           			
	        			return;
	        		}  
	        		//
	        		 Toast.makeText(FlowzrSyncActivity.this, R.string.flowzr_sync_auth_inprogress, Toast.LENGTH_SHORT).show();
	        		 FlowzrBillTask ft= new FlowzrBillTask(FlowzrSyncActivity.this);
	        		 ft.execute();	        		
	        		 //visitFlowzr(accountName);
  		      		}
		});

        Button textViewAboutAnon = (Button) findViewById(R.id.visitFlowzr);
        textViewAboutAnon.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
	        		if (isOnline(FlowzrSyncActivity.this)) {
                        visitFlowzr(MyPreferences.getFlowzrAccount(getApplicationContext()));
	        		} else {
	    				showErrorPopup(FlowzrSyncActivity.this, R.string.flowzr_sync_error_no_network);
	        		}
  	      	}
		});

        TextView textViewNotes = (TextView) findViewById(R.id.flowzrPleaseNote);
        textViewNotes.setMovementMethod(LinkMovementMethod.getInstance());
        textViewNotes.setText(Html.fromHtml(getString(R.string.flowzr_terms_of_use)));
        
        if (MyPreferences.isAutoSync(this)) {
	        if (checkPlayServices()) {
	            gcm = GoogleCloudMessaging.getInstance(this);
	            regid = getRegistrationId(getApplicationContext());
	
	            if (regid.equals("")) {
	                registerInBackground();
	            }
	            Log.i(TAG,"Google Cloud Messaging registered as :" + regid);
	        } else {
	            Log.i(TAG, "No valid Google Play Services APK found.");
	        }
        }
		initToolbar();
	}

	public void resetLastTime () {
    	SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
    	if (editor!=null) {
    		editor.putLong("PROPERTY_LAST_SYNC_TIMESTAMP", 0);
    		editor.apply();
    	}
	}
	
	private String getRegistrationId(Context context) {
	    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	    String registrationId = prefs.getString(PROPERTY_REG_ID, "");
	    if (registrationId.equals("")) {
	        Log.i(TAG, "GCM Registration not found in prefs.");
	        return "";
	    }
	    // Check if app was updated; if so, it must clear the registration ID
	    // since the existing regID is not guaranteed to work with the new
	    // app version.
	    String registeredVersion = prefs.getString(FlowzrSyncOptions.PROPERTY_APP_VERSION, "");
	    String currentVersion;
		try {
			currentVersion = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
		    if (!registeredVersion.equals(currentVersion)) {
		        Log.i(TAG, "App version changed.");
		        return "";
		    }
		} catch (NameNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	    return registrationId;
	}

	
	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and app versionCode in the application's
	 * shared preferences.
	 */
	@SuppressWarnings("unchecked")
	private void registerInBackground() {
	    new AsyncTask() {

			@Override
			protected Object doInBackground(Object... params) {
				 String msg;
		            try {
		                if (gcm == null) {
		                    gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
		                }
				        Log.i(TAG, "Registering GCM in background ...");		                
		                regid = gcm.register(FlowzrSyncOptions.GCM_SENDER_ID);
		                msg = "Device registered, registration ID=" + regid;

		                // You should send the registration ID to your server over HTTP,
		                // so it can use GCM/HTTP or CCS to send messages to your app.
		                // The request to your server should be authenticated if your app
		                // is using accounts.
		                sendRegistrationIdToBackend();

		                // For this demo: we don't need to send it because the device
		                // will send upstream messages to a server that echo back the
		                // message using the 'from' address in the message.

		                // Persist the regID - no need to register again.
		                storeRegistrationId(getApplicationContext(), regid);
		            } catch (IOException ex) {
		                msg = "Error :" + ex.getMessage();
		                // If there is an error, don't just keep trying to register.
						// Require the user to click a button again, or perform
		                // exponential back-off.
				        Log.i(TAG, msg);		                
		            }
		            return msg;
				
				
			}
	    }.execute(null, null, null);
	   
	}
    private void sendRegistrationIdToBackend() {
    	//GCM registration key is sent somewhere else
      }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String appVersion;
		try {
			appVersion = context.getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
	        Log.i(TAG, "Saving regId on app version " + appVersion);
	        SharedPreferences.Editor editor = prefs.edit();
	        editor.putString(PROPERTY_REG_ID, regId);
	        editor.putString(FlowzrSyncOptions.PROPERTY_APP_VERSION, appVersion);
	        editor.apply();
		} catch (NameNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
    
    
    private void visitFlowzr(String accountName) {
        String url=FlowzrSyncEngine.FLOWZR_BASE_URL + "/paywall/";
        if (accountName !=null) {
            url=url + accountName;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

	private void showErrorPopup(Context context, int message) {
		new AlertDialog.Builder(context)
		.setMessage(message)
		.setTitle(R.string.error)
		.setPositiveButton(R.string.ok, null)
		.setCancelable(true)
		.create().show();
	}	

	 
	private boolean checkPlayServices() {
	    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
	    if (resultCode != ConnectionResult.SUCCESS) {
	        if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
	            GooglePlayServicesUtil.getErrorDialog(resultCode, this,
	                    PLAY_SERVICES_RESOLUTION_REQUEST).show();
	        } else {
	            Log.w(TAG, "This device is does not support Google Play Services.");
	            finish();
	        }
	        return false;
	    }
	    return true;
	}
	
    @Override
    protected void onPause() {
        super.onPause();
        //PinProtection.lock(this.getApplicationContext());
    }

    @Override
	protected void onResume() {
		super.onResume();
        PinProtection.unlock(this);
	}
}
