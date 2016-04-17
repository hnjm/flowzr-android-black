/*******************************************************************************
 * Copyright (c) 2016 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Emmanuel Florent - initial implementation
 *
 ******************************************************************************/

package com.flowzr.test;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.DatabaseHelper;
import com.flowzr.db.MyEntityManager;

import org.junit.After;
import org.junit.Before;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.pressBack;
import static android.support.test.espresso.matcher.ViewMatchers.withText;


public class AbstractDBTest extends MyFragmentTest {

    protected DatabaseAdapter db;
    protected MyEntityManager em;
    protected DatabaseHelper dbHelper;

    @Before
    public void setUp() {
        super.setUp();
        Context context = InstrumentationRegistry.getTargetContext();
        dbHelper = new DatabaseHelper(context);
        db = new DatabaseAdapter(context, dbHelper);
        db.open();
        em = db.em();
    }

    @After
    public void tearDown() {
        dbHelper.close();

    }

}
