package com.flowzr.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.flowzr.R;
import com.flowzr.adapter.BlotterListAdapter;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.filter.DateTimeCriteria;
import com.flowzr.model.Category;
import com.flowzr.model.CategoryTree;
import com.flowzr.model.CategoryTreeNavigator;
import com.flowzr.utils.MenuItemInfo;
import com.flowzr.utils.MyPreferences;

import greendroid.widget.QuickActionGrid;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 3/14/12 10:40 PM
 */
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 3/14/12 10:40 PM
 */
public class CategorySelectorFragment extends AbstractListFragment {

    public static final String SELECTED_CATEGORY_ID = "SELECTED_CATEGORY_ID";
    public static final String INCLUDE_SPLIT_CATEGORY = "INCLUDE_SPLIT_CATEGORY";

    public static final int CATEGORY_PICK = 1000;
    public static final int CATEGORY_ADD = 1001;
    
    private int incomeColor;
    private int expenseColor;

    private CategoryTreeNavigator navigator;
    private Map<Long, String> attributes;

    private Button bBack;

    public CategorySelectorFragment() {
        super(R.layout.category_selector);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
	    db = new DatabaseAdapter(getActivity());
		db.open();
        navigator = new CategoryTreeNavigator(db);
    	super.onCreate(savedInstanceState);
        Resources resources = getResources();
        this.incomeColor = resources.getColor(R.color.category_type_income);
        this.expenseColor = resources.getColor(R.color.category_type_expense);


        if (MyPreferences.isSeparateIncomeExpense(getActivity())) {
            navigator.separateIncomeAndExpense();
        }
        attributes = db.getAllAttributesMap();

   
        
    }
    
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();		
		inflater.inflate(R.menu.category_selector_actions, menu);    
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
        case R.id.bBack: 
            if (navigator.goBack()) {
                recreateAdapter();
            }    	    
	    case R.id.bSelect:
            confirmSelection();
	    }
	    return true;
	}
    

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
        //actionGrid = new QuickActionGrid(this.getActivity());
	    actionGrid=null;
	        
	        Intent intent = getActivity().getIntent();
	        if (intent != null) {
	            boolean includeSplit = intent.getBooleanExtra(INCLUDE_SPLIT_CATEGORY, false);
	            if (includeSplit) {
	                navigator.addSplitCategoryToTheTop();
	            }
	            long selectedCategoryId = intent.getLongExtra(SELECTED_CATEGORY_ID, 0);
	            navigator.selectCategory(selectedCategoryId);
	        }
	}
    
    private void confirmSelection() {
    	Log.i("flowzr","confirm selection");
        Intent data = new Intent();
        data.putExtra(SELECTED_CATEGORY_ID, navigator.selectedCategoryId);
        getActivity().setResult(Activity.RESULT_OK, data);
        getActivity().finish();
    }

//    @Override
//    protected List<MenuItemInfo> createContextMenus(long id) {
//        return Collections.emptyList();
//    }

    @Override
    protected Cursor createCursor() {
        return null;
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        //bBack.setEnabled(navigator.canGoBack());
        return new CategoryAdapter(navigator.categories);
    }

    @Override
    protected void deleteItem(View v, int position, long id) {
    }

    @Override
    protected void editItem(View v, int position, long id) {
    }

    @Override
    protected void viewItem(View v, int position, long id) {
    	navigator.selectCategory(id);
    	navigator.selectedCategoryId=id;
    	if (navigator.navigateTo(id)) {
            recreateAdapter();
        } 
        else {
        	recreateAdapter();
//            if (MyPreferences.isAutoSelectChildCategory(getActivity())) {
//                confirmSelection();
//            }
        }
    }

    public static boolean pickCategory(Activity activity, long selectedCategoryId, boolean includeSplitCategory) {
        if (MyPreferences.isUseHierarchicalCategorySelector(activity)) {
            Intent intent = new Intent(activity, EntityListActivity.class);
            intent.putExtra(EntityListActivity.REQUEST_CATEGORY_SELECTOR,true);            
            intent.putExtra(CategorySelectorFragment.SELECTED_CATEGORY_ID, selectedCategoryId);
            intent.putExtra(CategorySelectorFragment.INCLUDE_SPLIT_CATEGORY, includeSplitCategory);
            activity.startActivityForResult(intent, CATEGORY_PICK);
            return true;
        }
        return false;
    }

    private class CategoryAdapter extends BaseAdapter {

        private final CategoryTree<Category> categories;

        private CategoryAdapter(CategoryTree<Category> categories) {
            this.categories = categories;
        }

        @Override
        public int getCount() {
            return categories.size();
        }

        @Override
        public Category getItem(int i) {
            return categories.getAt(i);
        }

        @Override
        public long getItemId(int i) {
            return getItem(i).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
        	CategorySelectorViewHolder v;
            if (convertView == null) {
            	LayoutInflater inflater = getLayoutInflater(null);
                convertView = inflater.inflate(R.layout.category_selector_list_item, parent, false);
                v = new CategorySelectorViewHolder(convertView);
                convertView.setTag(v);
            } else {
                v = ( CategorySelectorViewHolder)convertView.getTag();
            }
            Category c = getItem(position);
            if (c.id == CategoryTreeNavigator.INCOME_CATEGORY_ID) {
                v.centerView.setText(getString(R.string.income));                
            } else if (c.id == CategoryTreeNavigator.EXPENSE_CATEGORY_ID) {
                v.centerView.setText(getString(R.string.expense));
            } else {
                v.centerView.setText(c.title);
            }
            v.bottomView.setText(c.tag);
            v.indicator.setBackgroundColor(c.isIncome() ? incomeColor : expenseColor);
            v.rightCenterView.setVisibility(View.INVISIBLE);
            v.iconView.setVisibility(View.INVISIBLE);
            if (attributes != null && attributes.containsKey(c.id)) {
                v.rightView.setText(attributes.get(c.id));
                v.rightView.setVisibility(View.VISIBLE);
            } else {
                v.rightView.setVisibility(View.GONE);
            }
            v.topView.setVisibility(View.INVISIBLE);
            if (navigator.isSelected(c.id)) {
                v.layout.setBackgroundResource(R.color.f_blue_lighter1);
            } else {
                v.layout.setBackgroundResource(0);
            }
            return convertView;
        }

    }
    
    public static class CategorySelectorViewHolder {

        public final RelativeLayout layout;
        public final TextView indicator;
        public final TextView topView;
        public final TextView centerView;
        public final TextView bottomView;
        public final TextView rightCenterView;
        public final TextView rightView;
        public final ImageView iconView;
        public final CheckBox checkBox;

        public CategorySelectorViewHolder(View view) {
            layout = (RelativeLayout) view.findViewById(R.id.layout);
            indicator = (TextView) view.findViewById(R.id.indicator);
            topView = (TextView) view.findViewById(R.id.top);
            centerView = (TextView) view.findViewById(R.id.center);
            bottomView = (TextView) view.findViewById(R.id.bottom);
            rightCenterView = (TextView) view.findViewById(R.id.right_center);
            rightView = (TextView) view.findViewById(R.id.right);
            iconView = (ImageView) view.findViewById(R.id.right_top);
            checkBox = (CheckBox) view.findViewById(R.id.cb);
        }

    }

	@Override
	protected String getMyTitle() {
		// TODO Auto-generated method stub
		return null;
	}
    

}