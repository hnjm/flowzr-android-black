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
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.flowzr.R;
import com.flowzr.db.DatabaseHelper;
import com.flowzr.model.Account;
import com.flowzr.utils.TransactionUtils;
import com.flowzr.widget.AmountInput;
import com.flowzr.widget.RateLayoutView;


public class SplitTransferActivity extends AbstractSplitActivity {

    private RateLayoutView rateView;

    protected TextView accountText;
    protected Cursor accountCursor;
    protected ListAdapter accountAdapter;

    public SplitTransferActivity() {
        super(R.layout.split_fixed);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.split_fixed;
    }


    @Override
    protected void createUI(LinearLayout layout) {
        accountText = x.addListNode(layout, R.id.account, R.string.account, R.string.select_to_account);
        rateView = new RateLayoutView(this, x, layout);
        rateView.createTransferUI( this);
        rateView.setAmountFromChangeListener(new AmountInput.OnAmountChangedListener() {
            @Override
            public void onAmountChanged(long oldAmount, long newAmount) {
                setUnsplitAmount(split.unsplitAmount - newAmount);
            }
        });
    }

    @Override
    protected void fetchData() {
        accountCursor = db.em().getAllActiveAccounts();
        getActivity().startManagingCursor(accountCursor);
        accountAdapter = TransactionUtils.createAccountAdapter(getContext(), accountCursor);
    }

    @Override
    protected void updateUI() {
        super.updateUI();
        selectFromAccount(split.fromAccountId);
        selectToAccount(split.toAccountId);
        setFromAmount(split.fromAmount);
        setToAmount(split.toAmount);
    }

    @Override
    protected boolean updateFromUI() {
        super.updateFromUI();
        split.fromAmount = rateView.getFromAmount();
        split.toAmount = rateView.getToAmount();
        if (split.fromAccountId == split.toAccountId) {
            Toast.makeText(getContext(), R.string.select_to_account_differ_from_to_account, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void selectFromAccount(long accountId) {
        if (accountId > 0) {
            Account account = em.getAccount(accountId);
            rateView.selectCurrencyFrom(account.currency);
        }
    }

    private void selectToAccount(long accountId) {
        if (accountId > 0) {
            Account account = em.getAccount(accountId);
            rateView.selectCurrencyTo(account.currency);
            accountText.setText(account.title);
            split.toAccountId = accountId;
        }
    }

    private void setFromAmount(long amount) {
        rateView.setFromAmount(amount);
    }

    private void setToAmount(long amount) {
        rateView.setToAmount(amount);
    }


    @Override
    protected void onClick(View v, int id) {
        super.onClick(v, id);
        if (id == R.id.account) {
            x.select(getContext(), R.id.account, R.string.account_to, accountCursor, accountAdapter,
                    DatabaseHelper.AccountColumns.ID, split.toAccountId);
        }
    }

    @Override
    public void onSelectedId(int id, long selectedId) {
        switch(id) {
            case R.id.account:
                selectToAccount(selectedId);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AppCompatActivity.RESULT_OK) {
            rateView.onActivityResult(requestCode, data);
        }
    }

}
