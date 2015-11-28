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
package com.flowzr.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.flowzr.R;
import com.flowzr.graph.Amount;
import com.flowzr.model.*;
import com.flowzr.utils.RecurUtils;
import com.flowzr.utils.RecurUtils.Recur;
import com.flowzr.utils.Utils;
import com.flowzr.widget.AmountInput;
import com.flowzr.widget.CalculatorInput;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BudgetActivity extends AbstractEditorActivity {
	
	public static final String BUDGET_ID_EXTRA = "budgetId";

	private static final int NEW_CATEGORY_REQUEST = 1;
	private static final int NEW_PROJECT_REQUEST = 2;
	private static final int RECUR_REQUEST = 3;
	public static final int CALCULATOR_REQUEST = 4 ;

	private AmountInput amountInput;

	private EditText titleText;
	private TextView categoryText;
	private TextView projectText;
	private TextView accountText;
	private TextView periodRecurText;
	private CheckBox cbMode;
	private CheckBox cbIncludeSubCategories;
	private CheckBox cbIncludeCredit;
    private CheckBox cbSavingBudget;

	private Budget budget = new Budget();

    private List<AccountOption> accountOptions;
	private List<Category> categories;
	private List<Project> projects;

    private ListAdapter accountAdapter;
    private int selectedAccountOption;
	private TextView totalText;

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //getMenuInflater().inflate(R.menu.ok_cancel, menu);
        return true;
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.budget);
		initToolbar();
        accountOptions = createAccountsList();
        accountAdapter = new ArrayAdapter<AccountOption>(this, android.R.layout.simple_spinner_dropdown_item, accountOptions);

		categories = db.getCategoriesList(true);
		projects = em.getActiveProjectsList(true);
		
		LinearLayout layout = (LinearLayout) findViewById(R.id.list);
		LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		titleText = (EditText) findViewById(R.id.title);
		// (EditText) layoutInflater.inflate(R.layout.edit_text, null);
		//x.addEditNode(layout, R.string.title, titleText);

		amountInput = new AmountInput(this);
		amountInput.setOwner(this);
		amountInput.setIncome();
		//amountInput.disableIncomeExpenseButton();

		//x.addEditNode(layout, R.string.amount, amountInput);

		periodRecurText = x.addListNode2(layout, R.id.period_recur,R.drawable.ic_repeat_white_48dp, R.string.period_recur,  getResources().getString(R.string.no_recur));
		accountText = x.addListNode2(layout, R.id.account, R.drawable.ic_action_accounts, R.string.account, getResources().getString(R.string.select_account));
		categoryText=x.addListNodeCategory(layout);
		projectText = x.addListNode2(layout, R.id.project, R.drawable.ic_action_important, R.string.project, getResources().getString(R.string.no_projects));

		cbIncludeSubCategories = x.addCheckboxNode(layout,
				R.id.include_subcategories, R.string.include_subcategories,R.drawable.ic_expand_more_white_48dp,
				R.string.include_subcategories_summary, true);

		cbMode = x.addCheckboxNode(layout, R.id.budget_mode,R.string.budget_mode,R.drawable.ic_select_all_white_48dp,
				R.string.budget_mode_summary, false);
        cbIncludeCredit = x.addCheckboxNode(layout,
                R.id.include_credit, R.string.include_credit,R.drawable.ic_toggle_income,
                R.string.include_credit_summary, true);
        cbSavingBudget = x.addCheckboxNode(layout,
				R.id.type, R.string.budget_type_saving, R.drawable.account_type_asset,
				R.string.budget_type_saving_summary, false);

		if (findViewById(R.id.saveButton)!=null) {
			findViewById(R.id.saveButton).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (checkSelected(budget.currency != null ? budget.currency : budget.account, R.string.select_account)) {
						updateBudgetFromUI();
						long id = em.insertBudget(budget);
						Intent intent = new Intent();
						intent.putExtra(BUDGET_ID_EXTRA, id);
						setResult(RESULT_OK, intent);
						finish();
					}
				}
			});
		}

		totalText = ( TextView ) findViewById(R.id.total);
		totalText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				Intent intent = new Intent(getApplicationContext(), CalculatorInput.class);
				if (budget.currencyId > -1) {
					intent.putExtra(AmountInput.EXTRA_CURRENCY, budget.currencyId);
				}
				intent.putExtra(AmountInput.EXTRA_AMOUNT, totalText.getText().toString().trim());
				startActivityForResult(intent, CALCULATOR_REQUEST);
			}
		});

		Intent intent = getIntent();
		if (intent != null) {
			long id = intent.getLongExtra(BUDGET_ID_EXTRA, -1);
			if (id != -1) {
				budget = em.load(Budget.class, id);
				editBudget();
			} else {
				selectRecur(RecurUtils.createDefaultRecur().toString());
			}
		}
		titleText.requestFocus();
		//ImageButton toggle = (ImageButton) findViewById(R.id.toggle);
	}



    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {	
        	case R.id.action_done:
				if (checkSelected(budget.currency != null ? budget.currency : budget.account, R.string.select_account)) {
					updateBudgetFromUI();
					long id = em.insertBudget(budget);
					Intent intent = new Intent();
					intent.putExtra(BUDGET_ID_EXTRA, id);
					setResult(RESULT_OK, intent);
					finish();
				}
        		return true;
        	case R.id.action_cancel:
                setResult(RESULT_CANCELED);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
	
    private List<AccountOption> createAccountsList() {
        List<AccountOption> accounts = new ArrayList<AccountOption>();
        List<Currency> currenciesList = em.getAllCurrenciesList("name");
        for (Currency currency : currenciesList) {
            String title = getString(R.string.account_by_currency, currency.name);
            accounts.add(new AccountOption(title, currency, null));
        }
        List<Account> accountsList = em.getAllAccountsList();
        for (Account account : accountsList) {
            accounts.add(new AccountOption(account.title, null, account));
        }
        return accounts;
    }

    private void editBudget() {
		titleText.setText(budget.title);

		totalText = ( TextView ) findViewById(R.id.total);
		Total t =new Total(budget.currency);
		t.balance=budget.amount;
		u = new Utils(this);
		u.setTotal(totalText, t);
        totalText.setTextColor(getResources().getColor(R.color.f_blue_lighter1));
		//amountInput.setAmount(budget.amount);
		updateEntities(this.categories, budget.categories);
		selectCategories();
		updateEntities(this.projects, budget.projects);
		selectProjects();
		selectAccount(budget);
		selectRecur(budget.recur);
		cbIncludeSubCategories.setChecked(budget.includeSubcategories);
		cbIncludeCredit.setChecked(budget.includeCredit);
		cbMode.setChecked(budget.expanded);
        cbSavingBudget.setChecked(budget.amount < 0);
	}

	private void updateEntities(List<? extends MyEntity> list, String selected) {
		if (!Utils.isEmpty(selected)) {
			String[] a = selected.split(",");
			for (String s : a) {
				long id = Long.parseLong(s);
				for (MyEntity e : list) {
					if (e.id == id) {
						e.checked = true;
						break;
					}
				}
			}
		}
	}
	
	private String getSelectedAsString(List<? extends MyEntity> list) {
		StringBuilder sb = new StringBuilder();
		for (MyEntity e : list) {
			if (e.checked) {
				if (sb.length() > 0) {
					sb.append(",");
				}
				sb.append(e.id);
			}
		}
		return sb.length() > 0 ? sb.toString() : "";
	}

	protected void updateBudgetFromUI() {
		budget.title = titleText.getText().toString();
        if (cbSavingBudget.isChecked()) {
            budget.amount = -budget.amount;
        }
		budget.includeSubcategories = cbIncludeSubCategories.isChecked();
		budget.includeCredit = cbIncludeCredit.isChecked();
		budget.expanded = cbMode.isChecked();
		budget.categories = getSelectedAsString(categories);
		budget.projects = getSelectedAsString(projects);
	}

    @Override
	protected void onClick(View v, int id) {
		switch (id) {
		case R.id.include_subcategories:
			cbIncludeSubCategories.performClick();
			break;
		case R.id.include_credit:
			cbIncludeCredit.performClick();
			break;
		case R.id.budget_mode:
			cbMode.performClick();
			break;
        case R.id.type:
            cbSavingBudget.performClick();
            break;
		case R.id.category:
			x.selectMultiChoice(this, R.id.category, R.string.categories, categories);
			break;
		case R.id.category_add: {
			Intent intent = new Intent(this, CategoryActivity.class);
			startActivityForResult(intent, NEW_CATEGORY_REQUEST);
			} break;
		case R.id.project:
			x.selectMultiChoice(this, R.id.project, R.string.projects, projects);
			break;
		case R.id.project_add: {
			Intent intent = new Intent(this, ProjectActivity.class);
			startActivityForResult(intent, NEW_PROJECT_REQUEST);
			} break;
		case R.id.account:
			x.selectPosition(this, R.id.account, R.string.account, accountAdapter, selectedAccountOption);
			break;
		case R.id.period_recur: {
			Intent intent = new Intent(this, RecurActivity.class);
			if (budget.recur != null) {
				intent.putExtra(RecurActivity.EXTRA_RECUR, budget.recur);
			}
			startActivityForResult(intent, RECUR_REQUEST);
			} break;
		}
	}

    @Override
    public void onSelectedPos(int id, int selectedPos) {
        switch (id) {
            case R.id.account:
                selectAccount(selectedPos);
                break;
        }
    }

    @Override
	public void onSelected(int id, List<? extends MultiChoiceItem> items) {
		switch (id) {
		case R.id.category:
			selectCategories();
			break;
		case R.id.project:
			selectProjects();
			break;
		}
	}

    private void selectAccount(Budget budget) {
        for (int i=0; i<accountOptions.size(); i++) {
            AccountOption option = accountOptions.get(i);
            if (option.matches(budget)) {
                selectAccount(i);
                break;
            }
        }
    }

    private void selectAccount(int selectedPos) {
        AccountOption option = accountOptions.get(selectedPos);
        option.updateBudget(budget);
        selectedAccountOption = selectedPos;
        accountText.setText(option.title);
        if (option.currency != null) {
            //amountInput.setCurrency(option.currency);
        } else {
            //amountInput.setCurrency(option.account.currency);
        }
    }

    private void selectProjects() {
		String selectedProjects = getCheckedEntities(this.projects);
		if (Utils.isEmpty(selectedProjects)) {
			projectText.setText(R.string.no_projects);
		} else {
			projectText.setText(selectedProjects);			
		}
	}

	private void selectCategories() {
		String selectedCategories = getCheckedEntities(this.categories);
		if (Utils.isEmpty(selectedCategories)) {
			categoryText.setText(R.string.no_categories);
		} else {
			categoryText.setText(selectedCategories);
		}
	}
	
	private String getCheckedEntities(List<? extends MyEntity> list) {
		StringBuilder sb = new StringBuilder();
		for (MyEntity e : list) {
			if (e.checked) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(e.title);
			}			
		}		
		return sb.toString();
	}
	
	private void selectRecur(String recur) {
		if (recur != null) {
			budget.recur = recur;
			Recur r = RecurUtils.createFromExtraString(recur);
			periodRecurText.setText(r.toString(this));
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			Total t =new Total(budget.currency);
			String amount = data.getStringExtra(AmountInput.EXTRA_AMOUNT);
			if (amount != null) {
				try {
					BigDecimal d = new BigDecimal(amount).setScale(2,
							BigDecimal.ROUND_HALF_UP);
					t.balance=d.unscaledValue().longValue();
					budget.amount=d.unscaledValue().longValue();
					u = new Utils(this);
					u.setTotal(totalText, t);
					totalText.setTextColor(getResources().getColor(R.color.f_blue_lighter1));
				} catch (NumberFormatException ex) {
					ex.printStackTrace();
				}
			}

			switch (requestCode) {
                case NEW_CATEGORY_REQUEST:
                    categories = merge(categories, db.getCategoriesList(true));
                    break;
                case NEW_PROJECT_REQUEST:
                    projects = merge(projects, em.getActiveProjectsList(true));
                    break;
                case RECUR_REQUEST:
                    String recur = data.getStringExtra(RecurActivity.EXTRA_RECUR);
                    if (recur != null) {
                        selectRecur(recur);
                    }
                    break;
                default:
                    break;
			}
		}
	}

	private static <T extends MyEntity> List<T> merge(List<T> oldList, List<T> newList) {
		for (T newT : newList) {
			for (Iterator<T> i = oldList.iterator(); i.hasNext(); ) {
				T oldT = i.next();
				if (newT.id == oldT.id) {
					newT.checked = oldT.checked;
					i.remove();
					break;
				}
			}
		}
		return newList;
	}

    private static class AccountOption {

        public final String title;
        public final Currency currency;
        public final Account account;

        private AccountOption(String title, Currency currency, Account account) {
            this.title = title;
            this.currency = currency;
            this.account = account;
        }

        @Override
        public String toString() {
            return title;
        }

        public boolean matches(Budget budget) {
            return (currency != null && budget.currency != null && currency.id == budget.currency.id) ||
                   (account != null && budget.account != null && account.id == budget.account.id);
        }

        public void updateBudget(Budget budget) {
            budget.currency = currency;
            budget.account = account;
        }

    }

}
