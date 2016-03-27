package com.flowzr.test;



import org.junit.Before;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.pressBack;
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



}
