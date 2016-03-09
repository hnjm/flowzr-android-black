/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.flowzr.activity;

import android.os.Bundle;
import android.view.View;

import com.flowzr.R;
import com.flowzr.model.MultiChoiceItem;
import com.flowzr.model.Total;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 3/15/12 16:40 PM
 */
public class AccountListTotalsDetailsActivity extends AbstractTotalsDetailsFragment  {


    public AccountListTotalsDetailsActivity() {
        super(R.string.account_total_in_currency);
    }


    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
    }


	@Override
	public void onSaveInstanceState(final Bundle outState) {
		// super.onSaveInstanceState(outState);
	}

    protected Total getTotalInHomeCurrency() {
        return db.getAccountsTotalInHomeCurrency();
    }

    protected Total[] getTotals() {
        return db.getAccountsTotal();
    }

	@Override
	public void onSelectedPos(int id, int selectedPos) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSelectedId(int id, long selectedId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSelected(int id, List<? extends MultiChoiceItem> items) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}

}
