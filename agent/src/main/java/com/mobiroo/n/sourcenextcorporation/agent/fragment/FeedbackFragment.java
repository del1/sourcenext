package com.mobiroo.n.sourcenextcorporation.agent.fragment;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.plus.PlusOneButton;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.util.AlertDialogUtility;
import com.mobiroo.n.sourcenextcorporation.agent.util.DebugFileUtils;


public class FeedbackFragment extends Fragment {

	private PlusOneButton               mPlusOneButton;
	private LinearLayout                mRateButton;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_feedback,  null);
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPlusOneButton = (PlusOneButton) view.findViewById(R.id.plus_one_button);

		mRateButton = (LinearLayout) view.findViewById(R.id.rate_on_play_button);
		mRateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Uri uri = Uri.parse("market://details?id=" + getActivity().getPackageName());
				Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);  
				try {
					startActivity(goToMarket);
				} catch (ActivityNotFoundException e) {                    
				}
			}
		});

		LinearLayout container = (LinearLayout) view.findViewById(R.id.tags_list);
		container.addView(new HelpOption(getString(R.string.help_email), R.drawable.ic_action_gmail).getView(getActivity()));
		container.addView(new HelpOption(getString(R.string.help_gplus), R.drawable.ic_action_gplus).getView(getActivity()));
		container.addView(new HelpOption(getString(R.string.help_trigger), R.drawable.ic_action_globe).getView(getActivity()));
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		mPlusOneButton.initialize("https://play.google.com/store/apps/details?id=com.mobiroo.n.sourcenextcorporation.agent", 2083);
	}


	private void openHelpLink(String option) {
		if (option.equals(getString(R.string.help_email))) {
            DebugFileUtils.sendEmail(getActivity());
		} else if (option.equals(getString(R.string.help_gplus))) {
			AlertDialogUtility.showDialog(getActivity(), getString(R.string.external_link_dialog_title), getString(R.string.external_link_dialog_message), getString(R.string.label_yes), getString(R.string.label_no), new AlertDialogUtility.AlertDialogClickListner() {
				@Override
				public void onOkClick() {
					// Open a link to tryagent's Google+ page
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse("https://plus.google.com/108884363382776992488/posts"));
					startActivity(intent);
				}

				@Override
				public void onCancelClick() {

				}
			});

		} else if (option.equals(getString(R.string.help_trigger))) {
			AlertDialogUtility.showDialog(getActivity(), getString(R.string.external_link_dialog_title), getString(R.string.external_link_dialog_message), getString(R.string.label_yes), getString(R.string.label_no), new AlertDialogUtility.AlertDialogClickListner() {
			@Override
			public void onOkClick() {
				// Open a link to trigger on app store
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.jwsoft.nfcactionlauncher"));
				startActivity(intent);
			}

			@Override
			public void onCancelClick() {

			}
		});
		}
	}


	private class HelpOption implements View.OnClickListener {
		public String Label;
		public int ImageResource;

		public HelpOption(String label, int resid) {
			Label = label;
			ImageResource = resid;
		}

		public View getView(Context context) {

			View convertView = View.inflate(context,R.layout.list_item_help_option, null);
			(convertView.findViewById(R.id.container)).setOnClickListener(this);
			HelpOption option = this;
			((TextView) convertView.findViewById(R.id.row1Text)).setText(option.Label);
			((ImageView) convertView.findViewById(R.id.row1Image)).setImageResource(option.ImageResource);

			return convertView;
		}

		@Override
		public void onClick(View v) {
			openHelpLink(this.Label);
		}


	}

}
