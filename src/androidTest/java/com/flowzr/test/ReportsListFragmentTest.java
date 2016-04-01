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


import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import android.content.res.Resources;
import android.support.annotation.StringRes;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.v7.widget.Toolbar;
import android.test.suitebuilder.annotation.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.widget.ListView;
import android.widget.TextClock;
import android.widget.TextView;

import com.flowzr.activity.MainActivity;
import com.flowzr.R;
import com.flowzr.activity.ReportFragment;
import com.flowzr.activity.ReportsListFragment;
import com.flowzr.report.Report;
import com.flowzr.report.ReportType;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isFocusable;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.Is.is;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReportsListFragmentTest extends MyFragmentTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    @Test
    public void openReportsList () {
        onView(withId(R.id.drawer_V)).perform(DrawerActions.open());
        onView(Matchers.allOf(ViewMatchers.withText(R.string.reports))).perform(click());
    }

    @Test
    public void testDrawNavTo() {
        openReportsList();
        onView(allOf(withContentDescription(R.string.reports))).check (ViewAssertions.matches (withListSize (ReportsListFragment.reports.length)));
            matchToolbarTitle(R.string.reports);
    }


    @Test
    public void test1stLvlConventionalReports() {
        ReportType[] reports = new ReportType[]{
                ReportType.BY_PERIOD,
                ReportType.BY_CATEGORY,
                ReportType.BY_PAYEE,
                ReportType.BY_LOCATION,
                ReportType.BY_PROJECT,
                ReportType.BY_ACCOUNT_BY_PERIOD,
                ReportType.BY_CATEGORY_BY_PERIOD,
                ReportType.BY_PAYEE_BY_PERIOD,
                ReportType.BY_LOCATION_BY_PERIOD,
                ReportType.BY_PROJECT_BY_PERIOD
        };

        openReportsList();
        onView(Matchers.allOf(ViewMatchers.withText(ReportType.BY_PERIOD.titleId))).perform(click());
        matchToolbarTitle(ReportType.BY_PERIOD.titleId);
        pressBack();
        onView(Matchers.allOf(ViewMatchers.withText(ReportType.BY_PERIOD.titleId))).perform(click());
        matchToolbarTitle(ReportType.BY_PERIOD.titleId);
        pressBack();

        for (int i=0;i<reports.length;i++) {
                onView(Matchers.allOf(ViewMatchers.withText(ReportType.values()[i].titleId))).perform(click());
                if (reports[i].isConventionalBarReport()) {
                    matchToolbarTitle(ReportType.values()[i].titleId);
                }
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