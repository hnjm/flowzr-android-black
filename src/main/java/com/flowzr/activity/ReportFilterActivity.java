/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
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
import com.flowzr.db.DatabaseHelper;
import com.flowzr.filter.Criteria;
import com.flowzr.filter.DateTimeCriteria;
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

public class ReportFilterActivity extends AbstractEditorActivity {

    private static final TransactionStatus[] statuses = TransactionStatus.values();

    private WhereFilter filter = WhereFilter.empty();

    private TextView period;
    private TextView account;
    private TextView currency;
    private TextView category;
    private TextView project;
    private TextView payee;
    private TextView location;
    private TextView status;

    private DateFormat df;
    private String filterValueNotFound;

    @Override
    protected int getLayoutId() {
        return R.layout.blotter_filter;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        df = DateUtils.getShortDateFormat(getContext());
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

        Intent intent = getActivity().getIntent();
        if (intent != null) {
            filter = WhereFilter.fromIntent(intent);
            updatePeriodFromFilter();
            updateAccountFromFilter();
            updateCurrencyFromFilter();
            updateCategoryFromFilter();
            updateProjectFromFilter();
            updatePayeeFromFilter();
            updateLocationFromFilter();
            updateStatusFromFilter();
        }

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
                finishAndClose(AppCompatActivity.RESULT_CANCELED);
                return true;
            //@TODO clear report filter
//        	case R.id.action_select_all:
// 				setResult(RESULT_FIRST_USER);
// 				finish();
//                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    
    private void showMinusButton(TextView textView) {
        ImageView v = findMinusButton(textView);
        v.setVisibility(View.VISIBLE);
    }

    private void hideMinusButton(TextView textView) {
        ImageView v = findMinusButton(textView);
        v.setVisibility(View.GONE);
    }

    private ImageView findMinusButton(TextView textView) {
        LinearLayout layout = (LinearLayout) textView.getParent().getParent();
        return (ImageView) layout.getChildAt(layout.getChildCount()-1);
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
            category.setText(R.string.no_filter);
            hideMinusButton(category);
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
            T e = em.get(entityClass, c.getLongValue1());
            if (e != null) {
                filterView.setText(e.title);
            } else {
                filterView.setText(filterValueNotFound);
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
                fragment.setTargetFragment(this,1);
                activity.startFragmentForResult(fragment,this);
                break;
            case R.id.period_clear:
                clear(BlotterFilter.DATETIME, period);
                break;
            case R.id.account: {
                Cursor cursor = em.getAllAccounts();
                getActivity().startManagingCursor(cursor);
                ListAdapter adapter = TransactionUtils.createAccountAdapter(getContext(), cursor);
                Criteria c = filter.get(BlotterFilter.FROM_ACCOUNT_ID);
                long selectedId = c != null ? c.getLongValue1() : -1;
                x.select(getContext(), R.id.account, R.string.account, cursor, adapter, "_id", selectedId);
            } break;
            case R.id.account_clear:
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
                Cursor cursor = db.getCategories(false);
                getActivity().startManagingCursor(cursor);
                ListAdapter adapter = TransactionUtils.createCategoryAdapter(db,getContext(), cursor);
                Criteria c = filter.get(BlotterFilter.CATEGORY_LEFT);
                long selectedId = c != null ? c.getLongValue1() : -1;
                x.select(getContext(), R.id.category, R.string.category, cursor, adapter, DatabaseHelper.CategoryViewColumns.left.name(), selectedId);
            } break;
            case R.id.category_clear:
                clear(BlotterFilter.CATEGORY_LEFT, category);
                break;
            case R.id.project: {
                ArrayList<Project> projects = em.getActiveProjectsList(false);
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
                Cursor cursor = em.getAllLocations(false);
                getActivity().startManagingCursor(cursor);
                ListAdapter adapter = TransactionUtils.createLocationAdapter(getContext(), cursor);
                Criteria c = filter.get(BlotterFilter.LOCATION_ID);
                long selectedId = c != null ? c.getLongValue1() : -1;
                x.select(getContext(), R.id.location, R.string.location, cursor, adapter, "_id", selectedId);
            } break;
            case R.id.location_clear:
                clear(BlotterFilter.LOCATION_ID, location);
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
                Category cat = db.getCategoryByLeft(selectedId);
                filter.put(Criteria.btw(BlotterFilter.CATEGORY_LEFT, String.valueOf(cat.left), String.valueOf(cat.right)));
                updateCategoryFromFilter();
                break;
            case R.id.project:
                filter.put(Criteria.eq(BlotterFilter.PROJECT_ID, String.valueOf(selectedId)));
                updateProjectFromFilter();
                break;
            case R.id.payee:
                filter.put(Criteria.eq(BlotterFilter.PAYEE_ID, String.valueOf(selectedId)));
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
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == AppCompatActivity.RESULT_FIRST_USER) {
                onClick(period, R.id.period_clear);
            } else if (resultCode == AppCompatActivity.RESULT_OK) {
                DateTimeCriteria c = WhereFilter.dateTimeFromIntent(data);
                filter.put(c);
                updatePeriodFromFilter();
            }
        }
    }
	
}
