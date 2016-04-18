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
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import com.flowzr.R;
import com.flowzr.utils.MyPreferences;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;

public class FlowzrPreferencesActivity extends PreferenceActivity {

    private static final String[] ACCOUNT_TYPE = new String[] {GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE};

    private static final int CHOOSE_ACCOUNT = 101;
    private static final int CHOOSE_SERVER = 102;

    GoogleAccountManager googleAccountManager;

    @Override
    public void onBackPressed() {
        setResult(MainActivity.RESULT_OK);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.flowzr_preferences);

        googleAccountManager = new GoogleAccountManager(this);

        PreferenceScreen preferenceScreen = getPreferenceScreen();

        Preference pFlowzrAccount = preferenceScreen.findPreference("flowzr_account");
        pFlowzrAccount.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                chooseFlowzrAccount();
                return true;
            }
        });
        selectAccount();
        selectApiUrl();
    }

    private void chooseFlowzrAccount() {
        try {
            Account selectedAccount = getFlowzrSelectedAccount();
            Intent intent = AccountPicker.newChooseAccountIntent(selectedAccount, null, ACCOUNT_TYPE, true,
                    null, null, null, null);
            startActivityForResult(intent, CHOOSE_ACCOUNT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.google_drive_account_select_error, Toast.LENGTH_LONG).show();
        }
    }

    private Account getFlowzrSelectedAccount() {
        Account selectedAccount = null;
        String account = MyPreferences.getFlowzrAccount(this);
        if (account != null) {
            selectedAccount = googleAccountManager.getAccountByName(account);
        }
        return selectedAccount;
    }

    private String getSyncApiUrl() {
        return MyPreferences.getSyncApiUrl(this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CHOOSE_ACCOUNT:
                    if (data != null) {
                        Bundle b = data.getExtras();
                        String accountName = b.getString(AccountManager.KEY_ACCOUNT_NAME);
                        Log.d("Preferences", "Selected account: " + accountName);
                        if (accountName != null && accountName.length() > 0) {
                            Account account = googleAccountManager.getAccountByName(accountName);
                            MyPreferences.setFlowzrAccount(this, account.name);
                            selectAccount();
                        }
                    }
                    break;
            }
        }
    }

    private void selectAccount() {
        Preference pDriveAccount = getPreferenceScreen().findPreference("flowzr_account");
        Account account = getFlowzrSelectedAccount();
        if (account != null) {
            pDriveAccount.setSummary(account.name);
        }
    }

    private void selectApiUrl() {
        Preference pDriveAccount = getPreferenceScreen().findPreference("sync_api_url");
        pDriveAccount.setSummary(getSyncApiUrl());
    }


}