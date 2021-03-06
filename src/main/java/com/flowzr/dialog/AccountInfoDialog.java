/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.flowzr.dialog;


import android.app.Dialog;
import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flowzr.R;
import com.flowzr.activity.AbstractTotalListFragment;
import com.flowzr.activity.AccountActivity;
import com.flowzr.activity.AccountListFragment;
import com.flowzr.activity.MainActivity;
import com.flowzr.activity.MyFragmentAPI;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.MyEntityManager;
import com.flowzr.model.Account;
import com.flowzr.model.AccountType;
import com.flowzr.model.CardIssuer;
import com.flowzr.utils.Utils;
import com.flowzr.view.NodeInflater;

import static com.flowzr.utils.Utils.isNotEmpty;

public class AccountInfoDialog {

    private final MainActivity activity;
    private final AccountListFragment parentActivity;
    private final long accountId;
    private final MyEntityManager em;
    private final NodeInflater inflater;
    private final LayoutInflater layoutInflater;
    private final Utils u;

    public AccountInfoDialog(AccountListFragment parentActivity,MainActivity activity, long accountId,
                             DatabaseAdapter db, NodeInflater inflater) {
        this.parentActivity = parentActivity;
        this.activity = activity;
        this.accountId = accountId;
        //DatabaseAdapter db1 = db;
        this.em = db.em();
        this.inflater = inflater;
        this.layoutInflater = (LayoutInflater) parentActivity.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.u = new Utils(parentActivity.getActivity());
    }

    public void show() {
        Account a = em.getAccount(accountId);
        if (a == null) {
            Toast t = Toast.makeText(parentActivity.getActivity(), R.string.no_account, Toast.LENGTH_LONG);
            t.show();
            return;
        }

        View v = layoutInflater.inflate(R.layout.info_dialog, null);
        LinearLayout layout = (LinearLayout) v.findViewById(R.id.list);

        View titleView = createTitleView(a);
        createNodes(a, layout);

        showDialog(v, titleView);
    }

    private View createTitleView(Account a) {
        View titleView = layoutInflater.inflate(R.layout.info_dialog_title, null);
        TextView titleLabel = (TextView) titleView.findViewById(R.id.label);
        TextView titleData = (TextView) titleView.findViewById(R.id.data);
        ImageView titleIcon = (ImageView) titleView.findViewById(R.id.icon);

        titleLabel.setText(a.title);

        AccountType type = AccountType.valueOf(a.type);
        titleData.setText(type.titleId);
        titleIcon.setImageResource(type.iconId);

        return titleView;
    }

    private void createNodes(Account a, LinearLayout layout) {
        AccountType type = AccountType.valueOf(a.type);
        if (type.isCard) {
            CardIssuer issuer = CardIssuer.valueOf(a.cardIssuer);
            add(layout, R.string.issuer, issuerTitle(a), issuer);
        }
        add(layout, R.string.currency, a.currency.title);

        if (type.isCreditCard && a.limitAmount != 0) {
            long limitAmount = Math.abs(a.limitAmount);
            long balance = limitAmount + a.totalAmount;
            TextView amountView = add(layout, R.string.amount, "");
            u.setAmountText(amountView, a.currency, a.totalAmount, true);
            TextView limitAmountView = add(layout, R.string.balance, "");
            u.setAmountText(limitAmountView, a.currency, balance, true);
        } else {
            TextView amountView = add(layout, R.string.balance, "");
            u.setAmountText(amountView, a.currency, a.totalAmount, true);
        }
        add(layout, R.string.note, a.note);
    }

    private String issuerTitle(Account a) {
        return (isNotEmpty(a.issuer) ? a.issuer : "")+" "+(isNotEmpty(a.number) ? "#"+a.number : "");
    }

    private void showDialog(final View v, View titleView) {
        final Dialog d = new AlertDialog.Builder(parentActivity.getActivity())
                .setCustomTitle(titleView)
                .setView(v)
                .create();
        d.setCanceledOnTouchOutside(true);

        Button bEdit = (Button) v.findViewById(R.id.bEdit);
        bEdit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                d.dismiss();
                Bundle bundle= new Bundle();
                bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,AccountActivity.class.getCanonicalName());
                bundle.putLong(MyFragmentAPI.ENTITY_ID_EXTRA, accountId);
                activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
            }
        });


        Button bClose = (Button) v.findViewById(R.id.bClose);
        bClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                d.dismiss();
            }
        });

        d.show();
    }

    private void add(LinearLayout layout, @SuppressWarnings("SameParameterValue") int labelId, String data, CardIssuer cardIssuer) {
        inflater.new Builder(layout, R.layout.select_entry_simple_icon)
                .withIcon(cardIssuer.iconId).withLabel(labelId).withData(data).create();
    }

    private TextView add(LinearLayout layout, int labelId, String data) {
        View v = inflater.new Builder(layout, R.layout.select_entry_simple).withLabel(labelId)
                .withData(data).create();
        return (TextView)v.findViewById(R.id.data);
    }

    private LinearLayout add(LinearLayout layout, String label, String data) {
        return (LinearLayout) inflater.new Builder(layout, R.layout.select_entry_simple).withLabel(label)
                .withData(data).create();
    }

}
