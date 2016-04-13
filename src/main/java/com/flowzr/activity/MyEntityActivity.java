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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.DatabaseHelper;
import com.flowzr.db.MyEntityManager;
import com.flowzr.model.MyEntity;

public abstract class
MyEntityActivity<T extends MyEntity> extends AbstractEditorActivity {
	

    private final Class<T> clazz;

	private DatabaseAdapter db;	
	private MyEntityManager em;

	private T entity;


	 @Override
	 public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	 setHasOptionsMenu(true);
	 return inflater.inflate(getLayoutId(), container, false);
	 }


    protected MyEntityActivity(Class<T> clazz) {
        try {
            this.clazz = clazz;
            this.entity = clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


	public boolean finishAndClose(int result) {
		Bundle bundle = new  Bundle();
		bundle.putInt(MyFragmentAPI.RESULT_EXTRA,result);
		activity.onFragmentMessage(MyFragmentAPI.REQUEST_MYENTITY_FINISH,bundle);
		return true;
	}

	public boolean finishAndClose(Bundle bundle) {
		activity.onFragmentMessage(MyFragmentAPI.REQUEST_MYENTITY_FINISH,bundle);
		return true;
	}

    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		db = new DatabaseAdapter(getContext());
		db.open();
		em = db.em();

        long id = getArguments().getLong(MyFragmentAPI.ENTITY_ID_EXTRA, -1);
        if (id != -1) {
            entity = em.load(clazz, id);
            editEntity();
        }
		
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {	 
        	case R.id.action_done:
				EditText title = (EditText)getView().findViewById(R.id.title);
				entity.title = title.getText().toString();
				long id = em.saveOrUpdate(entity);
				Intent intent = new Intent();
				intent.putExtra(DatabaseHelper.EntityColumns.ID, id);
				finishAndClose(intent.getExtras());
        		return true;
	    	case R.id.action_cancel:
				finishAndClose(AppCompatActivity.RESULT_CANCELED);
	    		return true;        
        }
        return super.onOptionsItemSelected(item);
    }
    
	private void editEntity() {
		EditText title = (EditText)getView().findViewById(R.id.title);
		title.setText(entity.title);
        getActivity().setTitle(entity.title);
	}

	@Override
	public void onDestroy() {
		if (db!=null) {
			db.close();
		}

		super.onDestroy();
	}
	
}
