/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 *     Abdsandryk Souza - adding 2D chart reports
 *     Emmanuel Florent - port to Android API 11+
 ******************************************************************************/
package com.flowzr.model;



import com.flowzr.R;
import android.support.v4.app.Fragment;

import com.flowzr.activity.CategorySelectorFragment;
import com.flowzr.activity.CurrencyListFragment;
import com.flowzr.activity.ExchangeRatesListFragment;
import com.flowzr.activity.LocationsListFragment;
import com.flowzr.activity.PayeeListFragment;
import com.flowzr.activity.ProjectListFragment;
import com.flowzr.utils.EntityEnum;


public enum EntityType implements EntityEnum {

    CURRENCIES(R.string.currencies, R.drawable.ic_attach_money),
    EXCHANGE_RATES(R.string.exchange_rates, R.drawable.ic_trending_up),
    CATEGORIES(R.string.categories, R.drawable.ic_label),
    PAYEES(R.string.payees, R.drawable.ic_person),
    PROJECTS(R.string.projects, R.drawable.ic_star_border),
    LOCATIONS(R.string.locations, R.drawable.ic_my_location);

    public final int titleId;
    public final int iconId;

    EntityType(int titleId, int iconId) {
        this.titleId = titleId;
        this.iconId = iconId;
    }

    @Override
    public int getTitleId() {
        return titleId;
    }

    @Override
    public int getIconId() {
        return iconId;
    }

    public Fragment getActivityClass() {
        switch (this.titleId) {
            case R.string.currencies:
                return new CurrencyListFragment();
            case R.string.exchange_rates:
                return new ExchangeRatesListFragment();
            case R.string.categories:
                return new CategorySelectorFragment();
            case R.string.payees:
                return new PayeeListFragment();
            case R.string.projects:
                return new ProjectListFragment();
            case R.string.locations:
                return new LocationsListFragment();
        }
        return null;
    }

}
