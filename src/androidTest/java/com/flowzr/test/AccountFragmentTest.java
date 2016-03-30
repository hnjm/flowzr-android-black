package com.flowzr.test;


import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import android.test.suitebuilder.annotation.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import com.flowzr.activity.MainActivity;
import com.flowzr.R;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isFocusable;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class AccountFragmentTest extends MyFragmentTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    @Test
    public void testAccountEmptyViewExists() {
        onView(allOf(withId(R.id.emptyView),withContentDescription(R.string.no_accounts))).check(matches(isFocusable()));
    }


}