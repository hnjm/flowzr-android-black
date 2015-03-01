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

import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.DatabaseHelper.AttributeColumns;
import com.flowzr.model.Attribute;
import com.flowzr.rates.ExchangeRate;
import com.flowzr.utils.PinProtection;
import com.flowzr.utils.Utils;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

public class AttributeActivity extends AbstractEditorActivity implements OnItemSelectedListener {
	
	public static final String CATEGORY_ID = "category_id";

	private DatabaseAdapter db;
	
	private Spinner typeSpinner;
	private EditText nameTextView;
	private EditText valuesTextView;
	private EditText defaultValueTextView;
	private CheckBox defaultValueCheckBox;
	
	private Attribute attribute = new Attribute();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		setContentView(R.layout.attribute);

		db = new DatabaseAdapter(this);
		db.open();
		
		typeSpinner = (Spinner)findViewById(R.id.type);
		typeSpinner.setOnItemSelectedListener(this);

		nameTextView = (EditText)findViewById(R.id.name);
		valuesTextView = (EditText)findViewById(R.id.values);
		defaultValueTextView = (EditText)findViewById(R.id.default_value_text);
		defaultValueCheckBox = (CheckBox)findViewById(R.id.default_value_check);

		Intent intent = getIntent();
		if (intent != null) {
			long id = intent.getLongExtra(AttributeColumns.ID, -1);
			if (id != -1) {
				attribute = db.getAttribute(id);
				editAttribute();
			}
		}		
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {	 
        	case R.id.action_done:
        		updateAttributeFromUI();
				if (Utils.checkEditText(nameTextView, "name", true, 256)) {
					long id = db.insertOrUpdate(attribute);
					Intent intent = new Intent();				
					intent.putExtra(AttributeColumns.ID, id);
					setResult(RESULT_OK, intent);
					finish();
				}   	        		
        		return true;
	    	case R.id.action_cancel:
				setResult(RESULT_CANCELED);
				finish();
	    		return true;        
        }
        return super.onOptionsItemSelected(item);
    }
	
	private void updateAttributeFromUI() {
		attribute.name = nameTextView.getText().toString();
		attribute.listValues = Utils.text(valuesTextView);
		attribute.type = typeSpinner.getSelectedItemPosition()+1;
		if (attribute.type == Attribute.TYPE_CHECKBOX) {
			attribute.defaultValue = String.valueOf(defaultValueCheckBox.isChecked());
		} else {
			attribute.defaultValue = Utils.text(defaultValueTextView);
		}
	}

	private void editAttribute() {
		nameTextView.setText(attribute.name);
		typeSpinner.setSelection(attribute.type-1);
		if (attribute.listValues != null) {
			valuesTextView.setText(attribute.listValues);
		}
		if (attribute.defaultValue != null) {
			if (attribute.type == Attribute.TYPE_CHECKBOX) {
				defaultValueCheckBox.setChecked(Boolean.valueOf(attribute.defaultValue));
			} else {
				defaultValueTextView.setText(attribute.defaultValue);				
			}			
		}
	}

	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();		
	}
	
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		boolean showDefaultCheck = Attribute.TYPE_CHECKBOX - position == 1;
		findViewById(R.id.default_value_layout1).setVisibility(!showDefaultCheck ? View.VISIBLE : View.GONE);
		findViewById(R.id.default_value_check).setVisibility(showDefaultCheck ? View.VISIBLE : View.GONE);
		boolean showValues = Attribute.TYPE_LIST - position == 1 || showDefaultCheck;
		findViewById(R.id.values_layout).setVisibility(showValues ? View.VISIBLE : View.GONE);
		if (showDefaultCheck) {
			valuesTextView.setHint(R.string.checkbox_values_hint);
		} else {
			valuesTextView.setHint(R.string.attribute_values_hint);
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}

	@Override
	protected void onClick(View v, int id) {
		// TODO Auto-generated method stub
		
	}
	
}