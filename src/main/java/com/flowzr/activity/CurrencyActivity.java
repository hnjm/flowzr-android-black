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
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.MyEntityManager;
import com.flowzr.model.Currency;
import com.flowzr.model.SymbolFormat;
import com.flowzr.utils.CurrencyCache;

import java.text.DecimalFormatSymbols;

import static com.flowzr.utils.Utils.checkEditText;
import static com.flowzr.utils.Utils.text;

public class CurrencyActivity extends AbstractEditorActivity {
	
	public static final String CURRENCY_ID_EXTRA = "currencyId";
	private static final DecimalFormatSymbols s = new DecimalFormatSymbols();
	
	private DatabaseAdapter db;
	private MyEntityManager em;
	
	private String[] decimalSeparatorsItems;
	private String[] groupSeparatorsItems;
    private SymbolFormat[] symbolFormats;

    private EditText name;
    private EditText title;
    private EditText symbol;
    private CheckBox isDefault;
	private Spinner decimals;
    private Spinner decimalSeparators;
    private Spinner groupSeparators;
    private Spinner symbolFormat;

	private int maxDecimals; 

	private Currency currency = new Currency();

    @Override
    protected int getLayoutId() {
        return R.layout.currency;
    }

	@Override
public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		db = new DatabaseAdapter(getContext());
		db.open();
		em = db.em();

        name = (EditText)getView().findViewById(R.id.name);
        title = (EditText)getView().findViewById(R.id.title);
        symbol = (EditText)getView().findViewById(R.id.symbol);
        isDefault = (CheckBox)getView().findViewById(R.id.is_default);
		decimals = (Spinner)getView().findViewById(R.id.spinnerDecimals);
		decimalSeparators = (Spinner)getView().findViewById(R.id.spinnerDecimalSeparators);
		groupSeparators = (Spinner)getView().findViewById(R.id.spinnerGroupSeparators);
		groupSeparators.setSelection(1);
        symbolFormat = (Spinner)getView().findViewById(R.id.spinnerSymbolFormat);
        symbolFormat.setSelection(0);

		maxDecimals = decimals.getCount();
		
		decimalSeparatorsItems = getResources().getStringArray(R.array.decimal_separators);
		groupSeparatorsItems = getResources().getStringArray(R.array.group_separators);
        symbolFormats = SymbolFormat.values();

		long id = getArguments().getLong(MyFragmentAPI.ENTITY_ID_EXTRA, -1);
		if (id != -1) {
			currency = em.load(Currency.class, id);
			editCurrency();
		}	  else {
			makeDefaultIfNecessary();
		}


	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {	 
        	case R.id.action_done:
				if (checkEditText(title, "title", true, 100)
						&& checkEditText(name, "code", true, 3)
						&& checkEditText(symbol, "symbol", true, 3)) {
					currency.title = text(title);
					currency.name = text(name);
					currency.symbol = text(symbol);
					currency.isDefault = isDefault.isChecked();
					currency.decimals = maxDecimals-decimals.getSelectedItemPosition();
					currency.decimalSeparator = decimalSeparators.getSelectedItem().toString();
					currency.groupSeparator = groupSeparators.getSelectedItem().toString();
                    currency.symbolFormat = symbolFormats[symbolFormat.getSelectedItemPosition() -1 ];
					long id = em.saveOrUpdate(currency);
					CurrencyCache.initialize(em);
					Intent data = new Intent();
					data.putExtra(CURRENCY_ID_EXTRA, id);
                    finishAndClose(data.getExtras());
				}        	        		
        		return true;
	    	case R.id.action_cancel:
                finishAndClose(AppCompatActivity.RESULT_CANCELED);
	    		return true;        
        }
        return super.onOptionsItemSelected(item);
    }
	
    private void makeDefaultIfNecessary() {
        isDefault.setChecked(em.getAllCurrenciesList().isEmpty());
    }

    private void editCurrency() {
		Currency currency = this.currency;
		EditText name = (EditText)getView().findViewById(R.id.name);
		name.setText(currency.name);
		EditText title = (EditText)getView().findViewById(R.id.title);
		title.setText(currency.title);
		EditText symbol = (EditText)getView().findViewById(R.id.symbol);
		symbol.setText(currency.symbol);
		CheckBox isDefault = (CheckBox)getView().findViewById(R.id.is_default);
		isDefault.setChecked(currency.isDefault);
		decimals.setSelection(maxDecimals-currency.decimals);
		decimalSeparators.setSelection(indexOf(decimalSeparatorsItems, currency.decimalSeparator, s.getDecimalSeparator()) +1);
		groupSeparators.setSelection(indexOf(groupSeparatorsItems, currency.groupSeparator, s.getGroupingSeparator()) +1);
        symbolFormat.setSelection(currency.symbolFormat.ordinal() + 1);
	}

	private int indexOf(String[] a, String v, char c) {
		int count = a.length;
		int d = -1;
		for (int i=0; i<count; i++) {
			String s = a[i];
			if (v != null && s.charAt(1) == v.charAt(1)) {
				return i ;
			} 
			if (s.charAt(1) == c) {
				d = i;
			}
		}
		return d ;
	}

	@Override
	public void onDestroy() {
		db.close();
		super.onDestroy();
	}
	
	@Override
	protected void onClick(View v, int id) {
		// Auto-generated method stub
		
	}
}
