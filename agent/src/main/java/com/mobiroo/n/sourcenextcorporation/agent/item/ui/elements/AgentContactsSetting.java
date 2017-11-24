package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.activity.choosers.ContactsChooserActivity;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.SettingSaver;

public class AgentContactsSetting extends AgentUIElement implements SettingSaver {

	@Override
	public SettingType getType() {
		return SettingType.PICKLIST;
	}

	public static final int BLUETOOTH_CHOOSER = 0x1;
	
	protected int mName;
	protected String mContactsStringName;
	protected TextView mTitleView;
	protected TextView mEditContactSettingView;
	protected boolean mEnabled;
	
	public AgentContactsSetting(AgentConfigurationProvider aca, int name, String prefName, String contactsString) {
		mPrefName = prefName;
		mName = name;
		mAgentConfigure = aca;
		mContactsStringName = contactsString;
		mEnabled = true;
	}
	
	@Override
	public View getView(Context context) {
		View contactSettingView = View.inflate(context,
				R.layout.list_item_config_contacts, null);

		((TextView) contactSettingView.findViewById(R.id.title))
		.setText(getName());
		
		mTitleView = (TextView) contactSettingView.findViewById(R.id.title);
		mEditContactSettingView = (TextView) contactSettingView.findViewById(R.id.contactsEdit);

		mEditContactSettingView.setText(getContactsString());
		mEditContactSettingView.setPaintFlags(mEditContactSettingView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		mEditContactSettingView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(AgentContactsSetting.this.mAgentConfigure.getActivity(), ContactsChooserActivity.class);
				intent.putExtra(ContactsChooserActivity.CONTACTS_LIST, mContactsStringName);
				AgentContactsSetting.this.mAgentConfigure.startUpdateSettingActivityForResult(intent,
						getActivityResultKey());
			}
		});

        Typeface font = Typeface.createFromAsset(context.getAssets(), "Roboto-Light.ttf");  
		mEditContactSettingView.setTypeface(font);
		((TextView) contactSettingView.findViewById(R.id.title))
		.setTypeface(font);
		
		if(mEnabled)
			enableElement();
		else
			disableElement();
		
		return contactSettingView;
	}


	@Override
	public int getName() {
		return mName;
	}
	
	@Override
	public boolean isEnabled() {
		return true;
	}
	
	protected String getContactsString() {
		if ((mContactsStringName == null) || (mContactsStringName.trim().equals(""))) {
			return mAgentConfigure.getActivity().getResources().getString(R.string.agent_contact_val_noone);
		}
		
		String[] ids = mContactsStringName.split(AgentPreferences.STRING_SPLIT);
		if (ids.length == 0) {
			return mAgentConfigure.getActivity().getResources().getString(R.string.agent_contact_val_noone);
		}
		
		if ((ids.length == 1)  && (ids[0].equals(AgentPreferences.SMS_AUTORESPOND_CONTACT_EVERYONE))) {
			return mAgentConfigure.getActivity().getResources().getString(R.string.agent_contact_val_mycontacts);
		}
		if ((ids.length == 1)  && (ids[0].equals(AgentPreferences.SMS_AUTORESPOND_CONTACT_STRANGERS))) {
			return mAgentConfigure.getActivity().getResources().getString(R.string.agent_contact_val_strangers_only);
		}

		boolean strangers = false;
		boolean everyone = false;
		for (String id : ids) {
			if (id.equals(AgentPreferences.SMS_AUTORESPOND_CONTACT_EVERYONE)) {everyone = true;}
			if (id.equals(AgentPreferences.SMS_AUTORESPOND_CONTACT_STRANGERS)) {strangers = true;}
			if (strangers && everyone) {
				return mAgentConfigure.getActivity().getResources().getString(R.string.agent_contact_val_anyone);				
			}
		}
		
		return mAgentConfigure.getActivity().getResources().getString(R.string.agent_contact_val_custom);
	}

	
	@Override
	public int getActivityResultKey() {
		return mPrefName.hashCode() & 32767;
	}

	@Override
	public void saveSetting(Intent data) {
		mContactsStringName = data.getExtras().getString(ContactsChooserActivity.CONTACTS_LIST);
		mAgentConfigure.updateSetting(mPrefName, mContactsStringName);
		mEditContactSettingView.setText(getContactsString());
	}

	
	@Override
	public void disableElement() {
		if(mEditContactSettingView != null) {
			mEditContactSettingView.setEnabled(false);
			mEditContactSettingView.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_disabled));
			mTitleView.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_disabled));
		}
		mEnabled = false;
	}

	@Override
	public void enableElement() {
		if(mEditContactSettingView != null && mAgentConfigure.getActivity() != null) {
			mEditContactSettingView.setEnabled(true);
			mEditContactSettingView.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_enabled));
			mTitleView.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_enabled));
		}
		mEnabled = true;
	}
}
