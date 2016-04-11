/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk Souza - implementing 2D chart reports
 *     Emmanuel Florent - port to Android API 11+
 ******************************************************************************/
package com.flowzr.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.flowzr.R;

public class EntityListActivity extends AbstractActionBarActivity {




	public void setMyTitle(String t) {
		SpannableString s = new SpannableString(t);
		s.setSpan(new TypefaceSpan("sans-serif"), 0, s.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		if (getSupportActionBar()!=null) {
			getSupportActionBar().setTitle(s);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
		fragment.onActivityResult(requestCode, resultCode, data);
		setResult(RESULT_OK);
	}




	protected void initToolbar() {
		final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Toast.makeText(this, R.string.integrity_fix_in_progress, Toast.LENGTH_SHORT).show();
	    Intent intent=getIntent();
        setContentView(R.layout.main);
		initToolbar();
		setupDrawer();

	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            {
                if (mDrawerLayout!=null) {
                    if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                        mDrawerLayout.closeDrawers();
                    } else {
                        mDrawerLayout.openDrawer(GravityCompat.START);
                    }
                }
                return true;
            }

		}
		return super.onOptionsItemSelected(item);
	}

    @Override
    public void onBackPressed() {
        if (mDrawerLayout!=null) {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawers();
                return;
            }
        }
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (f instanceof ReportFragment) {
            if (((ReportFragment) f).viewingPieChart) {
                ((ReportFragment) f).selectReport();
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

}
