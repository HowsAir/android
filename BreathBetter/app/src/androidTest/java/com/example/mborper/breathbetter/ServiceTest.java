package com.example.mborper.breathbetter;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;

import androidx.test.rule.GrantPermissionRule;
import android.Manifest;

@RunWith(AndroidJUnit4.class)
public class ServiceTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    // Add this rule to grant permissions automatically
    @Rule
    public GrantPermissionRule grantPermissionRule =
            GrantPermissionRule.grant(Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION);

    @Test
    public void testToastOnServiceStart() {
        // Simulates a click on the button to start the service
        onView(withId(R.id.startSearching)).perform(click());

        activityScenarioRule.getScenario().onActivity(activity -> {
            // Asegúrate de que el Toast aparezca en la ventana correcta
            onView(withText("Service bound successfully"))
                    .inRoot(withDecorView(not(is(activity.getWindow().getDecorView()))))
                    .check(matches(isDisplayed()));
        });
    }
}

