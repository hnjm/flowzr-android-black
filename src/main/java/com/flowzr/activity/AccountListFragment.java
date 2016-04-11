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

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.flowzr.R;
import com.flowzr.adapter.AccountListAdapter2;
import com.flowzr.blotter.BlotterFilter;
import com.flowzr.blotter.TotalCalculationTask;
import com.flowzr.datetime.Period;
import com.flowzr.datetime.PeriodType;
import com.flowzr.db.DatabaseHelper;
import com.flowzr.dialog.AccountInfoDialog;
import com.flowzr.filter.DateTimeCriteria;
import com.flowzr.filter.WhereFilter;
import com.flowzr.model.Account;
import com.flowzr.model.Total;
import com.flowzr.utils.MyPreferences;
import com.flowzr.view.NodeInflater;
import java.util.Calendar;


public class AccountListFragment extends AbstractTotalListFragment  {

	protected TextView totalText;
    OnAccountSelectedListener mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity=(MainActivity)context;
        try {
            mListener = (OnAccountSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnAccountSelectedListener");
        }
    }

     
    public AccountListFragment() {
        super(R.layout.account_list);
    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	setHasOptionsMenu(true);
    	return inflater.inflate(R.layout.account_list, container, false);
	}





    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getView()!=null && getView().findViewById(R.id.fragment_land_container)!=null) {
            Fragment fragment=new AccountListTotalsDetailsActivity();
            getChildFragmentManager().beginTransaction().replace(R.id.fragment_land_container, fragment).addToBackStack(null).commit();
            getChildFragmentManager().executePendingTransactions();

        }
        recreateCursor();
        recreateAdapter();
        calculateTotals();
        //integrityCheck();
    }


    // TODO set context menu position, filter closed accounts
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    DateTimeCriteria criteria;
	    switch (item.getItemId()) {
	        case R.id.action_add_account: 
	            addItem();
	            return true;
//	        case R.id.action_hide_closed: 
//	        	//@TODO unimplemented account list hide closed accounts
//	            return true;
	        case R.id.action_integrity_fix: 
	            integrityCheck();
	            return true;
            case R.id.action_list_template:
                Bundle bundle = new Bundle();
                bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA, SelectTemplateFragment.class.getCanonicalName());
                activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
                return true;
			case R.id.action_planner:
				@SuppressWarnings("ConstantConditions") WhereFilter filter = new WhereFilter(getView().getResources().getString(R.string.planner));
                Calendar date = Calendar.getInstance();
	            date.add(Calendar.MONTH, 1);
	            criteria = new DateTimeCriteria(PeriodType.THIS_MONTH);
		        long now = System.currentTimeMillis();
		        if (now > criteria.getLongValue1()) {
		            Period period = criteria.getPeriod();
		            period.start = now;
		            criteria = new DateTimeCriteria(period);
		        }
		        filter.put(criteria);
                bundle = new Bundle();
                filter.toBundle(bundle);
                bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,PlannerFragment.class.getCanonicalName());
                activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
            	return true;	            
			case R.id.action_scheduled_transaction:
				@SuppressWarnings("ConstantConditions") WhereFilter blotterFilter = new WhereFilter(getView().getResources().getString(R.string.scheduled));
				blotterFilter.eq(BlotterFilter.IS_TEMPLATE, String.valueOf(2));
		        blotterFilter.eq(BlotterFilter.PARENT_ID, String.valueOf(0));
                bundle = new Bundle();
                blotterFilter.toBundle(bundle);
                bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,ScheduledListFragment.class.getCanonicalName());
                activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
				return true;
			default:
	            return super.onOptionsItemSelected(item);
	    }
	}

    // toolbar

    @Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();		
		inflater.inflate(R.menu.accounts_actions, menu);
        getActivity().setTitle(getString(R.string.accounts));
		super.onCreateOptionsMenu(menu, inflater);
	}

    // account context menu

    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	                                ContextMenuInfo menuInfo) {
	    super.onCreateContextMenu(menu, v, menuInfo);
	    MenuInflater inflater = getActivity().getMenuInflater();
	    inflater.inflate(R.menu.account_context, menu);
	}

   	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
        AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.context_account_delete_old: { 
            	deleteOldTransaction(mi.id);
            }
			case R.id.context_account_close: {
				closeAccount(mi.id);
                break;
			} 			
        	case R.id.context_account_delete:
        		deleteItem(mi.id);
        		break;            
        	case R.id.context_account_info:
                showAccountInfo(mi.id);
                break;
            case R.id.context_account_edit:
                editAccount(mi.id);
                break;
            case R.id.context_account_add_transaction:
                addTransaction(mi.id, TransactionActivity.class);
                break;
            case R.id.context_account_add_transfer:
                addTransaction(mi.id, TransferActivity.class);
                break;
            case R.id.context_account_update_balance:
                updateAccountBalance(mi.id);
                break;        
        }
		return false;
	}    

    private void deleteOldTransaction(long id) {
        Bundle bundle = new Bundle();
        bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA, PurgeAccountActivity.class.getCanonicalName());
        bundle.putLong(MyFragmentAPI.ENTITY_ID_EXTRA, id);
        bundle.putLong(DatabaseHelper.AttributeColumns.ID, id);
        activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
    }
    
    private void closeAccount(long id) {
		Account a = em.getAccount(id);
		a.isActive = !a.isActive;
		em.saveAccount(a);
		recreateCursor();
    }
	
    private void addTransaction(long accountId, Class<? extends AbstractTransactionActivity> clazz) {
        Bundle bundle = new Bundle();
        bundle.putLong(TransactionActivity.ACCOUNT_ID_EXTRA, accountId);
        bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA, TransactionActivity.class.getCanonicalName());
        activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
    }

    @Override
    public void recreateCursor() {
        super.recreateCursor();
        calculateTotals();
    }

    private AccountTotalsCalculationTask totalCalculationTask;


	private void calculateTotals() {
        //noinspection ConstantConditions,ConstantConditions
        totalText = ( TextView ) getView().findViewById(R.id.total);
        if (totalText!=null) {
			if (totalCalculationTask != null) {
				totalCalculationTask.stop();
				totalCalculationTask.cancel(true);
			}		
	
	        totalText.setOnClickListener(new View.OnClickListener() {
	            @Override
	            public void onClick(View view) {
                    Bundle bundle = new Bundle();
                    bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,AccountListTotalsDetailsActivity.class.getCanonicalName());
                    activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);


	            }
	        });

	        totalCalculationTask = new AccountTotalsCalculationTask(this.getActivity(), totalText);
			totalCalculationTask.execute();
        }
	}


	
	public class AccountTotalsCalculationTask extends TotalCalculationTask {

        public AccountTotalsCalculationTask(Context context, TextView totalText) {
            super(context, totalText);
        }

        @Override
        public Total getTotalInHomeCurrency() {
            return db.getAccountsTotalInHomeCurrency();
        }

        @Override
        public Total[] getTotals() {
            return new Total[0];
        }

    }

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new AccountListAdapter2(getActivity(), cursor);
	}

	@Override
	protected Cursor createCursor() {
        if (MyPreferences.isHideClosedAccounts(getActivity())) {
            return em.getAllActiveAccounts();
        } else {
            return em.getAllAccounts();
        }
	}
	
    private boolean updateAccountBalance(long id) {
        Account a = em.getAccount(id);
        if (a != null) {
            Bundle bundle = new Bundle();
            bundle.putLong(TransactionActivity.ACCOUNT_ID_EXTRA, a.id);
            bundle.putLong(TransactionActivity.CURRENT_BALANCE_EXTRA, a.totalAmount);
            bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA,TransactionActivity.class.getCanonicalName());
            activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
            return true;
        }
        return false;
    }

    @Override
	protected void addItem() {
        Bundle bundle = new Bundle();
        bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA, AccountActivity.class.getCanonicalName());
        activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
	}


	protected void deleteItem(final long id) {
        new AlertDialog.Builder(this.getActivity())
                .setMessage(R.string.delete_account_confirm)
                .setPositiveButton(R.string.yes, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        db.deleteAccount(id);
                        recreateCursor();
                        ((MainActivity)getActivity()).mAdapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();


    }
    
	@Override
	protected void deleteItem(int position, final long id) {
		deleteItem(id);
		
	}

	@Override
	public void editItem(long id) {
        editAccount(id);
	}

    private void editAccount(long id) {
        Bundle bundle = new Bundle();
        bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA, AccountActivity.class.getCanonicalName());
        bundle.putLong(MyFragmentAPI.ENTITY_ID_EXTRA, id);
        activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
    }

    private void showAccountInfo(long id) {
        LayoutInflater layoutInflater = (LayoutInflater)this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        NodeInflater inflater = new NodeInflater(layoutInflater);
        AccountInfoDialog accountInfoDialog = new AccountInfoDialog(this,activity, id, db, inflater);
        accountInfoDialog.show();
    }
       
    @Override
	protected void viewItem(View v, int position, long id) {
    	showAccountTransactions(id);
	}

    public interface OnAccountSelectedListener {
        void onAccountSelected(String title, long id);
    }
    
    private void showAccountTransactions(long id) {
        Account account = em.getAccount(id);
        if (account != null) {
        	mListener.onAccountSelected(account.title,id);            
        }
    }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != MainActivity.RESULT_CANCELED ) {
            recreateCursor();
		}
	}



	public static Fragment newInstance(Bundle args) {
	      AccountListFragment f = new AccountListFragment();
          f.setArguments(args);
          return f;
	}    

	@Override
	protected String getMyTitle() {
		return getResources().getString(R.string.accounts);
	}

}