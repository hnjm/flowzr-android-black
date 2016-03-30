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


import android.content.ComponentName;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.flowzr.R;
import com.flowzr.rates.ExchangeRateProviderFactory;
import com.flowzr.utils.MyPreferences;
import com.flowzr.utils.PinProtection;

public class PreferencesActivity extends PreferenceActivity {

    Preference pOpenExchangeRatesAppId;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        this.setTheme(R.style.CheckBox);
        super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

        //ActionBar actionBar = getActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Preference pLocale = preferenceScreen.findPreference("ui_language");
        pLocale.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String locale = (String) newValue;
                MyPreferences.switchLocale(PreferencesActivity.this, locale);
				return true;
			}
		});
		Preference pNewTransactionShortcut = preferenceScreen.findPreference("shortcut_new_transaction");
		pNewTransactionShortcut.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                addShortcut(".activity.TransactionActivity", R.string.transaction, R.drawable.ic_edit);
                return true;
            }

        });
		Preference pNewTransferShortcut = preferenceScreen.findPreference("shortcut_new_transfer");
		pNewTransferShortcut.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				addShortcut(".activity.TransferActivity", R.string.transfer, R.drawable.ic_swap_vert);
				return true;
			}
		});

        Preference pExchangeProvider = preferenceScreen.findPreference("exchange_rate_provider");
        pOpenExchangeRatesAppId = preferenceScreen.findPreference("openexchangerates_app_id");
        pExchangeProvider.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                pOpenExchangeRatesAppId.setEnabled(isOpenExchangeRatesProvider((String) newValue));
                return true;
            }

            private boolean isOpenExchangeRatesProvider(String provider) {
                return ExchangeRateProviderFactory.openexchangerates.name().equals(provider);
            }
        });

	}

    private void enableOpenExchangeApp() {
        pOpenExchangeRatesAppId.setEnabled(MyPreferences.isOpenExchangeRatesProviderSelected(this));
    }

    private void addShortcut(String activity, int nameId, int iconId) {
		Intent intent = createShortcutIntent(activity, getString(nameId), Intent.ShortcutIconResource.fromContext(this, iconId), 
				"com.android.launcher.action.INSTALL_SHORTCUT");
		sendBroadcast(intent);
	}

	private Intent createShortcutIntent(String activity, String shortcutName, ShortcutIconResource shortcutIcon, String action) {
		Intent shortcutIntent = new Intent();
		shortcutIntent.setComponent(new ComponentName(this.getPackageName(), activity));
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		Intent intent = new Intent();
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortcutIcon);
		intent.setAction(action);
		return intent;
	}

 
	@Override
	protected void onResume() {
		super.onResume();
        PinProtection.unlock(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //PinProtection.lock(this.getApplicationContext());
    }
    	
}
