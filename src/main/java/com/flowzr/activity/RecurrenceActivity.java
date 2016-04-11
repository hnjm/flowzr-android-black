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

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.flowzr.R;
import com.flowzr.datetime.DateUtils;
import com.flowzr.recur.DateRecurrenceIterator;
import com.flowzr.recur.Recurrence;
import com.flowzr.recur.RecurrenceFrequency;
import com.flowzr.recur.RecurrencePattern;
import com.flowzr.recur.RecurrencePeriod;
import com.flowzr.recur.RecurrenceUntil;
import com.flowzr.recur.RecurrenceView;
import com.flowzr.recur.RecurrenceViewFactory;
import com.flowzr.utils.EnumUtils;
import com.flowzr.view.NodeInflater;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public class RecurrenceActivity extends AbstractEditorActivity {
	
	public static final String RECURRENCE_PATTERN = "recurrence_pattern";
	
	private static final RecurrenceFrequency[] frequencies = RecurrenceFrequency.values();
	private static final RecurrenceUntil[] untils = RecurrenceUntil.values();
	
	private LinearLayout layout;
	private RecurrenceViewFactory viewFactory;
	
	private TextView startDateView;
	private TextView startTimeView;
	private Recurrence recurrence = Recurrence.noRecur();
	private RecurrenceView recurrencePatternView;
	private RecurrenceView recurrencePeriodView;


	@Override
	public String getMyTag() {
		return MyFragmentAPI.REQUEST_MYENTITY_FINISH;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.recurrence;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setHasOptionsMenu(true);
        NodeInflater nodeInflater = new NodeInflater(inflater);
        x = new ActivityLayout(nodeInflater, this);
		return inflater.inflate(R.layout.recurrence, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		layout = (LinearLayout)getView().findViewById(R.id.layout);
		viewFactory = new RecurrenceViewFactory(getActivity(),this);
		
		//Intent intent = getActivity().getIntent();
		Bundle bundle= getArguments();
		if (bundle != null) {
			String recurrencePattern = bundle.getString(RECURRENCE_PATTERN);
			if (recurrencePattern != null) {
				try {
					recurrence = Recurrence.parse(recurrencePattern);
				} catch (Exception e) {
					recurrence = Recurrence.noRecur();
				}
				recurrencePatternView = viewFactory.create(recurrence.pattern);
				recurrencePeriodView = viewFactory.create(recurrence.period.until);
			}
		}
		
		createNodes();	
		if (recurrencePatternView != null) {
			recurrencePatternView.stateFromString(recurrence.pattern.params);
			if (recurrencePeriodView != null) {
				recurrencePeriodView.stateFromString(recurrence.period.params);
			}
		}
			
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {	 
        	case R.id.action_done:
				if (recurrencePatternView == null) {
					Intent intent = new Intent();
                    Bundle bundle=new Bundle();
                    //bundle.putAll(intent.getExtras());
                    bundle.putInt(MyFragmentAPI.ENTITY_REQUEST_EXTRA,AbstractTransactionActivity.RECURRENCE_REQUEST);
					finishAndClose(intent.getExtras());
				} else {
					if (recurrencePatternView.validateState() && (recurrencePeriodView == null || recurrencePeriodView.validateState())) {
                        Bundle bundle=new Bundle();
                        bundle.putInt(MyFragmentAPI.ENTITY_REQUEST_EXTRA,AbstractTransactionActivity.RECURRENCE_REQUEST);
						bundle.putString(RECURRENCE_PATTERN, stateToString());
						finishAndClose(bundle);
					}
				}	        		
        		return true;
	    	case R.id.action_cancel:
				finishAndClose(AppCompatActivity.RESULT_CANCELED);
	    		return true;        
        }
        return super.onOptionsItemSelected(item);
    }
    
	
	protected String stateToString() {
		if (recurrencePatternView != null) {
			recurrence.pattern = RecurrencePattern.parse(recurrencePatternView.stateToString());
			if (recurrencePeriodView != null) {
				recurrence.period = RecurrencePeriod.parse(recurrencePeriodView.stateToString());
			} else {
				recurrence.period = RecurrencePeriod.noEndDate();				
			}
		} else {
			recurrence.pattern = RecurrencePattern.noRecur();
			recurrence.period = RecurrencePeriod.noEndDate();
		}
		return recurrence.stateToString();
	}

	public void createNodes() {
		layout.removeAllViews();
		x.addListNode2(layout, R.id.recurrence_pattern, R.drawable.ic_repeat, R.string.recurrence_pattern , getString(recurrence.pattern.frequency.titleId));
		if (recurrencePatternView != null) {
			recurrencePatternView.createNodes(layout);
			startDateView = x.addInfoNode(layout, R.id.start_date, R.string.recurrence_period_starts_on_date, 
					DateUtils.getShortDateFormat(getContext()).format(recurrence.getStartDate().getTime()));
			startTimeView = x.addInfoNode(layout, R.id.start_time, R.string.recurrence_period_starts_on_time, 
					DateUtils.getTimeFormat(getContext()).format(recurrence.getStartDate().getTime()));
			if (recurrence.pattern.frequency != RecurrenceFrequency.GEEKY) {
				x.addListNode2(layout, R.id.recurrence_period, R.drawable.ic_today, R.string.recurrence_period, getString(recurrence.period.until.titleId));
				if (recurrencePeriodView != null) {
					recurrencePeriodView.createNodes(layout);
				}
			}
			x.addInfoNodeSingle(layout, R.id.result, R.string.recurrence_evaluate);			
		}
	}

	@Override
	protected void onClick(View v, int id) {
		switch (id) {
			case R.id.recurrence_pattern: {
				ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(getContext(), frequencies);
				x.selectPosition(getContext(), R.id.recurrence_pattern, R.string.recurrence_pattern, adapter, recurrence.pattern.frequency.ordinal());
			} break;
			case R.id.recurrence_period: {
				ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(getContext(), untils);
				x.selectPosition(getContext(), R.id.recurrence_period, R.string.recurrence_period, adapter, recurrence.period.until.ordinal());
			} break;
			case R.id.start_date: {
				final Calendar c = recurrence.getStartDate();
				new DatePickerDialog(getContext(), new OnDateSetListener(){
					@Override
					public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
						recurrence.updateStartDate(year, monthOfYear, dayOfMonth);
						startDateView.setText(DateUtils.getMediumDateFormat(getContext()).format(c.getTime()));
					}				
				}, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
			} break;
			case R.id.start_time: {
				final Calendar c = recurrence.getStartDate();
				boolean is24Format = DateUtils.is24HourFormat(getContext());
				new TimePickerDialog(getContext(), new OnTimeSetListener(){
					@Override
					public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
						recurrence.updateStartTime(hourOfDay, minute, 0);
						startTimeView.setText(DateUtils.getTimeFormat(getContext()).format(c.getTime()));
					}				
				}, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), is24Format).show();
			} break;
			case R.id.result: {
				try {
                    String stateAsString = stateToString();
					Recurrence r = Recurrence.parse(stateAsString);
					DateRecurrenceIterator ri = r.createIterator(new Date());
					StringBuilder sb = new StringBuilder();
					DateFormat df = DateUtils.getMediumDateFormat(getContext());
					String n = String.format("%n");
					int count = 0;
					while (count++ < 10 && ri.hasNext()) {
						Date nextDate = ri.next();
						if (count > 1) {
							sb.append(n);
						}
						sb.append(df.format(nextDate.getTime()));					
					}
					if (ri.hasNext()) {
						sb.append(n).append("...");
					}
					new AlertDialog.Builder(getContext())
						.setTitle(getString(r.pattern.frequency.titleId))
						.setMessage(sb.toString())
						.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						})
						.show();
				} catch (Exception ex) {
					Toast.makeText(getContext(), ex.getClass().getSimpleName()+":"+ex.getMessage(), Toast.LENGTH_SHORT).show();
				}
			} break;
		}
	}

	@Override
	public void onSelectedPos(int id, int selectedPos) {
		switch (id) {
		case R.id.recurrence_pattern:
			RecurrenceFrequency newFrequency = frequencies[selectedPos];
			if (recurrence.pattern.frequency != newFrequency) {
				recurrence.pattern = RecurrencePattern.empty(newFrequency);
				recurrencePatternView = viewFactory.create(recurrence.pattern);
				createNodes();
			}
			break;
		case R.id.recurrence_period:
			RecurrenceUntil newUntil = untils[selectedPos];
			if (recurrence.period.until != newUntil) {
				recurrence.period = RecurrencePeriod.empty(newUntil);
				recurrencePeriodView = viewFactory.create(newUntil);
				createNodes();
			}
			break;
		}
	}
	
}
