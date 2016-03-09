/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package com.flowzr.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.text.format.DateUtils;

import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.DatabaseHelper.BlotterColumns;
import com.flowzr.model.Currency;
import com.flowzr.utils.CurrencyCache;
import com.flowzr.utils.Utils;

import java.util.Calendar;

import static com.flowzr.utils.TransactionTitleUtils.generateTransactionTitle;

public class TransactionsListAdapter extends BlotterListAdapter {
	
	public TransactionsListAdapter(Context context, DatabaseAdapter db, Cursor c) {
		super(context, db, c);
	}

    @Override
    protected void bindView(BlotterViewHolder v, Context context, Cursor cursor) {
        long toAccountId = cursor.getLong(BlotterColumns.to_account_id.ordinal());
        String payee = cursor.getString(BlotterColumns.payee.ordinal());
        String note = cursor.getString(BlotterColumns.note.ordinal());
        long locationId = cursor.getLong(BlotterColumns.location_id.ordinal());
        String location = "";
        if (locationId > 0) {
            location = cursor.getString(BlotterColumns.location.ordinal());
        }
        String toAccount = cursor.getString(BlotterColumns.to_account_title.ordinal());
        long fromAmount = cursor.getLong(BlotterColumns.from_amount.ordinal());
        if (toAccountId > 0) {
            v.topView.setText(R.string.transfer);
            if (fromAmount > 0) {
                note = toAccount+" \u00BB";
            } else {
                note = "\u00AB "+toAccount;
            }
            u.setTransferTextColor(v.centerView);
        } else {
            String title = cursor.getString(BlotterColumns.from_account_title.ordinal());
            v.topView.setText(title);
            v.centerView.setTextColor(Color.WHITE);
        }

        long categoryId = cursor.getLong(BlotterColumns.category_id.ordinal());
        String category = "";
        if (categoryId != 0) {
            category = cursor.getString(BlotterColumns.category_title.ordinal());
        }
        String text = generateTransactionTitle(sb, payee, note, location, categoryId, category);
        v.centerView.setText(text);
        sb.setLength(0);

        long currencyId = cursor.getLong(BlotterColumns.from_account_currency_id.ordinal());
        Currency c = CurrencyCache.getCurrency(em, currencyId);
        long originalCurrencyId = cursor.getLong(BlotterColumns.original_currency_id.ordinal());
        if (originalCurrencyId > 0) {
            Currency originalCurrency = CurrencyCache.getCurrency(em, originalCurrencyId);
            long originalAmount = cursor.getLong(BlotterColumns.original_from_amount.ordinal());
            u.setAmountText(sb, v.rightView, originalCurrency, originalAmount, c, fromAmount, true);
        } else {
            u.setAmountText(v.rightView, c, fromAmount, true);
        }
        if (fromAmount > 0) {
            v.iconView.setImageDrawable(icBlotterIncome);
        } else if (fromAmount < 0) {
            v.iconView.setImageDrawable(icBlotterExpense);
        }
        long date = cursor.getLong(BlotterColumns.datetime.ordinal());
        dt.setTime(date);
        v.bottomView.setText(DateUtils.formatDateTime(context, dt.getTime(),
                DateUtils.FORMAT_SHOW_TIME));

        v.month_TV.setText(DateUtils.formatDateTime(context, dt.getTime(),
        		DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY |DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NO_NOON | DateUtils.FORMAT_NO_MIDNIGHT | DateUtils.FORMAT_ABBREV_MONTH));
        
        Calendar cal=Calendar.getInstance();
        cal.setTimeInMillis(date);                
        v.day_TV.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
                       
        if (date > System.currentTimeMillis()) {
            u.setFutureTextColor(v.bottomView);
            u.setFutureTextColor(v.day_TV);
            u.setFutureTextColor(v.month_TV);            
        } else {
            v.bottomView.setTextColor(v.topView.getTextColors().getDefaultColor());
            v.day_TV.setTextColor(v.topView.getTextColors().getDefaultColor());
        	v.month_TV.setTextColor(v.topView.getTextColors().getDefaultColor());
        }

        long balance = cursor.getLong(BlotterColumns.from_account_balance.ordinal());
        v.rightCenterView.setText(Utils.amountToString(c, balance, false));
        removeRightCenterViewIfNeeded(v);
        setIndicatorColor(v, cursor);
    }

}