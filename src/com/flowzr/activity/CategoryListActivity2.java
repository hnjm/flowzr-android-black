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

import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.flowzr.R;
import com.flowzr.adapter.CategoryListAdapter2;
import com.flowzr.blotter.BlotterFilter;
import com.flowzr.filter.Criteria;
import com.flowzr.model.Category;
import com.flowzr.model.CategoryTree;
import com.flowzr.model.MyLocation;

import greendroid.widget.QuickActionGrid;
import greendroid.widget.QuickActionWidget;

import java.util.ArrayList;
import java.util.Map;

public class CategoryListActivity2 extends AbstractListFragment {
	
	private static final int NEW_CATEGORY_REQUEST = 1;
	private static final int EDIT_CATEGORY_REQUEST = 2;
	

    public CategoryListActivity2() {
		super(R.layout.category_list);
	}

	private CategoryTree<Category> categories;
	private Map<Long, String> attributes;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		categories = db.getCategoriesTree(false);
		attributes = db.getAllAttributesMap();
		recreateCursor();
	}

	
	
	@Override
	protected void addItem() {
		Intent intent = new Intent(CategoryListActivity2.this.getActivity(), CategoryActivity.class);
		startActivityForResult(intent, NEW_CATEGORY_REQUEST);
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		CategoryListAdapter2 a = new CategoryListAdapter2(this.getActivity(), categories);
		a.setAttributes(attributes);
		return a;
	}

	@Override
	protected Cursor createCursor() {
		return null;
	}
	
    @Override
    public void recreateCursor() {
        long t0 = System.currentTimeMillis();
        categories = db.getCategoriesTree(false);
        attributes = db.getAllAttributesMap();
        updateAdapter();
        long t1 = System.currentTimeMillis();
        Log.i("CategoryListActivity2", "Requery in "+(t1-t0)+"ms");
    }

    private void updateAdapter() {
		((CategoryListAdapter2)adapter).setCategories(categories);
		((CategoryListAdapter2)adapter).setAttributes(attributes);
		notifyDataSetChanged();
	}

	@Override
	protected void deleteItem(View v, int position, final long id) {
		Category c = (Category)getListAdapter().getItem(position);
		new AlertDialog.Builder(this.getActivity())
			.setTitle(c.getTitle())
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setMessage(R.string.delete_category_dialog)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					db.deleteCategory(id);
					recreateCursor();
				}				
			})
			.setNegativeButton(R.string.no, null)
			.show();		
	}

	@Override
	public void editItem(View v, int position, long id) {
		Intent intent = new Intent(CategoryListActivity2.this.getActivity(), CategoryActivity.class);
		intent.putExtra(CategoryActivity.CATEGORY_ID_EXTRA, id);
		startActivityForResult(intent, EDIT_CATEGORY_REQUEST);		
	}

	@Override
	protected void viewItem(View v, final int position, long id) {
		Intent intent = new Intent(this.getActivity(), MainActivity.class);
		Category cat = db.getCategory(id);
        Criteria blotterFilter = Criteria.btw(BlotterFilter.CATEGORY_LEFT, String.valueOf(cat.left), String.valueOf(cat.right));
        blotterFilter.toIntent(cat.getTitle(), intent);
		intent.putExtra(MainActivity.REQUEST_BLOTTER, true);
        startActivity(intent);	
        getActivity().finish();
	}	
	
	
	protected void prepareActionGrid() {
        actionGrid = new QuickActionGrid(this.getActivity());
        actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_discard , R.string.delete)); 	//0
        actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_edit, R.string.edit));			//1
        actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_about, R.string.view));		//2   
        actionGrid.setOnQuickActionClickListener(myGridActionListener);
//		final Category c = (Category)getListAdapter().getItem(position);
//		final ArrayList<PositionAction> actions = new ArrayList<PositionAction>();
//		Category p = c.parent;
//		CategoryTree<Category> categories;  
//		if (p == null) {
//			categories = this.categories;
//		} else {
//			categories = p.children;
//		}
//		final int pos = categories.indexOf(c);
//		if (pos > 0) {
//			actions.add(top);
//			actions.add(up);
//		}
//		if (pos < categories.size() - 1) {
//			actions.add(down);
//			actions.add(bottom);
//		}
//		if (c.hasChildren()) {
//			actions.add(sortByTitle);			
//		}
//		final ListAdapter a = new CategoryPositionListAdapter(actions);
//		final CategoryTree<Category> tree = categories;  
//		new AlertDialog.Builder(this.getActivity())
//			.setTitle(c.getTitle())
//			.setAdapter(a, new DialogInterface.OnClickListener(){
//				@Override
//				public void onClick(DialogInterface dialog, int which) {
//					PositionAction action = actions.get(which);
//					if (action.execute(tree, pos)) {
//						db.updateCategoryTree(tree);
//						notifyDataSetChanged();
//					}
//				}
//			})
//			.show();
	
	}
	
	private QuickActionWidget.OnQuickActionClickListener myGridActionListener = new QuickActionWidget.OnQuickActionClickListener() {
        public void onQuickActionClicked(QuickActionWidget widget, int position) {
            switch (position) {
            	case 0:
            		deleteItem(getView(),0,selectedId);
            		break;            
            	case 1:
                    editItem(getView(),0,selectedId);
                    break;
                case 2:
            		viewItem(getView(),0,selectedId);
                    break;
            }
        }
    };	
	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();		
		inflater.inflate(R.menu.categories_actions, menu);    
		super.onCreateOptionsMenu(menu, inflater);
	}	
		
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
		    case R.id.action_attributes: 
				((EntityListActivity)getActivity()).loadFragment(new AttributeListActivity());
	            return true;
		    case R.id.action_re_index: 
	                db.restoreNoCategory();
	                recreateCursor();
		            return true;
		    case R.id.action_sort_by_title: 
                if (categories.sortByTitle()) {
                    db.updateCategoryTree(categories);
                    recreateCursor();
                }
	            return true;
		    case R.id.action_expand: 
				((CategoryListAdapter2)adapter).expandAllCategories();
	            return true;
	        case R.id.action_collapse: 
				((CategoryListAdapter2)adapter).collapseAllCategories();
	            return true;
	        case R.id.action_add: 
	            addItem();
	            return true;	            
	        default:
	            return super.onOptionsItemSelected(item);	            
	    }
	}
	
	protected void notifyDataSetChanged() {
		((CategoryListAdapter2)adapter).notifyDataSetChanged();
	}

	protected void notifyDataSetInvalidated() {
		((CategoryListAdapter2)adapter).notifyDataSetInvalidated();
	}

	private abstract class PositionAction {
		final int icon;
		final int title;
		public PositionAction(int icon, int title) {
			this.icon = icon;
			this.title = title;
		}
		public abstract boolean execute(CategoryTree<Category> tree, int pos);
	}
	
	private final PositionAction top = new PositionAction(R.drawable.ic_btn_round_top, R.string.position_move_top){
		@Override
		public boolean execute(CategoryTree<Category> tree, int pos) {
			return tree.moveCategoryToTheTop(pos);
		}
	};
	
	private final PositionAction up = new PositionAction(R.drawable.ic_btn_round_up, R.string.position_move_up){
		@Override
		public boolean execute(CategoryTree<Category> tree, int pos) {
			return tree.moveCategoryUp(pos);
		}
	};
	
	private final PositionAction down = new PositionAction(R.drawable.ic_btn_round_down, R.string.position_move_down){
		@Override
		public boolean execute(CategoryTree<Category> tree, int pos) {
			return tree.moveCategoryDown(pos);
		}
	};
	
	private final PositionAction bottom = new PositionAction(R.drawable.ic_btn_round_bottom, R.string.position_move_bottom){
		@Override
		public boolean execute(CategoryTree<Category> tree, int pos) {
			return tree.moveCategoryToTheBottom(pos);
		}
	};
	
	private final PositionAction sortByTitle = new PositionAction(R.drawable.ic_btn_round_sort_by_title, R.string.sort_by_title){
		@Override
		public boolean execute(CategoryTree<Category> tree, int pos) {
			return tree.sortByTitle();
		}
	};

	private class CategoryPositionListAdapter extends BaseAdapter {
		
		private final ArrayList<PositionAction> actions;
		
		public CategoryPositionListAdapter(ArrayList<PositionAction> actions) {
			this.actions = actions;
		}

		@Override
		public int getCount() {
			return actions.size();
		}

		@Override
		public PositionAction getItem(int position) {
			return actions.get(position);
		}

		@Override
		public long getItemId(int position) {
			return actions.get(position).hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(MainActivity.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.position_list_item, parent, false);
			}
			ImageView v = (ImageView)convertView.findViewById(R.id.icon);
			TextView t = (TextView)convertView.findViewById(R.id.line1);
			PositionAction a = actions.get(position);
			v.setImageResource(a.icon);
			t.setText(a.title);			
			return convertView;
		}
		
	}
	
	@Override
	protected String getMyTitle() {
		return getString(R.string.category);
	}

}
