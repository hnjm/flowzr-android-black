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
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import com.flowzr.R;
import com.flowzr.utils.Utils;

public class AboutActivity extends AbstractActionBarActivity  {

	ViewPager viewPager;
	public MyAdapter mAdapter;

	@Override
	public void onBackPressed() {
		finish();
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
            {
                finish();
                return true;
            }

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

	@Override
    protected void onCreate(Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);    
		setContentView(R.layout.main);
		initToolbar();
        setTitle(getResources().getString(R.string.app_name) + " (" + Utils.getAppVersion(this) + ")");
		viewPager = (ViewPager) findViewById(R.id.pager);
		mAdapter = new MyAdapter(getSupportFragmentManager(),new Intent());
		viewPager.setAdapter(mAdapter);
    }


 	public static class MyAdapter extends FragmentStatePagerAdapter {
  		Bundle bundle;
  	    Fragment fragment=null;

		@Override
		public CharSequence getPageTitle(int position) {


                switch (position) {
                    case 0:
						return "About";
                    case 1:
						return "What's new";
                    case 2:
                        return "License";
					default:
						return "About";
            }
		}

		public MyAdapter(FragmentManager fm, Intent i) {
  	        super(fm);

  	        bundle=i.getExtras();
  	        bundle=new Bundle();  	       
  	    }

  	    public void setFilter(Bundle b) {
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
                fragment=frag1;
  	          return frag1;
  	    	}
  	    	if (position==1) {  	    
  	  	        WebViewFragment frag2= new WebViewFragment();
  	  	        frag2.init("file:///android_asset/whatsnew.htm");
                fragment=frag2;
   	    		return frag2;
  	    	}
  	    	if (position==2) {  	    		
  	  	        WebViewFragment frag4= new WebViewFragment();
  	  	        frag4.init("file:///android_asset/gpl-2.0-standalone.htm");
                fragment=frag4;
  	    		return frag4;
  	    	}
  	    	return null;
  	    }
  	}

}
