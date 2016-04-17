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
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ScrollView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.flowzr.R;
import com.flowzr.adapter.CategoryListAdapter;
import com.flowzr.db.DatabaseHelper;
import com.flowzr.db.DatabaseHelper.AttributeColumns;
import com.flowzr.db.DatabaseHelper.CategoryColumns;
import com.flowzr.model.Attribute;
import com.flowzr.model.Category;
import com.flowzr.model.Currency;
import com.flowzr.utils.StringUtil;

import java.util.ArrayList;

import static com.flowzr.utils.Utils.checkEditText;
import static com.flowzr.utils.Utils.formatRateDate;
import static com.flowzr.utils.Utils.text;

public class CategoryActivity extends AbstractEditorActivity {
	
	public static final String CATEGORY_ID_EXTRA = "categoryId";
	public static final int NEW_ATTRIBUTE_REQUEST = 1;
	public static final int EDIT_ATTRIBUTE_REQUEST = 2;
	public static final int CATEGORY_ADD = 34;

	private String[] types;
	
	private Cursor attributeCursor;
	private ListAdapter attributeAdapter;

    private ToggleButton incomeExpenseButton;

	private EditText categoryTitle;
	private TextView parentCategoryText;
	private Cursor categoryCursor;
	private CategoryListAdapter categoryAdapter;

	private ScrollView scrollView;
	private LinearLayout attributesLayout;
	private LinearLayout parentAttributesLayout;

    private Category category = new Category(-1);

    @Override
    protected int getLayoutId() {
        return R.layout.category;
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		types = getResources().getStringArray(R.array.attribute_types);
		scrollView = (ScrollView)getView().findViewById(R.id.scroll);

		Bundle b = getArguments();
		if (b != null) {
			long id = b.getLong(MyFragmentAPI.ENTITY_ID_EXTRA, -1);
			if (id != -1) {
				category = db.getCategory(id);
			}
		}

		attributeCursor = db.getAllAttributes();
		getActivity().startManagingCursor(attributeCursor);
		attributeAdapter = new SimpleCursorAdapter(getContext(), android.R.layout.simple_spinner_dropdown_item,
				attributeCursor, new String[]{AttributeColumns.NAME}, new int[]{android.R.id.text1});
		
		if (category.id == -1) {
			categoryCursor = db.getCategories(true);
		} else {
			categoryCursor = db.getCategoriesWithoutSubtree(category.id);
		}
		getActivity().startManagingCursor(categoryCursor);

		LinearLayout layout = (LinearLayout)getView().findViewById(R.id.layout);
        LayoutInflater layoutInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		layoutInflater.inflate(R.layout.category_title, layout, true);

        categoryTitle=(EditText)layoutInflater
                .inflate(R.layout.category_title, null)
                .findViewById(R.id.title);

        categoryTitle = (EditText) getView().findViewById(R.id.title);
        categoryTitle.setSingleLine();
        incomeExpenseButton = (ToggleButton) getView().findViewById(R.id.toggle);

		parentCategoryText = x.addListNode(layout, R.id.category, R.string.parent, R.string.select_category);

		attributesLayout = (LinearLayout)x.addTitleNodeNoDivider(layout, R.string.attributes).findViewById(R.id.layout);
		x.addInfoNodePlus(attributesLayout, R.id.new_attribute, R.id.add_attribute, R.string.add_attribute);
		addAttributes();				
		parentAttributesLayout = (LinearLayout)x.addTitleNodeNoDivider(layout, R.string.parent_attributes).findViewById(R.id.layout);
		addParentAttributes();		
		
		categoryAdapter = new CategoryListAdapter(
				db, getContext(), android.R.layout.simple_spinner_dropdown_item, categoryCursor);
		editCategory();
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {	 
        	case R.id.action_done:
        		if (checkEditText(categoryTitle, "title", true, 100)) {						
					category.title = text(categoryTitle);
                    setCategoryType(category);
					int count = attributesLayout.getChildCount();
					ArrayList<Attribute> attributes = new ArrayList<>(count);
					for (int i=0; i<count; i++) {
						View v = attributesLayout.getChildAt(i);
						Object o = v.getTag();
						if (o instanceof Attribute) {
							attributes.add((Attribute)o);
						}
					}
					long id = db.insertOrUpdate(category, attributes);
					Intent data = new Intent();
					data.putExtra(DatabaseHelper.CategoryColumns._id.name(), id);
                    finishAndClose(data.getExtras());
				} 	        		
        		return true;
	    	case R.id.action_cancel:
                finishAndClose(AppCompatActivity.RESULT_CANCELED);
	    		return true;        
        }
        return super.onOptionsItemSelected(item);
    }
	
    private void setCategoryType(Category category) {
        if (category.getParentId() > 0) {
            category.copyTypeFromParent();
        } else {
            if (incomeExpenseButton.isChecked()) {
                category.makeThisCategoryIncome();
            } else {
                category.makeThisCategoryExpense();
            }
        }
    }

	private void editCategory() {
		selectParentCategory(category.getParentId());
		categoryTitle.setText(category.title);
	}

    private void updateIncomeExpenseType() {
        if (category.getParentId() > 0) {
            if (category.parent.isIncome()) {
                incomeExpenseButton.setChecked(true);
            } else {
                incomeExpenseButton.setChecked(false);
            }
            incomeExpenseButton.setEnabled(false);
        } else {
            incomeExpenseButton.setChecked(category.isIncome());
            incomeExpenseButton.setEnabled(true);
        }
    }

    private void addAttributes() {
		long categoryId = category.id;
		if (categoryId == -1) {
			categoryId = 0;
		}				
		ArrayList<Attribute> attributes = db.getAttributesForCategory(categoryId);
		for (Attribute a : attributes) {
			addAttribute(a);
		}
	}

	private void addParentAttributes() {
		long categoryId = category.getParentId();
		ArrayList<Attribute> attributes = db.getAllAttributesForCategory(categoryId);
		if (attributes.size() > 0) {
			for (Attribute a : attributes) {
				View v = x.inflater.new Builder(parentAttributesLayout, R.layout.select_entry_simple).create();
				v.setTag(a);
				setAttributeData(v, a);
			}
		} else {
			x.addInfoNodeSingle(parentAttributesLayout, -1, R.string.no_attributes);
		}		
	}

	private void addAttribute(Attribute a) {		
		View v = x.inflater.new Builder(attributesLayout, R.layout.select_entry_simple_minus).withId(R.id.edit_attribute, this).create();
		setAttributeData(v, a);
		ImageView plusImageView = (ImageView)v.findViewById(R.id.plus_minus);
		plusImageView.setId(R.id.remove_attribute);
		plusImageView.setOnClickListener(this);
		plusImageView.setTag(v.getTag());
		v.setTag(a);
		scrollView.fullScroll(ScrollView.FOCUS_DOWN);
	}

	private void setAttributeData(View v, Attribute a) {
		TextView labelView = (TextView)v.findViewById(R.id.label);
		labelView.setText(a.name);		
		TextView dataView = (TextView)v.findViewById(R.id.data);
		dataView.setText(types[a.type-1]);		
	}

	@Override
	protected void onClick(View v, int id) {
		switch(id) {
			case R.id.category:				
				x.select(getContext(), R.id.category, R.string.parent, categoryCursor, categoryAdapter,
						CategoryColumns._id.name(), category.getParentId());
				break;
			case R.id.new_attribute:				
				x.select(getContext(), R.id.new_attribute, R.string.attribute, attributeCursor, attributeAdapter,
						AttributeColumns.ID, -1);
				break;
			case R.id.add_attribute: {
                Bundle bundle = new Bundle();
                bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA, AttributeActivity.class.getCanonicalName());
                activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
			} break;
			case R.id.edit_attribute: {
				Object o = v.getTag();
				if (o instanceof Attribute) {
                    Bundle bundle = new Bundle();
                    Fragment fragment = new AttributeActivity();
                    bundle.putLong(MyFragmentAPI.ENTITY_ID_EXTRA, ((Attribute)o).id);
					bundle.putLong(AttributeColumns.ID, ((Attribute)o).id);
                    fragment.setArguments(bundle);
                    activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
				}
			} break;
			case R.id.remove_attribute:
				attributesLayout.removeView((View)v.getTag());
				attributesLayout.removeView((View)v.getParent());
				scrollView.fullScroll(ScrollView.FOCUS_DOWN);
				break;
		}
	}	

	@Override
	public void onSelectedId(int id, long selectedId) {
		switch(id) {
			case R.id.category:
				selectParentCategory(selectedId);
				break;
			case R.id.new_attribute:
				Attribute a = db.getAttribute(selectedId);
				addAttribute(a);
				break;
		}
	}

	private void selectParentCategory(long parentId) {
        Category c = em.getCategory(parentId);
		if (c != null) {
            category.parent = c;
            parentCategoryText.setText(c.title);
		}
        updateIncomeExpenseType();
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == AppCompatActivity.RESULT_OK) {
			switch(requestCode) {
			case NEW_ATTRIBUTE_REQUEST: {
				long attributeId = data.getLongExtra(AttributeColumns.ID, -1);
				if (attributeId != -1) {
					Attribute a = db.getAttribute(attributeId);
					addAttribute(a);
				}
			} break;
			case EDIT_ATTRIBUTE_REQUEST: {
				long attributeId = data.getLongExtra(AttributeColumns.ID, -1);
				if (attributeId != -1) {
					Attribute a = db.getAttribute(attributeId);
					attributeCursor.requery();
					updateAttribute(attributesLayout, a);
					updateAttribute(parentAttributesLayout, a);
				}
			} break;
			}
		}
	}

	private void updateAttribute(LinearLayout layout, Attribute a) {
		int count = layout.getChildCount();
		for (int i=0; i<count; i++) {
			View v = layout.getChildAt(i);
			Object o = v.getTag();
			if (o instanceof Attribute) {
				Attribute a2 = (Attribute)o;
				if (a2.id == a.id) {								
					setAttributeData(v, a);
				}
			}
		}
	}

}
