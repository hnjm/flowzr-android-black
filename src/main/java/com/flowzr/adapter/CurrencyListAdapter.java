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
package com.flowzr.adapter;

import android.content.Context;
import android.database.Cursor;

import com.flowzr.R;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.model.Currency;
import com.flowzr.orb.EntityManager;
import com.flowzr.utils.Utils;

public class CurrencyListAdapter extends AbstractGenericListAdapter {
	
	public CurrencyListAdapter(DatabaseAdapter db, Context context, Cursor c) {
		super(db, context, c);
	}

	@Override
	protected void bindView(GenericViewHolder v, Context context, Cursor cursor) {
		Currency c = EntityManager.loadFromCursor(cursor, Currency.class);
		v.lineView.setText(c.title);
		v.numberView.setText(c.name);
		v.amountView.setText(Utils.amountToString(c, 100000));
		if (c.isDefault) {
			v.iconView.setImageResource(R.drawable.ic_home_currency);
		} else {
			v.iconView.setImageDrawable(null);
		}
	}

}
