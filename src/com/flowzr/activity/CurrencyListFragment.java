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

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import com.flowzr.R;
import com.flowzr.adapter.CurrencyListAdapter;
import com.flowzr.model.Currency;
import com.flowzr.model.EntityType;
import com.flowzr.utils.MenuItemInfo;

import java.util.List;

/**
 * @TODO: contextual menus (set default currency)
 * @author eflorent
 *
 */
public class CurrencyListFragment extends AbstractListFragment {
	
	private static final int NEW_CURRENCY_REQUEST = 1;
	private static final int EDIT_CURRENCY_REQUEST = 2;
    private static final int MENU_MAKE_DEFAULT = MENU_ADD + 1;

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
	            ((EntityListActivity) getActivity()).loadFragment(new ExchangeRatesListFragment());   
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

    @Override
	protected void addItem() {
        new CurrencySelector(this.getActivity(), em, new CurrencySelector.OnCurrencyCreatedListener() {
            @Override
            public void onCreated(long currencyId) {
                if (currencyId == 0) {
                    Intent intent = new Intent(CurrencyListFragment.this.getActivity(), CurrencyActivity.class);
                    startActivityForResult(intent, NEW_CURRENCY_REQUEST);
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
		Intent intent = new Intent(this.getActivity(), CurrencyActivity.class);
		intent.putExtra(CurrencyActivity.CURRENCY_ID_EXTRA, id);
		startActivityForResult(intent, EDIT_CURRENCY_REQUEST);		
	}	
	
	@Override
	protected void viewItem(View v, int position, long id) {
		editItem(v, position, id);
	}		

	@Override
	protected String getMyTitle() {
		return getString(R.string.currency);
	}
}

