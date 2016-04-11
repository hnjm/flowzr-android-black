/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.flowzr.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;
import android.view.View;

import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.utils.IntegrityCheck;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 8/21/12 10:28 PM
 */
public class IntegrityCheckTask extends AsyncTask<Void, Void, Boolean> {

    private final MainActivity activity;

    public IntegrityCheckTask(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    protected Boolean doInBackground(Void... objects) {
        View textView = getResultView();
        if (textView != null) {
            DatabaseAdapter db = new DatabaseAdapter(activity);
            IntegrityCheck check = new IntegrityCheck(db);
            return check.isBroken();
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        View textView = getResultView();
        if (textView != null) {
            textView.setVisibility(result != null && result ? View.VISIBLE : View.GONE);
        }
    }

    private View getResultView() {
    	if (activity!=null) {
    		try {
    			return activity.findViewById(R.id.integrity_error);
    		} catch (Exception e) {
    			e.printStackTrace();
    			return null;
    		}
    	} else {
    		return null;
    	}
    }

}
