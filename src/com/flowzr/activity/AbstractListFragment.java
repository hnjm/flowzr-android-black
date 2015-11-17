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


import greendroid.widget.QuickActionGrid;
import greendroid.widget.QuickActionWidget;
import android.os.Parcelable;
import android.support.v4.app.ListFragment;
import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.MyEntityManager;
import android.app.Activity;
import android.content.Intent;
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
import static com.flowzr.utils.AndroidUtils.isGreenDroidSupported;

public abstract class AbstractListFragment extends ListFragment implements RefreshSupportedActivity {
	
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
	protected ImageButton bAdd;
    protected QuickActionWidget actionGrid;
    protected long selectedId = -1;	
	
	
	protected AbstractListFragment(int contentId) {
		this.contentId = contentId;				
	}
	
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	setHasOptionsMenu(true);
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
    }	
    
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	return inflater.inflate(contentId, container, false);
	}
	
	
	

	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
	    getActivity().setTitle(getMyTitle());
		getListView().setEmptyView(getActivity().findViewById(R.id.emptyView));	    		
        recreateAdapter();	
        if (isGreenDroidSupported()) {	
            prepareActionGrid();    	
        	getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
	            @Override
	            public boolean onItemLongClick(AdapterView<?> parent, View view,final int pos,
	                    final long id) {
	                showContextMenu(parent, view, pos, id);
	                return true;
	            }
	        });
        } else {
        	registerForContextMenu(getListView());
        }
        
	}

	//protected abstract boolean showContextMenu(AdapterView<?> parent, View view,final int pos, final long id);
	
	protected boolean showContextMenu(AdapterView<?> parent, View view,final int pos, final long id) {
        selectedId = id;
        if (actionGrid!=null) {
        	actionGrid.show(view);
        } // ex: myEntityList has no context menu
		return true;
	}
	
	
    protected void recreateAdapter() {
	        adapter = createAdapter(cursor);
	        setListAdapter(adapter);
    }
  
    
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
	
	protected void prepareActionGrid() {
        actionGrid = new QuickActionGrid(this.getActivity());
        actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_discard , R.string.delete)); 	//0
        actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_edit, R.string.edit));			//1
        actionGrid.addQuickAction(new MyQuickAction(this.getActivity(), R.drawable.ic_action_about, R.string.view));		//2   
        actionGrid.setOnQuickActionClickListener(accountActionListener);
	}	
    
	private QuickActionWidget.OnQuickActionClickListener accountActionListener = new QuickActionWidget.OnQuickActionClickListener() {
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
		Log.e("flowzr", "abstract list fragment in activity result");
		if (resultCode == MainActivity.RESULT_OK) {
			recreateCursor();
		}
	}

	protected abstract String getMyTitle() ;

}
