package com.mobiroo.n.sourcenextcorporation.agent.compat;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.R;

public class AppIssueFragment extends ListFragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_app_issues, container,
				false);

		IssueAdapter adapter = new IssueAdapter(getActivity(),
				R.layout.list_item_issue,
				AppIssueDetector.getAllIssues(getActivity()));
		setListAdapter(adapter);

		return view;
	}


	public class IssueAdapter extends ArrayAdapter<AppIssue> {
		private AppIssue[] mIssues;

		public AppIssue getIssue(int position) {
			return mIssues[position];
		}
		
		public IssueAdapter(Context context, int textViewResourceId,
				AppIssue[] issues) {
			super(context, textViewResourceId, issues);
			mIssues = issues;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;

			if (v == null) {
				LayoutInflater inflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.list_item_issue, null);
			}

			final AppIssue issue = mIssues[position];
			TextView issueTitle = (TextView) v.findViewById(R.id.issue_title);
			
			v.findViewById(R.id.container).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					AppIssueDialogFactory.getPlainDialogForIssue(issue, getActivity()).show();
				}
				
			});
			
			issueTitle.setText(issue.getTitle());

			return v;
		}
	}
}
