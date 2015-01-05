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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.DatabaseHelper;
import com.flowzr.db.MyEntityManager;
import com.flowzr.model.MyEntity;
import com.flowzr.utils.PinProtection;

public abstract class MyEntityActivity<T extends MyEntity> extends AbstractEditorActivity {
	
	public static final String ENTITY_ID_EXTRA = "entityId";

    private final Class<T> clazz;

	private DatabaseAdapter db;	
	private MyEntityManager em;

	private T entity;

    protected MyEntityActivity(Class<T> clazz) {
        try {
            this.clazz = clazz;
            this.entity = clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.project);

		db = new DatabaseAdapter(this);
		db.open();
		
		em = db.em();
		
		Intent intent = getIntent();
		if (intent != null) {
			long id = intent.getLongExtra(ENTITY_ID_EXTRA, -1);
			if (id != -1) {
				entity = em.load(clazz, id);
				editEntity();
			}
		}
		
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {	 
        	case R.id.action_done:
				EditText title = (EditText)findViewById(R.id.title);
				entity.title = title.getText().toString();
				long id = em.saveOrUpdate(entity);
				Intent intent = new Intent();
				intent.putExtra(DatabaseHelper.EntityColumns.ID, id);
				setResult(RESULT_OK, intent);
				finish();   	        		
        		return true;
	    	case R.id.action_cancel:
				setResult(RESULT_CANCELED);
				finish();
	    		return true;        
        }
        return super.onOptionsItemSelected(item);
    }
    
	private void editEntity() {
		EditText title = (EditText)findViewById(R.id.title);
		title.setText(entity.title);
	}

	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();
	}
	
}
