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

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.flowzr.R;
import com.flowzr.datetime.DateUtils;
import com.flowzr.utils.LocalizableEnum;
import com.flowzr.utils.RecurUtils;
import com.flowzr.utils.RecurUtils.DayOfWeek;
import com.flowzr.utils.RecurUtils.EveryXDay;
import com.flowzr.utils.RecurUtils.Layoutable;
import com.flowzr.utils.RecurUtils.Recur;
import com.flowzr.utils.RecurUtils.RecurInterval;
import com.flowzr.utils.RecurUtils.RecurPeriod;
import com.flowzr.utils.RecurUtils.SemiMonthly;
import com.flowzr.utils.RecurUtils.Weekly;
import com.flowzr.utils.Utils;
import com.flowzr.view.NodeInflater;

import java.text.DateFormat;
import java.util.Calendar;

// @TODO check recurActivity call

public class RecurActivity extends AbstractEditorActivity {
	
	public static final String EXTRA_RECUR = "recur";
			
	private static final RecurPeriod[] periods = RecurPeriod.values();
	
	private Spinner sInterval;
	private Spinner sPeriod;
	private LinearLayout layoutInterval;
	private LinearLayout layoutRecur;
	private Button bStartDate;
    private LayoutInflater inflater;
	private final Calendar startDate = Calendar.getInstance();
	private final Calendar stopsOnDate = Calendar.getInstance();
	
	private DateFormat df;

	@Override
	protected int getLayoutId() {
		return R.layout.recur;
	}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        NodeInflater nodeInflater = new NodeInflater(inflater);
        x = new ActivityLayout(nodeInflater, this);
        final Bundle args = getArguments();
        contentId = args != null ? args.getInt("EXTRA_LAYOUT", this.getLayoutId()) : this.getLayoutId();
        this.inflater=inflater;
        return inflater.inflate(getLayoutId(), container, false);
    }

    public void onAttach(Context a) {
        super.onAttach(a);
        setHasOptionsMenu(true);
        activity=(MainActivity)a;
    }

	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		//setContentView(R.layout.recur);
		//initToolbar();
		//getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		df = DateUtils.getLongDateFormat(getContext());
		
		stopsOnDate.add(Calendar.YEAR, 1);
		
		sInterval = (Spinner)getView().findViewById(R.id.intervalSpinner);
		sPeriod = (Spinner)getView().findViewById(R.id.recurSpinner);
		layoutInterval = (LinearLayout)getView().findViewById(R.id.layoutInterval);
		layoutRecur = (LinearLayout)getView().findViewById(R.id.recurInterval);
		
		bStartDate = (Button)getView().findViewById(R.id.bStartDate);
		bStartDate.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(final View v) {
				final Calendar c = startDate;
				DatePickerDialog d = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener(){
					@Override
					public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
						c.set(Calendar.YEAR, year);
						c.set(Calendar.MONTH, monthOfYear);
						c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
						DateUtils.startOfDay(c);
						editStartDate(c.getTimeInMillis());
					}
				}, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
				d.show();
			}
		});
		
		addSpinnerItems(sInterval, new RecurInterval[]{RecurInterval.NO_RECUR, RecurInterval.WEEKLY, RecurInterval.MONTHLY});
		addSpinnerItems(sPeriod, periods);
		
		//LayoutInflater inflater = getLayoutInflater();
		//addLayouts(inflater, layoutInterval, intervals);
		addLayouts(inflater, layoutRecur, periods);
		
		Recur recur = RecurUtils.createDefaultRecur();

		Bundle bundle = getArguments();
		if (bundle != null) {
			String extra = bundle.getString(EXTRA_RECUR);
			if (extra != null) {
				recur = RecurUtils.createFromExtraString(extra); 
			}
		}				
		editRecur(recur);
		
		sInterval.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {				
				RecurInterval interval = getRecurInterval(sInterval.getSelectedItem());
				selectInterval(interval);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
			
		});

		sPeriod.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				RecurPeriod period = periods[position];
				selectPeriod(period);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
			
		});




		
	}

	@Override
	protected void onClick(View v, int id) {

	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {	
        	case R.id.action_done:
				RecurInterval interval = getRecurInterval(sInterval.getSelectedItem());
				RecurPeriod period = periods[sPeriod.getSelectedItemPosition()];
				Recur r = RecurUtils.createRecur(interval);
				r.startDate = startDate.getTimeInMillis();
				r.period = period;
				if (updateInterval(r) && updatePeriod(r)) {
					Bundle bundle  = new Bundle();

					bundle.putString(EXTRA_RECUR, r.toString());
                    // @TODO clean
                    bundle.putInt(MyFragmentAPI.ENTITY_REQUEST_EXTRA,BudgetActivity.RECUR_REQUEST);
                    finishAndClose(bundle);
				}

        		return true;
            case R.id.home:
        	case R.id.action_cancel:
                finishAndClose(AppCompatActivity.RESULT_CANCELED);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.ok, menu);
        getActivity().setTitle(getString(R.string.recur));
        super.onCreateOptionsMenu(menu, inflater);
    }
    
	private static class SpinnerItem {
		public final String title;
		public final String value;
		public SpinnerItem(String title, String value) {
			super();
			this.title = title;
			this.value = value;
		}
		@Override
		public String toString() {
			return title;
		}		
	}
	
	private void addSpinnerItems(Spinner spinner, LocalizableEnum[] a) {
		int length = a.length;
		SpinnerItem[] items = new SpinnerItem[length];
		for (int i=0; i<length; i++) {
			LocalizableEnum x = a[i];
			String title = getString(x.getTitleId());
			String value = x.name();
			items[i] = new SpinnerItem(title, value);
		}
		ArrayAdapter<SpinnerItem> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
	}

	protected RecurInterval getRecurInterval(Object item) {
		return RecurInterval.valueOf(((SpinnerItem)item).value);
	}

	protected boolean updateInterval(Recur r) {
		RecurInterval interval = r.interval;
		View v = selectInterval(interval);
		switch (interval) {
		case EVERY_X_DAY:
			return updateEveryXDay(v, r);
		case WEEKLY:
			return updateWeekly(v, r);
		case SEMI_MONTHLY:
			return updateSemiMonthly(v, r);
		}
		return true;
	}

	protected boolean updatePeriod(Recur r) {
		RecurPeriod period = r.period;
		View v = selectPeriod(period);
		switch (period) {
		case EXACTLY_TIMES:
			return updateExactlyTimes(v, r);
		case STOPS_ON_DATE:
			return updateStopsOnDate(v, r);
		}
		return true;
	}

	private void addLayouts(LayoutInflater inflater, LinearLayout layout, Layoutable[] items) {
		for (Layoutable i : items) {
			int layoutId = i.getLayoutId();
			if (layoutId != 0) {
				final View v = inflater.inflate(layoutId, null);
				v.setTag(i);
				v.setVisibility(View.INVISIBLE);
				if (i == RecurPeriod.STOPS_ON_DATE) {
					Button b = (Button)v.findViewById(R.id.bStopsOnDate);
					final Calendar c = this.stopsOnDate;
					editStopsOnDate(v, c.getTimeInMillis());
					b.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(final View view) {
							DatePickerDialog d = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener(){
								@Override
								public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
									c.set(Calendar.YEAR, year);
									c.set(Calendar.MONTH, monthOfYear);
									c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
									DateUtils.endOfDay(c);
									editStopsOnDate(v, c.getTimeInMillis());
								}
							}, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
							d.show();
						}
					});
				}
				layout.addView(v, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			}			
		}
	}

	private void editRecur(Recur recur) {
		editStartDate(recur.startDate);
		RecurInterval interval = recur.interval;
		SpinnerAdapter adapter = sInterval.getAdapter();
		int count = adapter.getCount();
		for (int i=0; i<count; i++) {
			SpinnerItem item = (SpinnerItem)adapter.getItem(i);
			if (interval == RecurInterval.valueOf(item.value)) {
				sInterval.setSelection(i);
				break;
			}
		}		
		View v = selectInterval(interval);
		switch (interval) {
		case EVERY_X_DAY:
			editEveryXDay(v, recur);
			break;
		case WEEKLY:
			editWeekly(v, recur);
			break;
		case SEMI_MONTHLY:
			editSemiMonthly(v, recur);
			break;
		}
		RecurPeriod period = recur.period;
		sPeriod.setSelection(period.ordinal());
		v = selectPeriod(period);
		switch (period) {
		case EXACTLY_TIMES:
			editExactlyTimes(v, recur.periodParam);
			break;
		case STOPS_ON_DATE:
			editStopsOnDate(v, recur.periodParam);
			break;
		}
	}

	private void editEveryXDay(View v, Recur recur) {
		EveryXDay x = (EveryXDay)recur;
		EditText t = (EditText)v.findViewById(R.id.edEveryXDays);
		t.setText(String.valueOf(x.days));
	}

	private void editWeekly(View v, Recur recur) {
		//return;
	}

	private void editSemiMonthly(View v, Recur recur) {
		SemiMonthly sm = (SemiMonthly)recur;
		EditText t1 = (EditText)v.findViewById(R.id.edFirstDay);
		t1.setText(String.valueOf(sm.firstDay));
		EditText t2 = (EditText)v.findViewById(R.id.edSecondDay);
		t2.setText(String.valueOf(sm.secondDay));
	}

	private boolean updateEveryXDay(View v, Recur r) {
		EveryXDay x = (EveryXDay)r;
		EditText t = (EditText)v.findViewById(R.id.edEveryXDays);
		if (Utils.isEmpty(t)) {
			showError(t, R.string.recur_error_specify_days);
			return false;
		}
		x.days = Integer.parseInt(Utils.text(t));
		return true;
	}

	private void showError(EditText t, int messageId) {
		t.setError(getString(messageId));
	}

	private boolean updateWeekly(View v, Recur r) {
		Weekly w = (Weekly)r;
		int i = startDate.get(Calendar.DAY_OF_WEEK);
		DayOfWeek[] days = DayOfWeek.values();
		for (DayOfWeek d : days) {
			w.unset(d);
		}
		w.set(days[i - 1]);
		return true;
	}

	private boolean updateSemiMonthly(View v, Recur r) {
		SemiMonthly sm = (SemiMonthly)r;
		EditText t1 = (EditText)v.findViewById(R.id.edFirstDay);
		if (Utils.isEmpty(t1)) {
			showError(t1, R.string.recur_error_specify_first_day);
			return false;
		}
		sm.firstDay = Integer.parseInt(Utils.text(t1));
		EditText t2 = (EditText)v.findViewById(R.id.edSecondDay);
		if (Utils.isEmpty(t2)) {
			showError(t2, R.string.recur_error_specify_second_day);
			return false;
		}
		sm.secondDay = Integer.parseInt(Utils.text(t2));
		return true;
	}

	private void editExactlyTimes(View v, long times) {
		EditText e = (EditText)v.findViewById(R.id.edTimes);
		e.setText(times > 0 ? String.valueOf(times) : "1");
	}

	private void editStartDate(long date) {
		Calendar c = startDate;
		c.setTimeInMillis(date);
		bStartDate.setText(df.format(c.getTime()));
	}

	private void editStopsOnDate(View v, long date) {
		Calendar c = stopsOnDate;
		c.setTimeInMillis(date);
		Button b = (Button)v.findViewById(R.id.bStopsOnDate);
		b.setText(df.format(c.getTime()));
	}

	private boolean updateExactlyTimes(View v, Recur r) {		
		EditText e = (EditText)v.findViewById(R.id.edTimes);
		if (Utils.isEmpty(e)) {
			showError(e, R.string.recur_error_specify_times);
			return false;
		}
		r.periodParam = Long.parseLong(Utils.text(e));
		return true;
	}

	private boolean updateStopsOnDate(View v, Recur r) {
		r.periodParam = stopsOnDate.getTimeInMillis();
		return true;
	}

	protected View selectInterval(RecurInterval interval) {
		if (interval == RecurInterval.NO_RECUR) {
            sPeriod.setSelection(RecurPeriod.STOPS_ON_DATE.ordinal());
			sPeriod.setEnabled(false);
		} else {
			sPeriod.setEnabled(true);
		}
		return selectInLayout(layoutInterval, interval);
	}

	protected View selectPeriod(RecurPeriod period) {
		return selectInLayout(layoutRecur, period);
	}

	private View selectInLayout(LinearLayout layout, Object tag) {
		View selected = null;
		int count = layout.getChildCount();
		for (int i = 0; i<count; i++) {
			View v = layout.getChildAt(i);
			if (tag == v.getTag()) {
				selected = v;				
			} else {
				v.setVisibility(View.GONE);
			}
		}
		if (selected != null) {
			selected.setVisibility(View.VISIBLE);
		}
		return selected;
	}
	

}
