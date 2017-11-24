package com.mobiroo.n.sourcenextcorporation.agent.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;
import android.widget.FrameLayout;
import com.mobiroo.drm.MobirooDrm;
import com.mobiroo.drm.MobirooDrmInspector;
import com.mobiroo.drm.MobirooValidator;


import com.mobiroo.n.sourcenextcorporation.agent.fragment.onboarding.BaseOnboardingFragment;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.onboarding.DriveAgentOnboardingFragment;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.onboarding.IntroFragment;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.onboarding.MeetingAgentOnboardingFragment;
import com.mobiroo.n.sourcenextcorporation.agent.item.onboarding.ConfigurationDataInterface;
import com.mobiroo.n.sourcenextcorporation.agent.item.onboarding.OnboardingCallbackInterface;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by krohnjw on 4/23/2014.
 */
public class OnboardingActivity extends FragmentActivity {

    public class OnboardingCallback implements OnboardingCallbackInterface {

        @Override
        public void goToNext(Action action, String next) {
            Intent i = new Intent();
            switch (action) {
                case FINISH:
                    i.putExtra(EXTRA_INSTALL, true);
                    i.putExtra(EXTRA_CONFIG_DATA, getPrefsForBundle());
                    setResult(RESULT_OK, i);
                    finish();
                    break;
                case SKIP_ONBOARDING:
                    i.putExtra(EXTRA_INSTALL, false);
                    setResult(RESULT_CANCELED, i);
                    finish();
                    break;
                case NEXT_FRAGMENT:
                    showFragment(next);
                    break;
            }
        }

        @Override
        public void updateConfiguration(String tag, ConfigurationDataInterface data) {
            mPendingPrefs.put(tag, data.getPrefs());
        }
    }

    public static final String EXTRA_CONFIG_DATA = "com.mobiroo.n.sourcenextcorporation.agent.extra_config_data";
    public static final String EXTRA_INSTALL = "com.mobiroo.n.sourcenextcorporation.agent.extra_install";

    private HashMap<String, BaseOnboardingFragment> mFragments;
    private OnboardingCallback mCallback;

    private BaseOnboardingFragment FRAGMENT_INTRO = new IntroFragment();
    private BaseOnboardingFragment FRAGMENT_DRIVE = new DriveAgentOnboardingFragment();
    private BaseOnboardingFragment FRAGMENT_MEETING = new MeetingAgentOnboardingFragment();

    private final int FRAME_ID = 837281;

    private HashMap<String, String[]> mPendingPrefs;

    @Override
    public void onCreate(Bundle instance) {
        super.onCreate(instance);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (instance == null) {
            FrameLayout frame = new FrameLayout(this);
            frame.setId(FRAME_ID);
            setContentView(frame);

            mCallback = new OnboardingCallback();
            mPendingPrefs = new HashMap<String, String[]>();
            /* Define fragment order */
            FRAGMENT_INTRO.setNextFragment(FRAGMENT_MEETING.getFragmentTag());
            FRAGMENT_INTRO.setCallback(mCallback);

            FRAGMENT_DRIVE.setNextFragment(null);
            FRAGMENT_DRIVE.setCallback(mCallback);

            FRAGMENT_MEETING.setNextFragment(FRAGMENT_DRIVE.getFragmentTag());
            FRAGMENT_MEETING.setCallback(mCallback);

            mFragments = new HashMap<String, BaseOnboardingFragment>()
            {
                {
                    put(FRAGMENT_INTRO.getFragmentTag(), FRAGMENT_INTRO);
                    put(FRAGMENT_MEETING.getFragmentTag(), FRAGMENT_MEETING);
                    put(FRAGMENT_DRIVE.getFragmentTag(), FRAGMENT_DRIVE);
                }
            };

            showFragment(FRAGMENT_INTRO.getFragmentTag());
        }
    }

   /* @Override
    protected void onResume() {
        validate(OnboardingActivity.this);
        super.onResume();
    }*/

    private void validate(final Activity myActivity) {
        myActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MobirooDrm.setDebugLog(true);
                MobirooDrm.validateActivity(myActivity);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (getCurrentFragment().onBackPressed()) {
            super.onBackPressed();
        }
    }

    private void showFragment(String tag) {
        if (tag == null) {
            // Schedule a finish
        } else {
            Fragment f = mFragments.get(tag);
            if (f != null) {
                showFragment(f);
            }
        }
    }

    private BaseOnboardingFragment getCurrentFragment() {
        return (BaseOnboardingFragment) getSupportFragmentManager().findFragmentById(FRAME_ID);
    }

    private void showFragment(Fragment fragment) {
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        if (!FRAGMENT_INTRO.getFragmentTag().equals(((BaseOnboardingFragment) fragment).getFragmentTag())) {
            t.addToBackStack(((BaseOnboardingFragment) fragment).getFragmentTag());
        }
        t.replace(FRAME_ID, fragment);
        t.commit();
    }

    private String[] getPrefsForBundle() {
        ArrayList<String> prefs = new ArrayList<String>();
        for (String[] data: mPendingPrefs.values()) {
            for (String d: data) {
                prefs.add(d);
            }
        }
        return prefs.toArray(new String[prefs.size()]);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward permission request result to the fragments
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

}
