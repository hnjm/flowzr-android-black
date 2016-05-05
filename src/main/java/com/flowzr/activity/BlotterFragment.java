/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk - menu option to call Credit Card Bill functionality
 *     Emmanuel Florent - port to Android API 11+
 ******************************************************************************/
package com.flowzr.activity;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.flowzr.R;
import com.flowzr.adapter.BlotterListAdapter;
import com.flowzr.adapter.TransactionsListAdapter;
import com.flowzr.blotter.AccountTotalCalculationTask;
import com.flowzr.blotter.BlotterFilter;
import com.flowzr.blotter.BlotterTotalCalculationTask;
import com.flowzr.blotter.TotalCalculationTask;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.dialog.TransactionInfoDialog;
import com.flowzr.filter.WhereFilter;
import com.flowzr.model.Account;
import com.flowzr.model.Transaction;
import com.flowzr.view.FloatingActionButton;
import com.flowzr.view.MyFloatingActionMenu;
import com.flowzr.view.NodeInflater;
import java.util.ArrayList;
import java.util.List;
import static com.flowzr.utils.AndroidUtils.isCompatible;


public class BlotterFragment extends AbstractTotalListFragment {

    public BlotterFragment() {
        super(R.layout.blotter);
    }

    public void recreateAdapter() {
        super.recreateAdapter();
    }


    public static final String SAVE_FILTER = "saveFilter";

    protected ImageButton bFilter;
    private TotalCalculationTask calculationTask;

    protected boolean saveFilter;
    protected static WhereFilter blotterFilter = WhereFilter.empty();
    protected TextView totalText;

    @Override
    public void onResume() {
        super.onResume();
        if (isCompatible(14)) {
            final MyFloatingActionMenu menu1 = (MyFloatingActionMenu) activity.findViewById(R.id.menu1);
            if (menu1 != null) {
                menu1.setVisibility(View.VISIBLE);
            }
        }
        recreateAdapter();
        calculateTotals();
        setUpFab();
        activity.supportInvalidateOptionsMenu();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.blotter_actions, menu);
        if (blotterFilter.getAccountId()==-1) {
            if (menu.findItem(R.id.opt_menu_month)!=null) {
                menu.findItem(R.id.opt_menu_month).setVisible(false);
            }
            if (menu.findItem(R.id.opt_menu_bill)!=null) {
                menu.findItem(R.id.opt_menu_bill).setVisible(false);
            }
        }
        if (isCompatible(14)) {
            menu.removeItem(R.id.bAdd);
            menu.removeItem(R.id.bTransfer);
        }
    }

    public static Fragment newInstance(Bundle args) {
        BlotterFragment f = new BlotterFragment();
        f.setArguments(args);
        if (args!=null) {
            blotterFilter = WhereFilter.fromBundle(args);
        }
        return f;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        if (savedInstanceState != null  ) {
            saveFilter=false;
        }
        Bundle args=this.getArguments();
        if (args != null   ) {;
            blotterFilter = WhereFilter.fromBundle(args);
            saveFilter = args.getBoolean(SAVE_FILTER, false);
        }

        super.onActivityCreated(savedInstanceState);

        if (args!= null && args.getInt(MyFragmentAPI.ENTITY_REQUEST_EXTRA)==AbstractTotalListFragment.NEW_TRANSACTION_FROM_TEMPLATE_REQUEST) {
            Intent intent = new Intent();
            intent.putExtras(args);
            createTransactionFromTemplate(intent);
        }

        totalText = (TextView)getView().findViewById(R.id.total); // set for calculation task
        if (totalText!=null) { //ex: ScheduledListFragment
            totalText.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View arg0, MotionEvent arg1) {
                    showTotals();
                    return false;
                }
            });
        }

        if (getView()!=null && getView().findViewById(R.id.fragment_land_container)!=null) {
            Fragment fragment=new BlotterTotalsDetailsActivity();
            fragment.setArguments(getArguments());
            getChildFragmentManager().beginTransaction().replace(R.id.fragment_land_container, fragment).addToBackStack(null).commit();
            getChildFragmentManager().executePendingTransactions();
        }
    }

    public void setUpFab() {
        if (isCompatible(14)) {
            final MyFloatingActionMenu menu1 = (MyFloatingActionMenu) activity.findViewById(R.id.menu1);
            if (menu1 != null) {
                FloatingActionButton fab1 = (FloatingActionButton) activity.findViewById(R.id.fab1);
                FloatingActionButton fab2 = (FloatingActionButton) activity.findViewById(R.id.fab2);
                menu1.getMenuIconView().setImageResource(R.drawable.ic_add);
                fab1.setImageResource(R.drawable.ic_add);
                fab2.setImageResource(R.drawable.ic_swap_vert);
                fab1.setLabelText(getResources().getString(R.string.add_transaction));
                fab2.setLabelText(getResources().getString(R.string.add_transfer));


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
                menu1.setClosedOnTouchOutside(true);

                mUiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        menu1.showMenuButton(false);
                    }
                }, 450);

                menu1.setOnMenuToggleListener(new MyFloatingActionMenu.OnMenuToggleListener() {
                    @Override
                    public void onMenuToggle(boolean opened) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                menu1.close(false);
                            }
                        }, 3500);
                    }
                });
                if (fab1!=null) {
                    fab1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            menu1.toggle(false);
                            //fragment.
                            addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class);
                        }
                    });
                }
                if (fab2!=null) {
                    fab2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            menu1.toggle(false);
                            addItem(NEW_TRANSFER_REQUEST, TransferActivity.class);
                        }
                    });
                }

                getView().findViewById(android.R.id.list)
                        .setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                switch (event.getAction()) {
                                    case MotionEvent.ACTION_SCROLL:
                                    case MotionEvent.ACTION_MOVE:
                                        menu1.hideMenu(true);
                                        break;
                                    case MotionEvent.ACTION_CANCEL:
                                    case MotionEvent.ACTION_UP:
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                menu1.showMenu(true);
                                            }
                                        }, 1500);
                                        break;
                                }
                                return false;
                            }
                        });
            }
        }
    }


    protected void calculateTotals() {
            if (totalText!=null) {
                if (calculationTask != null) {
                    calculationTask.stop();
                    calculationTask.cancel(true);
                }
                calculationTask = createTotalCalculationTask();
                calculationTask.execute();
            }
    }

    protected TotalCalculationTask createTotalCalculationTask() {
        WhereFilter filter = WhereFilter.copyOf(blotterFilter);
        if (filter.getAccountId() > 0) {
            return new AccountTotalCalculationTask(this.getActivity(), db, filter, totalText);
        } else {
            return new BlotterTotalCalculationTask(this.getActivity(), db, filter, totalText);
        }
    }

    @Override
    public void recreateCursor() {
        super.recreateCursor();
        calculateTotals();
    }

    private void showTotals() {
        Bundle bundle = new Bundle ();
        bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,BlotterTotalsDetailsActivity.class.getCanonicalName());
        blotterFilter.toBundle(bundle);
        activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
        calculateTotals();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.transaction_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (!super.onContextItemSelected(item)) {
            AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
            switch (item.getItemId()) {
                case R.id.context_transaction_edit:
                    editTransaction(mi.id);
                    return true;
                case R.id.context_transaction_info:
                    showTransactionInfo(mi.id);
                    return true;
                case R.id.context_transaction_duplicate:
                    duplicateTransaction(mi.id, 1);
                    return true;
                case R.id.context_transaction_template:
                    new BlotterOperations(this, db, mi.id).duplicateAsTemplate();
                    Toast.makeText(this.getActivity(), R.string.save_as_template_success, Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.context_transaction_delete:
                    deleteTransaction(mi.id);
                    return true;
                case R.id.context_transaction_reconcile:
                    reconcileTransaction(mi.id);
                    return true;
                case R.id.context_transaction_clear:
                    clearTransaction(mi.id);
                    return true;
            }
        }
        return false;
    }

    private void clearTransaction(long selectedId) {
        new BlotterOperations(this, db, selectedId).clearTransaction();
        recreateCursor();
    }

    private void reconcileTransaction(long selectedId) {
        new BlotterOperations(this, db, selectedId).reconcileTransaction();
        recreateCursor();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        blotterFilter.toBundle(outState);
        outState.putBoolean(BlotterFilterActivity.IS_ACCOUNT_FILTER, !saveFilter);
    }


    protected void selectTemplate() {
        Bundle bundle = new Bundle();
        bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,SelectTemplateFragment.class.getCanonicalName());
        Fragment fragment= new SelectTemplateFragment();
        fragment.setArguments(bundle);
        fragment.setTargetFragment(this,NEW_TRANSACTION_FROM_TEMPLATE_REQUEST);
        activity.startFragmentForResult(fragment,this);
    }


    private long duplicateTransaction(long id, int multiplier) {

        long newId = new BlotterOperations(this, db, id).duplicateTransaction(multiplier);
        String toastText;
        try {
            if (multiplier > 1) {
                toastText = getActivity().getString(R.string.duplicate_success_with_multiplier, multiplier);
            } else {
                toastText = getActivity().getString(R.string.duplicate_success);
            }
            Toast.makeText(this.getActivity(), toastText, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        recreateCursor();
        AccountWidget.updateWidgets(BlotterFragment.this.getActivity());
        return newId;
    }

    @Override
    protected void addItem() {
        addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class);
    }

    protected void addItem(int requestId, Class<? extends AbstractTransactionActivity> clazz) {

        Intent intent = new Intent(BlotterFragment.this.getActivity(), clazz);
        long accountId = blotterFilter.getAccountId();
        if (accountId != -1) {
            intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, accountId);
        }
        if (requestId==NEW_TRANSFER_REQUEST) {
            intent.putExtra(TransactionActivity.IS_TRANSFER_EXTRA,true);
        }


        if ( blotterFilter.get(BlotterFilter.BUDGET_ID)!=null) {
            intent.putExtra(TransactionActivity.BUDGET_ID_EXTRA, blotterFilter.getBudgetId());
        }

        if ( blotterFilter.get(BlotterFilter.PROJECT_ID)!=null) {
            intent.putExtra(TransactionActivity.PROJECT_ID_EXTRA, (long) blotterFilter.get(BlotterFilter.PROJECT_ID).getIntValue());
        }


        if ( blotterFilter.get(BlotterFilter.CATEGORY_LEFT)!=null) {
            intent.putExtra(TransactionActivity.CATEGORY_ID_EXTRA, (long)blotterFilter.get(BlotterFilter.CATEGORY_LEFT).getIntValue());
        }


        if ( blotterFilter.get(BlotterFilter.LOCATION_ID)!=null) {
            intent.putExtra(TransactionActivity.LOCATION_ID_EXTRA, blotterFilter.get(BlotterFilter.LOCATION_ID).getLongValue1());
        }

        if ( blotterFilter.get(BlotterFilter.PAYEE_ID)!=null) {
            intent.putExtra(TransactionActivity.PAYEE_ID_EXTRA,blotterFilter.get(BlotterFilter.PAYEE_ID).getLongValue1());
        }

        if ( blotterFilter.get(BlotterFilter.STATUS)!=null) {
            intent.putExtra(TransactionActivity.STATUS_EXTRA, blotterFilter.get(BlotterFilter.STATUS).getStringValue());
        }

        intent.putExtra(TransactionActivity.TEMPLATE_EXTRA, blotterFilter.getIsTemplate());
        intent.putExtra(MyFragmentAPI.ENTITY_CLASS_EXTRA, clazz.getCanonicalName());
        Bundle bundle=new Bundle();
        bundle.putAll(intent.getExtras());
        activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);

    }

    @Override
    protected Cursor createCursor() {
        Cursor c;
        long accountId = blotterFilter.getAccountId();
        long budgetId = blotterFilter.getBudgetId();
        if (budgetId>0) {
            blotterFilter.clear();
        }
        if (accountId != -1) {
            c = db.getBlotterForAccount(blotterFilter);
        } else {

            c = db.getBlotter(blotterFilter);
        }
        return c;
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        long accountId = blotterFilter.getAccountId();
        if (accountId != -1) {
            return new TransactionsListAdapter(this.getActivity(), db, cursor);
        } else {
            return new BlotterListAdapter(this.getActivity(), db, cursor);
        }
    }

    @Override
    protected void deleteItem(int position, final long id) {
        deleteTransaction(id);
        recreateCursor();
    }

    @Override
    protected void editItem(long id) {

    }

    private void deleteTransaction(long id) {
        new BlotterOperations(this, db, id).deleteTransaction();
        recreateCursor();
        recreateAdapter();
    }

    protected void afterDeletingTransaction(long id) {
        recreateCursor();
        recreateAdapter();
        AccountWidget.updateWidgets(this.getActivity());
        AbstractActionBarActivity.mAdapter.notifyDataSetChanged();
    }

    private void editTransaction(long id) {
        new BlotterOperations(this, db, id).editTransaction();
    }



    public void setFilter(Bundle bundle) {
        blotterFilter.clear();
        blotterFilter = WhereFilter.fromBundle(bundle);
        saveFilter = true;
        saveFilter();
        recreateCursor();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data.getExtras().containsKey(WhereFilter.FILTER_EXTRA)) {
            if (resultCode == MainActivity.RESULT_FIRST_USER) {
                blotterFilter.clear();
            } else if (resultCode == MainActivity.RESULT_OK) {
                blotterFilter.clear();
                blotterFilter = WhereFilter.fromIntent(data);
            }
            saveFilter=true;
            saveFilter();

        }

        if (resultCode == MainActivity.RESULT_OK
                && requestCode == NEW_TRANSACTION_FROM_TEMPLATE_REQUEST) {
            createTransactionFromTemplate(data);
        }

        if (resultCode != MainActivity.RESULT_CANCELED) {
            try {
                recreateCursor();
            } catch (java.lang.IllegalStateException e) {
                e.printStackTrace();
            }
       }

    }



    public void createTransactionFromTemplate(Intent data) {
        if (data==null) {
            return;
        }
        if (db==null) {
            db=new DatabaseAdapter(getContext());
        }
        long templateId = data.getLongExtra(SelectTemplateFragment.TEMPATE_ID, -1);
        int multiplier = data.getIntExtra(SelectTemplateFragment.MULTIPLIER, 1);
        boolean edit = data.getBooleanExtra(SelectTemplateFragment.EDIT_AFTER_CREATION, false);
        if (templateId > 0) {
            long id = duplicateTransaction(templateId, multiplier);
            Transaction t = db.getTransaction(id);
            if (t.fromAmount == 0 || edit) {
                new BlotterOperations(this, db, id).asNewFromTemplate().editTransaction();
            }
        }

    }

    private void saveFilter() {
        SharedPreferences preferences = activity.getPreferences(0);
        blotterFilter.toSharedPreferences(preferences);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem item= menu.findItem(R.id.action_filter);
        if (item!=null) { //ex templates
            item.setIcon(blotterFilter.isEmpty() ? R.drawable.ic_filter_list : R.drawable.ic_menu_filter_on);
        }
        item= menu.findItem(R.id.opt_menu_bill);
        if (item!=null) { //ex templates
            if (blotterFilter.getAccountId()>0) {
                item.setEnabled(true);
            } else {
                item.setEnabled(false);
            }
        }
        item= menu.findItem(R.id.opt_menu_bill);
        if (item!=null) { //ex templates
            if (blotterFilter.getAccountId()>0) {
                item.setEnabled(true);
            } else {
                item.setEnabled(false);
            }
        }

        String title = blotterFilter.getTitle();
        if (title != null && !title.equals("")) {
            getActivity().setTitle(title);
        } else {
            getActivity().setTitle(R.string.blotter);
        }

    }

    @Override
    protected void viewItem(View v, int position, long id) {
        editTransaction(id);
    }

    private void showTransactionInfo(long id) {
        LayoutInflater layoutInflater = (LayoutInflater)this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        NodeInflater inflater = new NodeInflater(layoutInflater);
        TransactionInfoDialog transactionInfoView = new TransactionInfoDialog(this.getActivity(), db, inflater);
        transactionInfoView.show(this, id);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        long accountId = blotterFilter.getAccountId();
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_list_template:
                selectTemplate();
                return true;
            case R.id.action_mass_op:
                Bundle bundle = new Bundle();
                blotterFilter.toBundle(bundle);
                bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,MassOpFragment.class.getCanonicalName());
                activity.onFragmentMessage(MyFragmentAPI.REQUEST_BLOTTER,bundle);
                return true;
            case R.id.add_transaction:
            case R.id.bAdd:
                addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class);
                return true;
            case R.id.add_transfer:
            case R.id.bTransfer:
                addItem(NEW_TRANSFER_REQUEST, TransferActivity.class);
                return true;

            case R.id.opt_menu_month:
                // call credit card bill activity sending account id
                bundle = new Bundle();
                bundle.putLong(MonthlyViewActivity.ACCOUNT_EXTRA, blotterFilter.getAccountId());
                bundle.putLong(MyFragmentAPI.ENTITY_ID_EXTRA, blotterFilter.getAccountId());
                bundle.putBoolean(MonthlyViewActivity.BILL_PREVIEW_EXTRA, false);
                bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,MonthlyViewActivity.class.getCanonicalName());
                activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
                return true;
            case R.id.action_filter:
                bundle = new Bundle();
                blotterFilter.toBundle(bundle);
                bundle.putBoolean(BlotterFilterActivity.IS_ACCOUNT_FILTER, blotterFilter.getAccountId() > 0);
                bundle.putInt(MyFragmentAPI.ENTITY_REQUEST_EXTRA,FILTER_REQUEST);
                bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,BlotterFilterActivity.class.getCanonicalName());
                Fragment fragment = new BlotterFilterActivity();
                fragment.setTargetFragment(this, FILTER_REQUEST);
                fragment.setArguments(bundle);
                activity.startFragmentForResult(fragment,this);
                return true;
            case R.id.opt_menu_bill:
                if (accountId != -1) {
                    Account account = em.getAccount(accountId);
                    if (account.paymentDay > 0 && account.closingDay>0) {
                        bundle = new Bundle();
                        bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,MonthlyViewActivity.class.getCanonicalName());
                        bundle.putLong(MonthlyViewActivity.ACCOUNT_EXTRA, accountId);
                        bundle.putLong(MyFragmentAPI.ENTITY_ID_EXTRA, accountId);
                        bundle.putBoolean(MonthlyViewActivity.BILL_PREVIEW_EXTRA, true);
                        activity.onFragmentMessage(MyFragmentAPI.REQUEST_BLOTTER,bundle);
                        return true;
                    } else {
                        // display message: need payment and closing day
                        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this.getActivity());
                        dlgAlert.setMessage(R.string.statement_error);
                        dlgAlert.setTitle(R.string.ccard_statement);
                        dlgAlert.setPositiveButton(R.string.ok, null);
                        dlgAlert.setCancelable(true);
                        dlgAlert.create().show();
                        return true;
                    }
                } else {
                    return true;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected String getMyTitle() {
        if (blotterFilter!=null
                && blotterFilter.getTitle()!=null
                && ! blotterFilter.getTitle().equals("")) {
            return blotterFilter.getTitle();
        } else {
            return getResources().getString(R.string.blotter);
        }
    }
}