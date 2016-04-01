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

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.flowzr.R;
import com.flowzr.db.DatabaseHelper;
import com.flowzr.model.Account;
import com.flowzr.model.Category;
import com.flowzr.model.Currency;
import com.flowzr.model.MyEntity;
import com.flowzr.model.Payee;
import com.flowzr.model.Total;
import com.flowzr.model.Transaction;
import com.flowzr.utils.CurrencyCache;
import com.flowzr.utils.MyPreferences;
import com.flowzr.utils.SplitAdjuster;
import com.flowzr.utils.TransactionUtils;
import com.flowzr.view.MyFloatingActionMenu;
import com.flowzr.widget.AmountInput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import static com.flowzr.utils.Utils.isNotEmpty;

public class TransactionActivity extends AbstractTransactionActivity {

    public static String IS_TRANSFER_EXTRA="IS_TRANSFER_EXTRAS";
    private final Currency currencyAsAccount = new Currency();

    private long idSequence = 0;
    private final IdentityHashMap<View, Transaction> viewToSplitMap = new IdentityHashMap<>();

	private TextView differenceText;
	private boolean isUpdateBalanceMode = false;
	private long currentBalance;


    private LinearLayout splitsLayout;
    private TextView unsplitAmountText;
    private TextView currencyText;


    private long selectedOriginCurrencyId = -1;


    public TransactionActivity() {
	}

	protected int getLayoutId() {
		return R.layout.transaction_free;
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.transaction_actions, menu);
        return true;
    }


    
	@Override
	protected void internalOnCreate() {
        initToolbar();
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(CURRENT_BALANCE_EXTRA)) {
				currentBalance = intent.getLongExtra(CURRENT_BALANCE_EXTRA, 0);
				isUpdateBalanceMode = true;
			} else if(intent.hasExtra(AMOUNT_EXTRA)) {
				currentBalance = intent.getLongExtra(AMOUNT_EXTRA, 0);
			}
		}
		if (transaction.isTemplateLike()) {
			setTitle(transaction.isTemplate() ? R.string.transaction_template : R.string.transaction_schedule);
			if (transaction.isTemplate()) {
				dateText.setEnabled(false);
				timeText.setEnabled(false);
			}
		}

        currencyAsAccount.name = getString(R.string.original_currency_as_account);

        ToggleButton toggleView = (ToggleButton) findViewById(R.id.toggle);
        if (transaction.fromAmount>0) {
            toggleView.setChecked(true);
        }
        toggleView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean income) {
                if (income) {
                    transaction.fromAmount = Math.abs(rateView.getFromAmount());
                } else {
                    transaction.fromAmount = -Math.abs(rateView.getFromAmount());
                }

                rateView.setFromAmount(transaction.fromAmount);
                Total t = new Total(rateView.getCurrencyFrom());
                t.balance = transaction.fromAmount;
                u.setTotal(totalText, t);
            }
        });


        final MyFloatingActionMenu menu1 = (MyFloatingActionMenu) findViewById(R.id.menu1);
        if (menu1!=null) {
            menu1.setOnMenuButtonClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    menu1.toggle(true);
                }
            });

            Handler mUiHandler = new Handler();
            List<MyFloatingActionMenu> menus = new ArrayList<>();
            menus.add(menu1);
            menu1.hideMenuButton(true);
            int delay = 400;
            for (final MyFloatingActionMenu menu : menus) {
                mUiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        menu.showMenuButton(true);
                    }
                }, delay);
                delay += 150;
            }
            menu1.setClosedOnTouchOutside(true);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    menu1.showMenuButton(true);
                }
            }, delay + 150);

            menu1.setOnMenuToggleListener(new MyFloatingActionMenu.OnMenuToggleListener() {
                @Override
                public void onMenuToggle(boolean opened) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            menu1.close(true);
                            menu1.close(true);
                        }
                    },3500);
                }
            });

            if (findViewById(R.id.saveAddButton)!=null) {
                findViewById(R.id.saveAddButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onOKClicked();
                        Intent intent2 = new Intent(getApplicationContext(), TransactionActivity.class);
                        intent2.putExtra(DATETIME_EXTRA, transaction.dateTime);
                        intent2.putExtra(ACCOUNT_ID_EXTRA, transaction.fromAccountId);
                        if (saveAndFinish()) {
                            intent2.putExtra(DATETIME_EXTRA, transaction.dateTime);
                            startActivityForResult(intent2, -1);
                        }
                    }
                });
            }


        }


	}

    protected void switchIncomeExpenseButton(Category category) {
        ToggleButton toggleView = (ToggleButton) findViewById(R.id.toggle);
        if (category.isIncome()) {
            toggleView.setChecked(true);
        } else if (category.isExpense()) {
            toggleView.setChecked(false);
        }
    }
// TODO Qucikwidget unsplit action grid
/**
    private void prepareUnsplitActionGrid() {
        if (isGreenDroidSupported()) {
            unsplitActionGrid = new QuickActionGrid(this);
            unsplitActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.ic_edit, R.string.transaction));
            unsplitActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.ic_swap_vert, R.string.transfer));
            unsplitActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.ic_action_split, R.string.unsplit_adjust_amount));
            unsplitActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.ic_action_split, R.string.unsplit_adjust_evenly));
            unsplitActionGrid.addQuickAction(new MyQuickAction(this, R.drawable.ic_action_split, R.string.unsplit_adjust_last));
            unsplitActionGrid.setOnQuickActionClickListener(unsplitActionListener);
        }
    }

    private QuickActionWidget.OnQuickActionClickListener unsplitActionListener = new QuickActionWidget.OnQuickActionClickListener() {
        public void onQuickActionClicked(QuickActionWidget widget, int position) {
            switch (position) {
                case 0:
                    createSplit(false);
                    break;
                case 1:
                    createSplit(true);
                    break;
                case 2:
                    unsplitAdjustAmount();
                    break;
                case 3:
                    unsplitAdjustEvenly();
                    break;
                case 4:
                    unsplitAdjustLast();
                    break;
            }
        }

    };
**/
    private void unsplitAdjustAmount() {
        long splitAmount = calculateSplitAmount();
        rateView.setFromAmount(splitAmount);
        updateUnsplitAmount();
    }

    private void unsplitAdjustEvenly() {
        long unsplitAmount = calculateUnsplitAmount();
        if (unsplitAmount != 0) {
            List<Transaction> splits = new ArrayList<>(viewToSplitMap.values());
            SplitAdjuster.adjustEvenly(splits, unsplitAmount);
            updateSplits();
        }
    }

    private void unsplitAdjustLast() {
        long unsplitAmount = calculateUnsplitAmount();
        if (unsplitAmount != 0) {
            Transaction latestTransaction = null;
            for (Transaction t : viewToSplitMap.values()) {
                if (latestTransaction == null || latestTransaction.id > t.id) {
                    latestTransaction = t;
                }
            }
            if (latestTransaction != null) {
                SplitAdjuster.adjustSplit(latestTransaction, unsplitAmount);
                updateSplits();
            }
        }
    }

    private void updateSplits() {
        for (Map.Entry<View, Transaction> entry : viewToSplitMap.entrySet()) {
            View v = entry.getKey();
            Transaction split = entry.getValue();
            setSplitData(v, split);
        }
        updateUnsplitAmount();
    }

    @Override
    protected void fetchCategories() {
        categorySelector.fetchCategories(!isUpdateBalanceMode);
    }

    @Override
    protected void fetchCategories(long[] cids) {
        categorySelector.fetchCategories(cids);
    }

    @Override
	protected void createListNodes(LinearLayout layout) {
        rateView.createTransactionUI();
        // difference
        if (isUpdateBalanceMode) {
            differenceText = x.addInfoNode(layout, -1, R.string.difference, "0");
            rateView.setFromAmount(currentBalance);
            rateView.setAmountFromChangeListener(new AmountInput.OnAmountChangedListener() {
                @Override
                public void onAmountChanged(long oldAmount, long newAmount) {
                    long balanceDifference = newAmount - currentBalance;
                    u.setAmountText(differenceText, rateView.getCurrencyFrom(), balanceDifference, true);
                    Total t = new Total(rateView.getCurrencyFrom());
                    t.balance=currentBalance;
                    u.setTotal(totalText,t);
                }
            });
            if (currentBalance > 0) {
                rateView.setIncome();
            } else {
                rateView.setExpense();
            }
        } else {
            if (currentBalance > 0) {
                rateView.setIncome();
            } else {
                rateView.setExpense();
            }



            createSplitsLayout(layout);
            rateView.setAmountFromChangeListener(new AmountInput.OnAmountChangedListener() {
                @Override
                public void onAmountChanged(long oldAmount, long newAmount) {
                    updateUnsplitAmount();
                    Total t = new Total(rateView.getCurrencyFrom());
                    t.balance=newAmount;
                    u.setTotal(totalText,t);
                }
            });
        }

        if (!isUpdateBalanceMode && MyPreferences.isShowCurrency(this)) {
            currencyText = x.addListNode2(layout, R.id.original_currency, R.drawable.ic_attach_money,R.string.currency, getResources().getString(R.string.original_currency_as_account));
        } else {
            currencyText = new TextView(this);
        }

		//account(LinearLayout layout, int id,int drawableId, int labelId, String defaultValue)
		accountText = x.addListNode2(layout, R.id.account, R.drawable.ic_account_balance_wallet, R.string.account, getResources().getString(R.string.select_account));
        //amount



        //payee
        isShowPayee = MyPreferences.isShowPayee(this);
        if (isShowPayee) {
            createPayeeNode(layout);
            payeeText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long id) {
                    if (isRememberLastCategory) {
                        selectLastCategoryForPayee(id);
                    }
                }
            });
        }
		//category
        categorySelector.createNode(layout, true);


	}

    private void selectLastCategoryForPayee(long id) {
        Payee p = em.get(Payee.class, id);
        if (p != null) {
            categorySelector.selectCategory(p.lastCategoryId);
        }
    }

    private void createSplitsLayout(LinearLayout layout) {
        splitsLayout = new LinearLayout(this);
        splitsLayout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(splitsLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void addOrRemoveSplits() {
        if (splitsLayout == null) {
            return;
        }
        if (categorySelector.isSplitCategorySelected()) {
            View v = x.addNodeUnsplit(splitsLayout);
            unsplitAmountText = (TextView)v.findViewById(R.id.data);
            updateUnsplitAmount();
        } else {
            splitsLayout.removeAllViews();
        }
    }

    private void updateUnsplitAmount() {
        if (unsplitAmountText != null) {
            long amountDifference = calculateUnsplitAmount();
            u.setAmountText(unsplitAmountText, rateView.getCurrencyFrom(), amountDifference, false);
        }
    }

    private long calculateUnsplitAmount() {
        long splitAmount = calculateSplitAmount();
        return rateView.getFromAmount()-splitAmount;
    }

    private long calculateSplitAmount() {
        long amount = 0;
        for (Transaction split : viewToSplitMap.values()) {
            amount += split.fromAmount;
        }
        return amount;
    }



	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (hasFocus) {
			accountText.requestFocusFromTouch();
		}
	}

	@Override
	protected boolean onOKClicked() {
		if (checkSelectedId(getSelectedAccountId(), R.string.select_account) &&
            checkUnsplitAmount()) {
			updateTransactionFromUI();
			return true;
		}
		return false;
	}

    private boolean checkUnsplitAmount() {
        if (categorySelector.isSplitCategorySelected()) {
            long unsplitAmount = calculateUnsplitAmount();
            if (unsplitAmount != 0) {
                Toast.makeText(this, R.string.unsplit_amount_greater_than_zero, Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    @Override
	protected void editTransaction(Transaction transaction) {
        selectAccount(transaction.fromAccountId, false);
        commonEditTransaction(transaction);
        selectCurrency(transaction);
        fetchSplits();
        selectPayee(transaction.payeeId);
	}

    private void selectCurrency(Transaction transaction) {
        if (transaction.originalCurrencyId > 0) {
            selectOriginalCurrency(transaction.originalCurrencyId);
            rateView.setFromAmount(transaction.originalFromAmount);
            rateView.setToAmount(transaction.fromAmount);
        } else {
            rateView.setFromAmount(transaction.fromAmount);
        }
        Total t = new Total(rateView.getCurrencyFrom());
        t.balance=transaction.fromAmount;
        u.setTotal(totalText,t);
    }

    private void fetchSplits() {
        List<Transaction> splits = em.getSplitsForTransaction(transaction.id);
        for (Transaction split : splits) {
            split.categoryAttributes = db.getAllAttributesForTransaction(split.id);
            if (split.originalCurrencyId > 0) {
                split.fromAmount = split.originalFromAmount;
            }
            addOrEditSplit(split);
        }
    }

    private void updateTransactionFromUI() {
		updateTransactionFromUI(transaction);
        transaction.fromAccountId = selectedAccount.id;
		long amount = rateView.getFromAmount();
		if (isUpdateBalanceMode) {
			amount -= currentBalance;
		}
		transaction.fromAmount = amount;
        updateTransactionOriginalAmount();
        if (categorySelector.isSplitCategorySelected()) {
            transaction.splits = new LinkedList<>(viewToSplitMap.values());
        } else {
            transaction.splits = null;
        }
	}

    private void updateTransactionOriginalAmount() {
        if (isDifferentCurrency()) {
            transaction.originalCurrencyId = selectedOriginCurrencyId;
            transaction.originalFromAmount = rateView.getFromAmount();
            transaction.fromAmount = rateView.getToAmount();
        } else {
            transaction.originalCurrencyId = 0;
            transaction.originalFromAmount = 0;
        }
    }

    private boolean isDifferentCurrency() {
        return selectedOriginCurrencyId > 0 && selectedOriginCurrencyId != selectedAccount.currency.id;
    }


    @Override
    protected Account selectAccount(long accountId, boolean selectLast) {
        Account a = super.selectAccount(accountId, selectLast);
        if (a != null) {
            if (selectLast && !isShowPayee && isRememberLastCategory) {
                categorySelector.selectCategory(a.lastCategoryId);
            }
        }
        if (selectedOriginCurrencyId > 0) {
            selectOriginalCurrency(selectedOriginCurrencyId);
        }
        return a;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_settings:
                Intent intent = new Intent(this.getApplicationContext(), TransactionPreferencesActivity.class);
                startActivityForResult(intent, BLOTTER_PREFERENCES);
                return true;
            case R.id.saveButton:
        		onOKClicked();
        		 saveAndFinish();
        		return true;
        	case R.id.saveAddButton:
        		onOKClicked();
                Intent intent2= getIntent();
        		intent2.putExtra(DATETIME_EXTRA, transaction.dateTime);
        		if (saveAndFinish()) { 
                    intent2.putExtra(DATETIME_EXTRA, transaction.dateTime);
                    startActivityForResult(intent2, -1);
                    return true;
                }
        	case R.id.action_cancel:
                setResult(RESULT_CANCELED);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    

    @Override
    protected void onClick(View v, int id) {
        super.onClick(v, id);
        switch (id) {
            case R.id.unsplit_action:
                showQuickActionsDialog();
                break;
            case R.id.add_split:
                createSplit(false);
                break;
            case R.id.add_split_transfer:
                if (selectedOriginCurrencyId > 0) {
                    Toast.makeText(this, R.string.split_transfer_not_supported_yet, Toast.LENGTH_LONG).show();
                    break;
                }
                createSplit(true);
                break;
            case R.id.delete_split:
                View parentView = (View)v.getParent();
                deleteSplit(parentView);
                break;
            case R.id.original_currency:
                List<Currency> currencies = em.getAllCurrenciesList();
                currencies.add(0, currencyAsAccount);
                ListAdapter adapter = TransactionUtils.createCurrencyAdapter(this, currencies);
                int selectedPos = MyEntity.indexOf(currencies, selectedOriginCurrencyId);
                x.selectItemId(this, R.id.currency, R.string.currency, adapter, selectedPos);
                break;
        }
        Transaction split = viewToSplitMap.get(v);
        if (split != null) {
            split.unsplitAmount = split.fromAmount + calculateUnsplitAmount();
            editSplit(split, split.toAccountId > 0 ? SplitTransferActivity.class : SplitTransactionActivity.class);
        }
    }


    @Override
    public void onSelectedId(int id, long selectedId) {
        super.onSelectedId(id, selectedId);
        switch (id) {
            case R.id.currency:
                selectOriginalCurrency(selectedId);
                break;
        }
    }

    private void selectOriginalCurrency(long selectedId) {
        selectedOriginCurrencyId = selectedId;
        if (selectedId == -1) {
            if (selectedAccount != null) {
                if (selectedAccount.currency.id == rateView.getCurrencyToId()) {
                    rateView.setFromAmount(rateView.getToAmount());
                }
            }
            selectAccountCurrency();
        } else {
            long toAmount = rateView.getToAmount();
            Currency currency = CurrencyCache.getCurrency(em, selectedId);
            rateView.selectCurrencyFrom(currency);
            if (selectedAccount != null) {
                if (selectedId == selectedAccount.currency.id) {
                    if (selectedId == rateView.getCurrencyToId()) {
                        rateView.setFromAmount(toAmount);
                    }
                    selectAccountCurrency();
                    return;
                }
                rateView.selectCurrencyTo(selectedAccount.currency);
            }
            currencyText.setText(currency.name);
        }
    }

    private void selectAccountCurrency() {
        rateView.selectSameCurrency(selectedAccount != null ? selectedAccount.currency : Currency.EMPTY);
        currencyText.setText(R.string.original_currency_as_account);
    }

    private void showQuickActionsDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.unsplit_amount)
            .setItems(R.array.unsplit_quick_action_items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // TODO show unsplit dialog
                    //unsplitActionListener.onQuickActionClicked(unsplitActionGrid, i);
                }
            })
            .show();
    }


    private void createSplit(boolean asTransfer) {
        Transaction split = new Transaction();
        split.id = --idSequence;
        split.fromAccountId = getSelectedAccountId();
        split.fromAmount = split.unsplitAmount = calculateUnsplitAmount();
        split.originalCurrencyId = selectedOriginCurrencyId;
        editSplit(split, asTransfer ? SplitTransferActivity.class : SplitTransactionActivity.class);
    }

    private void editSplit(Transaction split, Class splitActivityClass) {
        Intent intent = new Intent(this, splitActivityClass);
        split.toIntentAsSplit(intent);
        startActivityForResult(intent, SPLIT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPLIT_REQUEST) {
            if (resultCode == RESULT_OK) {
                Transaction split = Transaction.fromIntentAsSplit(data);
                addOrEditSplit(split);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            if (categorySelector.isSplitCategorySelected()) {
                ActivityState state = new ActivityState();
                state.categoryId = categorySelector.getSelectedCategoryId();
                state.idSequence = idSequence;
                state.splits = new ArrayList<>(viewToSplitMap.values());
                ByteArrayOutputStream s = new ByteArrayOutputStream();
                //noinspection TryFinallyCanBeTryWithResources
                try {
                    ObjectOutputStream out = new ObjectOutputStream(s);
                    out.writeObject(state);
                    outState.putByteArray(ACTIVITY_STATE, s.toByteArray());
                } finally {
                    s.close();
                }
            }
        } catch (IOException e) {
            Log.e("Financisto", "Unable to save state", e);
        }
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        byte[] bytes = savedInstanceState.getByteArray(ACTIVITY_STATE);
        if (bytes != null) {
            try {
                ByteArrayInputStream s = new ByteArrayInputStream(bytes);
                try {
                    ObjectInputStream in = new ObjectInputStream(s);
                    ActivityState state = (ActivityState) in.readObject();
                    if (state.categoryId == Category.SPLIT_CATEGORY_ID) {
                        Log.d("Financisto", "Restoring splits...");
                        viewToSplitMap.clear();
                        splitsLayout.removeAllViews();
                        idSequence = state.idSequence;
                        categorySelector.selectCategory(state.categoryId);
                        for (Transaction split : state.splits) {
                            addOrEditSplit(split);
                        }
                    }
                } finally {
                    s.close();
                }
            } catch (Exception e) {
                Log.e("Financisto", "Unable to restore state", e);
            }
        }
    }

    private void addOrEditSplit(Transaction split) {
        View v = findView(split);
        if (v  == null) {
            v = x.addSplitNodeMinus(splitsLayout, R.id.edit_aplit, R.id.delete_split, R.string.split, "");
        }
        setSplitData(v, split);
        viewToSplitMap.put(v, split);
        updateUnsplitAmount();
    }

    private View findView(Transaction split) {
        for (Map.Entry<View, Transaction> entry : viewToSplitMap.entrySet()) {
            Transaction s = entry.getValue();
            if (s.id == split.id) {
                return  entry.getKey();
            }
        }
        return null;
    }

    private void setSplitData(View v, Transaction split) {
        TextView label = (TextView)v.findViewById(R.id.label);
        TextView data = (TextView)v.findViewById(R.id.data);
        setSplitData(split, label, data);
    }

    private void setSplitData(Transaction split, TextView label, TextView data) {
        if (split.isTransfer()) {
            setSplitDataTransfer(split, label, data);
        } else {
            setSplitDataTransaction(split, label, data);
        }
    }

    private void setSplitDataTransaction(Transaction split, TextView label, TextView data) {
        label.setText(createSplitTransactionTitle(split));
        Currency currency = getCurrency();
        u.setAmountText(data, currency, split.fromAmount, false);
    }

    private String createSplitTransactionTitle(Transaction split) {
        StringBuilder sb = new StringBuilder();
        Category category = db.getCategory(split.categoryId);
        sb.append(category.title);
        if (isNotEmpty(split.note)) {
            sb.append(" (").append(split.note).append(")");
        }
        return sb.toString();
    }

    private void setSplitDataTransfer(Transaction split, TextView label, TextView data) {
        Account fromAccount = em.getAccount(split.fromAccountId);
        Account toAccount = em.getAccount(split.toAccountId);
        if (label!=null) {
        	u.setTransferTitleText(label, fromAccount, toAccount);
        }
        u.setTransferAmountText(data, fromAccount.currency, split.fromAmount, toAccount.currency, split.toAmount);

    }

    private void deleteSplit(View v) {
        Transaction split = viewToSplitMap.remove(v);
        if (split != null) {
            removeSplitView(v);
            updateUnsplitAmount();
            if (split.remoteKey!=null) {
            	db.writeDeleteLog(DatabaseHelper.TRANSACTION_TABLE, split.remoteKey);
            }
        }
    }

    private void removeSplitView(View v) {
        splitsLayout.removeView(v);
        View dividerView = (View)v.getTag();
        if (dividerView != null) {
            splitsLayout.removeView(dividerView);
        }
    }

    private Currency getCurrency() {
        if (selectedOriginCurrencyId > 0) {
            return CurrencyCache.getCurrency(em, selectedOriginCurrencyId);
        }
        if (selectedAccount != null) {
            return selectedAccount.currency;
        }
        return Currency.EMPTY;
    }

    @Override
    protected void onDestroy() {
        if (payeeAdapter != null) {
            payeeAdapter.changeCursor(null);
        }
        super.onDestroy();
    }

    private static class ActivityState implements Serializable {
        public long categoryId;
        public long idSequence;
        public List<Transaction> splits;
    }


}
