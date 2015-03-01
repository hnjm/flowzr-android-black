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

import android.app.Activity;
import android.app.AlertDialog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.*;
import greendroid.widget.QuickActionGrid;
import greendroid.widget.QuickActionWidget;
import com.flowzr.R;
import com.flowzr.adapter.BlotterListAdapter;
import com.flowzr.adapter.TransactionsListAdapter;
import com.flowzr.blotter.AccountTotalCalculationTask;
import com.flowzr.blotter.BlotterTotalCalculationTask;
import com.flowzr.blotter.TotalCalculationTask;
import com.flowzr.filter.WhereFilter;
import com.flowzr.dialog.TransactionInfoDialog;
import com.flowzr.model.Account;
import com.flowzr.model.Transaction;
import com.flowzr.view.NodeInflater;
import static com.flowzr.utils.AndroidUtils.isGreenDroidSupported;

public class BlotterFragment extends AbstractTotalListFragment {
	
	
	public BlotterFragment() {
		super(R.layout.blotter);
	}

    public void recreateAdapter() {
    	super.recreateAdapter();
    }
	
	public static final String SAVE_FILTER = "saveFilter";
	public static final String EXTRA_FILTER_ACCOUNTS = "filterAccounts";

	private static final int NEW_TRANSACTION_REQUEST = 1;
	private static final int NEW_TRANSFER_REQUEST = 3;
	public static final int NEW_TRANSACTION_FROM_TEMPLATE_REQUEST = 5;
	private static final int MONTHLY_VIEW_REQUEST = 6;
	private static final int BILL_PREVIEW_REQUEST = 7;
	
	protected static final int FILTER_REQUEST = 6;
	private static final int MENU_DUPLICATE = MENU_ADD+1;
	private static final int MENU_SAVE_AS_TEMPLATE = MENU_ADD+2;
	
	protected ImageButton bFilter;
    protected ImageButton bTransfer;
    protected ImageButton bTemplate;

    //private QuickActionWidget transactionActionGrid;
    private QuickActionWidget addButtonActionGrid;
    
	private TotalCalculationTask calculationTask;

	protected boolean saveFilter;
	protected WhereFilter blotterFilter = WhereFilter.empty();

    private boolean isAccountBlotter = false;

	protected TextView totalText;
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	    menu.clear();
	    inflater.inflate(R.menu.blotter_actions, menu);
	}    
    
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		
		Bundle args=this.getArguments();

		if (args != null && blotterFilter.isEmpty()) {
			blotterFilter = WhereFilter.fromBundle(args);			
			saveFilter = args.getBoolean(SAVE_FILTER, false);
            isAccountBlotter = args.getBoolean(BlotterFilterActivity.IS_ACCOUNT_FILTER, false);
		}	else {
			Intent intent = getActivity().getIntent();
			if (intent != null) {			
				blotterFilter = WhereFilter.fromIntent(intent);
				saveFilter = intent.getBooleanExtra(SAVE_FILTER, false);
	            isAccountBlotter = intent.getBooleanExtra(BlotterFilterActivity.IS_ACCOUNT_FILTER, false);
			}	
    		if (saveFilter) {
    			saveFilter();
    		}			
		}




	}

	public static Fragment newInstance(Bundle args) {
	    BlotterFragment f = new BlotterFragment();
        f.setArguments(args);        
        return f;
	}   

	public void onResume(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			blotterFilter = WhereFilter.fromBundle(savedInstanceState);
		}    	
    }
    
    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	
		if (savedInstanceState != null) {
			blotterFilter = WhereFilter.fromBundle(savedInstanceState);
		}
		if (saveFilter && blotterFilter.isEmpty()) {	
			blotterFilter = WhereFilter.fromSharedPreferences(getActivity().getPreferences(0));
		}		

		Bundle args=this.getArguments();
		if (args != null) {
			blotterFilter = WhereFilter.fromBundle(args);			
			saveFilter = args.getBoolean(SAVE_FILTER, false);
            isAccountBlotter = args.getBoolean(BlotterFilterActivity.IS_ACCOUNT_FILTER, false);
		}	else {
			Intent intent = getActivity().getIntent();
			if (intent != null) {			
				blotterFilter = WhereFilter.fromIntent(intent);
				saveFilter = intent.getBooleanExtra(SAVE_FILTER, false);
	            isAccountBlotter = intent.getBooleanExtra(BlotterFilterActivity.IS_ACCOUNT_FILTER, false);
			}	
    		if (saveFilter) {
    			saveFilter();
    		}			
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
		calculateTotals();
		getActivity().setTitle(blotterFilter.getTitle());
		prepareAddButtonActionGrid();
        if (args != null && args.containsKey(BlotterFragment.EXTRA_REQUEST_TYPE)) {
            if (args.getInt(BlotterFragment.EXTRA_REQUEST_TYPE)==BlotterFragment.NEW_TRANSACTION_FROM_TEMPLATE_REQUEST) {
                createFromTemplate();
            }
        }
    }    
    
	protected void calculateTotals() {	

        if (getView().findViewById(R.id.fragment_land_container)!=null) {       	
    		Fragment fragment=new BlotterTotalsDetailsFragment();
    		Bundle bundle= new Bundle();
    		blotterFilter.toBundle(bundle);
    		fragment.setArguments(bundle);        	
            getChildFragmentManager().beginTransaction().replace(R.id.fragment_land_container, fragment).commitAllowingStateLoss();
            getChildFragmentManager().executePendingTransactions();
        } else {
    		if (totalText!=null) {
    			if (calculationTask != null) {
    				calculationTask.stop();
    				calculationTask.cancel(true);
    			}
    	        calculationTask = createTotalCalculationTask();
    			calculationTask.execute();
    		}
        	
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
//		Fragment fragment=new BlotterTotalsDetailsActivity();
//		Bundle bundle= new Bundle();
//		blotterFilter.toBundle(bundle);
//		fragment.setArguments(bundle);        	
//        getFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
//        getFragmentManager().executePendingTransactions();   
//        ((MainActivity) getActivity()).loadTabFragment(fragment, R.layout.totals_details, bundle, 1);
        
		Intent intent=new Intent(getActivity(),EntityListActivity.class);
		intent.putExtra(EntityListActivity.REQUEST_BLOTTER_TOTALS, true);
		blotterFilter.toIntent(intent);
		startActivity(intent);        
    }

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	                                ContextMenuInfo menuInfo) {
	    super.onCreateContextMenu(menu, v, menuInfo);
	    MenuInflater inflater = getActivity().getMenuInflater();
	    inflater.inflate(R.menu.transaction_context, menu);
	}  
    
    protected void prepareActionGrid() {
            actionGrid = new QuickActionGrid(this.getActivity());
            actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_about, R.string.info));
            actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_edit, R.string.edit));
            actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_discard, R.string.delete));
            actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_split, R.string.duplicate));
            actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.transaction_status_cleared, R.string.clear));
            actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.transaction_status_reconciled, R.string.reconcile));
            actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_copy, R.string.template));
            actionGrid.setOnQuickActionClickListener(transactionActionListener);
    }
    
    private QuickActionWidget.OnQuickActionClickListener transactionActionListener = new QuickActionWidget.OnQuickActionClickListener() {
        public void onQuickActionClicked(QuickActionWidget widget, int position) {
            switch (position) {
                case 0:
                    showTransactionInfo(selectedId);
                    break;
                case 1:
                    editTransaction(selectedId);
                    break;
                case 2:
                    deleteTransaction(selectedId);
                    break;
                case 3:
                    duplicateTransaction(selectedId, 1);
                    break;
                case 4:
                    clearTransaction(selectedId);
                    break;
                case 5:
                    reconcileTransaction(selectedId);
                    break;
                case 6:
                	duplicateAsTemplate(selectedId);
                    break;
            }
        }
    };

    private void prepareAddButtonActionGrid() {
        if (isGreenDroidSupported()) {
            addButtonActionGrid = new QuickActionGrid(this.getActivity());
            addButtonActionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_new, R.string.transaction));
            addButtonActionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_import_export, R.string.transfer));
            addButtonActionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_paste, R.string.template));
            addButtonActionGrid.setOnQuickActionClickListener(addButtonActionListener);
        }
    }

    private QuickActionWidget.OnQuickActionClickListener addButtonActionListener = new QuickActionWidget.OnQuickActionClickListener() {
        public void onQuickActionClicked(QuickActionWidget widget, int position) {
            switch (position) {
                case 0:
                    addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class);
                    break;
                case 1:
                    addItem(NEW_TRANSFER_REQUEST, TransferActivity.class);
                    break;
                case 2:
                    createFromTemplate();
                    break;
            }
        }

    };
    
    private void duplicateAsTemplate(long id) {
        new BlotterOperations(this, db,id).duplicateAsTemplate();
		Toast.makeText(this.getActivity(), R.string.save_as_template_success, Toast.LENGTH_SHORT).show();    	
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
	}

	protected void createFromTemplate() {
		Bundle bundle= new Bundle();
		bundle.putInt(EXTRA_REQUEST_TYPE, NEW_TRANSACTION_FROM_TEMPLATE_REQUEST);		
		Intent intent = new Intent(getActivity(), EntityListActivity.class);
		intent.putExtra(EntityListActivity.REQUEST_NEW_TRANSACTION_FROM_TEMPLATE, true);
		intent.putExtra(EXTRA_REQUEST_TYPE, NEW_TRANSACTION_FROM_TEMPLATE_REQUEST);
		startActivityForResult(intent, NEW_TRANSACTION_FROM_TEMPLATE_REQUEST);    	
    	
	}
	
	
	private long duplicateTransaction(long id, int multiplier) {
        long newId = new BlotterOperations(this, db, id).duplicateTransaction(multiplier);
		String toastText;
		if (multiplier > 1) {
			toastText = getString(R.string.duplicate_success_with_multiplier, multiplier);
		} else {
			toastText = getString(R.string.duplicate_success);
		}
		Toast.makeText(this.getActivity(), toastText, Toast.LENGTH_LONG).show();
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
        intent.putExtra(TransactionActivity.TEMPLATE_EXTRA, blotterFilter.getIsTemplate());
        startActivityForResult(intent, requestId);
    }

	@Override
	protected Cursor createCursor() {
		Cursor c;
		long accountId = blotterFilter.getAccountId();
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
	protected void deleteItem(View v, int position, final long id) {
        deleteTransaction(id);
	}

    private void deleteTransaction(long id) {
        new BlotterOperations(this, db, id).deleteTransaction();
    }

    protected void afterDeletingTransaction(long id) {
    	try {
        recreateCursor();
    	} catch (Exception e) {
    		
    	}
        AccountWidget.updateWidgets(this.getActivity());
		//((MainActivity)getActivity()).mAdapter.notifyDataSetChanged();        
    }

	@Override
	public void editItem(View v, int position, long id) {

		//editTransaction(id);
	}

    private void editTransaction(long id) {

		new BlotterOperations(this, db, id).editTransaction();
    }

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i("flowzr","Fragment on activity result");
        if (requestCode == FILTER_REQUEST) {
			if (resultCode == MainActivity.RESULT_FIRST_USER) {
				blotterFilter.clear();	
				((MainActivity)getActivity()).mAdapter.setFilter(new Bundle());	
			} else if (resultCode == MainActivity.RESULT_OK) {
				blotterFilter = WhereFilter.fromIntent(data);							
			}	
			if (saveFilter) {
				saveFilter();
			}
			recreateCursor();			
		} 
		
		if (resultCode == MainActivity.RESULT_OK && requestCode == NEW_TRANSACTION_FROM_TEMPLATE_REQUEST) {
            createTransactionFromTemplate(data);
		}

		if (resultCode != MainActivity.RESULT_CANCELED ) {
			getActivity().supportInvalidateOptionsMenu();
			calculateTotals();
		}
  
    }

    public void createTransactionFromTemplate(Intent data) {
        long templateId = data.getLongExtra(SelectTemplateActivity.TEMPATE_ID, -1);
        int multiplier = data.getIntExtra(SelectTemplateActivity.MULTIPLIER, 1);
        boolean edit = data.getBooleanExtra(SelectTemplateActivity.EDIT_AFTER_CREATION, false);
        if (templateId > 0) {
            long id = duplicateTransaction(templateId, multiplier);
            Transaction t = db.getTransaction(id);
            if (t.fromAmount == 0 || edit) {
                new BlotterOperations(this, db, id).asNewFromTemplate().editTransaction();
            }
        }
    }

    private void saveFilter() {
		SharedPreferences preferences = getActivity().getPreferences(0);
		blotterFilter.toSharedPreferences(preferences);
	}

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
    	
		MenuItem item= menu.findItem(R.id.action_filter);
		if (item!=null) { //ex templates
			item.setIcon(blotterFilter.isEmpty() ? R.drawable.ic_menu_filter_off : R.drawable.ic_menu_filter_on);	
		}

		String title = blotterFilter.getTitle();
		if (title != null) {
			getActivity().setTitle(title);
		}
    }
    
    @Override
	protected void viewItem(View v, int position, long id) {
		editTransaction(id);//showTransactionInfo(id);
							// paradoxal but correct should rename clickItem
							// (blotter: edit transaction, account: show account)		 
	}

    private void showTransactionInfo(long id) {
        LayoutInflater layoutInflater = (LayoutInflater)this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        NodeInflater inflater = new NodeInflater(layoutInflater);    	
        TransactionInfoDialog transactionInfoView = new TransactionInfoDialog(this.getActivity(), db, inflater);
        transactionInfoView.show(this, id);
    }
    
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (!super.onContextItemSelected(item)) {
            AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			switch (item.getItemId()) {
			case MENU_DUPLICATE:
				duplicateTransaction(mi.id, 1);
				return true;
			case MENU_SAVE_AS_TEMPLATE:
                new BlotterOperations(this, db, mi.id).duplicateAsTemplate();
				Toast.makeText(this.getActivity(), R.string.save_as_template_success, Toast.LENGTH_SHORT).show();
				return true;
			}	
		}
		return false;
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		long accountId = blotterFilter.getAccountId();
		Intent intent=null;
		switch (item.getItemId()) {
			case R.id.action_list_template:
				createFromTemplate();
				return true;
//			case R.id.action_list_template:
//		    	((MainActivity) getActivity()).loadTabFragment(new TemplatesListActivity(),R.layout.blotter, new Bundle(),MainActivity.TAB_BLOTTER); 	 		
//				return true;
			case R.id.action_mass_op:
				intent = new Intent(getActivity(),EntityListActivity.class);
				intent.putExtra(EntityListActivity.REQUEST_MASS_OP, true);
				blotterFilter.toIntent(intent);
				startActivity(intent);									
		    	//((MainActivity) getActivity()).loadTabFragment(new MassOpActivity(),R.layout.blotter_mass_op, new Bundle(),MainActivity.TAB_BLOTTER);
				return true;
			case R.id.bAdd:
				addItem(NEW_TRANSACTION_REQUEST, TransactionActivity.class);
				return true;
			case R.id.bTransfer:
				addItem(NEW_TRANSFER_REQUEST, TransferActivity.class);
				return true;

	        case R.id.opt_menu_month:
	        	// call credit card bill activity sending account id
	    		intent = new Intent(this.getActivity(), MonthlyViewActivity.class);
	    		intent.putExtra(MonthlyViewActivity.ACCOUNT_EXTRA, accountId);
	        	intent.putExtra(MonthlyViewActivity.BILL_PREVIEW_EXTRA, false);
	    		startActivityForResult(intent, MONTHLY_VIEW_REQUEST);
	            return true;
	        case R.id.action_filter:
	            intent = new Intent(BlotterFragment.this.getActivity(), BlotterFilterActivity.class);
				blotterFilter.toIntent(intent);
                intent.putExtra(BlotterFilterActivity.IS_ACCOUNT_FILTER, isAccountBlotter && blotterFilter.getAccountId() > 0);
				startActivityForResult(intent, FILTER_REQUEST);
				return true;
	        case R.id.opt_menu_bill:
	    		if (accountId != -1) {
	    			Account account = em.getAccount(accountId);
	    			intent = new Intent(this.getActivity(), MonthlyViewActivity.class);
	    			intent.putExtra(MonthlyViewActivity.ACCOUNT_EXTRA, accountId);
		        	// call credit card bill activity sending account id
		        	if (account.paymentDay>0 && account.closingDay>0) {
			        	intent.putExtra(MonthlyViewActivity.BILL_PREVIEW_EXTRA, true);
			    		startActivityForResult(intent, BILL_PREVIEW_REQUEST);
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

	@Override
	protected String getMyTitle() {
		if (blotterFilter!=null) {
			return blotterFilter.getTitle();
		} else {
			return getResources().getString(R.string.blotter);
		}
	}
}
