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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import com.flowzr.R;
import com.flowzr.model.Account;
import com.flowzr.model.AccountType;
import com.flowzr.model.CardIssuer;
import com.flowzr.datetime.DateUtils;
import com.flowzr.utils.MyPreferences;
import com.flowzr.utils.Utils;
import com.flowzr.orb.EntityManager;


import java.text.DateFormat;
import java.util.Date;

import com.google.android.gms.internal.db;

public class AccountListAdapter2 extends ResourceCursorAdapter {
	
	private final Utils u;
	private DateFormat df;
    private boolean isShowAccountLastTransactionDate;

	public AccountListAdapter2(Context context, Cursor c) {
		super(context, R.layout.generic_list_item_2, c);
		this.u = new Utils(context);
		this.df = DateUtils.getShortDateFormat(context);
        this.isShowAccountLastTransactionDate = MyPreferences.isShowAccountLastTransactionDate(context);
	}		

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = super.newView(context, cursor, parent);		
		return GenericViewHolder2.create(view);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		Account a = EntityManager.loadFromCursor(cursor, Account.class);
		GenericViewHolder2 v = (GenericViewHolder2)view.getTag();

		v.centerView.setText(a.title);

		AccountType type = AccountType.valueOf(a.type);
		if (type.isCard && a.cardIssuer != null) {
			CardIssuer cardIssuer = CardIssuer.valueOf(a.cardIssuer);
			v.iconView.setImageResource(cardIssuer.iconId);
		} else {
			v.iconView.setImageResource(type.iconId);
		}
		if (a.isActive) {
			v.iconView.getDrawable().mutate().setAlpha(0xFF);
			v.iconOverView.setVisibility(View.INVISIBLE);			
		} else {
			v.iconView.getDrawable().mutate().setAlpha(0x77);
			v.iconOverView.setVisibility(View.VISIBLE);
		}

		StringBuilder sb = new StringBuilder();
		if (!Utils.isEmpty(a.issuer)) {
			sb.append(a.issuer);
		}
		if (!Utils.isEmpty(a.number)) {
			sb.append(" #").append(a.number);
		}
		if (sb.length() == 0) {
			sb.append(context.getString(type.titleId));
		}
		v.topView.setText(sb.toString());

        long date = a.creationDate;
        if (isShowAccountLastTransactionDate && a.lastTransactionDate > 0) {
            date = a.lastTransactionDate;
        }
        //v.iconView.setVisibility(View.GONE);
		v.bottomView.setText(df.format(new Date(date)));
		v.rightCenterView.setVisibility(View.VISIBLE);
		long amount = a.totalAmount;
		
		if (type == AccountType.CREDIT_CARD && a.limitAmount != 0) {
            long limitAmount = Math.abs(a.limitAmount);
			long balance = limitAmount + amount;
            long balancePercentage = 10000*balance/limitAmount;
			u.setAmountText(v.rightView, a.currency, amount, false);
			u.setAmountText(v.rightCenterView, a.currency, balance, false);
			v.bottomView.setVisibility(View.VISIBLE);
			v.rightView.setVisibility(View.VISIBLE);
			v.progressBar.setMax(10000);
			v.progressBar.setProgress((int)balancePercentage);
			v.progressBar.setVisibility(View.VISIBLE);
		} else {
			u.setAmountText(v.rightCenterView, a.currency, amount, false);
			v.rightView.setVisibility(View.VISIBLE);
			v.progressBar.setVisibility(View.GONE);
			v.progressText.setVisibility(View.GONE);
		}
	}



}
