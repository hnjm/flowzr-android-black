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
import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.MyEntityManager;



public abstract class AbstractListFragment extends ListFragment implements RefreshSupportedActivity {
	

	protected static final int MENU_ADD = Menu.FIRST+4;
	protected int contentId;
    protected LayoutInflater inflater;
	protected Cursor cursor;
	protected ListAdapter adapter;
	protected DatabaseAdapter db;
	protected MyEntityManager em;
	protected ImageButton bAdd;
    protected long selectedId = -1;
	protected MainActivity activity;


	protected AbstractListFragment(int contentId) {
		this.contentId = contentId;				
	}

	protected abstract String getEditActivityClass();

	@Override
    public void onAttach(Context a) {
    	super.onAttach(a);
    	setHasOptionsMenu(true);
		activity=(MainActivity)a;
    }


	@Override
	public void onResume() {
		super.onResume();
		recreateCursor();
	}


	public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);        
        final Bundle args = getArguments();
        this.contentId = args != null ? args.getInt("EXTRA_LAYOUT", this.contentId) : this.contentId;
	    db = new DatabaseAdapter(getActivity());
		db.open();
		
		em = db.em();
		
		cursor = createCursor();
		if (cursor != null) {
			getActivity().startManagingCursor(cursor);
		}		
		
        createAdapter(cursor);
		Log.e("flowzr","list fragment on create");
		//getActivity().setTitle(this.getMyTitle());
    }	
    
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	return inflater.inflate(contentId, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
		Log.e("flowzr","list fragment on activity created created");
	    getActivity().setTitle(getMyTitle());
		getListView().setEmptyView(getView().findViewById(R.id.emptyView));
        recreateAdapter();
        registerForContextMenu(getListView());
	}

    protected void recreateAdapter() {
	        adapter = createAdapter(cursor);
	        setListAdapter(adapter);
    }


	protected abstract void internalOnCreate(Bundle savedInstanceState);

	protected abstract Cursor createCursor();

	protected abstract ListAdapter createAdapter(Cursor cursor);
	
	@Override
	public void onDestroy() {
		if (db!=null) {
			db.close();
		}
		super.onDestroy();
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.action_add: 
	            addItem();
	            return true;
			default:
	            return super.onOptionsItemSelected(item);	            
	    }
	}
	

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	                                ContextMenuInfo menuInfo) {
	    super.onCreateContextMenu(menu, v, menuInfo);
	    MenuInflater inflater = getActivity().getMenuInflater();
	    inflater.inflate(R.menu.entity_context, menu);
	}  	

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		super.onContextItemSelected(item);
        AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		switch (item.getItemId()) {
			case R.id.context_entity_info: {
				viewItem(mi.targetView, mi.position, mi.id);
				return true;
			} 			
			case R.id.context_entity_edit: {
				editItem(mi.targetView, mi.position, mi.id);
				return true;
			} 			
			case R.id.context_entity_delete: {
				deleteItem(mi.targetView, mi.position, mi.id);
				return true;
			} 			
		}
		return false;
	}

	
	public void onItemClick( View v, int position, long id)  {
		 viewItem(v, position, id);
	}
	
    @Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		 viewItem(v, position, id);
	}
	
    protected void addItem() {
	}

	protected abstract void deleteItem(View v, int position, long id);

	protected abstract void editItem(View v, int position, long id);

	protected abstract void viewItem(View v, int position, long id);

	public void recreateCursor() {
        Parcelable state =  getListView().onSaveInstanceState();
        try {
            if (cursor != null) {
                getActivity().stopManagingCursor(cursor);
                cursor.close();
            }
            cursor = createCursor();
            if (cursor != null) {
                getActivity().startManagingCursor(cursor);
                recreateAdapter();
            }
        } finally {
        	getListView().onRestoreInstanceState(state);
        }
	}

    @Override
    public void integrityCheck() {
        new IntegrityCheckTask(activity).execute();
    }

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == MainActivity.RESULT_OK) {
			recreateCursor();
		}
	}

	protected abstract String getMyTitle() ;

}
