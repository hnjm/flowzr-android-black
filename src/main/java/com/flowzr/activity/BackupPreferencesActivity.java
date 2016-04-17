/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Emmanuel Florent - port to Android API 11+
 ******************************************************************************/
package com.flowzr.activity;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.flowzr.R;
import com.flowzr.dialog.FolderBrowser;
import com.flowzr.export.Export;
import com.flowzr.export.dropbox.Dropbox;
import com.flowzr.utils.MyPreferences;
import com.flowzr.utils.PinProtection;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.plus.Plus;

public class BackupPreferencesActivity extends PreferenceActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    //private static final String[] ACCOUNT_TYPE = new String[] {GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE};

    private static final int REQUEST_CODE_RESOLUTION = 3;
    private static final int SELECT_DATABASE_FOLDER = 100;
    private static final int CHOOSE_DRIVE_ACCOUNT = 101;
    private static final int UNLINK_DRIVE = 102;
    private static int drive_action=0; // to fw to onConnect
    private GoogleApiClient mGoogleApiClient;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);   
		addPreferencesFromResource(R.xml.backup_preferences);

        mGoogleApiClient= new GoogleApiClient.Builder(this)
                .addApi(Plus.API)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
        PreferenceScreen preferenceScreen = getPreferenceScreen();

        Preference pDatabaseBackupFolder = preferenceScreen.findPreference("database_backup_folder");
        pDatabaseBackupFolder.setOnPreferenceClickListener(new OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                selectDatabaseBackupFolder();
                return true;
            }
        });
        Preference pAuthDropbox = preferenceScreen.findPreference("dropbox_authorize");
        pAuthDropbox.setOnPreferenceClickListener(new OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                authDropbox();
                return true;
            }
        });
        Preference pDeauthDropbox = preferenceScreen.findPreference("dropbox_unlink");
        pDeauthDropbox.setOnPreferenceClickListener(new OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                deAuthDropbox();
                return true;
            }
        });

        Preference pDriveAccount = preferenceScreen.findPreference("google_drive_backup_account");
        pDriveAccount.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                chooseDriveAccount();
                return true;
            }
        });
        Preference pDriveAccountUnlink = preferenceScreen.findPreference("google_unlink");
        pDriveAccountUnlink.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                deAuthDrive();
                return true;
            }
        });


        linkToDropbox();
        setCurrentDatabaseBackupFolder();
        renderDriveAccount();
        // @TODO enable google drive autobackup
        Preference pDriveAutoUpload = preferenceScreen.findPreference("google_drive_upload_autobackup");
        pDriveAutoUpload.setEnabled(false);
	}

    private String getDriveAccountAndStoreInPrefs() {
        if (mGoogleApiClient!=null) {
            if (mGoogleApiClient.isConnected()) {
                try {
                    String accountName = Plus.AccountApi.getAccountName(mGoogleApiClient);
                    if (accountName != null) {
                        MyPreferences.setGoogleDriveAccount(this, accountName);
                        return accountName;
                    }
                } catch (Exception e) {

                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private void renderDriveAccount() {
        Preference pDriveAccount = getPreferenceScreen().findPreference("google_drive_backup_account");
        String driveAccount=MyPreferences.getGoogleDriveAccount(this);
        if (driveAccount!=null) {
            pDriveAccount.setSummary(driveAccount);
        } else {
            pDriveAccount.setSummary(R.string.google_drive_backup_account_summary);
        }
    }


    private void chooseDriveAccount() {
        drive_action=CHOOSE_DRIVE_ACCOUNT;
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        } else {
            mGoogleApiClient.clearDefaultAccountAndReconnect();
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();
        }
    }

    private void deAuthDrive() {
        MyPreferences.setGoogleDriveAccount(this,null);
        renderDriveAccount();
        if (mGoogleApiClient.isConnected()) {
            drive_action=0;
            Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
            //Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            //mGoogleApiClient.clearDefaultAccountAndReconnect();
            mGoogleApiClient.disconnect();
        } else {
            drive_action=UNLINK_DRIVE;
            mGoogleApiClient.connect();
        }

    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {

        // Called whenever the API client fails to connect
        if (drive_action==CHOOSE_DRIVE_ACCOUNT ) {

            if (!result.hasResolution()) {

                // show the localized error dialog.
                GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
                return;
            }
            try {
                result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (drive_action==CHOOSE_DRIVE_ACCOUNT) {
            mGoogleApiClient.clearDefaultAccountAndReconnect();
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();
        } else if (drive_action==UNLINK_DRIVE) {
            Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
            mGoogleApiClient.disconnect();
        } else {
            getDriveAccountAndStoreInPrefs();
            renderDriveAccount();
        }
        drive_action=0;
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Toast.makeText(this, R.string.gdocs_connection_failed, Toast.LENGTH_LONG).show();
    }


    private void linkToDropbox() {
        boolean dropboxAuthorized = MyPreferences.isDropboxAuthorized(this);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.findPreference("dropbox_unlink").setEnabled(dropboxAuthorized);
        preferenceScreen.findPreference("dropbox_upload_backup").setEnabled(dropboxAuthorized);
        preferenceScreen.findPreference("dropbox_upload_autobackup").setEnabled(dropboxAuthorized);
    }

    private void selectDatabaseBackupFolder() {
        Intent intent = new Intent(this, FolderBrowser.class);
        intent.putExtra(FolderBrowser.PATH, getDatabaseBackupFolder());
        startActivityForResult(intent, SELECT_DATABASE_FOLDER);
    }
    
    private String getDatabaseBackupFolder() {
        return Export.getBackupFolder(this).getAbsolutePath();
    }

    private void setCurrentDatabaseBackupFolder() {
        Preference pDatabaseBackupFolder = getPreferenceScreen().findPreference("database_backup_folder");
        String summary = getString(R.string.database_backup_folder_summary, getDatabaseBackupFolder());
        pDatabaseBackupFolder.setSummary(summary);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==REQUEST_CODE_RESOLUTION && resultCode==RESULT_OK) {
            mGoogleApiClient.connect();
        }
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SELECT_DATABASE_FOLDER:
                    String databaseBackupFolder = data.getStringExtra(FolderBrowser.PATH);
                    MyPreferences.setDatabaseBackupFolder(this, databaseBackupFolder);
                    setCurrentDatabaseBackupFolder();
                    break;
            }
        }
    }

    Dropbox dropbox = new Dropbox(this);

    private void authDropbox() {
        dropbox.startAuth();
    }

    private void deAuthDropbox() {
        dropbox.deAuth();
        linkToDropbox();
    }



    @Override
	protected void onPause() {
		super.onPause();
        if (mGoogleApiClient!=null) {
            mGoogleApiClient.disconnect();
        }
		//PinProtection.lock(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		PinProtection.unlock(this);
        if (mGoogleApiClient == null) {
            mGoogleApiClient= new GoogleApiClient.Builder(this)
                    .addApi(Plus.API)
                    .addApi(Drive.API)
                    .setAccountName(MyPreferences.getGoogleDriveAccount(this))
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

        }
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        dropbox.completeAuth();
        linkToDropbox();
    }
    
}
