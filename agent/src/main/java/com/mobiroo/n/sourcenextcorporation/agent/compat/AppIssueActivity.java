package com.mobiroo.n.sourcenextcorporation.agent.compat;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Window;

import com.mobiroo.n.sourcenextcorporation.agent.R;

public class AppIssueActivity extends FragmentActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.activity_agent_issues);
	}
}
