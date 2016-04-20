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


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.MyEntityManager;
import com.flowzr.utils.IntegrityFix;


public abstract class AbstractTotalListFragment extends ListFragment implements RefreshSupportedActivity {

	//protected static final int MENU_ADD = Menu.FIRST+4;
	protected static final int NEW_ACCOUNT_REQUEST = 100;
	public static final int EDIT_ACCOUNT_REQUEST = 101;
	protected static final int VIEW_ACCOUNT_REQUEST = 102;
	protected static final int PURGE_ACCOUNT_REQUEST = 103;

	protected static final int NEW_TRANSACTION_REQUEST = 104;
	protected static final int NEW_TRANSFER_REQUEST = 105;
	protected static final int NEW_TRANSACTION_FROM_TEMPLATE_REQUEST = 106;
	protected static final int MONTHLY_VIEW_REQUEST = 107;
	protected static final int BILL_PREVIEW_REQUEST = 108;

	protected static final int FILTER_REQUEST = 109;
	protected static final int MENU_DUPLICATE = 110;
	protected static final int MENU_SAVE_AS_TEMPLATE = 111;

	protected static final int NEW_BUDGET_REQUEST = 112;
	protected static final int EDIT_BUDGET_REQUEST = 113;
	protected static final int FILTER_BUDGET_REQUEST = 114;
	//protected static final int VIEW_BUDGET_REQUEST = 115;

	protected static final String EXTRA_LAYOUT="EXTRA_LAYOUT";
	public static final String EXTRA_REQUEST_TYPE="EXTRA_REQUEST_TYPE"; 
	protected int contentId;
		
    protected LayoutInflater inflater;
	protected Cursor cursor;
	protected ListAdapter adapter;
	protected DatabaseAdapter db;
	protected MyEntityManager em;
	protected MenuItem bAdd;
    protected long selectedId = -1;

	protected MainActivity activity;

	protected AbstractTotalListFragment(int contentId) {
		this.contentId = contentId;				
	}

    public void onAttach(Context a) {
        super.onAttach(a);
        setHasOptionsMenu(true);
        activity=(MainActivity)a;
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
		registerForContextMenu(getListView());
	}


    public void recreateAdapter() {           		
			getListView().setEmptyView(getView().findViewById(R.id.emptyView));	 	
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

	private boolean clickable = true;

    @Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (clickable) {
			clickable=false;
			viewItem(v, position, id);
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					clickable=true;
				}
			}, 2000);
		}
	}

    protected void addItem() {
	}

	protected abstract void deleteItem(int position, long id);

	protected abstract void editItem(long id);

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
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(this.isAdded()) {
			recreateCursor();
		}
	}


	protected abstract String getMyTitle() ;


    @Override
    public void integrityCheck() {
        new IntegrityFixTask().execute();
        recreateAdapter();
        recreateCursor();
    }


    private class IntegrityFixTask extends AsyncTask<Void, Void, Void> {

		ProgressDialog progressDialog;

		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(getActivity(), null, getString(R.string.integrity_fix_in_progress), true);
			progressDialog.show();
		}

		@Override
		protected void onPostExecute(Void o) {
			((MainActivity)getActivity()).refreshCurrentTab();
			progressDialog.dismiss();
		}

		@Override
		protected Void doInBackground(Void... objects) {
			DatabaseAdapter db = new DatabaseAdapter(AbstractTotalListFragment.this.getActivity());
			new IntegrityFix(db).fix();
			return null;
		}
	}
}
