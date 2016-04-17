/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Emmanuel Florent - Port to AppCompat 21,  add icon title
 ******************************************************************************/
package com.flowzr.activity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.flowzr.R;
import com.flowzr.db.DatabaseHelper.AccountColumns;
import com.flowzr.model.Account;
import com.flowzr.model.Category;
import com.flowzr.model.Total;
import com.flowzr.model.Transaction;
import com.flowzr.utils.MyPreferences;
import com.flowzr.utils.Utils;
import com.flowzr.widget.AmountInput;

public class TransferActivity extends AbstractTransactionActivity {

	private static final int BLOTTER_PREFERENCES = 5002 ;
	private TextView accountFromText;
    private TextView accountToText;

	private long selectedAccountFromId = -1;
	private long selectedAccountToId = -1;

    protected int getLayoutId() {
        return R.layout.transfer_free;
    }

	@Override
	protected void internalOnCreate() {
		super.internalOnCreate();

		if (transaction.isTemplateLike()) {
			getActivity().setTitle(transaction.isTemplate() ? R.string.transfer_template : R.string.transfer_schedule);
			if (transaction.isTemplate()) {			
				dateText.setEnabled(false);
				timeText.setEnabled(false);			
			}			
		}
		SwitchCompat toggleView = (SwitchCompat) getView().findViewById(R.id.toggle);
		toggleView.setVisibility(View.GONE); // .setBackgroundDrawable(getTransferIconDrawable());
		if (getView().findViewById(R.id.saveAddButton)!=null) {
			getView().findViewById(R.id.saveAddButton).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onOKClicked();
					Bundle bundle = new Bundle();
					bundle.putLong(DATETIME_EXTRA, transaction.dateTime);
					bundle.putLong(ACCOUNT_ID_EXTRA, transaction.fromAccountId);
					if (saveAndFinish()) {
						bundle.putLong(DATETIME_EXTRA, transaction.dateTime);
						Fragment fragment= new TransferActivity();
						fragment.setArguments(bundle);
						activity.startFragmentForResult(fragment,TransferActivity.this);
					}
				}
			});
		}
	}

	private static Drawable icon;

	private Drawable getTransferIconDrawable() {
		if (icon==null) {
			icon = getResources().getDrawable(R.drawable.ic_swap_vert);
		}
		return icon;
	}

    protected void fetchCategories() {
        categorySelector.fetchCategories(false);
        categorySelector.doNotShowSplitCategory();
    }


	protected void fetchCategories(long[] cids) {
		categorySelector.fetchCategories(cids);
		categorySelector.doNotShowSplitCategory();
	}

	@Override
	protected void createListNodes(LinearLayout layout) {
		layout=(LinearLayout)getView().findViewById(R.id.listlayout);
        accountFromText = x.addListNode2(layout, R.id.account_from, R.drawable.ic_action_account_from, R.string.account_from,getResources().getString(R.string.select_account));
		accountToText = x.addListNode2(layout, R.id.account_to, R.drawable.ic_action_account_to, R.string.account_to, getResources().getString(R.string.select_account));
        rateView.createTransferUI(this);
        // payee
        isShowPayee = MyPreferences.isShowPayeeInTransfers(getContext());
        if (isShowPayee) {
            createPayeeNode(layout);
        }
		// category
        if (MyPreferences.isShowCategoryInTransferScreen(getContext())) {
            categorySelector.createNode(layout, false);
        } else {
            categorySelector.createDummyNode();
        }

		Total t = new Total(rateView.getCurrencyFrom());
		t.balance=transaction.fromAmount;
		u.setTotal(totalText, t);
		rateView.setAmountFromChangeListener(new AmountInput.OnAmountChangedListener() {
			@Override
			public void onAmountChanged(long oldAmount, long newAmount) {
				Total t = new Total(rateView.getCurrencyFrom());
				t.balance = newAmount;
				u.setTotal(totalText, t);
			}
		});

	}
	
    @Override
    protected void editTransaction(Transaction transaction) {

		Total t = new Total(rateView.getCurrencyFrom());
		t.balance=transaction.fromAmount;
		u = new Utils(getContext());
		u.setTotal(totalText, t);
		//totalText.setTextColor(getResources().getColor(R.color.f_blue_lighter1));
		//amountInput.setAmount(transaction.fromAmount);

        if (transaction.fromAccountId > 0) {
            Account fromAccount = em.getAccount(transaction.fromAccountId);
            selectAccount(fromAccount, accountFromText, false);
            rateView.selectCurrencyFrom(fromAccount.currency);
            rateView.setFromAmount(transaction.fromAmount);
            selectedAccountFromId = transaction.fromAccountId;
        }
        commonEditTransaction(transaction);
        if (transaction.toAccountId > 0) {
            Account toAccount = em.getAccount(transaction.toAccountId);
            selectAccount(toAccount, accountToText, false);
            rateView.selectCurrencyTo(toAccount.currency);
            rateView.setToAmount(transaction.toAmount);
            selectedAccountToId = transaction.toAccountId;
        }
        selectPayee(transaction.payeeId);
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
			case R.id.saveButton:
				onOKClicked();
				saveAndFinish();
				return true;
			case R.id.saveAddButton:
				onOKClicked();
				Intent intent2 = new Intent(getContext(), TransferActivity.class);
				intent2.putExtra(DATETIME_EXTRA, transaction.dateTime);
				intent2.putExtra(ACCOUNT_ID_EXTRA, transaction.fromAccountId);
				Fragment fragment = new TransferActivity();

				Bundle bundle= new Bundle();
				bundle.putLong(DATETIME_EXTRA, transaction.dateTime);
				bundle.putLong(ACCOUNT_ID_EXTRA, transaction.fromAccountId);

				if (saveAndFinish()) {
					bundle.putLong(DATETIME_EXTRA, transaction.dateTime);
					fragment.setArguments(bundle);
					activity.startFragmentForResult(fragment,this);

				}
				return true;
			case R.id.action_settings:
				Intent intent = new Intent(this.getContext(), TransactionPreferencesActivity.class);
				startActivityForResult(intent, BLOTTER_PREFERENCES);
				return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
	@Override
	protected boolean onOKClicked() {
		if (selectedAccountFromId == -1) {
			Toast.makeText(getContext(), R.string.select_from_account, Toast.LENGTH_SHORT).show();
			return false;
		}
		if (selectedAccountToId == -1) {
			Toast.makeText(getContext(), R.string.select_to_account, Toast.LENGTH_SHORT).show();
			return false;
		}
        if (selectedAccountFromId == selectedAccountToId) {
            Toast.makeText(getContext(), R.string.select_to_account_differ_from_to_account, Toast.LENGTH_SHORT).show();
            return false;
        }
        updateTransferFromUI();
		return true;
	}

	private void updateTransferFromUI() {
		updateTransactionFromUI(transaction);
		transaction.fromAccountId = selectedAccountFromId;
		transaction.toAccountId = selectedAccountToId;
        transaction.fromAmount = rateView.getFromAmount();
        transaction.toAmount = rateView.getToAmount();
	}

	
	
	@Override
	protected void onClick(View v, int id) {
		super.onClick(v, id);
		switch (id) {
			case R.id.account_from:				
				x.select(getContext(), R.id.account_from, R.string.account, accountCursor, accountAdapter,
						AccountColumns.ID, selectedAccountFromId);
				break;
			case R.id.account_to:				
				x.select(getContext(), R.id.account_to, R.string.account, accountCursor, accountAdapter,
						AccountColumns.ID, selectedAccountToId);
				break;
		}
	}

	@Override
	public void onSelectedId(int id, long selectedId) {
		super.onSelectedId(id, selectedId);
		switch (id) {
			case R.id.account_from:		
				selectFromAccount(selectedId);
				break;
			case R.id.account_to:
				selectToAccount(selectedId);
				break;
		}
	}
	
	private void selectFromAccount(long selectedId) {
        selectAccount(selectedId, true);
	}
	
	private void selectToAccount(long selectedId) {
        Account account = em.getAccount(selectedId);
        if (account != null) {
            selectAccount(account, accountToText, false);
            selectedAccountToId = selectedId;
            rateView.selectCurrencyTo(account.currency);
        }
	}

	@Override
	protected Account selectAccount(long accountId, boolean selectLast) {
        Account account = em.getAccount(accountId);
        if (account != null) {
            selectAccount(account, accountFromText, selectLast);
            selectedAccountFromId = accountId;
            rateView.selectCurrencyFrom(account.currency);
        }
        return account;
	}

    @Override
    protected void switchIncomeExpenseButton(Category category) {

    }

    protected void selectAccount(Account account, TextView accountText, boolean selectLast) {
        accountText.setText(account.title);
        if (selectLast && isRememberLastAccount) {
            selectToAccount(account.lastAccountId);
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
