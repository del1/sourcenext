package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.R;

public class AgentTextLineSetting extends AgentUIElement {
	int mId;
	String mText;
	View mSettingView;
	Context mContext;
	
	boolean mEnabled;
	
	TextView mTitleView;
	EditText mTextView;
	
	public AgentTextLineSetting(AgentConfigurationProvider aca, int name, String prefName, String text) {
		mAgentConfigure = aca;
		mText = text;
		mPrefName = prefName;
		mName = name;
		mId = prefName.hashCode();
		mEnabled = false;
	}
	
	@Override
	public View getView(Context context) {
		// TODO Auto-generated method stub
		mSettingView = View.inflate(context,
				R.layout.list_item_config_text_single, null);
		mContext = context;
		
		mTitleView = (TextView) mSettingView.findViewById(R.id.title);
        Typeface font = Typeface.createFromAsset(context.getAssets(), "Roboto-Light.ttf");  
        mTitleView.setTypeface(font);
		
		mTextView = (EditText) mSettingView.findViewById(R.id.textEdit);
		mTextView.setId(mId);
		
		TextView savingView = (TextView) mSettingView.findViewById(R.id.saving);

		mTitleView.setText(mName);
		mTextView.setText(mText);
		savingView.setVisibility(View.GONE);
		
		mTextView.addTextChangedListener(new AgentTextLineSettingWatcher());
		
		if(mEnabled)
			enableElement();
		else
			disableElement();
		
		return mSettingView;
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public SettingType getType() {
		// TODO Auto-generated method stub
		return SettingType.TEXT;
	}

	
	protected class AgentTextLineSettingWatcher implements TextWatcher {
				
		@Override
		public void afterTextChanged(Editable s) {
			// TODO Auto-generated method stub
			TextView savingView = (TextView) mSettingView.findViewById(R.id.saving);
			savingView.setVisibility(View.VISIBLE);
			
			AgentTextLineSetting.this.mAgentConfigure.updateSetting(AgentTextLineSetting.this.mPrefName, s.toString(), new Runnable() {
				public void run() { }
			});
			
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// TODO Auto-generated method stub
			
		}
		
	}


	@Override
	public void disableElement() {
		if(mTextView != null) {
			mTextView.setEnabled(false);

			if (mAgentConfigure.getActivity() != null) {
				mTitleView.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_disabled));
			}
		}
		mEnabled = false;
	}

	@Override
	public void enableElement() {
		if(mTextView != null) {
			mTextView.setEnabled(true);

			if (mAgentConfigure.getActivity() != null) {
				mTitleView.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_enabled));
			}
		}
		mEnabled = true;
	}
}
