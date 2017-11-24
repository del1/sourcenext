package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.R;

public class AgentLabelSetting extends AgentUIElement {
    public enum LABEL_TYPE {NORMAL, HEADER, SUBHEADER}
    protected int mLabelColor;
    protected View mRootView;

    protected float getSizeForType(LABEL_TYPE type) {
        switch(type) {
            case NORMAL:
                return 14;
            case HEADER:
                return 18;
            case SUBHEADER:
                return 18;
            default:
                return 14;
        }
    }

    protected Typeface getFontForType(LABEL_TYPE type) {
        switch(type) {
            case NORMAL:
                return Typeface.create("sans-serif-light", Typeface.NORMAL);
            case HEADER:
                return Typeface.create("sans-serif-condensed", Typeface.BOLD);
            case SUBHEADER:
                return Typeface.create("sans-serif-condensed", Typeface.NORMAL);
            default:
                return Typeface.create("sans-serif-light", Typeface.NORMAL);
        }
    }

    protected int getDrawableForType(LABEL_TYPE type) {
        switch(type) {
            case NORMAL:
                return R.drawable.label_normal_background;
            case HEADER:
                return R.drawable.label_normal_background;
            case SUBHEADER:
                return R.drawable.label_header_background;
            default:
                return R.drawable.label_normal_background;
        }
    }

    protected int getColorForType(LABEL_TYPE type) {
        switch(type) {
            case NORMAL:
                return Color.BLACK;
            case HEADER:
                return Color.GRAY;
            case SUBHEADER:
                return Color.BLACK;
            default:
                return Color.BLACK;
        }
    }

	protected String mText;
	protected TextView mTextView;
	protected boolean mEnabled;
    protected LABEL_TYPE mType;
	
	public AgentLabelSetting(AgentConfigurationProvider aca, int text) {
		mAgentConfigure = aca;
		mText = aca.getActivity().getString(text);
		mEnabled = true;
        mType = LABEL_TYPE.NORMAL;
	}

    public AgentLabelSetting(AgentConfigurationProvider aca, String text, LABEL_TYPE type) {
        mAgentConfigure = aca;
        mText = text;
        mEnabled = true;
        mType = type;
    }

    protected int getViewResource() { return R.layout.list_item_label; }

	@Override
	public View getView(Context context) {
		View labelView = View.inflate(context, getViewResource(), null);
        mRootView = labelView;

		mTextView = (TextView) labelView.findViewById(R.id.text);
		mTextView.setText(mType.equals(LABEL_TYPE.HEADER) ? mText.toUpperCase() : mText);
		mTextView.setTypeface(getFontForType(mType));
		mTextView.setTextSize(getSizeForType(mType));

        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            mTextView.setBackgroundDrawable(context.getResources().getDrawable(getDrawableForType(mType)));
        } else {
            mTextView.setBackground(context.getResources().getDrawable(getDrawableForType(mType)));
        }

        mLabelColor = getColorForType(mType);
        mTextView.setTextColor(mLabelColor);

		if(mEnabled) {
			enableElement();
		} else {
			disableElement();
		}
		
		return labelView;
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public SettingType getType() {
		// TODO Auto-generated method stub
		return SettingType.STATIC;
	}

	@Override
	public void disableElement() {
		if(mTextView != null) {
			mTextView.setTextColor(mAgentConfigure.getActivity().getResources().getColor(R.color.setting_disabled));
		}
		
		mEnabled = false;
	}

	@Override
	public void enableElement() {
		if(mTextView != null) {
			mTextView.setTextColor(mLabelColor);
		}
		
		mEnabled = true;
	}

}
