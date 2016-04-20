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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.flowzr.R;
import com.flowzr.adapter.ReportAdapter;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.DatabaseHelper.ReportColumns;
import com.flowzr.filter.Criteria;
import com.flowzr.filter.WhereFilter;
import com.flowzr.graph.GraphUnit;
import com.flowzr.model.Total;
import com.flowzr.report.IncomeExpense;
import com.flowzr.report.PeriodReport;
import com.flowzr.report.Report;
import com.flowzr.report.ReportData;
import com.flowzr.utils.Utils;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;

import java.math.BigDecimal;

import static com.flowzr.utils.AndroidUtils.isGreenDroidSupported;

public class ReportFragment extends AbstractListFragment implements RefreshSupportedActivity {

    public ReportFragment() {
		super(R.layout.report);
	}

	protected static final int FILTER_REQUEST = 1;
	public static final int REPORT_PREFERENCES = 1;
	
    public static final String FILTER_INCOME_EXPENSE = "FILTER_INCOME_EXPENSE";
    
	private DatabaseAdapter db;
    private Report currentReport;
    private ReportAsyncTask reportTask;
	
	private WhereFilter filter = WhereFilter.empty();
    private boolean saveFilter = false;
    boolean viewingPieChart = false;
    
    
    private IncomeExpense incomeExpenseState = IncomeExpense.BOTH;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();		
		inflater.inflate(R.menu.reports_actions, menu);    
	}

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem item= menu.findItem(R.id.bFilter);
        if (item!=null) {
            item.setIcon(filter.isEmpty() ? R.drawable.ic_filter_list : R.drawable.ic_menu_filter_on);
        }
        if (currentReport!=null && incomeExpenseState!=null && getView().findViewById(R.id.total_V)!=null) {
        	item= menu.findItem(R.id.bToggle);
			if (currentReport instanceof PeriodReport) {
				getView().findViewById(R.id.total_V).setVisibility(View.GONE);
	        } else {
	        	getView().findViewById(R.id.total_V).setVisibility(View.VISIBLE);
	        }
		    item.setIcon(incomeExpenseState.getIconId());    
	        
	      String reportTitle = getString(currentReport.reportType.titleId);
	      String incomeExpenseTitle = getString(incomeExpenseState.getTitleId());
	      getActivity().setTitle(reportTitle+" ("+incomeExpenseTitle+")");
	      if (!isGreenDroidSupported()) {
	    	  menu.findItem(R.id.bPieChart).setVisible(false);
	      } else {
	  		menu.findItem(R.id.bPieChart).setVisible(true);
	      }
	      if (viewingPieChart) {
		      menu.findItem(R.id.zoomin).setVisible(true);	 
		      menu.findItem(R.id.zoomout).setVisible(true);	
		      menu.findItem(R.id.bPieChart).setVisible(false);	       	      
	      } else {
	    	  menu.findItem(R.id.zoomin).setVisible(false);	 
	    	  menu.findItem(R.id.zoomout).setVisible(false);	
	    	  menu.findItem(R.id.bPieChart).setVisible(true);	  	    	  
	      }
		}
    }
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    Intent intent;

	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.bFilter:
                Fragment fragment = new  ReportFilterActivity();
                Bundle bundle= new Bundle();
                filter.toBundle(bundle);
                fragment.setArguments(bundle);
                bundle.putInt(MyFragmentAPI.ENTITY_REQUEST_EXTRA,FILTER_REQUEST);
                activity.startFragmentForResult(fragment,this);
	            return true;
	        case R.id.bToggle: 
	        	toggleIncomeExpense();
	            return true;
	        case R.id.bPieChart: 
	            if (isGreenDroidSupported()) {
	            	showPieChart();
	            }
	            return true;
	        case R.id.zoomin: 
	        	//if (mChartView!=null)
	        	mChartView.zoomIn();
	            return true;
	        case R.id.zoomout: 
	        	//if (mChartView!=null)
	        	mChartView.zoomOut();	        	
	            return true;	            
	        
	        case R.id.settings:
				intent = new Intent(this.getActivity(), ReportPreferencesActivity.class);
			    startActivityForResult(intent, REPORT_PREFERENCES);	        	
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

    @Override
    protected String getEditActivityClass() {
        return null;
    }

    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Bundle bundle=getArguments();
		if (bundle != null) {
		    db = new DatabaseAdapter(getActivity());
			db.open();
            currentReport = ReportsListFragment.createReport(this.getActivity(), db.em(), bundle);
			filter = WhereFilter.fromBundle(bundle);
            if (bundle.getString(FILTER_INCOME_EXPENSE) != null) {
                incomeExpenseState = IncomeExpense.valueOf(bundle.getString(FILTER_INCOME_EXPENSE));
            }
            if (currentReport!=null) {
	            if (filter.isEmpty()) {
	                loadFilter();
	            }
				selectReport();
            }
		}
	
	}

    @Override
    protected void internalOnCreate(Bundle savedInstanceState) {

    }


    private SharedPreferences getPreferencesForReport() {
        return getActivity().getSharedPreferences("ReportActivity_"+currentReport.reportType.name()+"_DEFAULT", 0);
    }

    private void toggleIncomeExpense() {
        IncomeExpense[] values = IncomeExpense.values();
        int nextIndex = incomeExpenseState.ordinal() + 1;
        incomeExpenseState = nextIndex < values.length ? values[nextIndex] : values[0];
        getActivity().supportInvalidateOptionsMenu();
        saveFilter();
        selectReport();
    }

    private void showPieChart() {
        new PieChartGeneratorTask().execute();
    }

    private void applyAnimationToListView() {
        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(50);
        set.addAnimation(animation);

        animation = new TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f
        );
        animation.setDuration(100);
        set.addAnimation(animation);

        LayoutAnimationController controller = new LayoutAnimationController(set, 0.5f);
        ListView listView = getListView();
        listView.setLayoutAnimation(controller);
    }



	void selectReport() {
        cancelCurrentReportTask();
        reportTask = new ReportAsyncTask(currentReport, incomeExpenseState);
        reportTask.execute();
	}

    private void cancelCurrentReportTask() {
        if (reportTask != null) {
            reportTask.cancel(true);
        }
    }

    private void applyFilter() {
        TextView tv = (TextView)getView().findViewById(R.id.period);
        if (tv!=null) {
	        if (currentReport instanceof PeriodReport) {
	            tv.setVisibility(View.GONE);
	        } else {
	            Criteria c = filter.get(ReportColumns.DATETIME);
	            if (c != null) {
	                tv.setText(DateUtils.formatDateRange(this.getActivity(), c.getLongValue1(), c.getLongValue2(),
	                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH));
		            tv.setVisibility(View.VISIBLE);
	            } else {
	                tv.setText(R.string.no_filter);
		            tv.setVisibility(View.GONE);	                
	            }

	        }
        }
    }


    @Override
	public void onDestroy() {
        cancelCurrentReportTask();
        if (db!=null) {
        	db.close();
        }
		super.onDestroy();
	}

	@Override
	public void recreateCursor() {
		//selectReport();
	}

    @Override
    public void integrityCheck() {
    }

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == FILTER_REQUEST) {
			if (resultCode == MainActivity.RESULT_FIRST_USER) {
				filter.clear();
                saveFilter();
                selectReport();

			} else if (resultCode == MainActivity.RESULT_OK) {
                filter = WhereFilter.fromIntent(data);
                saveFilter();
                selectReport();
			}
            getActivity().supportInvalidateOptionsMenu();
		}
	}

    private void saveFilter() {
        if (saveFilter) {
            SharedPreferences preferences = getPreferencesForReport();
            filter.toSharedPreferences(preferences);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(FILTER_INCOME_EXPENSE, incomeExpenseState.name());
            editor.apply();
        }
        applyFilter();
    }

    private void loadFilter() {
        SharedPreferences preferences = getPreferencesForReport();
        filter = WhereFilter.fromSharedPreferences(preferences);
        incomeExpenseState = IncomeExpense.valueOf(preferences.getString(FILTER_INCOME_EXPENSE, IncomeExpense.BOTH.name()));
        saveFilter = true;
    }

    private void displayTotal(Total total) {
        if (currentReport.shouldDisplayTotal() && getView()!=null) {
            TextView totalText = (TextView)getView().findViewById(R.id.total);
            if (totalText!=null) {
            	Utils u = new Utils(this.getActivity());
            	u.setTotal(totalText, total);
            }
        }
    }

    private class ReportAsyncTask extends AsyncTask<Void, Void, ReportData> {

        private final Report report;
        private final IncomeExpense incomeExpense;
        private long t0=0;

        private ReportAsyncTask(Report report, IncomeExpense incomeExpense) {
            this.report = report;
            this.incomeExpense = incomeExpense;
        }

        @Override
        protected void onPreExecute() {
        	t0=System.currentTimeMillis();
        	getActivity().setProgressBarIndeterminateVisibility(true);    
        	if (getView()!=null && getView().findViewById(R.id.empty_text)!=null) {	 
        		Log.d("Financisto","calculating report ...");
        		getView().findViewById(R.id.emptyView).setVisibility(View.VISIBLE);
        		((TextView)getView().findViewById(R.id.empty_text)).setText(R.string.calculating);
        		getView().findViewById(android.R.id.list).setVisibility(View.GONE);        		
        		getView().findViewById(R.id.chart_container).setVisibility(View.GONE);	         		       		
        	}  
        }

        @Override
        protected ReportData doInBackground(Void...voids) {
            report.setIncomeExpense(incomeExpense);
            return report.getReport(db, WhereFilter.copyOf(filter));
        }

        @Override
        protected void onPostExecute(ReportData data) {
            displayTotal(data.total);
            if (getView()!=null && getView().findViewById(R.id.empty_text)!=null) {
	            getActivity().setProgressBarIndeterminateVisibility(false);            
	            ((TextView) getView().findViewById(R.id.empty_text)).setText(R.string.empty_report);           
            }
            ReportAdapter adapter = new ReportAdapter(ReportFragment.this.getActivity(), data.units);
            setListAdapter(adapter);
            try {
            	applyAnimationToListView();
                getActivity().supportInvalidateOptionsMenu();
            } catch(Exception e) {
            		//fragment is away ...
            }
            viewingPieChart=false;
            long t=System.currentTimeMillis();
            Log.d("Financisto", "Load time = " + (t - t0) + " ms");            

        }

    }

    GraphicalView mChartView;
    
    private class PieChartGeneratorTask extends AsyncTask<Void, Void, Boolean> {

    	DefaultRenderer renderer = new DefaultRenderer();
        CategorySeries series = new CategorySeries("AAA");
        long t0=0;

        
        @Override
        protected void onPreExecute() {       	
        	t0=System.currentTimeMillis();
            getActivity().setProgressBarIndeterminateVisibility(true);
        	if (getView()!=null && getView().findViewById(R.id.empty_text)!=null) {	 
        		Log.d("Financisto","calculating pie chart report");
        		getView().findViewById(R.id.emptyView).setVisibility(View.VISIBLE);
        		((TextView)getView().findViewById(R.id.empty_text)).setText(R.string.calculating);
        		getView().findViewById(android.R.id.list).setVisibility(View.GONE);        		
        		getView().findViewById(R.id.chart_container).setVisibility(View.GONE);	         		       		
        	}                        
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            return createPieChart();
        }

        private boolean createPieChart() {
            
            renderer.setLabelsTextSize(getResources().getDimension(R.dimen.report_labels_text_size));
            renderer.setLegendTextSize(getResources().getDimension(R.dimen.report_legend_text_size));
            
            renderer.setChartTitleTextSize(getResources().getDimension(R.dimen.report_chart_text_size));

            renderer.setLegendHeight(getResources().getDimensionPixelOffset(R.dimen.report_legend_height));
            
            renderer.setChartTitleTextSize(getResources().getDimension(R.dimen.report_chart_text_size));
            //renderer.setAxisTitleTextSize(getResources().getDimension(R.dimen.report_axis_title_text_size));
            renderer.setLabelsTextSize(getResources().getDimension(R.dimen.report_labels_text_size));
            renderer.setLegendTextSize(getResources().getDimension(R.dimen.report_legend_text_size));
            renderer.setLegendHeight(getResources().getDimensionPixelOffset(R.dimen.report_legend_height));
            renderer.setMargins(new int[]{
                    getResources().getDimensionPixelOffset(R.dimen.report_margin_top),
                    getResources().getDimensionPixelOffset(R.dimen.report_margin_left),
                    getResources().getDimensionPixelOffset(R.dimen.report_margin_bottom),
                    getResources().getDimensionPixelOffset(R.dimen.report_margin_right)});

            ReportData report = currentReport.getReportForChart(db, WhereFilter.copyOf(filter));

            long total = Math.abs(report.total.amount)+Math.abs(report.total.balance);
            int[] colors = generateColors(2*report.units.size());
            int i = 0;
            for (GraphUnit unit : report.units) {
                addSeries(series, renderer, unit.name, unit.getIncomeExpense().income, total, colors[i++]);
                addSeries(series, renderer, unit.name, unit.getIncomeExpense().expense, total, colors[i++]);
            }
                       
            renderer.setZoomEnabled(false);
            renderer.setExternalZoomEnabled(true);
            renderer.setChartTitleTextSize(20);
            
            return true;
        }

        public int[] generateColors(int n) {
            int[] colors = new int[n];
            for (int i = 0; i < n; i++) {
                colors[i] = Color.HSVToColor(new float[]{360*(float)i/(float)n, .75f, .85f});
            }
            return colors;
        }

        private void addSeries(CategorySeries series, DefaultRenderer renderer, String name, BigDecimal expense, long total, int color) {
            long amount = expense.longValue();
            if (amount != 0 && total != 0) {
                long percentage = 100*Math.abs(amount)/total;
                series.add((amount > 0 ? "+" : "-") + name + "(" + percentage + "%)", percentage);
                SimpleSeriesRenderer r = new SimpleSeriesRenderer();
                r.setColor(color);
                renderer.addSeriesRenderer(r);
            }
        }

        @Override
        protected void onPostExecute(Boolean b) {
        	//now run on UI thread
        	// possible on back pressed by the time of the task so handle if not view
        	if (getView()!=null) {
	        	if ( getView().findViewById(R.id.chart_container)!=null) { 
		        	LinearLayout chartContainer = (LinearLayout) getView().findViewById(R.id.chart_container);
		        	mChartView=ChartFactory.getPieChartView(ReportFragment.this.getActivity(), series, renderer);	        	
		        	chartContainer.removeAllViews();
		        	chartContainer.addView(mChartView);   	        		
		            viewingPieChart=true;	
		        	getView().findViewById(android.R.id.list).setVisibility(View.GONE);        		
		        	getView().findViewById(R.id.chart_container).setVisibility(View.VISIBLE);	         		       		
		            getActivity().supportInvalidateOptionsMenu();
		            mChartView.zoomOut();
	        	}
        	}
            getActivity().setProgressBarIndeterminateVisibility(false);	     		
            long t=System.currentTimeMillis();
            Log.d("Financisto", "Load time = " + (t - t0) + " ms");
        }

    }



	@Override
	protected void viewItem(View v, int position, long id) {
		if (currentReport != null) {
            Bundle bundle = currentReport.createFragmentBundle(getContext(), db, WhereFilter.copyOf(filter), id);
            activity.onFragmentMessage(MyFragmentAPI.REQUEST_BLOTTER,bundle);
		}
	}

	@Override
	protected Cursor createCursor() {
		// Auto-generated method stub
		return null;
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		// Auto-generated method stub
		return null;
	}

	@Override
	protected void deleteItem(View v, int position, long id) {
		// Auto-generated method stub
		
	}

	@Override
	protected void editItem(View v, int position, long id) {
		// TAuto-generated method stub
		
	}

	@Override
	protected String getMyTitle() {
		// TAuto-generated method stub
		return null;
	}

    
}
