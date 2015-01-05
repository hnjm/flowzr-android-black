/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.flowzr.activity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.flowzr.R;
import com.flowzr.backup.DatabaseExport;
import com.flowzr.model.Account;
import com.flowzr.datetime.DateUtils;

import java.text.DateFormat;
import java.util.Calendar;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 6/12/12 11:14 PM
 */
public class PurgeAccountActivity extends AbstractEditorActivity {

    public static final String ACCOUNT_ID = "ACCOUNT_ID";

    private Account account;
    private Calendar date;

    private LinearLayout layout;
    private CheckBox databaseBackup;
    private TextView dateText;
    private DateFormat df;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.purge_account);

        df = DateUtils.getLongDateFormat(this);

        layout = (LinearLayout)findViewById(R.id.layout);
        date = Calendar.getInstance();
        date.add(Calendar.YEAR, -1);
        date.add(Calendar.DAY_OF_YEAR, -1);

        Button bOk = (Button)findViewById(R.id.bOK);
        bOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteOldTransactions();
            }
        });

        Button bCancel = (Button)findViewById(R.id.bCancel);
        bCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        loadAccount();
        createNodes();
        setDateText();
    }

    private void deleteOldTransactions() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.purge_account_confirm_title)
            .setMessage(getString(R.string.purge_account_confirm_message, new Object[]{account.title, getDateString()}))
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    new PurgeAccountTask().execute();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void loadAccount() {
        Intent intent = getIntent();
        if (intent == null) {
            Toast.makeText(this, "No account specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        long accountId = intent.getLongExtra(ACCOUNT_ID, -1);
        if (accountId <= 0) {
            Toast.makeText(this, "Invalid account specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        account = em.getAccount(accountId);
        if (account == null) {
            Toast.makeText(this, "No account found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void createNodes() {
        x.addInfoNode(layout, 0, R.string.account, account.title);
        x.addInfoNode(layout, 0, R.string.warning, R.string.purge_account_date_summary);
        dateText = x.addInfoNode(layout, R.id.date, R.string.date, "?");
        databaseBackup = x.addCheckboxNode(layout, R.id.backup, R.string.database_backup, R.string.purge_account_backup_database, true);
    }

    @Override
    protected void onClick(View v, int id) {
        switch (id) {
            case R.id.date:
                DatePickerDialog d = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener(){
                    @Override
                    public void onDateSet(DatePicker arg0, int y, int m, int d) {
                        date.set(y, m, d);
                        setDateText();
                    }
                }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
                d.show();
                break;
            case R.id.backup:
                databaseBackup.setChecked(!databaseBackup.isChecked());
                break;
        }
    }

    private void setDateText() {
        dateText.setText(getDateString());
    }

    private String getDateString() {
        return df.format(date.getTime());
    }

    private class PurgeAccountTask extends AsyncTask<Void, Void, Void> {

        private Context context;
        private Dialog d;

        private PurgeAccountTask() {
            this.context = PurgeAccountActivity.this;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            d = ProgressDialog.show(context, null, getString(R.string.purge_account_in_progress), true);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            d.dismiss();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (databaseBackup.isChecked()) {
                DatabaseExport export = new DatabaseExport(context, db.db(), true);
                try {
                    export.export();
                } catch (Exception e) {
                    Log.e("Financisto", "Unexpected error", e);
                    Toast.makeText(context, R.string.purge_account_unable_to_do_backup, Toast.LENGTH_LONG).show();
                    return null;
                }
            }
            db.purgeAccountAtDate(account, date.getTimeInMillis());
            setResult(RESULT_OK);
            finish();
            return null;
        }

    }

}
