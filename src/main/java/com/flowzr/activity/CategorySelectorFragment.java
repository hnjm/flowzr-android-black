package com.flowzr.activity;

import android.support.v4.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
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
import com.flowzr.blotter.BlotterFilter;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.DatabaseHelper;
import com.flowzr.filter.Criteria;
import com.flowzr.filter.SingleCategoryCriteria;
import com.flowzr.filter.WhereFilter;
import com.flowzr.model.Category;
import com.flowzr.model.CategoryTree;
import com.flowzr.model.CategoryTreeNavigator;
import com.flowzr.utils.MyPreferences;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 3/14/12 10:40 PM
 */


public class CategorySelectorFragment extends AbstractListFragment {

    //public static final String SELECTED_CATEGORY_ID = "SELECTED_CATEGORY_ID";
    public static final String INCLUDE_SPLIT_CATEGORY = "INCLUDE_SPLIT_CATEGORY";

    public static final int CATEGORY_PICK = 1000;
    public static final int CATEGORY_ADD = 1001;

    private int incomeColor;
    private int expenseColor;

    private CategoryTreeNavigator navigator;
    private Map<Long, String> attributes;

    private Button bBack;

    public CategorySelectorFragment() {
        super(R.layout.entity_list);
    }


    @Override
    protected String getEditActivityClass() {
        return CategoryActivity.class.getCanonicalName();
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
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
            // disable context menu
    }


     @Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
         super.onCreateOptionsMenu(menu, inflater);
         menu.clear();
		inflater.inflate(R.menu.category_selector_actions, menu);
        //if (!navigator.canGoBack()) {
        //    menu.removeItem(R.id.action_back);
        //}

	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        Bundle bundle = new Bundle();
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.action_back:
                if (navigator.goBack()) {
                    recreateAdapter();
                }
                //getActivity().supportInvalidateOptionsMenu();
                break;
            case R.id.action_done:
                confirmSelection();
            break;
            case R.id.action_add:
                bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA, getEditActivityClass());
                Fragment fragment = new CategoryActivity();
                fragment.setArguments(bundle);
                fragment.setTargetFragment(this,CATEGORY_ADD);
                activity.startFragmentForResult(fragment,this);
            break;
            case R.id.action_edit:
                bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA, getEditActivityClass());
                bundle.putLong(MyFragmentAPI.ENTITY_ID_EXTRA, navigator.selectedCategoryId);
                fragment = new CategoryActivity();
                fragment.setArguments(bundle);
                fragment.setTargetFragment(this,CATEGORY_PICK);
                activity.startFragmentForResult(fragment,this);
                break;
            case R.id.action_attributes:
                bundle = new Bundle();
                bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA, AttributeListFragment.class.getCanonicalName());
                activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
                break;
        }
        //action_sort_by_title
        //action_re_index
        //action_collapse
        //action_expand
        //action_attributes
	    return true;
	}


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        navigator = new CategoryTreeNavigator(db);
        recreateAdapter();
        navigator.selectedCategoryId=data.getLongExtra(MyFragmentAPI.ENTITY_ID_EXTRA, 0);
        navigator.selectCategory(data.getLongExtra(MyFragmentAPI.ENTITY_ID_EXTRA, 0));
        //confirmSelection();
    }


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
        if (getArguments()!=null) {
            Bundle bundle = new Bundle();
            bundle.putAll(getArguments());
            if (bundle.getBoolean(INCLUDE_SPLIT_CATEGORY, false)) {
                navigator.addSplitCategoryToTheTop();
            }
            navigator.selectCategory(bundle.getLong(MyFragmentAPI.ENTITY_ID_EXTRA, 0));
        } else {
            navigator.selectCategory(0);
        }

	}

    @Override
    protected void internalOnCreate(Bundle savedInstanceState) {

    }

    private void confirmSelection() {
        Bundle bundle = new Bundle();
        WhereFilter filter = WhereFilter.empty();
        if (navigator.selectedCategoryId == 0) {
                filter.put(new SingleCategoryCriteria(0));
        } else {
            Category cat = db.getCategory(navigator.selectedCategoryId);
            filter.put(Criteria.btw(BlotterFilter.CATEGORY_LEFT, String.valueOf(cat.left), String.valueOf(cat.right)));
        }
        filter.toBundle(bundle);
        bundle.putInt(MyFragmentAPI.RESULT_EXTRA, AppCompatActivity.RESULT_OK);
        bundle.putInt(MyFragmentAPI.ENTITY_REQUEST_EXTRA,CATEGORY_PICK);
        bundle.putLong(MyFragmentAPI.ENTITY_ID_EXTRA, navigator.selectedCategoryId);
        activity.onFragmentMessage(MyFragmentAPI.REQUEST_MYENTITY_FINISH,bundle);
    }

    @Override
    protected Cursor createCursor() {
        return null;
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
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
        navigator.navigateTo(id);
        recreateAdapter();
        //getActivity().supportInvalidateOptionsMenu();
        // @TODO category selector navigation and consider longpress doublick
        //if (!navigator.canGoBack()) {
        //    confirmSelection();
        //}
    }
/**
    public static boolean pickCategory(MainActivity activity, long selectedCategoryId, boolean includeSplitCategory) {
        Bundle bundle = new Bundle();
        bundle.putLong(MyFragmentAPI.ENTITY_ID_EXTRA, selectedCategoryId);
        bundle.putLong(MyFragmentAPI.ENTITY_REQUEST_EXTRA, AbstractTransactionActivity.CATEGORY_REQUEST);
        bundle.putBoolean(CategorySelectorFragment.INCLUDE_SPLIT_CATEGORY, includeSplitCategory);
        activity.onFragmentMessage(MyFragmentAPI.REQUEST_MYENTITY_FINISH,bundle);
        return true;
    }
*/

    public static boolean pickCategory(MainActivity activity,AbstractEditorActivity target, long selectedCategoryId, boolean includeSplitCategory) {
        Bundle bundle = new Bundle();
        bundle.putLong(MyFragmentAPI.ENTITY_ID_EXTRA, selectedCategoryId);
        bundle.putLong(MyFragmentAPI.ENTITY_REQUEST_EXTRA, CATEGORY_PICK);
        bundle.putBoolean(CategorySelectorFragment.INCLUDE_SPLIT_CATEGORY, includeSplitCategory);
        Fragment fragment = new CategorySelectorFragment();
        fragment.setArguments(bundle);
        fragment.setTargetFragment(target,CATEGORY_PICK);
        activity.startFragmentForResult(fragment,target);
        return true;
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
		return getResources().getString(R.string.categories);
	}
    

}