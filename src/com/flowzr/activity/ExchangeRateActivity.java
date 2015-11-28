/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.flowzr.activity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.flowzr.R;
import com.flowzr.model.Currency;
import com.flowzr.rates.ExchangeRate;
import com.flowzr.datetime.DateUtils;
import com.flowzr.utils.CurrencyCache;
import com.flowzr.widget.AmountInput;
import com.flowzr.widget.RateNode;
import com.flowzr.widget.RateNodeOwner;

import java.util.Calendar;

import static com.flowzr.utils.Utils.checkEditText;
import static com.flowzr.utils.Utils.formatRateDate;
import static com.flowzr.utils.Utils.text;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 1/19/12 7:41 PM
 */
public class ExchangeRateActivity extends AbstractEditorActivity implements RateNodeOwner {

    public static final String RATE_DATE = "RATE_DATE";
    public static final String TO_CURRENCY_ID = "TO_CURRENCY_ID";
    public static final String FROM_CURRENCY_ID = "FROM_CURRENCY_ID";

    private Currency fromCurrency;
    private Currency toCurrency;
    private long originalDate;
    private long date;
    private double rate = 1;

    private TextView dateNode;
    private RateNode rateNode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.exchange_rate);
        initToolbar();
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        if (validateIntent(intent)) {
            LinearLayout layout = (LinearLayout) findViewById(R.id.list);
            updateUI(layout);
        } else {
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {	 
        	case R.id.action_done:
              ExchangeRate rate = createRateFromUI();
              db.replaceRate(rate, originalDate);
              Intent data = new Intent();
              setResult(RESULT_OK, data);
              finish();      	        		
        		return true;
	    	case R.id.action_cancel:
				setResult(RESULT_CANCELED);
				finish();
	    		return true;        
        }
        return super.onOptionsItemSelected(item);
    }
    
    private ExchangeRate createRateFromUI() {
        ExchangeRate rate = new ExchangeRate();
        rate.fromCurrencyId = fromCurrency.id;
        rate.toCurrencyId = toCurrency.id;
        rate.date = date;
        rate.rate = rateNode.getRate();
        return rate;
    }

    private void updateUI(LinearLayout layout) {
        x.addInfoNode(layout, 0, R.string.rate_from_currency, fromCurrency.name);
        x.addInfoNode(layout, 0, R.string.rate_to_currency, toCurrency.name);
        dateNode = x.addInfoNode(layout, R.id.date, R.string.date, formatRateDate(this, date));
        rateNode = new RateNode(this, x, layout);
        rateNode.setRate(rate);
        rateNode.updateRateInfo();
    }

    private boolean validateIntent(Intent intent) {
        long fromCurrencyId = intent.getLongExtra(FROM_CURRENCY_ID, -1);
        fromCurrency = em.get(Currency.class, fromCurrencyId);
        if (fromCurrency == null) {
            finish();
            return false;
        }

        long toCurrencyId = intent.getLongExtra(TO_CURRENCY_ID, -1);
        toCurrency = em.get(Currency.class, toCurrencyId);
        if (toCurrency == null) {
            finish();
            return false;
        }

        long date = intent.getLongExtra(RATE_DATE, -1);
        if (date == -1) {
            date = DateUtils.atMidnight(System.currentTimeMillis());
        }
        this.originalDate = this.date = date;

        ExchangeRate rate = db.findRate(fromCurrency, toCurrency, date);
        if (rate != null) {
            this.rate = rate.rate;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == RateNode.EDIT_RATE) {
            String amount = data.getStringExtra(AmountInput.EXTRA_AMOUNT);
            if (amount != null) {
                rateNode.setRate(Float.parseFloat(amount));
                rateNode.updateRateInfo();
            }
        }
    }

    @Override
    protected void onClick(View v, int id) {
        switch (id) {
            case R.id.date:
                editDate();
                break;
        }
    }

    private void editDate() {
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(date);
        DatePickerDialog d = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener(){
            @Override
            public void onDateSet(DatePicker arg0, int y, int m, int d) {
                c.set(y, m, d);
                date = c.getTimeInMillis();
                dateNode.setText(formatRateDate(ExchangeRateActivity.this, date));
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        d.show();
    }

    @Override
    public void onBeforeRateDownload() {
        rateNode.disableAll();
    }

    @Override
    public void onAfterRateDownload() {
        rateNode.enableAll();
    }

    @Override
    public void onSuccessfulRateDownload() {
        rateNode.updateRateInfo();
    }

    @Override
    public void onRateChanged() {
        rateNode.updateRateInfo();
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public Currency getCurrencyFrom() {
        return fromCurrency;
    }

    @Override
    public Currency getCurrencyTo() {
        return toCurrency;
    }

}
