package com.mobiroo.n.sourcenextcorporation.agent.item.onboarding;

/**
 * Created by krohnjw on 4/23/2014.
 */
public interface OnboardingCallbackInterface {

    public enum Action {
        NEXT_FRAGMENT,
        SKIP_ONBOARDING,
        FINISH
    }

    public void goToNext(Action action, String next);
    public void updateConfiguration(String tag, ConfigurationDataInterface data);
}
