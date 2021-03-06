/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package com.flowzr.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.flowzr.R;
import com.flowzr.utils.MyPreferences;
import com.flowzr.utils.PinProtection;
import com.flowzr.view.PinView;

public class PinActivity extends AppCompatActivity implements PinView.PinListener {
	
	public static final String SUCCESS = "PIN_SUCCESS";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		String pin = MyPreferences.getPin(this);
		if (pin == null) {
			onSuccess(null);
		} else {
			PinView v = new PinView(this, this, pin, R.layout.lock);
			setContentView(v.getView());
		}
	}

	@Override
	public void onConfirm(String pinBase64) {		
	}

	@Override
	public void onSuccess(String pinBase64) {
        PinProtection.pinUnlock(this);
		Intent data = new Intent();
		data.putExtra(SUCCESS, true);
		setResult(RESULT_OK, data);
		finish();
	}

	@Override
	public void onBackPressed() {
        moveTaskToBack(true);
	}

}
