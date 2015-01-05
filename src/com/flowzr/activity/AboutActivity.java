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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import com.flowzr.R;
import com.flowzr.utils.Utils;

public class AboutActivity extends ActionBarActivity implements ViewPager.OnPageChangeListener, ActionBar.TabListener {

	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
	private Tab aboutTab;
	private Tab whatsnewTab;
	private Tab donateTab;
	private Tab licenseTab;
    
	ViewPager viewPager;

	public MyAdapter mAdapter;
	private ActionBar actionBar;
	
	@Override
    protected void onCreate(Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);    
		setContentView(R.layout.about);  
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);                
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        setTitle(getResources().getString(R.string.app_name) + " ("+Utils.getAppVersion(this)+")");
		viewPager = (ViewPager) findViewById(R.id.pager);	
		aboutTab = setupTab("about", R.string.about);
		whatsnewTab = setupTab("whatsnew", R.string.whats_new);
		donateTab =  setupTab("donate", R.string.donate);
		licenseTab = setupTab("gpl-2.0-standalone", R.string.license);
      
        aboutTab.setTabListener(this);
		whatsnewTab.setTabListener(this);		
		donateTab.setTabListener(this);
		licenseTab.setTabListener(this);	

		addMyTabs();
		
        Intent intent= getIntent();
		
		mAdapter = new MyAdapter(getSupportFragmentManager(),intent);

		viewPager.setPageTransformer(true, new ZoomOutPageTransformer());
		viewPager.setAdapter(mAdapter);
		viewPager.setOnPageChangeListener(this);
    }

	private void addMyTabs() {
		actionBar.addTab(aboutTab);
		actionBar.addTab(whatsnewTab);
		//actionBar.addTab(donateTab);
		actionBar.addTab(licenseTab);
	}

	private Tab setupTab(String name, int titleId) {
		Intent intent = new Intent(this, WebViewActivity.class);
	    intent.putExtra(WebViewActivity.FILENAME, name);
		Tab aTab = actionBar.newTab(); 
		aTab.setText(getString(titleId));
		return aTab;
    }

	@Override
	public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTabSelected(Tab arg0, FragmentTransaction arg1) {
		if (viewPager!=null) {
			viewPager.setCurrentItem(arg0.getPosition());			
		}
	}

	@Override
	public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPageSelected(int arg0) {
		actionBar.setSelectedNavigationItem(arg0);
		
	}
	
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
            getSupportActionBar().setSelectedNavigationItem(savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
        }
    }
 
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getSupportActionBar().getSelectedNavigationIndex());
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
                        finish();
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
    
 	public static class MyAdapter extends FragmentStatePagerAdapter {
  		Bundle bundle;
  	    Fragment fragment=null;

		public MyAdapter(FragmentManager fm, Intent i) {
  	        super(fm);
  	        bundle=i.getExtras();
  	        bundle=new Bundle();  	       
  	    }

  	    public void setFilter(Bundle b) {
  	    	bundle=b;
  	    }
  	    
  	    public void setMyArguments(Fragment f, Bundle b) {
  	    	fragment=f;
			bundle=b;	
		}

		@Override
  	    public int getCount() {
  	        return 3;
  	    }
  	    
  	   @Override
  	    public int getItemPosition(Object object){
  		   return POSITION_NONE;
  	    }
  	    
  	   @Override
  	    public Fragment getItem(int position) {
		   	
  	    	if (position==0) {
  	          WebViewFragment frag1= new WebViewFragment();
  	          frag1.init("file:///android_asset/about.htm");
  	          return frag1;
  	    	}
  	    	if (position==1) {  	    
  	  	        WebViewFragment frag2= new WebViewFragment();
  	  	        frag2.init("file:///android_asset/whatsnew.htm"); 	    		
   	    		return frag2;
  	    	}
//  	    	if (position==2) {
//  	  	        WebViewFragment frag3= new WebViewFragment();
//  	  	        frag3.init("file:///android_asset/donate.htm");
//  	    		return frag3;
//  	    	}
  	    	if (position==2) {  	    		
  	  	        WebViewFragment frag4= new WebViewFragment();
  	  	        frag4.init("file:///android_asset/gpl-2.0-standalone.htm");   	    		
  	    		return frag4;
  	    	}
  	    	return null;
  	    }
  	}

}
