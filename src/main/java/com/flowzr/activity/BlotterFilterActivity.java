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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.flowzr.R;
import com.flowzr.blotter.BlotterFilter;
import com.flowzr.datetime.DateUtils;
import com.flowzr.datetime.Period;
import com.flowzr.db.DatabaseHelper.CategoryViewColumns;
import com.flowzr.filter.Criteria;
import com.flowzr.filter.DateTimeCriteria;
import com.flowzr.filter.SingleCategoryCriteria;
import com.flowzr.filter.WhereFilter;
import com.flowzr.model.Account;
import com.flowzr.model.Category;
import com.flowzr.model.Currency;
import com.flowzr.model.MyEntity;
import com.flowzr.model.MyLocation;
import com.flowzr.model.Payee;
import com.flowzr.model.Project;
import com.flowzr.model.TransactionStatus;
import com.flowzr.utils.EnumUtils;
import com.flowzr.utils.TransactionUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class BlotterFilterActivity extends AbstractEditorActivity {	
	
    public static final String IS_ACCOUNT_FILTER = "IS_ACCOUNT_FILTER";

	private static final TransactionStatus[] statuses = TransactionStatus.values();

	private WhereFilter filter = WhereFilter.empty();
	
	private TextView period;
	private TextView account;
	private TextView currency;
	private TextView category;
	private TextView project;
    private TextView payee;
	private TextView location;
	private TextView sortOrder;
	private TextView status;
	
	private DateFormat df;
	private String[] sortBlotterEntries;

    private String filterValueNotFound;
    private long accountId;
    private boolean isAccountFilter;
    private int REQUEST_DATEFILTER=6767;

    @Override
    protected int getLayoutId() {
        return R.layout.blotter_filter;
    }

    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		df = DateUtils.getShortDateFormat(getContext());
		sortBlotterEntries = getResources().getStringArray(R.array.sort_blotter_entries);
        filterValueNotFound = getString(R.string.filter_value_not_found);

		LinearLayout layout = (LinearLayout)getView().findViewById(R.id.layout);
		period = x.addFilterNodeMinus(layout, R.id.period, R.id.period_clear, R.string.period, R.string.no_filter);
		account = x.addFilterNodeMinus(layout, R.id.account, R.id.account_clear, R.string.account, R.string.no_filter);
		currency = x.addFilterNodeMinus(layout, R.id.currency, R.id.currency_clear, R.string.currency, R.string.no_filter);
		category = x.addFilterNodeMinus(layout, R.id.category, R.id.category_clear, R.string.category, R.string.no_filter);
        payee = x.addFilterNodeMinus(layout, R.id.payee, R.id.payee_clear, R.string.payee, R.string.no_filter);
		project = x.addFilterNodeMinus(layout, R.id.project, R.id.project_clear, R.string.project, R.string.no_filter);
		location = x.addFilterNodeMinus(layout, R.id.location, R.id.location_clear, R.string.location, R.string.no_filter);
		status = x.addFilterNodeMinus(layout, R.id.status, R.id.status_clear, R.string.transaction_status, R.string.no_filter);
		sortOrder = x.addFilterNodeMinus(layout, R.id.sort_order, R.id.sort_order_clear, R.string.sort_order, sortBlotterEntries[0]);	
		Bundle bundle =getArguments();
		if (bundle != null) {
            filter=WhereFilter.fromBundle(bundle);
            getAccountIdFromFilter(bundle);
            updatePeriodFromFilter();
			updateAccountFromFilter();
			updateCurrencyFromFilter();
			updateCategoryFromFilter();
			updateProjectFromFilter();
            updatePayeeFromFilter();
			updateLocationFromFilter();
			updateSortOrderFromFilter();
			updateStatusFromFilter();
            disableAccountResetButtonIfNeeded();
		}



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
				Intent data = new Intent();
				filter.toIntent(data);
                finishAndClose(data.getExtras());
        		return true;
        	case R.id.action_cancel:
                finishAndClose(AppCompatActivity.RESULT_FIRST_USER);
                return true;
            /**
			case R.id.action_select_all:
                 updatePeriodFromFilter();
     			 updateAccountFromFilter();
     			 updateCurrencyFromFilter();
     			 updateCategoryFromFilter();
     			 updateProjectFromFilter();
                 updatePayeeFromFilter();
     			 updateLocationFromFilter();
     			 updateSortOrderFromFilter();
     			 updateStatusFromFilter();
                 disableAccountResetButtonIfNeeded();
                 return true; **/
        }
        return super.onOptionsItemSelected(item);
    }


    private boolean isAccountFilter() {
        return isAccountFilter && accountId > 0;
    }

    private void getAccountIdFromFilter(Bundle bundle) {
        isAccountFilter = bundle.getBoolean(IS_ACCOUNT_FILTER, false);
        accountId = filter.getAccountId();
    }

    private void disableAccountResetButtonIfNeeded() {
//        if (isAccountFilter()) {
//            hideMinusButton(account);
//        }
    }

    private void showMinusButton(TextView textView) {
        ImageView v = findMinusButton(textView);
		if (v!=null) {
			v.setVisibility(View.VISIBLE);
		}
    }

    private void hideMinusButton(TextView textView) {
        ImageView v = findMinusButton(textView);
		if (v!=null) {
			v.setVisibility(View.GONE);
		}
    }

    private ImageView findMinusButton(TextView textView) {
        LinearLayout layout = (LinearLayout) textView.getParent().getParent();
        return (ImageView) layout.getChildAt(layout.getChildCount()-1);
    }

    private void updateSortOrderFromFilter() {
		String s = filter.getSortOrder();
		if (BlotterFilter.SORT_OLDER_TO_NEWER.equals(s)) {
			sortOrder.setText(sortBlotterEntries[1]);
		} else {
			sortOrder.setText(sortBlotterEntries[0]);
		}
	}

	private void updateLocationFromFilter() {
		Criteria c = filter.get(BlotterFilter.LOCATION_ID);
		if (c != null) {
			MyLocation loc = em.get(MyLocation.class, c.getLongValue1());
			location.setText(loc != null ? loc.name : filterValueNotFound);
            showMinusButton(location);
		} else {
			location.setText(R.string.no_filter);
            hideMinusButton(location);
		}
	}

	private void updateProjectFromFilter() {
        updateEntityFromFilter(BlotterFilter.PROJECT_ID, Project.class, project);
	}

    private void updatePayeeFromFilter() {
        updateEntityFromFilter(BlotterFilter.PAYEE_ID, Payee.class, payee);
    }

	private void updateCategoryFromFilter() {
		Criteria c = filter.get(BlotterFilter.CATEGORY_LEFT);
		if (c != null) {
			Category cat = db.getCategoryByLeft(c.getLongValue1());
            if (cat.id > 0) {
			    category.setText(cat.title);
            } else {
                category.setText(filterValueNotFound);
            }
            showMinusButton(category);
		} else {
            c = filter.get(BlotterFilter.CATEGORY_ID);
            if (c != null) {
                long categoryId = c.getLongValue1();
                Category cat = db.getCategory(categoryId);
                category.setText(cat.title);
                showMinusButton(category);
            } else {
			    category.setText(R.string.no_filter);
                hideMinusButton(category);
            }
		}
	}

	private void updatePeriodFromFilter() {
		DateTimeCriteria c = (DateTimeCriteria)filter.get(BlotterFilter.DATETIME);
		if (c != null) {
			Period p = c.getPeriod();
			if (p.isCustom()) {
				long periodFrom = c.getLongValue1();
				long periodTo = c.getLongValue2();
				period.setText(df.format(new Date(periodFrom))+"-"+df.format(new Date(periodTo)));
			} else {
				period.setText(p.type.titleId);
			}
            showMinusButton(period);
		} else {
            clear(BlotterFilter.DATETIME, period);
		}
	}

	private void updateAccountFromFilter() {
        updateEntityFromFilter(BlotterFilter.FROM_ACCOUNT_ID, Account.class, account);
	}

	private void updateCurrencyFromFilter() {
        updateEntityFromFilter(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, Currency.class, currency);
	}

	private void updateStatusFromFilter() {
		Criteria c = filter.get(BlotterFilter.STATUS);
		if (c != null) {
			TransactionStatus s = TransactionStatus.valueOf(c.getStringValue());
			status.setText(getString(s.titleId));
            showMinusButton(status);
		} else {
			status.setText(R.string.no_filter);
            hideMinusButton(status);
		}
	}

    private <T extends MyEntity> void updateEntityFromFilter(String filterCriteriaName, Class<T> entityClass, TextView filterView) {
        Criteria c = filter.get(filterCriteriaName);
        if (c != null) {
            if (c.isNull()) {
                filterView.setText(R.string.no_payee);
            } else {
                long entityId = c.getLongValue1();
                T e = em.get(entityClass, entityId);
                if (e != null) {
                    filterView.setText(e.title);
                } else {
                    filterView.setText(filterValueNotFound);
                }
            }
            showMinusButton(filterView);
        } else {
            filterView.setText(R.string.no_filter);
            hideMinusButton(filterView);
        }
    }

	@Override
	protected void onClick(View v, int id) {
		switch (id) {
		case R.id.period:
			Fragment fragment = new DateFilterFragment();
			Bundle bundle= new Bundle();
			filter.toBundle(bundle);
			fragment.setArguments(bundle);
			bundle.putInt(MyFragmentAPI.ENTITY_REQUEST_EXTRA,REQUEST_DATEFILTER);
			fragment.setTargetFragment(this,1);
			activity.startFragmentForResult(fragment,this);
			break;
		case R.id.period_clear:
            clear(BlotterFilter.DATETIME, period);
			break;
		case R.id.account: {
//            if (isAccountFilter()) {
//                return;
//            }
			Cursor cursor = em.getAllAccounts();
			getActivity().startManagingCursor(cursor);
			ListAdapter adapter = TransactionUtils.createAccountAdapter(getContext(), cursor);
			Criteria c = filter.get(BlotterFilter.FROM_ACCOUNT_ID);
			long selectedId = c != null ? c.getLongValue1() : -1;
			x.select(getContext(), R.id.account, R.string.account, cursor, adapter, "_id", selectedId);
		} break;
		case R.id.account_clear:
//            if (isAccountFilter()) {
//                return;
//            }
		    clear(BlotterFilter.FROM_ACCOUNT_ID, account);
			break;
		case R.id.currency: {
			Cursor cursor = em.getAllCurrencies("name");
			getActivity().startManagingCursor(cursor);
			ListAdapter adapter = TransactionUtils.createCurrencyAdapter(getContext(), cursor);
			Criteria c = filter.get(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID);
			long selectedId = c != null ? c.getLongValue1() : -1;
			x.select(getContext(), R.id.currency, R.string.currency, cursor, adapter, "_id", selectedId);
		} break;
		case R.id.currency_clear:
			clear(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, currency);
			break;
		case R.id.category: {
			Cursor cursor = db.getCategories(true);
			getActivity().startManagingCursor(cursor);
			ListAdapter adapter = TransactionUtils.createCategoryAdapter(db, getContext(), cursor);
			Criteria c = filter.get(BlotterFilter.CATEGORY_LEFT);
            if (c != null) {
                Category cat = db.getCategoryByLeft(c.getLongValue1());
                x.select(getContext(), R.id.category, R.string.category, cursor, adapter, CategoryViewColumns._id.name(),
                        cat.id);
            } else {
                c = filter.get(BlotterFilter.CATEGORY_ID);
                x.select(getContext(), R.id.category, R.string.category, cursor, adapter, CategoryViewColumns._id.name(),
                        c != null ? c.getLongValue1() : -1);
            }
		} break;
		case R.id.category_clear:
            clearCategory();
			break;
		case R.id.project: {
			ArrayList<Project> projects = em.getActiveProjectsList(true);
			ListAdapter adapter = TransactionUtils.createProjectAdapter(getContext(), projects);
			Criteria c = filter.get(BlotterFilter.PROJECT_ID);
			long selectedId = c != null ? c.getLongValue1() : -1;
			int selectedPos = MyEntity.indexOf(projects, selectedId);
			x.selectItemId(getContext(), R.id.project, R.string.project, adapter, selectedPos);
		} break;
		case R.id.project_clear:
			clear(BlotterFilter.PROJECT_ID, project);
			break;
        case R.id.payee: {
            List<Payee> payees = em.getAllPayeeList();
            payees.add(0, noPayee());
            ListAdapter adapter = TransactionUtils.createPayeeAdapter(getContext(), payees);
            Criteria c = filter.get(BlotterFilter.PAYEE_ID);
            long selectedId = c != null ? c.getLongValue1() : -1;
            int selectedPos = MyEntity.indexOf(payees, selectedId);
            x.selectItemId(getContext(), R.id.payee, R.string.payee, adapter, selectedPos);
        } break;
        case R.id.payee_clear:
            clear(BlotterFilter.PAYEE_ID, payee);
            break;
		case R.id.location: {
			Cursor cursor = em.getAllLocations(true);
			getActivity().startManagingCursor(cursor);
			ListAdapter adapter = TransactionUtils.createLocationAdapter(getContext(), cursor);
			Criteria c = filter.get(BlotterFilter.LOCATION_ID);
			long selectedId = c != null ? c.getLongValue1() : -1;
			x.select(getContext(), R.id.location, R.string.location, cursor, adapter, "_id", selectedId);
		} break;
		case R.id.location_clear:
			clear(BlotterFilter.LOCATION_ID, location);
			break;
		case R.id.sort_order: {
			ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, sortBlotterEntries);
			int selectedId = BlotterFilter.SORT_OLDER_TO_NEWER.equals(filter.getSortOrder()) ? 1 : 0;
			x.selectPosition(getContext(), R.id.sort_order, R.string.sort_order, adapter, selectedId);
		} break;
		case R.id.sort_order_clear:
			filter.resetSort();
			filter.desc(BlotterFilter.DATETIME);
			updateSortOrderFromFilter();
			break;
		case R.id.status: {
			ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(getContext(), statuses);
			Criteria c = filter.get(BlotterFilter.STATUS);
			int selectedPos = c != null ? TransactionStatus.valueOf(c.getStringValue()).ordinal() : -1;
			x.selectPosition(getContext(), R.id.status, R.string.transaction_status, adapter, selectedPos);
		} break;
		case R.id.status_clear:
			clear(BlotterFilter.STATUS, status);
			break;
		}
	}

    private void clearCategory() {
        clear(BlotterFilter.CATEGORY_LEFT, category);
        clear(BlotterFilter.CATEGORY_ID, category);
    }

    private Payee noPayee() {
        Payee p = new Payee();
        p.id = 0;
        p.title = getString(R.string.no_payee);
        return p;
    }

    private void clear(String criteria, TextView textView) {
		filter.remove(criteria);
		textView.setText(R.string.no_filter);
        hideMinusButton(textView);
	}

	@Override
	public void onSelectedId(int id, long selectedId) {
		switch (id) {
		case R.id.account:
			filter.put(Criteria.eq(BlotterFilter.FROM_ACCOUNT_ID, String.valueOf(selectedId)));
			updateAccountFromFilter();
			break;
		case R.id.currency:
			filter.put(Criteria.eq(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, String.valueOf(selectedId)));
			updateCurrencyFromFilter();
			break;
		case R.id.category:
            clearCategory();
            if (selectedId == 0) {
                filter.put(new SingleCategoryCriteria(0));
            } else {
			    Category cat = db.getCategory(selectedId);
			    filter.put(Criteria.btw(BlotterFilter.CATEGORY_LEFT, String.valueOf(cat.left), String.valueOf(cat.right)));
            }
			updateCategoryFromFilter();
			break;
		case R.id.project:
			filter.put(Criteria.eq(BlotterFilter.PROJECT_ID, String.valueOf(selectedId)));
			updateProjectFromFilter();
			break;
        case R.id.payee:
            if (selectedId == 0) {
                filter.put(Criteria.isNull(BlotterFilter.PAYEE_ID));
            } else {
                filter.put(Criteria.eq(BlotterFilter.PAYEE_ID, String.valueOf(selectedId)));
            }
            updatePayeeFromFilter();
            break;
		case R.id.location:
			filter.put(Criteria.eq(BlotterFilter.LOCATION_ID, String.valueOf(selectedId)));
			updateLocationFromFilter();
			break;
		}
	}
	
	@Override
	public void onSelectedPos(int id, int selectedPos) {
		switch (id) {
		case R.id.status:
			filter.put(Criteria.eq(BlotterFilter.STATUS, statuses[selectedPos].name()));
			updateStatusFromFilter();			
			break;
		case R.id.sort_order:
			filter.resetSort();
			if (selectedPos == 1) {
				filter.asc(BlotterFilter.DATETIME);
			} else {
				filter.desc(BlotterFilter.DATETIME);
			}
			updateSortOrderFromFilter();
			break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == REQUEST_DATEFILTER) {
			if (resultCode == AppCompatActivity.RESULT_FIRST_USER) {
				onClick(period, R.id.period_clear);
			} else if (resultCode == AppCompatActivity.RESULT_OK ) {
				if (data.getStringExtra(DateFilterFragment.EXTRA_FILTER_PERIOD_TYPE)!=null) {
					DateTimeCriteria c = WhereFilter.dateTimeFromIntent(data);
					filter.put(c);
				}
				updatePeriodFromFilter();
			}
		}
	}
	
}
