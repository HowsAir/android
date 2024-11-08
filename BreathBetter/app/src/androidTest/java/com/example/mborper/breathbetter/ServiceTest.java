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

/**
 * Instrumented test for verifying the service start functionality in the app.
 * <p>
 * This test class uses Espresso to simulate user actions and check UI components
 * related to starting a service in the app, including permission handling and toast messages.
 *
 * @author Manuel Borregales
 * @since 2024-10-08
 */
@RunWith(AndroidJUnit4.class)
public class ServiceTest {

    /**
     * Rule to launch the MainActivity for testing.
     */
    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Rule to automatically grant necessary permissions for the test.
     * <p>
     * This rule grants permissions for Bluetooth and location access,
     * which are required to start the service.
     */
    @Rule
    public GrantPermissionRule grantPermissionRule =
            GrantPermissionRule.grant(Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION);

    /**
     * Tests that a toast message appears when the service starts.
     * <p>
     * Simulates a button click to start the service in MainActivity and verifies
     * that the "Service bound successfully" toast message is displayed in the correct window.
     */
    @Test
    public void testToastOnServiceStart() {
        // Simulates a click on the button to start the service
        onView(withId(R.id.startSearching)).perform(click());

        activityScenarioRule.getScenario().onActivity(activity -> {
            // Ensure that the toast appears in the correct window
            onView(withText("Service bound successfully"))
                    .inRoot(withDecorView(not(is(activity.getWindow().getDecorView()))))
                    .check(matches(isDisplayed()));
        });
    }
}
