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

import com.flowzr.activity.CategoryListFragment;
import com.flowzr.activity.CurrencyListFragment;
import com.flowzr.activity.ExchangeRatesListFragment;
import com.flowzr.activity.LocationsListFragment;
import com.flowzr.activity.PayeeListFragment;
import com.flowzr.activity.ProjectListFragment;
import com.flowzr.utils.EntityEnum;


public enum EntityType implements EntityEnum {

    CURRENCIES(R.string.currencies, R.drawable.ic_attach_money, new CurrencyListFragment()),
    EXCHANGE_RATES(R.string.exchange_rates, R.drawable.ic_trending_up, new ExchangeRatesListFragment()),
    CATEGORIES(R.string.categories, R.drawable.ic_label, new CategoryListFragment()),
    PAYEES(R.string.payees, R.drawable.ic_person, new PayeeListFragment()),
    PROJECTS(R.string.projects, R.drawable.ic_star_border, new ProjectListFragment()),
    LOCATIONS(R.string.locations, R.drawable.ic_my_location, new LocationsListFragment());

    public final int titleId;
    public final int iconId;
    public final Fragment actitivyClass;

    EntityType(int titleId, int iconId, Fragment activityClass) {
        this.titleId = titleId;
        this.iconId = iconId;
        this.actitivyClass = activityClass;
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
        return actitivyClass;
    }


    
}
