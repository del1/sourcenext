package com.mobiroo.n.sourcenextcorporation.agent.fragment.onboarding;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.mobiroo.n.sourcenextcorporation.agent.util.Usage;
import com.mobiroo.n.sourcenextcorporation.agent.R;

/**
 * Created by krohnjw on 4/23/2014.
 */
public class IntroFragment extends BaseOnboardingFragment {

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Insert content into place holder
        ((LinearLayout) view.findViewById(R.id.onboarding_content)).addView(View.inflate(getActivity(), R.layout.fragment_onboarding_intro, null));
    }

    @Override
    protected void leftClick() {
        Usage.logEvent(getActivity(), Usage.Events.ONBOARDING_SKIP, false);
        super.leftClick();
    }

    @Override
    protected void rightClick() {
        Usage.logEvent(getActivity(), Usage.Events.ONBOARDING_LETS_GET_STARTED, false);
        super.rightClick();
    }
}
