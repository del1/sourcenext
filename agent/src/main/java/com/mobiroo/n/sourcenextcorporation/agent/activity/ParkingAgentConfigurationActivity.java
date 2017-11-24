package com.mobiroo.n.sourcenextcorporation.agent.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentInfoFragment;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.ConfigureAgentFragment;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.FeedFragment;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.ParkingHistoryFragment;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;

import java.util.List;

public class ParkingAgentConfigurationActivity extends
		AgentConfigurationActivity {


    private static final int PARKING_MAP_LOCATION = 1;

    private static final String PREF_HAS_SHOWN_INTRO = "com.mobiroo.n.sourcenextcorporation.agent.parking_agent.has_shown_intro";

	@Override
	protected void populateViewFrags() {
		mViewFrags = new FragmentHolder[3];

		Bundle args0 = new Bundle();
		args0.putString(FeedFragment.AGENT_GUID, mAgentGuid);
		mViewFrags[0] = new FragmentHolder();
		mViewFrags[0].fragment = new AgentInfoFragment();
		mViewFrags[0].fragment.setArguments(args0);
		mViewFrags[0].title = getResources().getString(R.string.agent_menu_description);


		mViewFrags[PARKING_MAP_LOCATION] = new FragmentHolder();
		mViewFrags[PARKING_MAP_LOCATION].fragment = new ParkingHistoryFragment();
		mViewFrags[PARKING_MAP_LOCATION].title = getResources().getString(R.string.agent_menu_parking);


		Bundle args2 = new Bundle();
		args2.putString(FeedFragment.AGENT_GUID, mAgentGuid);
		mViewFrags[2] = new FragmentHolder();
		mViewFrags[2].fragment = new ConfigureAgentFragment();
		mViewFrags[2].fragment.setArguments(args2);
		mViewFrags[2].title = getResources().getString(R.string.agent_menu_config);
	}

    @Override
    protected int getDefaultEnabledPosition() {
        if (PrefsHelper.getPrefBool(this, PREF_HAS_SHOWN_INTRO, false)) {
            return PARKING_MAP_LOCATION;
        }

        PrefsHelper.setPrefBool(this, PREF_HAS_SHOWN_INTRO, true);
        return 0;
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
