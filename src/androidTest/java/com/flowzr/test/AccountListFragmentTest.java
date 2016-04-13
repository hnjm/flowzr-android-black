package com.flowzr.test;


import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.support.v7.widget.AppCompatTextView;
import android.test.suitebuilder.annotation.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import com.flowzr.activity.MainActivity;
import com.flowzr.R;
import com.flowzr.model.Account;
import com.flowzr.model.EntityType;
import com.flowzr.model.Payee;

import java.util.List;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.openContextualActionModeOverflowMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isFocusable;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class AccountListFragmentTest extends AbstractDBTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    @Test
    public void testAccountEmptyViewExists() {
        onView(allOf(withId(R.id.emptyView), withContentDescription(R.string.no_accounts))).check(matches(isFocusable()));
    }

    @Test
    public void testOpenScheduled() {
        onView(ViewMatchers.withId(R.id.action_scheduled_transaction)).perform(click());
        onView(allOf(withId(R.id.emptyView)));
        matchToolbarTitle(R.string.scheduled_transactions);
    }

    @Test
    public void testCanAdd() {
        onView(ViewMatchers.withId(R.id.action_add_account)).perform(click());
        onView(allOf(withId(R.id.emptyView)));
        matchToolbarTitle(R.string.add_account);
    }

    @Test
    public void testOpenPlanner() {
        // @TODO check toolbar layout test fail because multiple toolbar exist
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
        onView(withId(R.id.action_planner)).perform(click());
        onView(allOf(withId(R.id.period)));
        matchToolbarTitle(R.string.planner);
    }

    @Test
    public void testOpenFix() {
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
        onView(withId(R.id.action_integrity_fix)).perform(click());
    }

    @Test
    public void testTitle() {
        matchToolbarTitle(R.string.accounts);
    }

    @Test
    public void testCanOpenAccountsList() {
        onView(allOf(withId(R.id.emptyView), withContentDescription(R.string.no_accounts))).check(matches(isFocusable()));
    }

    @Test
    public void testCanOpenFromDrawer() {
        onView(withId(R.id.drawer_V)).perform(DrawerActions.open());
        onView(Matchers.allOf(isAssignableFrom(AppCompatCheckedTextView.class), withText(R.string.accounts))).perform(click());

    }


    @Test
    public void testCanOpenScheduled() {
        onView(allOf(withId(R.id.emptyView), withContentDescription(R.string.no_accounts))).check(matches(isFocusable()));
        matchToolbarTitle(R.string.accounts);
    }

    @Test
    public void canViewAllAccounts() {
        // get from database
        List<Account> accounts = em.getAllAccountsList();
        for (int i = 0; i < accounts.size() && i < 3; i++) {
            // & check all
            Account account = accounts.get(i);
            onView(withText(account.getTitle())).perform(scrollTo(), click());
            matchToolbarTitle(account.getTitle());
        }

    }
}