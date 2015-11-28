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

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.flowzr.R;
import com.flowzr.adapter.BudgetListAdapter;
import com.flowzr.blotter.BlotterFilter;
import com.flowzr.filter.WhereFilter;
import com.flowzr.filter.DateTimeCriteria;
import com.flowzr.db.BudgetsTotalCalculator;
import com.flowzr.filter.Criteria;
import com.flowzr.model.Account;
import com.flowzr.model.Budget;
import com.flowzr.model.Total;
import com.flowzr.datetime.PeriodType;
import com.flowzr.utils.RecurUtils;
import com.flowzr.utils.RecurUtils.Recur;
import com.flowzr.utils.RecurUtils.RecurInterval;
import com.flowzr.utils.Utils;

import greendroid.widget.QuickActionGrid;
import greendroid.widget.QuickActionWidget;

import java.util.ArrayList;

public class BudgetListFragment extends AbstractTotalListFragment {
	
	private static final int NEW_BUDGET_REQUEST = 1;
	private static final int EDIT_BUDGET_REQUEST = 2;
	private static final int VIEW_BUDGET_REQUEST = 3;
	private static final int FILTER_BUDGET_REQUEST = 4;

	protected TextView totalText;
	
	private WhereFilter filter = WhereFilter.empty();

	public BudgetListFragment() {
		super(R.layout.budget_list);
	}
	
	private ArrayList<Budget> budgets;
	private Handler handler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (filter.isEmpty()) {
			filter = WhereFilter.fromSharedPreferences(getActivity().getPreferences(0));
		}
		if (filter.isEmpty()) {
			filter.put(new DateTimeCriteria(PeriodType.THIS_MONTH));
		}

	}

  	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();		
		inflater.inflate(R.menu.budgets_actions, menu);    
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add:
			addItem();
			return true;
		case R.id.action_filter_budget:
			Intent intent = new Intent(BudgetListFragment.this.getActivity(), DateFilterActivity.class);
			filter.toIntent(intent);
			ActivityCompat.startActivityForResult(getActivity(), intent, FILTER_BUDGET_REQUEST, getScaleUpOption());
        default:
            return super.onOptionsItemSelected(item);
		}
    }

    @Override
	public void onListItemClick(ListView l, View v, int position, long id) {
        viewItem(v, position, id);
	}
    
    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
    	budgets = em.getAllBudgets(filter);
		handler = new Handler();
		recreateCursor();
        totalText = ( TextView ) getView().findViewById(R.id.total);
		if (getView().findViewById(R.id.fragment_land_container)!=null) {
    		Fragment fragment=new BudgetListTotalsDetailsActivity();
    		Bundle bundle= new Bundle();
    		fragment.setArguments(bundle);
			fragment.setRetainInstance(true);

            getChildFragmentManager().beginTransaction().replace(R.id.fragment_land_container, fragment).addToBackStack(null).commitAllowingStateLoss();
            getChildFragmentManager().executePendingTransactions();
        }        
        
        TextView total_title = ( TextView ) getView().findViewById(R.id.total_title);       
        if (total_title!=null) {
	        total_title.setTextAppearance(getActivity(), R.style.TextAppearance_ZeroAmount);
	        totalText.setOnClickListener(new View.OnClickListener() {
	            @Override
	            public void onClick(View view) {
	                showTotals();
	            }
	        });
	        calculateTotals();
        }
		if (getView().findViewById(R.id.bAdd)!=null) {
			getView().findViewById(R.id.bAdd).setVisibility(View.GONE);
			getView().findViewById(R.id.bAddTransfer).setVisibility(View.GONE);
		}

    }

    private void showTotals() {
		Intent intent=new Intent(getActivity(),EntityListActivity.class);
		intent.putExtra(EntityListActivity.REQUEST_BUDGET_TOTALS, true);
		filter.toIntent(intent);
		ActivityCompat.startActivity(getActivity(), intent,  getScaleUpOption());
    }

	private void saveFilter() {
		SharedPreferences preferences = getActivity().getPreferences(0);
		filter.toSharedPreferences(preferences);
		recreateCursor();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {		

		if (requestCode == FILTER_BUDGET_REQUEST) {
			if (resultCode == MainActivity.RESULT_FIRST_USER) {
				filter.clear();				
			} else if (resultCode == MainActivity.RESULT_OK) {
				String periodType = data.getStringExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_TYPE);
				PeriodType p = PeriodType.valueOf(periodType);
				if (PeriodType.CUSTOM == p) {
					long periodFrom = data.getLongExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_FROM, 0);
					long periodTo = data.getLongExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_TO, 0);
					filter.put(new DateTimeCriteria(periodFrom, periodTo));
				} else {
					filter.put(new DateTimeCriteria(p));
				}
			}
			saveFilter();
		}
		recreateAdapter();
		recreateCursor();
		if (resultCode != MainActivity.RESULT_CANCELED ) {
			((MainActivity)getActivity()).mAdapter.notifyDataSetChanged();
			//getActivity().supportInvalidateOptionsMenu();
		}
	}		

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new BudgetListAdapter(this.getActivity(), budgets);
	}
	
	@Override
	protected Cursor createCursor() {
		return null;
	}
	
    @Override
    public void recreateCursor() {
    	if (getListAdapter()!=null) {
	        budgets = em.getAllBudgets(filter);
	        updateAdapter();
	        calculateTotals();
    	}
    }

    private void updateAdapter() {
		((BudgetListAdapter)adapter).setBudgets(budgets);
	}

	private BudgetTotalsCalculationTask totalCalculationTask;

	private void calculateTotals() {
		if (totalCalculationTask != null) {
			totalCalculationTask.stop();
			totalCalculationTask.cancel(true);
		}		
		totalCalculationTask = new BudgetTotalsCalculationTask(totalText);
		totalCalculationTask.execute((Void[])null);
	}
	
	@Override
	protected void addItem() {
		Intent intent = new Intent(this.getActivity(), BudgetActivity.class);
		ActivityCompat.startActivityForResult(getActivity(), intent, NEW_BUDGET_REQUEST, getScaleUpOption());

	}

	@Override
	protected void deleteItem(View v, int position, final long id) {
		final Budget b = em.load(Budget.class, id);
		if (b.parentBudgetId > 0) {
			new AlertDialog.Builder(this.getActivity())
			.setMessage(R.string.delete_budget_recurring_select)
			.setPositiveButton(R.string.delete_budget_one_entry, new OnClickListener(){
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					em.deleteBudgetOneEntry(id);
					recreateCursor();
				}
			})
			.setNeutralButton(R.string.delete_budget_all_entries, new OnClickListener(){
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					em.deleteBudget(b.parentBudgetId);
					recreateCursor();
				}
			})			
			.setNegativeButton(R.string.cancel, null)
			.show();			
		} else {
			Recur recur = RecurUtils.createFromExtraString(b.recur);
			new AlertDialog.Builder(this.getActivity())
			.setMessage(recur.interval == RecurInterval.NO_RECUR ? R.string.delete_budget_confirm : R.string.delete_budget_recurring_confirm)
			.setPositiveButton(R.string.yes, new OnClickListener(){
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					em.deleteBudget(id);
					recreateCursor();
				}
			})
			.setNegativeButton(R.string.no, null)
			.show();
		}
	}

	@Override
	public void editItem(View v, int position, long id) {
		Budget b = em.load(Budget.class, id);
		Recur recur = b.getRecur();
		if (recur.interval != RecurInterval.NO_RECUR) {
			Toast t = Toast.makeText(this.getActivity(), R.string.edit_recurring_budget, Toast.LENGTH_LONG);
			t.show();
		}
		Intent intent = new Intent(this.getActivity(), BudgetActivity.class);
		intent.putExtra(BudgetActivity.BUDGET_ID_EXTRA, b.parentBudgetId > 0 ? b.parentBudgetId : id);
		ActivityCompat.startActivityForResult(getActivity(), intent, EDIT_BUDGET_REQUEST, getScaleUpOption());

	}
	//TODO check if used
	//protected String getContextMenuHeaderTitle(int position) {
	//	return getString(R.string.budget);
	//}

	@Override
	protected void viewItem(View v, int position, long id) {
        Budget b = em.load(Budget.class, id);
        Bundle bundle = new Bundle();
		Intent intent = new Intent(this.getActivity(), EntityListActivity.class);
		Criteria.eq(BlotterFilter.BUDGET_ID, String.valueOf(id))
			.toIntent(b.title, intent);
		intent.putExtra(MainActivity.REQUEST_BUDGET_BLOTTER, true);
		ActivityCompat.startActivity(getActivity(), intent, getScaleUpOption());

	}	
	
	public class BudgetTotalsCalculationTask extends AsyncTask<Void, Total, Total> {
		
		private volatile boolean isRunning = true;
		
		private final TextView totalText;
		
		public BudgetTotalsCalculationTask(TextView totalText) {
			this.totalText = totalText;
		}

		@Override
		protected Total doInBackground(Void... params) {
			try {
                BudgetsTotalCalculator c = new BudgetsTotalCalculator(db, budgets);
                c.updateBudgets(handler);
                return c.calculateTotalInHomeCurrency();
			} catch (Exception ex) {
				Log.e("BudgetTotals", "Unexpected error", ex);
				return Total.ZERO;
			}

		}

		@Override
		protected void onPostExecute(Total result) {
			if (isRunning) {
				if (BudgetListFragment.this.getActivity()!=null) {
	                Utils u = new Utils(BudgetListFragment.this.getActivity());
	                if (adapter!=null && totalText!=null) {
	                	u.setTotal(totalText, result);

						SpannableString spannablecontent=new SpannableString(totalText.getText());
						spannablecontent.setSpan(new AbsoluteSizeSpan(20,true), 0, spannablecontent.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						// set Text here
						totalText.setText(spannablecontent);
	                	((BudgetListAdapter)adapter).notifyDataSetChanged();
	                }
				}
			}
		}
		
		public void stop() {
			isRunning = false;
		}
		
	}

	@Override
	protected void prepareActionGrid() {
        actionGrid = new QuickActionGrid(this.getActivity());
        actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_discard , R.string.delete)); 	//0
        actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_edit, R.string.edit));			//1	
        actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_about, R.string.view));		//2		
        actionGrid.setOnQuickActionClickListener(budgetActionListener);        
	}
	
	private QuickActionWidget.OnQuickActionClickListener budgetActionListener = new QuickActionWidget.OnQuickActionClickListener() {
        public void onQuickActionClicked(QuickActionWidget widget, int position) {
            switch (position) {
            	case 0:
            		deleteItem(null, 0, selectedId);
            		break;            
                case 1:
                    editItem(null, 0, selectedId);
                    break;
            	case 2:
            		viewItem(null, 0, selectedId);
                    break;
            }
        }
    };


   	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
        AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        switch (item.getItemId()) {		
        	case R.id.context_budget_delete:
        		deleteItem(null, item.getItemId(),selectedId);
        		break;            
        	case R.id.context_budget_info:
                viewItem(null, item.getItemId(),selectedId);
                break;
            case R.id.context_budget_edit:
                editItem(null,item.getItemId(),selectedId);
                break;  
        }
		return false;
	}  
    
    
	public static Fragment newInstance(Bundle args) {
	      BudgetListFragment f = new BudgetListFragment();
          f.setArguments(args);
          return f;
	}    

	@Override
	protected String getMyTitle() {
		return getResources().getString(R.string.budgets);
	}
	
}
