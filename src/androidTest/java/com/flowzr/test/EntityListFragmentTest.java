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


import android.content.res.Resources;
import android.support.annotation.StringRes;
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
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.flowzr.R;
import com.flowzr.activity.MainActivity;

import com.flowzr.model.EntityType;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Entity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withTagKey;
import static org.hamcrest.Matchers.allOf;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class EntityListFragmentTest extends MyFragmentTest {

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
        matchToolbarTitle(EntityType.CURRENCIES.titleId);
        pressBack();

        for (int i=0;i<entities.length;i++) {
            onView(allOf(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
                    isAssignableFrom(AppCompatTextView.class),
                    ViewMatchers.withText(EntityType.values()[i].titleId))).perform(click());

                //onView(Matchers.allOf(ViewMatchers.withText(EntityType.values()[i].titleId))).perform(click());
                //if (reports[i].isConventionalBarReport()) {

                //}
                matchToolbarTitle(EntityType.values()[i].titleId);
                pressBack();
        }
    }


    private static ViewInteraction matchToolbarTitle(Integer titleId) {
        return onView(isAssignableFrom(Toolbar.class))
                .check(matches(withToolbarTitle(titleId)));
    }




    public static Matcher<Object> withToolbarTitle(@StringRes final int resourceId) {

        return new BoundedMatcher<Object, Toolbar>(Toolbar.class) {
            private String resourceName = null;
            private String expectedText = null;

            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("with title resource id: ");
                description.appendValue(resourceId);
                if (null != resourceName) {
                    description.appendText("[");
                    description.appendText(resourceName);
                    description.appendText("]");
                }
                if (null != expectedText) {
                    description.appendText(" value: ");
                    description.appendText(expectedText);
                }
            }

            @Override
            public boolean matchesSafely(final Toolbar toolbar) {
                if (null == expectedText) {
                    try {
                        expectedText = toolbar.getResources().getString(resourceId);
                        resourceName = toolbar.getResources().getResourceEntryName(resourceId);
                    } catch (Resources.NotFoundException ignored) {
                        // view could be from a context unaware of the resource id
                    }
                }
                return null != expectedText && toolbar.getTitle().toString().startsWith(expectedText);
            }
        };
    }

    public static Matcher<View> withListSize(final int size) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText ("ListView should have " + size + " items");
            }

            @Override
            public boolean matchesSafely(final View view) {
                return ((ListView) view).getChildCount() == size;
            }


        };
    }

}