/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.flowzr.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.DatabaseHelper;
import com.flowzr.db.MyEntityManager;
import com.flowzr.model.Attribute;
import com.flowzr.model.Category;
import com.flowzr.model.Transaction;
import com.flowzr.model.TransactionAttribute;
import com.flowzr.utils.TransactionUtils;
import com.flowzr.view.AttributeView;
import com.flowzr.view.AttributeViewFactory;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 7/4/12 5:35 PM
 */
/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 7/4/12 5:35 PM
 */
public class CategorySelector {

    private final Activity activity;
    private final DatabaseAdapter db;
    private final MyEntityManager em;
    private final ActivityLayout x;

    private TextView categoryText;
    private Cursor categoryCursor;
    private ListAdapter categoryAdapter;
    private LinearLayout attributesLayout;

    private long selectedCategoryId = 0;
    private CategorySelectorListener listener;
    private boolean showSplitCategory = true;

    public CategorySelector(Activity activity, DatabaseAdapter db, ActivityLayout x) {
        this.activity = activity;
        this.db = db;
        this.em = db.em();
        this.x = x;
    }

    public void setListener(CategorySelectorListener listener) {
        this.listener = listener;
    }

    public void doNotShowSplitCategory() {
        this.showSplitCategory = false;
    }

    public void fetchCategories(boolean fetchAllCategories) {
        if (fetchAllCategories) {
            categoryCursor = db.getAllCategories();
        } else {
            categoryCursor = db.getCategories(true);
        }
        activity.startManagingCursor(categoryCursor);
        categoryAdapter = TransactionUtils.createCategoryAdapter(db, activity, categoryCursor);
    }

    public void createNode(LinearLayout layout, boolean showSplitButton) {
        if (showSplitButton) {
            categoryText = x.addListNodeCategory(layout);
        } else {
            categoryText = x.addListNodePlus(layout, R.id.category, R.id.category_add, R.string.category, R.string.select_category);
        }
        categoryText.setText(R.string.no_category);
    }

    public void createDummyNode() {
        //LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //categoryText = (EditText) layoutInflater.inflate(R.layout.edit_text, null);
        categoryText = new EditText(activity);
    }

    public void onClick(int id) {
        switch (id) {
            case R.id.category: {
                if (!CategorySelectorFragment.pickCategory(activity, selectedCategoryId, showSplitCategory)) {
                    x.select(activity, R.id.category, R.string.category, categoryCursor, categoryAdapter,
                            DatabaseHelper.CategoryViewColumns._id.name(), selectedCategoryId);
                }
                break;
            }
            case R.id.category_add: {
                Intent intent = new Intent(activity, CategoryActivity.class);
                activity.startActivityForResult(intent, CategorySelectorFragment.CATEGORY_ADD);
                break;
            }
            case R.id.category_split:
                selectCategory(Category.SPLIT_CATEGORY_ID);
                break;
        }
    }

    public void onSelectedId(int id, long selectedId) {
        if (id == R.id.category) {
            selectCategory(selectedId);
        }
    }

    public long getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void selectCategory(long categoryId) {
        selectCategory(categoryId, true);
    }

    public void selectCategory(long categoryId, boolean selectLast) {
        if (selectedCategoryId != categoryId) {
            Category category = em.getCategory(categoryId);
            if (category != null) {
                categoryText.setText(Category.getTitle(category.title, category.level));
                selectedCategoryId = categoryId;
                if (listener != null) {
                    listener.onCategorySelected(category, selectLast);
                }
            }
        }
    }

    public void createAttributesLayout(LinearLayout layout) {
        attributesLayout = new LinearLayout(activity);
        attributesLayout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(attributesLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    protected List<TransactionAttribute> getAttributes() {
        List<TransactionAttribute> list = new LinkedList<TransactionAttribute>();
        long count = attributesLayout.getChildCount();
        for (int i=0; i<count; i++) {
            View v = attributesLayout.getChildAt(i);
            Object o = v.getTag();
            if (o instanceof AttributeView) {
                AttributeView av = (AttributeView)o;
                TransactionAttribute ta = av.newTransactionAttribute();
                list.add(ta);
            }
        }
        return list;
    }

    public void addAttributes(Transaction transaction) {
        attributesLayout.removeAllViews();
        ArrayList<Attribute> attributes = db.getAllAttributesForCategory(selectedCategoryId);
        Map<Long, String> values = transaction.categoryAttributes;
        for (Attribute a : attributes) {
            AttributeView av = inflateAttribute(a);
            String value = values != null ? values.get(a.id) : null;
            if (value == null) {
                value = a.defaultValue;
            }
            View v = av.inflateView(attributesLayout, value);
            v.setTag(av);
        }
    }

    private AttributeView inflateAttribute(Attribute attribute) {
        return AttributeViewFactory.createViewForAttribute(activity, attribute);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case  CategorySelectorFragment.CATEGORY_ADD: {
                    categoryCursor.requery();
                    long categoryId = data.getLongExtra(DatabaseHelper.CategoryColumns._id.name(), -1);
                    if (categoryId != -1) {
                        selectCategory(categoryId);
                    }
                    break;
                }
                case CategorySelectorFragment.CATEGORY_PICK: {
                    long categoryId = data.getLongExtra(CategorySelectorFragment.SELECTED_CATEGORY_ID, 0);
                    selectCategory(categoryId);
                    break;
                }
            }
        }
    }

    public boolean isSplitCategorySelected() {
        return Category.isSplit(selectedCategoryId);
    }

    public static interface CategorySelectorListener {
        void onCategorySelected(Category category, boolean selectLast);
    }

}
