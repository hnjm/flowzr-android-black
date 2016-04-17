/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *      Emmanuel Florent - port to Android API 11+
 ******************************************************************************/
package com.flowzr.activity;

import android.view.View;

import com.flowzr.R;
import com.flowzr.model.Payee;

public class
PayeeActivity extends MyEntityActivity<Payee> {

    public PayeeActivity() {
        super(Payee.class);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.entity_edit;
    }

    @Override
	protected void onClick(View v, int id) {
		// Auto-generated method stub
		
	}

}
