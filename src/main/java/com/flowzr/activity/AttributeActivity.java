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
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.DatabaseHelper.AttributeColumns;
import com.flowzr.model.Attribute;
import com.flowzr.utils.Utils;

import fr.ganfra.materialspinner.MaterialSpinner;

public class AttributeActivity extends AbstractEditorActivity implements OnItemSelectedListener {

	private DatabaseAdapter db;
	private Spinner typeSpinner;
	private EditText nameTextView;
	private EditText valuesTextView;
	private EditText defaultValueTextView;
	private CheckBox defaultValueCheckBox;
	private Attribute attribute = new Attribute();


    @Override
    protected int getLayoutId() {
        // @TODO improve layout R.layout.attribute
        return R.layout.attribute;
    }


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		db = new DatabaseAdapter(getContext());
		db.open();
		
		typeSpinner = (MaterialSpinner)getView().findViewById(R.id.type);
		typeSpinner.setOnItemSelectedListener(this);

		nameTextView = (EditText)getView().findViewById(R.id.title);
		valuesTextView = (EditText)getView().findViewById(R.id.values);
		defaultValueTextView = (EditText)getView().findViewById(R.id.default_value_text);
		defaultValueCheckBox = (CheckBox)getView().findViewById(R.id.default_value_check);

		Bundle bundle = getArguments();
		if (bundle != null) {
			long id = bundle.getLong(AttributeColumns.ID, -1);
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
                    finishAndClose(intent.getExtras());
				}   	        		
        		return true;
	    	case R.id.action_cancel:
                finishAndClose(AppCompatActivity.RESULT_CANCELED);
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
	public void onDestroy() {
		db.close();
		super.onDestroy();
	}
	
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		boolean showDefaultCheck = Attribute.TYPE_CHECKBOX - position == 1;
		getView().findViewById(R.id.default_value_layout1).setVisibility(!showDefaultCheck ? View.VISIBLE : View.GONE);
		getView().findViewById(R.id.default_value_check).setVisibility(showDefaultCheck ? View.VISIBLE : View.GONE);
		boolean showValues = Attribute.TYPE_LIST - position == 1 || showDefaultCheck;
		getView().findViewById(R.id.values_layout).setVisibility(showValues ? View.VISIBLE : View.GONE);
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

	}
	
}
