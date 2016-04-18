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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.flowzr.R;
import com.flowzr.model.Account;
import com.flowzr.model.Budget;
import com.flowzr.model.Category;
import com.flowzr.model.Currency;
import com.flowzr.model.MultiChoiceItem;
import com.flowzr.model.MyEntity;
import com.flowzr.model.Project;
import com.flowzr.model.Total;
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
	public static final int RECUR_REQUEST = 3;
	public static final int CALCULATOR_REQUEST = 4 ;

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
    protected int getLayoutId() {
        return R.layout.budget;
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        accountOptions = createAccountsList();
        accountAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, accountOptions);

		categories = db.getCategoriesList(true);
		projects = em.getActiveProjectsList(true);
		
		LinearLayout layout = (LinearLayout) getView().findViewById(R.id.list);
		//LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		titleText = (EditText) getView().findViewById(R.id.title);
		// (EditText) layoutInflater.inflate(R.layout.edit_text, null);
		//x.addEditNode(layout, R.string.title, titleText);

		AmountInput amountInput = new AmountInput(getContext());
		amountInput.setOwner(this);
		amountInput.setIncome();
		//amountInput.disableIncomeExpenseButton();

		//x.addEditNode(layout, R.string.amount, amountInput);

		periodRecurText = x.addListNode2(layout, R.id.period_recur,R.drawable.ic_repeat, R.string.period_recur,  getResources().getString(R.string.no_recur));
		accountText = x.addListNode2(layout, R.id.account, R.drawable.ic_account_balance_wallet, R.string.account, getResources().getString(R.string.select_account));
		categoryText=x.addListNodeCategory(layout);

		projectText = x.addListNode2(layout, R.id.project, R.drawable.ic_star_border, R.string.project, getResources().getString(R.string.no_projects));

		cbIncludeSubCategories = x.addCheckboxNode(layout,
				R.id.include_subcategories, R.string.include_subcategories,R.drawable.ic_expand_more,
				R.string.include_subcategories_summary, true);

		cbMode = x.addCheckboxNode(layout, R.id.budget_mode,R.string.budget_mode,R.drawable.ic_select_all,
				R.string.budget_mode_summary, false);
        cbIncludeCredit = x.addCheckboxNode(layout,
                R.id.include_credit, R.string.include_credit,R.drawable.ic_toggle_income,
                R.string.include_credit_summary, true);
        cbSavingBudget = x.addCheckboxNode(layout,
				R.id.type, R.string.budget_type_saving, R.drawable.account_type_asset,
				R.string.budget_type_saving_summary, false);

		totalText = ( TextView ) getView().findViewById(R.id.total);
		totalText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				Intent intent = new Intent(getContext(), CalculatorInput.class);
				if (budget.currencyId > -1) {
					intent.putExtra(AmountInput.EXTRA_CURRENCY, budget.currencyId);
				}
				intent.putExtra(AmountInput.EXTRA_AMOUNT, totalText.getText().toString().trim());
				startActivityForResult(intent, CALCULATOR_REQUEST);
			}
		});

		//Intent intent = getActivity().getIntent();
		Bundle bundle = getArguments();
		if (bundle != null) {
			long id = bundle.getLong(MyFragmentAPI.ENTITY_ID_EXTRA, -1);
			if (id != -1) {
				try {
					budget = em.load(Budget.class, id);
					editBudget();
				} catch (javax.persistence.EntityNotFoundException e) {
					// budget have been re-numbered
					e.printStackTrace();
					totalText.setText("0.00");
					selectRecur(RecurUtils.createDefaultRecur().toString());
				}

			} else {
				totalText.setText("0.00");
				selectRecur(RecurUtils.createDefaultRecur().toString());
			}
		}
		titleText.requestFocus();
	}

    public boolean finishAndClose(int result) {
        Bundle bundle = new  Bundle();
        bundle.putInt(MyFragmentAPI.RESULT_EXTRA,result);
        activity.onFragmentMessage(MyFragmentAPI.REQUEST_MYENTITY_FINISH,bundle);
        return true;
    }

    public boolean finishAndClose(Bundle bundle) {
        bundle.putInt(MyFragmentAPI.RESULT_EXTRA,AppCompatActivity.RESULT_OK);
        activity.onFragmentMessage(MyFragmentAPI.REQUEST_MYENTITY_FINISH,bundle);
        return true;
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
					intent.putExtra(MyFragmentAPI.ENTITY_ID_EXTRA, id);
                    finishAndClose(intent.getExtras());
				}
        		return true;
        	case R.id.action_cancel:
                finishAndClose(AppCompatActivity.RESULT_CANCELED);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
	
    private List<AccountOption> createAccountsList() {
        List<AccountOption> accounts = new ArrayList<>();
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

		totalText = ( TextView ) getView().findViewById(R.id.total);
		Total t =new Total(budget.currency);
		t.balance=budget.amount;
		u = new Utils(getContext());
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
			if (budget.amount>0) {
				budget.amount = -budget.amount;
			}
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
			x.selectMultiChoice(getContext(), R.id.category, R.string.categories, categories);
			break;
		case R.id.category_add:
            Bundle bundle=new Bundle();
            bundle.putInt(MyFragmentAPI.ENTITY_REQUEST_EXTRA,NEW_CATEGORY_REQUEST);
            Fragment fragment = new CategoryActivity();
            fragment.setArguments(bundle);
            fragment.setTargetFragment(this,NEW_CATEGORY_REQUEST);
            activity.startFragmentForResult(fragment,this);
            break;
		case R.id.project:
			x.selectMultiChoice(getContext(), R.id.project, R.string.projects, projects);
			break;
		case R.id.project_add: {
            bundle=new Bundle();
            fragment = new ProjectActivity();
            bundle.putInt(MyFragmentAPI.ENTITY_REQUEST_EXTRA,NEW_PROJECT_REQUEST);
            fragment.setArguments(bundle);
            fragment.setTargetFragment(this,NEW_PROJECT_REQUEST);
            activity.startFragmentForResult(fragment,this);
			} break;
		case R.id.account:
			x.selectPosition(getContext(), R.id.account, R.string.account, accountAdapter, selectedAccountOption);
			break;
		case R.id.period_recur: {
            bundle=new Bundle();
            fragment = new RecurActivity();
			if (budget.recur != null) {
				bundle.putString(RecurActivity.EXTRA_RECUR, budget.recur);
			}
            bundle.putInt(MyFragmentAPI.ENTITY_REQUEST_EXTRA,RECUR_REQUEST);
            fragment.setArguments(bundle);
            fragment.setTargetFragment(this,RECUR_REQUEST);
            activity.startFragmentForResult(fragment,this);

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
            periodRecurText.setText(r != null ? r.toString(getContext()) : "");
		}
	}

	@Override
public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == AppCompatActivity.RESULT_OK) {
			Total t =new Total(budget.currency);
			String amount = data.getStringExtra(AmountInput.EXTRA_AMOUNT);
			if (amount != null) {
				try {
					BigDecimal d = new BigDecimal(amount).setScale(2,
							BigDecimal.ROUND_HALF_UP);
					t.balance=d.unscaledValue().longValue();
					budget.amount=d.unscaledValue().longValue();
					u = new Utils(getContext());
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
