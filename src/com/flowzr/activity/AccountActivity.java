/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk - adding bill filtering parameters
 *     Emmanuel Florent - port to Android API 11+
 ******************************************************************************/
package com.flowzr.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.flowzr.R;
import com.flowzr.adapter.EntityEnumAdapter;
import com.flowzr.model.*;
import com.flowzr.utils.TransactionUtils;
import com.flowzr.utils.Utils;
import com.flowzr.widget.AmountInput;

import static com.flowzr.utils.Utils.text;

public class AccountActivity extends AbstractEditorActivity {
	
	public static final String ACCOUNT_ID_EXTRA = "accountId";
	
	private static final int NEW_CURRENCY_REQUEST = 1;
	private static final int REQUEST_NEW_CURRENCY =  667;

	private AmountInput amountInput;
	private AmountInput limitInput;
	private View limitAmountView;
	private EditText accountTitle;

	private Cursor currencyCursor;
	private TextView currencyText;
	private View accountTypeNode;
	private View cardIssuerNode;
	private View issuerNode;
	private EditText numberText;
	private View numberNode;
	private EditText issuerName;
	private EditText sortOrderText;
	private CheckBox isIncludedIntoTotals;
    private EditText noteText;
    private EditText closingDayText;
    private EditText paymentDayText;
    private View closingDayNode;
    private View paymentDayNode;

	private EntityEnumAdapter<AccountType> accountTypeAdapter;
	private EntityEnumAdapter<CardIssuer> cardIssuerAdapter;
	private ListAdapter currencyAdapter;	

	private Account account = new Account();

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		return true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account);
		initToolbar();

		LinearLayout layout = (LinearLayout)findViewById(R.id.layout);

		LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		accountTitle= (EditText) findViewById(R.id.title);
		accountTitle.setSingleLine();

		accountTypeAdapter = new EntityEnumAdapter<AccountType>(this, AccountType.values());
		accountTypeNode = x.addListNodeIcon(layout, R.id.account_type, R.string.account_type, R.string.account_type);

		cardIssuerAdapter = new EntityEnumAdapter<CardIssuer>(this, CardIssuer.values());
		cardIssuerNode = x.addListNodeIcon(layout, R.id.card_issuer, R.string.card_issuer, R.string.card_issuer);
		setVisibility(cardIssuerNode, View.GONE);

		currencyCursor = em.getAllCurrencies("name");
		startManagingCursor(currencyCursor);
		currencyAdapter = TransactionUtils.createCurrencyAdapter(this, currencyCursor);
		currencyText = x.addListNode2(layout, R.id.currency, android.R.color.transparent, R.id.currency_add, getResources().getString(R.string.select_currency));

		Intent intent = getIntent();
		if (intent != null) {
			long accountId = intent.getLongExtra(ACCOUNT_ID_EXTRA, -1);
			if (accountId != -1) {
				this.account = em.getAccount(accountId);
				if (this.account == null) {
					this.account = new Account();
				}
			}
		}


		amountInput = new AmountInput(this);
		amountInput.setOwner(this);
		limitInput = new AmountInput(this);
		limitInput.setOwner(this);

		limitInput.setExpense();
		limitInput.setColor(getResources().getColor(R.color.negative_amount));
		limitInput.disableIncomeExpenseButton();
		limitAmountView = x.addEditNode2(layout, R.drawable.ic_network_locked_white_48dp, R.string.limit_amount,limitInput);
		setVisibility(limitAmountView, View.GONE);

		if (account.id == -1) {
			x.addEditNode(layout, R.string.opening_amount, amountInput);
			amountInput.setIncome();
			amountInput.setColor(R.color.f_blue_lighter1);
		}


		noteText = (EditText) layoutInflater.inflate(R.layout.edit_text, null);
		noteText.setLines(2);
		noteText.setHint(R.string.note);
		x.addEditNode2(layout, R.drawable.ic_subject_white_48dp, noteText);

		issuerName = (EditText) layoutInflater.inflate(R.layout.edit_text, null);
		x.addEditNode2(layout, R.drawable.ic_contact_phone_white_48dp,issuerName);
		issuerName.setHint(R.string.issuer);
		issuerName.setSingleLine();

		numberText = (EditText) layoutInflater.inflate(R.layout.edit_text, null);
		numberNode=x.addEditNode2(layout, R.drawable.ic_vpn_key_white_48dp, numberText);
		numberText.setHint(R.string.card_number);
		numberText.setSingleLine();
		setVisibility(numberNode, View.GONE);

		closingDayText = (EditText) layoutInflater.inflate(R.layout.edit_text, null);
		closingDayText.setInputType(InputType.TYPE_CLASS_NUMBER);
		closingDayText.setHint(R.string.closing_day_hint);
		closingDayText.setSingleLine();
		closingDayNode = x.addEditNode2(layout, R.drawable.ic_schedule_white_48dp,R.string.closing_day, closingDayText);
		setVisibility(closingDayNode, View.GONE);

		paymentDayText = (EditText) layoutInflater.inflate(R.layout.edit_text, null);
		paymentDayText.setInputType(InputType.TYPE_CLASS_NUMBER);
		paymentDayText.setHint(R.string.payment_day_hint);
		paymentDayText.setSingleLine();
		paymentDayNode = x.addEditNode2(layout, R.drawable.ic_today_white_48dp,R.string.payment_day, paymentDayText);
		setVisibility(paymentDayNode, View.GONE);

		isIncludedIntoTotals = x.addCheckboxNode(layout,
				R.id.is_included_into_totals, R.string.is_included_into_totals,R.drawable.ic_functions_white_48dp,
				R.string.is_included_into_totals_summary, true);

		sortOrderText = (EditText) layoutInflater.inflate(R.layout.edit_text, null);
		sortOrderText.setInputType(InputType.TYPE_CLASS_NUMBER);
		sortOrderText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
		sortOrderText.setSingleLine();
		x.addEditNode2(layout, R.drawable.ic_sort_white_48dp, R.string.sort_order, sortOrderText);



		if (account.id > 0) {
			selectAccountType(AccountType.valueOf(account.type));
			editAccount();
			noteText.requestFocus();
		} else {
			accountTitle.setText("");
			accountTitle.requestFocus();
		}

		findViewById(R.id.saveButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				saveAndFinish();
			}
		});


	}

	public boolean saveAndFinish() {
		if (account.currency == null) {
			Toast.makeText(AccountActivity.this, R.string.select_currency, Toast.LENGTH_SHORT).show();
			return false;
		}
		if (Utils.isEmpty(accountTitle)) {
			accountTitle.setError(getString(R.string.title));
			return false;
		}
		AccountType type = AccountType.valueOf(account.type);
		if (type.hasIssuer) {
			account.issuer = Utils.text(issuerName);
		}
		if (type.hasNumber) {
			account.number = Utils.text(numberText);
		}

		/********** validate closing and payment days **********/
		if (type.isCreditCard) {
			String closingDay = Utils.text(closingDayText);
			account.closingDay = closingDay == null ? 0 : Integer.parseInt(closingDay);
			if (account.closingDay != 0) {
				if (account.closingDay>31) {
					Toast.makeText(AccountActivity.this, R.string.closing_day_error, Toast.LENGTH_SHORT).show();
					return false;
				}
			}

			String paymentDay = Utils.text(paymentDayText);
			account.paymentDay = paymentDay == null ? 0 : Integer.parseInt(paymentDay);
			if (account.paymentDay != 0) {
				if (account.paymentDay>31) {
					Toast.makeText(AccountActivity.this, R.string.payment_day_error, Toast.LENGTH_SHORT).show();
					return false;
				}
			}
		}

		account.title = text(accountTitle);
		account.creationDate = System.currentTimeMillis();
		String sortOrder = text(sortOrderText);
		account.sortOrder = sortOrder == null ? 0 : Integer.parseInt(sortOrder);
		account.isIncludeIntoTotals  = isIncludedIntoTotals.isChecked();
		account.limitAmount = -Math.abs(limitInput.getAmount());
		account.note = text(noteText);

		long accountId = em.saveAccount(account);
		long amount = amountInput.getAmount();
		if (amount != 0) {
			Transaction t = new Transaction();
			t.fromAccountId = accountId;
			t.categoryId = 0;
			t.note = getResources().getText(R.string.opening_amount) + " (" +account.title + ")";
			t.fromAmount = amount;
			db.insertOrUpdate(t, null);
		}
		Intent intent = new Intent();
		intent.putExtra(ACCOUNT_ID_EXTRA, accountId);
		setResult(RESULT_OK, intent);
		finish();
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {	
        	case R.id.action_done:
				saveAndFinish();
        		return true;
        	case R.id.action_cancel:
				setResult(RESULT_CANCELED);
				finish();
        		return true;
        	
        }
        return super.onOptionsItemSelected(item);
    }
	
	@Override
	protected void onClick(View v, int id) {
		switch(id) {
			case R.id.is_included_into_totals:
				isIncludedIntoTotals.performClick();
				break;
			case R.id.account_type:
				x.selectPosition(this, R.id.account_type, R.string.account_type, accountTypeAdapter, AccountType.valueOf(account.type).ordinal());
				break;
			case R.id.card_issuer:				
				x.selectPosition(this, R.id.card_issuer, R.string.card_issuer, cardIssuerAdapter, 
						account.cardIssuer != null ? CardIssuer.valueOf(account.cardIssuer).ordinal() : 0);
				break;
			case R.id.currency:
				x.selectWithAddOption(this, R.id.currency, R.string.currency, currencyCursor, currencyAdapter,
						"_id", account.currency != null ? account.currency.id : -1, R.string.new_currency,REQUEST_NEW_CURRENCY);
				break;
			case R.id.currency_add:
                addNewCurrency();
				break;
		}
	}

    private void addNewCurrency() {
        new CurrencySelector(this, em, new CurrencySelector.OnCurrencyCreatedListener() {
            @Override
            public void onCreated(long currencyId) {
                if (currencyId == 0) {
                    Intent intent = new Intent(AccountActivity.this, CurrencyActivity.class);
                    startActivityForResult(intent, NEW_CURRENCY_REQUEST);
                } else {
                    currencyCursor.requery();
                    selectCurrency(currencyId);
                }
            }
        }).show();
    }

    @Override
	public void onSelectedId(int id, long selectedId) {
		switch(id) {
			case R.id.currency:
				if (selectedId==REQUEST_NEW_CURRENCY) {
					addNewCurrency();
				} else {
					selectCurrency(selectedId);
				}
				break;
		}
	}

	@Override
	public void onSelectedPos(int id, int selectedPos) {
		switch(id) {
			case R.id.account_type:
				AccountType type = AccountType.values()[selectedPos];
				selectAccountType(type);
				break;
			case R.id.card_issuer:
				CardIssuer issuer = CardIssuer.values()[selectedPos];
				selectCardIssuer(issuer);
				break;
		}
	}

	private void selectAccountType(AccountType type) {
		ImageView icon = (ImageView)accountTypeNode.findViewById(R.id.icon);
		icon.setImageResource(type.iconId);
		TextView label = (TextView)accountTypeNode.findViewById(R.id.label);
		label.setText(type.titleId);

		setVisibility(cardIssuerNode, type.isCard ? View.VISIBLE : View.GONE);
		//setVisibility(issuerNode, type.hasIssuer ? View.VISIBLE : View.GONE);
		setVisibility(numberNode, type.hasNumber ? View.VISIBLE : View.GONE);
		setVisibility(closingDayNode, type.isCreditCard ? View.VISIBLE : View.GONE);
		setVisibility(paymentDayNode, type.isCreditCard ? View.VISIBLE : View.GONE);

		setVisibility(limitAmountView, type == AccountType.CREDIT_CARD ? View.VISIBLE : View.GONE);
		account.type = type.name();
		selectCardIssuer(account.cardIssuer != null 
				? CardIssuer.valueOf(account.cardIssuer)
				: CardIssuer.VISA);
	}

	private void selectCardIssuer(CardIssuer issuer) {
		ImageView icon = (ImageView)cardIssuerNode.findViewById(R.id.icon);
		icon.setImageResource(issuer.iconId);
		TextView label = (TextView)cardIssuerNode.findViewById(R.id.label);
		label.setText(issuer.titleId);
		account.cardIssuer = issuer.name();
	}

	private void selectCurrency(long currencyId) {
        Currency c = em.get(Currency.class, currencyId);
		if (c != null) {
			selectCurrency(c);
		}
	}
	
	private void selectCurrency(Currency c) {
		currencyText.setText(c.name);
		amountInput.setCurrency(c);
		limitInput.setCurrency(c);
		account.currency = c;		
	}

	private void editAccount() {
		selectAccountType(AccountType.valueOf(account.type));
		if (account.cardIssuer != null) {
			selectCardIssuer(CardIssuer.valueOf(account.cardIssuer));
		}
		selectCurrency(account.currency);
		accountTitle.setText(account.title);
		issuerName.setText(account.issuer);
		numberText.setText(account.number);
		sortOrderText.setText(String.valueOf(account.sortOrder));
		
		/******** bill filtering ********/
		if (account.closingDay>0) {
			closingDayText.setText(String.valueOf(account.closingDay));
		} 
		if (account.paymentDay>0) {
			paymentDayText.setText(String.valueOf(account.paymentDay));
		}
		/********************************/		
		
		isIncludedIntoTotals.setChecked(account.isIncludeIntoTotals);
		if (account.limitAmount != 0) {
			limitInput.setAmount(-Math.abs(account.limitAmount));
		}
        noteText.setText(account.note);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (amountInput.processActivityResult(requestCode, data)) {
				return;
			}
			if (limitInput.processActivityResult(requestCode, data)) {
				return;
			}
			switch(requestCode) {
			case NEW_CURRENCY_REQUEST:
				currencyCursor.requery();
				long currencyId = data.getLongExtra(CurrencyActivity.CURRENCY_ID_EXTRA, -1);
				if (currencyId != -1) {
					selectCurrency(currencyId);
				}
				break;
			}
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}	
		
}
