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
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.flowzr.R;
import com.flowzr.adapter.LocationListAdapter;
import com.flowzr.blotter.BlotterFilter;
import com.flowzr.filter.Criteria;
import com.flowzr.model.MyLocation;
import com.flowzr.orb.EntityManager;
import com.flowzr.utils.AddressGeocoder;

public class LocationsListFragment extends AbstractListFragment {
	
	private static final int MENU_RESOLVE = MENU_ADD+1;
	private static final int NEW_LOCATION_REQUEST = 1;
	private static final int EDIT_LOCATION_REQUEST = 2;
	
	public LocationsListFragment() {
		super(R.layout.location_list);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();		
		inflater.inflate(R.menu.add, menu);    
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	protected String getEditActivityClass() {
		return LocationActivity.class.getCanonicalName(); //.replace("class ","").trim();
	}

	@Override
	public void onAttach(Context a) {
		super.onAttach(a);
		setHasOptionsMenu(true);
		activity=(MainActivity)a;
	}
    
	@Override
	protected ListAdapter createAdapter(Cursor cursor) {
		return new LocationListAdapter(db, this.getActivity(), cursor);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (super.onContextItemSelected(item)) {
			return true;
		}
		if (item.getItemId() == MENU_RESOLVE) {
			AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			resolveAddress(mi.position, mi.id);
		}
		return false;
	}

	private void resolveAddress(int position, long id) {
		cursor.moveToPosition(position);
		MyLocation location = EntityManager.loadFromCursor(cursor, MyLocation.class);
		startGeocode(location);
	}

    @Override
    protected void internalOnCreate(Bundle savedInstanceState) {

    }

    @Override
	protected Cursor createCursor() {
		return em.getAllLocations(false);
	}
	
	@Override
	protected void addItem() {
		Intent intent = new Intent(this.getActivity(), LocationActivity.class);
		startActivityForResult(intent, NEW_LOCATION_REQUEST);
	}

	@Override
	protected void deleteItem(View v, int position, long id) {
		em.deleteLocation(id);
		cursor.requery();
	}

	@Override
	public void editItem(View v, int position, long id) {
		Intent intent = new Intent(this.getActivity(), LocationActivity.class);
		intent.putExtra(LocationActivity.LOCATION_ID_EXTRA, id);
		startActivityForResult(intent, EDIT_LOCATION_REQUEST);
	}

	@Override
	protected String getMyTitle() {
		return getString(R.string.locations);
	}

	@Override
	protected void viewItem(View v, int position, long id) {
		MyLocation e = em.load(MyLocation.class, id);
		Criteria blotterFilter = Criteria.eq(BlotterFilter.LOCATION_ID, String.valueOf(e.id));
		Bundle bundle = new Bundle();
		blotterFilter.toBundle(e.name,bundle);
		bundle.putString(MyFragmentAPI.ENTITY_CLASS_EXTRA, BlotterFragment.class.getCanonicalName());
		activity.onFragmentMessage(MyFragmentAPI.REQUEST_BLOTTER,bundle);
	}



	private void startGeocode(MyLocation location) {
        new GeocoderTask(location).execute(location.latitude, location.longitude);
    }

    private class GeocoderTask extends AsyncTask<Double, Void, String> {
        
    	private final AddressGeocoder geocoder;
        private final MyLocation location;

        private GeocoderTask(MyLocation location) {
        	this.geocoder = new AddressGeocoder(LocationsListFragment.this.getActivity());
            this.location = location;
        }

        @Override
        protected void onPreExecute() {
        	Log.d("Geocoder", "About to enter from onPreExecute");
            // Show progress spinner and disable buttons
            //setProgressBarIndeterminateVisibility(true);
            //setActionEnabled(false);
            Log.d("Geocoder", "About to exit from onPreExecute");
        }

        @Override
        protected String doInBackground(Double... args) {
        	Log.d("Geocoder", "About to enter from doInBackground");
            // Reverse geocode using location
            return geocoder.resolveAddressFromLocation(args[0], args[1]);
        }

        @Override
        protected void onPostExecute(String found) {
        	Log.d("Geocoder", "About to enter from onPostExecute");
            //setProgressBarIndeterminateVisibility(false);
            // Update GUI with resolved string
            if (found != null) {
				Toast t = Toast.makeText(LocationsListFragment.this.getActivity(), found, Toast.LENGTH_LONG);
				t.show();
				location.resolvedAddress = found;
				em.saveLocation(location);
				recreateCursor();
            } else if (geocoder.lastException != null) {
				Toast t = Toast.makeText(LocationsListFragment.this.getActivity(), R.string.service_is_not_available, Toast.LENGTH_LONG);
				t.show();            	
            }
            Log.d("Geocoder", "About to exit from onPostExecute");
        }
    }
}
