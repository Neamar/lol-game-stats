package fr.neamar.lolgamedata;


import android.support.test.espresso.ViewInteraction;

import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

public class PerformanceFlowTest extends FlowTest {
    @Test
    public void performanceFlowTest() {
        // Create account
        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.fab),
                        isDisplayed()));
        floatingActionButton.perform(click());

        ViewInteraction editText = onView(
                allOf(withId(R.id.summonerText),
                        isDisplayed()));
        editText.perform(replaceText("MOCK"), closeSoftKeyboard());

        ViewInteraction button = onView(
                allOf(withId(R.id.save), withText("Add account"),
                        isDisplayed()));
        button.perform(click());

        // Load current game (mocked)
        wait(10000);

        ViewInteraction championName = onView(
                allOf(withText("Illaoi"), isDisplayed()));
        championName.perform(click());

        wait(10000);

        ViewInteraction textView = onView(
                allOf(withId(R.id.championMasteryText),
                        isDisplayed()));
        textView.check(matches(withText("Champion mastery 7")));

        ViewInteraction textView4 = onView(
                allOf(withId(R.id.recentMatchesTitle),
                        isDisplayed()));
        textView4.check(matches(withText("Recent matches with Illaoi")));

        ViewInteraction actionMenuItemView = onView(
                allOf(withId(R.id.action_champion_details),
                        isDisplayed()));
        actionMenuItemView.perform(click());

        wait(10000);

        ViewInteraction textView5 = onView(
                allOf(withId(R.id.abilityName),
                        withText("Q — Tentacle Smash"),
                        isDisplayed()));
        textView5.check(matches(withText("Q — Tentacle Smash")));
    }
}
