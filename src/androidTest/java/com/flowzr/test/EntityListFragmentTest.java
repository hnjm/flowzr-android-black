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
import android.content.res.Resources;
import android.support.annotation.StringRes;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.test.InstrumentationTestRunner;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.flowzr.R;
import com.flowzr.activity.MainActivity;

import com.flowzr.adapter.EntityEnumAdapter;
import com.flowzr.adapter.EntityListAdapter;
import com.flowzr.adapter.MyEntityAdapter;
import com.flowzr.db.DatabaseAdapter;
import com.flowzr.db.DatabaseHelper;
import com.flowzr.db.MyEntityManager;
import com.flowzr.model.EntityType;
import com.flowzr.model.MyEntity;
import com.flowzr.model.Payee;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import javax.persistence.Entity;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.core.deps.guava.base.Predicates.instanceOf;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withTagKey;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.Is.is;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class EntityListFragmentTest extends AbstractDBTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    @Test
    public void openEntityList () {
        onView(withId(R.id.drawer_V)).perform(DrawerActions.open());
        onView(Matchers.allOf(ViewMatchers.withText(R.string.entities))).perform(click());
    }

    @Test
    public void testEntityListNoFilter() {
        openEntityList();
        // http://stackoverflow.com/questions/20807131/espresso-return-boolean-if-view-exists
        try {
            onView(withId(R.id.action_filter)).check(matches(isDisplayed()));
            //view is displayed logic
            onView(withId(-1)).check(matches(isDisplayed()));
        } catch (NoMatchingViewException e) {
            // test pass
        }
        try {
            onView(withId(R.id.action_add)).check(matches(isDisplayed()));
            //view is displayed logic
            onView(withId(-1)).check(matches(isDisplayed()));
        } catch (NoMatchingViewException e) {
            // test pass
        }
    }


    @Test
    public void testEntityListTitleAndSize() {
        openEntityList();
        onView(allOf(withContentDescription(R.string.entities))).check (ViewAssertions.matches (withListSize (EntityType.values().length)));
            matchToolbarTitle(R.string.entities);
    }


    @Test
    public void test1stLvlEntityList() {

        EntityType[] entities = new EntityType[]{
                EntityType.CURRENCIES,
                EntityType.EXCHANGE_RATES,
                EntityType.CATEGORIES,
                EntityType.PAYEES,
                EntityType.PROJECTS,
                EntityType.LOCATIONS
        };

        openEntityList();
        // will fail if drawer have equal number of item :(
        onView(allOf(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE), ViewMatchers.withText(R.string.currencies))).perform(click());
        matchToolbarTitle(EntityType.CURRENCIES.titleId);
        pressBack();
        onView(allOf(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),isAssignableFrom(AppCompatTextView.class), ViewMatchers.withText(R.string.currencies))).perform(click());
        matchToolbarTitle(EntityType.LOCATIONS.titleId);
        pressBack();

        for (int i=0;i<entities.length;i++) {
            onView(allOf(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
                    isAssignableFrom(AppCompatTextView.class),
                    withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
                    //isDisplayed(),
                    isCompletelyDisplayed(),
                    ViewMatchers.withText(EntityType.values()[i].titleId))).perform(click());

            matchToolbarTitle(EntityType.values()[i].titleId);
            //canViewAll();

            pressBack();
        }
    }


    @Test
    public void  canViewAllPayees() {

        openEntityList();
        onView(allOf(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
                ViewMatchers.withText(R.string.payees))).perform(click());

        // grab db & check all
        List<Payee> payees = em.getAllPayeeList();
        for (int i=0;i<payees.size() && i<3 ;i++) {
            Payee payee = payees.get(i);
            onView(allOf(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
                    isAssignableFrom(AppCompatTextView.class),
                    withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
                    isCompletelyDisplayed(),
                    ViewMatchers.withText(payee.getTitle()))).perform(click());
            matchToolbarTitle(payee.getTitle());
        }

    }



}