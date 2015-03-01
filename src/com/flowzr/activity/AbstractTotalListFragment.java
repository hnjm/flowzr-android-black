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


import greendroid.widget.QuickActionWidget;

import java.util.LinkedList;
import java.util.List;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.MyEntityManager;
import com.flowzr.utils.MenuItemInfo;
import com.flowzr.utils.PinProtection;
import com.flowzr.utils.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
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
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import static com.flowzr.utils.AndroidUtils.isGreenDroidSupported;

public abstract class AbstractTotalListFragment extends ListFragment implements RefreshSupportedActivity {
	
	protected static final int MENU_VIEW = Menu.FIRST+1;
	protected static final int MENU_EDIT = Menu.FIRST+2;
	protected static final int MENU_DELETE = Menu.FIRST+3;
	protected static final int MENU_ADD = Menu.FIRST+4;
	protected static final int CONTENT_ID_NOT_PROVIDED=-1;
	
	protected static final String EXTRA_LAYOUT="EXTRA_LAYOUT";
	public static final String EXTRA_REQUEST_TYPE="EXTRA_REQUEST_TYPE"; 
	protected int contentId;
		
    protected LayoutInflater inflater;
	protected Cursor cursor;
	protected ListAdapter adapter;
	protected DatabaseAdapter db;
	protected MyEntityManager em;
	protected MenuItem bAdd;
    protected QuickActionWidget actionGrid;
    protected long selectedId = -1;	
	
    protected boolean enablePin = true;
   	
	protected AbstractTotalListFragment(int contentId) {
		this.contentId = contentId;				
	}
    
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);        
        final Bundle args = getArguments();
        this.contentId = args != null ? args.getInt("EXTRA_LAYOUT", this.contentId) : this.contentId;
    }	
    
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	setHasOptionsMenu(true);
    	return inflater.inflate(contentId, container, false);
	}
		
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
  	    
	    db = new DatabaseAdapter(getActivity());
		db.open();
		
		em = db.em();
		
		cursor = createCursor();
		if (cursor != null) {
			getActivity().startManagingCursor(cursor);
		}		

        recreateAdapter();	
        if (isGreenDroidSupported()) {	
            prepareActionGrid();
			getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	                showContextMenu(parent, view, position, id);
				}
			});
        } else {
        	registerForContextMenu(getListView());
        }
        
	}

	//protected abstract boolean showContextMenu(AdapterView<?> parent, View view,final int pos, final long id);
	
	public boolean showContextMenu(AdapterView<?> parent, View view,final int pos, final long id) {
        selectedId = id;
        actionGrid.show(view);
		return true;
	}
		
    public void recreateAdapter() {           		
			getListView().setEmptyView(getView().findViewById(R.id.emptyView));	 	
			adapter = createAdapter(cursor);
	        setListAdapter(adapter);			
 
    }

    protected abstract void prepareActionGrid();
    
    protected abstract Cursor createCursor();

	protected abstract ListAdapter createAdapter(Cursor cursor);
	
	@Override
	public void onDestroy() {
		if (db!=null) {
			db.close();
		}
		super.onDestroy();
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
        new IntegrityCheckTask(this.getActivity()).execute();
    }

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == MainActivity.RESULT_OK) {
			recreateCursor();
		}
	}
    
    public static String getAppVersion(Context context) {
        try {
            PackageInfo info = Utils.getPackageInfo(context);
            return "v. "+info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

	protected String getMyTitle() {
		// TODO Auto-generated method stub
		return null;
	}

	
}
