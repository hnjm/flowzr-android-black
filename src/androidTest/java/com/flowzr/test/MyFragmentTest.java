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
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;

import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.pressBack;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withText;


public class MyFragmentTest {

    @Before
    public void setUp() {
        try {
            //  skip splash if any not to make the test flaky
            onView(withText(com.flowzr.R.string.whats_new)).perform(pressBack());
        } catch (Exception e) {
            // this is not the first time test used (splash is skipped)
        }
    }

    protected static ViewInteraction matchToolbarTitle(Integer titleId) {
        return onView(isAssignableFrom(Toolbar.class))
                .check(matches(withToolbarTitle(titleId)));
    }

    protected static ViewInteraction matchToolbarTitle(String title) {
        return onView(isAssignableFrom(Toolbar.class))
                .check(matches(withToolbarTitle(title)));
    }

    public static Matcher<Object> withToolbarTitle(@StringRes final String strTitle) {

        return new BoundedMatcher<Object, Toolbar>(Toolbar.class) {
            private String resourceName = null;
            private String expectedText = null;

            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("with title : ");
                description.appendValue(strTitle);
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
                        expectedText = strTitle;
                        resourceName = strTitle;
                    } catch (Resources.NotFoundException ignored) {
                        // view could be from a context unaware of the resource id
                    }
                }
                return null != expectedText && toolbar.getTitle().toString().startsWith(expectedText);
            }
        };
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
