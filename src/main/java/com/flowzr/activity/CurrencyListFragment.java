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
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;

import com.flowzr.R;
import com.flowzr.adapter.CurrencyListAdapter;
import com.flowzr.model.Currency;

// @TODO: contextual menus (set default currency)
public class CurrencyListFragment extends AbstractListFragment {

    private static final int MENU_MAKE_DEFAULT = 1000;

    public CurrencyListFragment() {
		super(R.layout.currency_list);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();		
		inflater.inflate(R.menu.currencies_actions, menu);    
		super.onCreateOptionsMenu(menu, inflater);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.action_add_currency: 
	            addItem();
	            return true;
	        case R.id.action_exchange_rates:
				Bundle bundle = new Bundle();
				bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA, ExchangeRatesListFragment.class.getCanonicalName());
				activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
	            return true;	            
			default:
	            return super.onOptionsItemSelected(item);	            
	    }
	}
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);
        switch (item.getItemId()) {
            case MENU_MAKE_DEFAULT: {
                AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
                makeCurrencyDefault(mi.id);
                return true;
            }
        }
        return false;
    }

    private void makeCurrencyDefault(long id) {
        Currency c = em.get(Currency.class, id);
        c.isDefault = true;
        em.saveOrUpdate(c);
        recreateCursor();
    }

    protected String getEditActivityClass() {
        return CurrencyActivity.class.getCanonicalName();
    }

    @Override
	protected void addItem() {
        new CurrencySelector(this.getActivity(), em, new CurrencySelector.OnCurrencyCreatedListener() {
            @Override
            public void onCreated(long currencyId) {
                if (currencyId == 0) {
                    Bundle bundle = new Bundle();
                    bundle.putLong(MyFragmentAPI.ENTITY_ID_EXTRA, currencyId);
                    bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA, getEditActivityClass());
                    activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
                } else {
                    recreateCursor();
                }
            }
        }).show();
	}

	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new CurrencyListAdapter(db, this.getActivity(), cursor);
	}

	@Override
	protected void internalOnCreate(Bundle savedInstanceState) {

	}

	@Override
	protected Cursor createCursor() {
		return em.getAllCurrencies("name");
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == MainActivity.RESULT_OK) {
			cursor.requery();
		}
	}

	@Override
	protected void deleteItem(View v, int position, long id) {
		if (em.deleteCurrency(id) == 1) {
			cursor.requery();
		} else {
			new AlertDialog.Builder(this.getActivity())
				.setTitle(R.string.delete)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setMessage(R.string.currency_delete_alert)
				.setNeutralButton(R.string.ok, null).show();
		}
	}

	@Override
	public void editItem(View v, int position, long id) {
        Bundle bundle = new Bundle();
        bundle.putLong(MyFragmentAPI.ENTITY_ID_EXTRA, id);
        bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA, getEditActivityClass());
        activity.onFragmentMessage(MyFragmentAPI.EDIT_ENTITY_REQUEST,bundle);
	}
	
	@Override
	protected void viewItem(View v, int position, long id) {
		editItem(v, position, id);
	}		

	@Override
	protected String getMyTitle() {
		return getString(R.string.currencies);
	}
}

