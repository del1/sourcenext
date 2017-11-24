package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.util.KeyValue;
import com.mobiroo.n.sourcenextcorporation.agent.R;

import java.util.ArrayList;

public class AgentSpinnerSetting extends AgentUIElement {
	protected boolean mIsEnabled;
	protected int mId;

	protected String mNameString;
	protected String mPrefValue;

	protected TextView mTitleView;
	protected Spinner mSpinner;

	protected ArrayList<KeyValue> mList;

	protected boolean mInitialized;

	public AgentSpinnerSetting(AgentConfigurationProvider aca, int name,
                               ArrayList<KeyValue> list, String prefName, String prefValue) {
		mInitialized = false;
		mName = name;
		mIsEnabled = true;
		mPrefName = prefName;
		mPrefValue = prefValue;
		mId = prefName.hashCode();
		mAgentConfigure = aca;
		mList = list;
	}

	@Override
	public int getName() {
		return mName;
	}

	protected String getNameString() {
		return mNameString;
	}

	@Override
	public SettingType getType() {
		return SettingType.PICKLIST;
	}

	public boolean isEditable() {
		return mIsEnabled;
	}

	@Override
	public View getView(final Context context) {
		View spinnerSettingView = View.inflate(context,
				R.layout.list_item_config_spinner, null);
		Typeface font = Typeface.createFromAsset(mAgentConfigure.getActivity()
				.getAssets(), "Roboto-Light.ttf");

		if (getName() != -1) {
			((TextView) spinnerSettingView.findViewById(R.id.name))
					.setText(getName());
		} else if (getNameString() != null) {
			((TextView) spinnerSettingView.findViewById(R.id.name))
					.setText(getNameString());
		}

		((TextView) spinnerSettingView.findViewById(R.id.name))
				.setTypeface(font);

		mTitleView = ((TextView) spinnerSettingView.findViewById(R.id.name));
		mSpinner = ((Spinner) spinnerSettingView.findViewById(R.id.spinner));

		CharSequence[] names = new CharSequence[mList.size()];
		for (int i = 0; i < mList.size(); i++) {
			names[i] = mList.get(i).key;
		}

		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
				context, android.R.layout.simple_spinner_item, names);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinner.setAdapter(adapter);

		setSpinner();

		mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				if (mInitialized) {
					mPrefValue = mList.get(pos).value;
					mAgentConfigure.updateSetting(mPrefName, mPrefValue);
				} else {
					mInitialized = true;
					// http://stackoverflow.com/questions/5624825/spinner-onitemselected-executes-when-it-is-not-suppose-to/5918177#5918177
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// do nothing
			}

		});

		if (mIsEnabled)
			enable();
		else
			disable();

		return spinnerSettingView;
	}

	protected void setSpinner() {
		int index = -1;
		for (int i = 0; i < mList.size(); i++) {
			if (mList.get(i).value.equals(mPrefValue)) {
				index = i;
				Logger.i("setSpinner: found value: " + mPrefValue);
			}
		}
		if (index != -1)
			mSpinner.setSelection(index);
		else
			Logger.i("setSpinner: no value found.");
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void disableElement() {
		if (mTitleView != null) {
			mSpinner.setEnabled(false);
			mTitleView.setTextColor(mAgentConfigure.getActivity()
					.getResources().getColor(R.color.setting_disabled));
		}
		mIsEnabled = false;
	}

	@Override
	public void enableElement() {
		if (mTitleView != null) {
			mSpinner.setEnabled(true);
			mTitleView.setTextColor(mAgentConfigure.getActivity()
					.getResources().getColor(R.color.setting_enabled));
		}
		mIsEnabled = true;
	}
}
