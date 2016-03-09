package com.flowzr.activity;

/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Abdsandryk Souza 
 *     Rodrigo Sousa
 *******************************************************************************/

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.flowzr.R;
import com.flowzr.model.Currency;
import com.flowzr.utils.CurrencyCache;
import com.flowzr.utils.MyPreferences;

import java.util.Collection;

public class ReportPreferencesActivity extends PreferenceActivity {

	/**
	 * The list of currencies.
	 */
	private String[] currencies;
	
	private String currency;
		
	/**
	 * The index of the selected currency
	 */
	private int selectedCurrenceIndex;
			
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);   
		addPreferencesFromResource(R.xml.report_preferences);	
		
		getCurrenciesList();		
		final EditTextPreference pReportReferenceCurrency = (EditTextPreference)getPreferenceScreen().findPreference("report_reference_currency");
		pReportReferenceCurrency.setOnPreferenceClickListener( 
				new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference arg0) {
						return pReportReferenceCurrency == null || showChoiceList(pReportReferenceCurrency);
					}
				}				
		);		
	}

	/**
	 * Get the list of currencies.
	 */
	private void getCurrenciesList() {
		String selectedCurrenceTitle = MyPreferences.getReferenceCurrencyTitle(this);
		Collection<Currency> currenciesList = CurrencyCache.getAllCurrencies();
		
		int count = currenciesList.size();

		selectedCurrenceIndex = -1;		
		currencies = new String[count];
        int i=0;
        for (Currency c : currenciesList) {
            if (c.title.equals(selectedCurrenceTitle)) {
                selectedCurrenceIndex = i;
            }
            currencies[i] = c.title;
            i++;
        }
	}



	private boolean showChoiceList(final EditTextPreference pReportReferenceCurrency) {
		//AlertDialog.Builder builder = new AlertDialog.Builder(ReportPreferencesActivity.this);
		//builder.setTitle(R.string.report_preferences_not_set);
		//builder.show();

		new AlertDialog.Builder(ReportPreferencesActivity.this)
		.setTitle(R.string.report_preferences_not_set)
		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
			// get user preferred currency 
			@Override
			public void onClick(DialogInterface dialog, int which) {
				pReportReferenceCurrency.setText(currency);
			}
		})
		.setSingleChoiceItems(currencies, selectedCurrenceIndex, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				selectedCurrenceIndex = which;
				currency = currencies[which];
			}
		})
		.show();

		Dialog dialog = pReportReferenceCurrency.getDialog();
		if(dialog!=null)
			dialog.cancel();
		return true;
	}
	

}
