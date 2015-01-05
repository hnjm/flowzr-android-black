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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ActivityNotFoundException;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.flowzr.R;
import com.flowzr.dialog.FolderBrowser;
import com.flowzr.export.Export;
import com.flowzr.export.dropbox.Dropbox;
import com.flowzr.utils.MyPreferences;
import com.flowzr.utils.PinProtection;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;

public class BackupPreferencesActivity extends PreferenceActivity {

    private static final String[] ACCOUNT_TYPE = new String[] {GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE};

    private static final int CHOOSE_ACCOUNT = 101;
    private static final int SELECT_DATABASE_FOLDER = 100;

    GoogleAccountManager googleAccountManager;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);   
		addPreferencesFromResource(R.xml.backup_preferences);

        googleAccountManager = new GoogleAccountManager(this);

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
        
        linkToDropbox();
        setCurrentDatabaseBackupFolder();
        selectAccount();        
	}

    private void chooseDriveAccount() {
        try {
            Account selectedAccount = getDriveSelectedAccount();
            Intent intent = AccountPicker.newChooseAccountIntent(selectedAccount, null, ACCOUNT_TYPE, true,
                    null, null, null, null);
            startActivityForResult(intent, CHOOSE_ACCOUNT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.google_drive_account_select_error, Toast.LENGTH_LONG).show();
        }
    }

    private Account getDriveSelectedAccount() {
        Account selectedAccount = null;
        String account = MyPreferences.getGoogleDriveAccount(this);
        if (account != null) {
            selectedAccount = googleAccountManager.getAccountByName(account);
        }
        return selectedAccount;
    }

    private Account getFlowzrSelectedAccount() {
        Account selectedAccount = null;
        String account = MyPreferences.getFlowzrAccount(this);
        if (account != null) {
            selectedAccount = googleAccountManager.getAccountByName(account);
        }
        return selectedAccount;
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
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SELECT_DATABASE_FOLDER:
                    String databaseBackupFolder = data.getStringExtra(FolderBrowser.PATH);
                    MyPreferences.setDatabaseBackupFolder(this, databaseBackupFolder);
                    setCurrentDatabaseBackupFolder();
                    break;
                case CHOOSE_ACCOUNT:
                    if (data != null) {
                        Bundle b = data.getExtras();
                        String accountName = b.getString(AccountManager.KEY_ACCOUNT_NAME);
                        Log.d("Preferences", "Selected account: " + accountName);
                        if (accountName != null && accountName.length() > 0) {
                            Account account = googleAccountManager.getAccountByName(accountName);
                            MyPreferences.setGoogleDriveAccount(this, account.name);
                            selectAccount();
                        }
                    }
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
    
    private void selectAccount() {
        Preference pDriveAccount = getPreferenceScreen().findPreference("google_drive_backup_account");
        Account account = getDriveSelectedAccount();
        if (account != null) {
            pDriveAccount.setSummary(account.name);
        }
    }
    
    @Override
	protected void onPause() {
		super.onPause();
		PinProtection.lock(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		PinProtection.unlock(this);
        dropbox.completeAuth();
        linkToDropbox();
    }
    
}
