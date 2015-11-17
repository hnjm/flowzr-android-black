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
import android.support.design.internal.NavigationMenuView;
import android.support.design.widget.NavigationView;
import android.support.multidex.MultiDex;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.os.Bundle;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.flowzr.R;
import com.flowzr.activity.MainActivity.MyAdapter;
import com.flowzr.adapter.NavDrawerListAdapter;
import com.flowzr.model.NavDrawerItem;
import com.flowzr.utils.*;
import java.util.ArrayList;

public class AbstractActionBarActivity  extends AppCompatActivity {


    private static final int CHANGE_PREFERENCES = 6;

    private static final int ACTIVITY_BACKUP = 8;    

    private static final int MENU_ACCOUNTS = 0;  
    private static final int MENU_BLOTTER = 1;  
    private static final int MENU_BUDGET = 2;      
    
    private static final int MENU_REPORTS = 3;     
    private static final int MENU_ENTITIES = 4;
    private static final int MENU_CLOUD_SYNC = 5;
    private static final int MENU_BACKUP = 6;
    private static final int MENU_PREFERENCES = 7;    
    private static final int MENU_ABOUT = 8;
    
	public static final int TAB_BLOTTER = 1;
	public static final int TAB_REPORT = 3;

    protected static final String REQUEST_BLOTTER = "REQUEST_BLOTTER";   
	
	public android.support.v7.app.ActionBar actionBar;
	// Within which the entire activity is enclosed
	protected DrawerLayout mDrawerLayout;
	// ListView represents Navigation Drawer
	protected ListView mDrawerList;	
	private ActionBarDrawerToggle mDrawerToggle;

	private TypedArray navMenuIcons;
	private ArrayList<NavDrawerItem> navDrawerItems;
	private NavDrawerListAdapter adapter;    
	protected static ViewPager viewPager;

	public MyAdapter mAdapter;

    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
	
    public void loadTabFragment(Fragment fragment, int rId, Bundle bundle, int tabId) {
        bundle.putInt(AbstractTotalListFragment.EXTRA_LAYOUT, rId);
        fragment.setArguments(bundle);
        mAdapter.setMyArguments(fragment, bundle);
        mAdapter.notifyDataSetChanged();
        try {
            viewPager.getAdapter().notifyDataSetChanged();
        } catch(Exception e) {
            e.printStackTrace();
        }
        viewPager.setCurrentItem(tabId);
    }


	protected void setupDrawer() {
				
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_V);
		mDrawerList = (ListView) findViewById(R.id.navigationDrawer_L);
		// setting list adapter for Navigation Drawer		
		String[] mDrawerStrings = { getResources().getString(R.string.account),
				getResources().getString(R.string.blotter),
				getResources().getString(R.string.budget),
				getResources().getString(R.string.reports),
				getResources().getString(R.string.entities),
				getResources().getString(R.string.flowzr_sync),
				getResources().getString(R.string.backup),
				getResources().getString(R.string.preferences),
				getResources().getString(R.string.about)				
				};
				
		
		navMenuIcons = getResources().obtainTypedArray(R.array.nav_drawer_icons);		
		navDrawerItems = new ArrayList<NavDrawerItem>();
		navDrawerItems.add(new NavDrawerItem(mDrawerStrings[0], navMenuIcons.getResourceId(0, -1)));
		navDrawerItems.add(new NavDrawerItem(mDrawerStrings[1], navMenuIcons.getResourceId(1, -1)));
		navDrawerItems.add(new NavDrawerItem(mDrawerStrings[2], navMenuIcons.getResourceId(2, -1)));
		navDrawerItems.add(new NavDrawerItem(mDrawerStrings[3], navMenuIcons.getResourceId(3, -1)));
		navDrawerItems.add(new NavDrawerItem(mDrawerStrings[4], navMenuIcons.getResourceId(4, -1)));
		navDrawerItems.add(new NavDrawerItem(mDrawerStrings[5], navMenuIcons.getResourceId(5, -1)));
		navDrawerItems.add(new NavDrawerItem(mDrawerStrings[6], navMenuIcons.getResourceId(6, -1)));
		navDrawerItems.add(new NavDrawerItem(mDrawerStrings[7], navMenuIcons.getResourceId(7, -1)));
		navDrawerItems.add(new NavDrawerItem(mDrawerStrings[8], navMenuIcons.getResourceId(8, -1)));		
				
		adapter = new NavDrawerListAdapter(getApplicationContext(),navDrawerItems);
		mDrawerList.setAdapter(adapter);

		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());


        mDrawerToggle = new ActionBarDrawerToggle(this,mDrawerLayout,R.string.menu,R.string.close) {

        };

        if (mDrawerLayout!=null) {
        	mDrawerLayout.setDrawerListener(mDrawerToggle);
            mDrawerToggle.syncState();
            mDrawerLayout.setVerticalScrollBarEnabled(false);
        }
	}
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerLayout!=null) {
        	mDrawerToggle.syncState();
        }
    }


    
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {            	
	    		case MENU_ACCOUNTS:      
	    			viewPager.setCurrentItem(0);
	            break;
	    		case MENU_BLOTTER: 
	    			loadTabFragment(new BlotterFragment(), R.layout.blotter, new Bundle(), 1);
	    			mAdapter.notifyDataSetChanged();
	  
	            break;
	    		case MENU_BUDGET:      
	    			viewPager.setCurrentItem(2);
	            break;            
	            case MENU_REPORTS:      
	        			Intent intent=new Intent(parent.getContext(),EntityListActivity.class);
	        			intent.putExtra(EntityListActivity.REQUEST_REPORTS, true);
	        			startActivity(intent);
	                break;
            	case MENU_ENTITIES:          		
	                startActivity(new Intent(parent.getContext(), EntityListActivity.class));
	                break;
	            case MENU_PREFERENCES:
	                startActivityForResult(new Intent(parent.getContext(), PreferencesActivity.class), CHANGE_PREFERENCES);
	                break;
	            case MENU_ABOUT:
	                startActivity(new Intent(parent.getContext(), AboutActivity.class));
	                break;
	            case MENU_CLOUD_SYNC:
	                startActivity(new Intent(parent.getContext(), FlowzrSyncActivity.class));
	                break;
	            case MENU_BACKUP:
	                startActivityForResult(new Intent(parent.getContext(), BackupListActivity.class), ACTIVITY_BACKUP);
	            	break;
            }
            if (mDrawerLayout!=null) {
            	mDrawerLayout.closeDrawer(mDrawerList);            
            }
		}
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        PinProtection.unlock(this);
        if (mDrawerLayout!=null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        PinProtection.unlock(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PinProtection.lock(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PinProtection.immediateLock(this);
    }

  	public void setMyTitle(String t) {
  	  SpannableString s = new SpannableString(t);
  	  s.setSpan(new TypefaceSpan("sans-serif"), 0, s.length(),
              Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      actionBar.setTitle(s);
  	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
            {
                TaskStackBuilder tsb = TaskStackBuilder.create(this);
                final int intentCount = tsb.getIntentCount();
                if (intentCount > 0)
                {
                    Intent upIntent = tsb.getIntents()[intentCount - 1];
                    if (NavUtils.shouldUpRecreateTask(this, upIntent))
                    {
                        // This activity is not part of the application's task, so create a new task with a synthesized back stack.
                        tsb.startActivities();
                        //finish();
                    }
                    else
                    {
                        // This activity is part of the application's task, so simply navigate up to the hierarchical parent activity.
                        NavUtils.navigateUpTo(this, upIntent);
                    }
                }
                else
                {
                    onBackPressed();
                }
                return true;
            }

        }
        return super.onOptionsItemSelected(item);
    }


}