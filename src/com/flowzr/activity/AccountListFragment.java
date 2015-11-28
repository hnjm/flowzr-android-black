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

import java.util.Calendar;

import android.app.Activity;
import android.content.res.Configuration;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import greendroid.widget.QuickActionGrid;
import greendroid.widget.QuickActionWidget;
import com.flowzr.R;
import com.flowzr.adapter.AccountListAdapter2;
import com.flowzr.blotter.BlotterFilter;
import com.flowzr.blotter.TotalCalculationTask;
import com.flowzr.datetime.Period;
import com.flowzr.datetime.PeriodType;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.dialog.AccountInfoDialog;
import com.flowzr.filter.Criteria;
import com.flowzr.filter.DateTimeCriteria;
import com.flowzr.filter.WhereFilter;
import com.flowzr.graph.Report2DChart;
import com.flowzr.model.Account;
import com.flowzr.model.Total;
import com.flowzr.report.ReportType;
import com.flowzr.utils.IntegrityFix;
import com.flowzr.utils.MyPreferences;
import com.flowzr.utils.PinProtection;
import com.flowzr.view.NodeInflater;


public class AccountListFragment extends AbstractTotalListFragment  {
	
	private static final int NEW_ACCOUNT_REQUEST = 1;
    public static final int EDIT_ACCOUNT_REQUEST = 2;
    private static final int VIEW_ACCOUNT_REQUEST = 3;
    private static final int PURGE_ACCOUNT_REQUEST = 4;
	protected TextView totalText;

    OnAccountSelectedListener mListener;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnAccountSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnArticleSelectedListener");
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
        if (getView().findViewById(R.id.fragment_land_container)!=null) {
            Fragment fragment=new AccountListTotalsDetailsActivity();
            getChildFragmentManager().beginTransaction().replace(R.id.fragment_land_container, fragment).addToBackStack(null).commit();
            getChildFragmentManager().executePendingTransactions();

        }
        recreateCursor();
        recreateAdapter();
        calculateTotals();
        integrityCheck();
    }



    protected void createFromTemplate() {
        Bundle bundle= new Bundle();
        bundle.putInt(EXTRA_REQUEST_TYPE, BlotterFragment.NEW_TRANSACTION_FROM_TEMPLATE_REQUEST);
        Intent intent = new Intent(getActivity(), EntityListActivity.class);
        intent.putExtra(EntityListActivity.REQUEST_NEW_TRANSACTION_FROM_TEMPLATE, true);
        intent.putExtra(EXTRA_REQUEST_TYPE, BlotterFragment.NEW_TRANSACTION_FROM_TEMPLATE_REQUEST);
        ActivityCompat.startActivityForResult(getActivity(), intent, BlotterFragment.NEW_TRANSACTION_FROM_TEMPLATE_REQUEST, getScaleUpOption());

    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    DateTimeCriteria criteria;
		Intent intent = null;
        Bundle options= new Bundle();
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.action_add_account: 
	            addItem();
	            return true;
//	        case R.id.action_hide_closed: 
//	        	//@TODO unimplemented account list hide closed accounts
//	            return true;
	        case R.id.action_integrity_fix: 
	            doIntegrityFix();
	            return true;
            case R.id.action_list_template:
                createFromTemplate();
                return true;
			case R.id.action_planner:
				WhereFilter filter=new WhereFilter(getView().getResources().getString(R.string.planner));
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
				intent = new Intent(getActivity(),EntityListActivity.class);
				intent.putExtra(EntityListActivity.REQUEST_PLANNER, true);
				filter.toIntent(intent);
                ActivityCompat.startActivity(getActivity(), intent, getScaleUpOption());
            	return true;	            
			case R.id.action_scheduled_transaction:
				WhereFilter blotterFilter = new WhereFilter(getView().getResources().getString(R.string.scheduled));
				blotterFilter.eq(BlotterFilter.IS_TEMPLATE, String.valueOf(2));
		        blotterFilter.eq(BlotterFilter.PARENT_ID, String.valueOf(0));
		        //blotterFilter.toBundle(bundle);       
				//((MainActivity) getActivity()).loadTabFragment(new ScheduledListActivity(),R.layout.scheduled_transactions, bundle,MainActivity.TAB_BLOTTER);
				intent = new Intent(getActivity(),EntityListActivity.class);
				intent.putExtra(EntityListActivity.REQUEST_SCHEDULED, true);
				blotterFilter.toIntent(intent);

                ActivityCompat.startActivity(getActivity(), intent, getScaleUpOption());
				return true;
			default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();		
		inflater.inflate(R.menu.accounts_actions, menu);    
		super.onCreateOptionsMenu(menu, inflater);
	}


	

			
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	                                ContextMenuInfo menuInfo) {
	    super.onCreateContextMenu(menu, v, menuInfo);
	    MenuInflater inflater = getActivity().getMenuInflater();
	    inflater.inflate(R.menu.account_context, menu);
	}    
	

	protected void prepareActionGrid() {
        actionGrid = new QuickActionGrid(this.getActivity());
        actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_discard , R.string.delete)); 	//0
        actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_view_as_list, R.string.blotter));			//1
        actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_edit, R.string.edit));			//2	
        actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_new, R.string.transaction));		//3
        actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_import_export, R.string.transfer));		//4     
        actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.amount_input, R.string.update_balance));		//5
        actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_cancel, R.string.delete_old_transactions)); //6
        actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_secure, R.string.close_account));			//7        
        actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_about, R.string.info));			//8
        actionGrid.setOnQuickActionClickListener(accountActionListener);
	}	
    
	private QuickActionWidget.OnQuickActionClickListener accountActionListener = new QuickActionWidget.OnQuickActionClickListener() {
        public void onQuickActionClicked(QuickActionWidget widget, int position) {
            switch (position) {
            	case 0:
            		deleteItem(selectedId);
            		break;            
            	case 1:
                    showAccountTransactions(selectedId);
                    break;
                case 2:
                    editAccount(selectedId);
                    break;
                case 3:
                    addTransaction(selectedId, TransactionActivity.class);
                    break;
                case 4:
                    addTransaction(selectedId, TransferActivity.class);
                    break;
                case 5:
                    updateAccountBalance(selectedId);
                    break;
                case 6:
                    deleteOldTransaction(selectedId);
                    break;
                case 7:
                    closeAccount(selectedId);
                    break;
                case 8:
                    showAccountInfo(selectedId);
                    break;
            }
        }
    };
 
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
			} 			
        	case R.id.context_account_delete:
        		deleteItem(selectedId);
        		break;            
        	case R.id.context_account_info:
                showAccountInfo(selectedId);
                break;
            case R.id.context_account_edit:
                editAccount(selectedId);
                break;
            case R.id.context_account_add_transaction:
                addTransaction(selectedId, TransactionActivity.class);
                break;
            case R.id.context_account_add_transfer:
                addTransaction(selectedId, TransferActivity.class);
                break;
            case R.id.context_account_update_balance:
                updateAccountBalance(selectedId);
                break;        
        }
		return false;
	}    
    
    private void deleteOldTransaction(long id) {
        Intent intent = new Intent(this.getActivity(), PurgeAccountActivity.class);
        intent.putExtra(PurgeAccountActivity.ACCOUNT_ID, id);
        ActivityCompat.startActivityForResult(getActivity(), intent, PURGE_ACCOUNT_REQUEST, getScaleUpOption());
    }
    
    private void closeAccount(long id) {
		Account a = em.getAccount(id);
		a.isActive = !a.isActive;
		em.saveAccount(a);
		recreateCursor();
    }
	
    private void addTransaction(long accountId, Class<? extends AbstractTransactionActivity> clazz) {
        Intent intent = new Intent(this.getActivity(), clazz);
        intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, accountId);
        ActivityCompat.startActivityForResult(getActivity(), intent, VIEW_ACCOUNT_REQUEST, getScaleUpOption());
    }

    @Override
    public void recreateCursor() {
        super.recreateCursor();
        calculateTotals();
    }

    private AccountTotalsCalculationTask totalCalculationTask;


	private void calculateTotals() {
        totalText = ( TextView ) getView().findViewById(R.id.total);
        if (totalText!=null) {
			if (totalCalculationTask != null) {
				totalCalculationTask.stop();
				totalCalculationTask.cancel(true);
			}		
	
	        totalText.setOnClickListener(new View.OnClickListener() {
	            @Override
	            public void onClick(View view) {
	                showTotals();
	            }
	        });
	        totalCalculationTask = new AccountTotalsCalculationTask(this.getActivity(), totalText);
			totalCalculationTask.execute();
        }
	}

    private void showTotals() {
		Intent intent=new Intent(getActivity(),EntityListActivity.class);
		intent.putExtra(EntityListActivity.REQUEST_ACCOUNT_TOTALS, true);
        Bundle options = ActivityOptionsCompat.makeScaleUpAnimation(
                getView(), 0, 0,
                getActivity().findViewById(android.R.id.content).getWidth(),
                getActivity().findViewById(android.R.id.content).getHeight()).toBundle();
        ActivityCompat.startActivity(getActivity(), intent,  options);
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
            Intent intent = new Intent(this.getActivity(), TransactionActivity.class);
            intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, a.id);
            intent.putExtra(TransactionActivity.CURRENT_BALANCE_EXTRA, a.totalAmount);
            ActivityCompat.startActivityForResult(getActivity(), intent, 0, getScaleUpOption());
            return true;
        }
        return false;
    }

    @Override
	protected void addItem() {		
		Intent intent = new Intent(AccountListFragment.this.getActivity(), AccountActivity.class);
        ActivityCompat.startActivityForResult(getActivity(), intent, NEW_ACCOUNT_REQUEST, getScaleUpOption());
	}


	protected void deleteItem(final long id) {
		new AlertDialog.Builder(this.getActivity())
			.setMessage(R.string.delete_account_confirm)
			.setPositiveButton(R.string.yes, new OnClickListener(){
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
	protected void deleteItem(View v, int position, final long id) {
		deleteItem(id);
		
	}

	@Override
	public void editItem(View v, int position, long id) {
        editAccount(id);
	}

    private void editAccount(long id) {
        Intent intent = new Intent(AccountListFragment.this.getActivity(), AccountActivity.class);
        intent.putExtra(AccountActivity.ACCOUNT_ID_EXTRA, id);
        ActivityCompat.startActivityForResult(getActivity(), intent, EDIT_ACCOUNT_REQUEST, getScaleUpOption());
    }

    private void showAccountInfo(long id) {
        LayoutInflater layoutInflater = (LayoutInflater)this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        NodeInflater inflater = new NodeInflater(layoutInflater);
        AccountInfoDialog accountInfoDialog = new AccountInfoDialog(this, id, db, inflater);
        accountInfoDialog.show();
    }
       
    @Override
	protected void viewItem(View v, int position, long id) {
    	showAccountTransactions(id);
	}

    public interface OnAccountSelectedListener {
        public void onAccountSelected(String title, long id);
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
			((MainActivity)getActivity()).mAdapter.notifyDataSetChanged();
		}

/*        if (resultCode == MainActivity.RESULT_OK && requestCode == NEW_TRANSACTION_FROM_TEMPLATE_REQUEST) {
            createTransactionFromTemplate(data);
        }*/
	}

    public void doIntegrityFix() {
        new IntegrityFixTask().execute();
    }
    private class IntegrityFixTask extends AsyncTask<Void, Void, Void> {

        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(getActivity(), null, getString(R.string.integrity_fix_in_progress), true);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Void o) {
            ((MainActivity)getActivity()).refreshCurrentTab();
            progressDialog.dismiss();
        }

        @Override
        protected Void doInBackground(Void... objects) {
            DatabaseAdapter db = new DatabaseAdapter(AccountListFragment.this.getActivity());
            new IntegrityFix(db).fix();
            return null;
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