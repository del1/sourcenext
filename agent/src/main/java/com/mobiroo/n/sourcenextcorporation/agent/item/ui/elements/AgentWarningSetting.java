package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements;

import android.graphics.Color;
import android.view.View;

import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;

/**
 * Created by omarseyal on 4/3/14.
 */
public class AgentWarningSetting extends AgentLabelSetting{

    public AgentWarningSetting(AgentConfigurationProvider aca, int text) {
        super(aca, text);
    }

    @Override
    protected int getColorForType(LABEL_TYPE type) {
        return Color.BLACK;
    }

    @Override
    protected int getViewResource() { return R.layout.list_item_warning; }

    @Override
    public void disableElement() {
        if(mRootView != null) {
            mRootView.setVisibility(View.GONE);
        }

        mEnabled = false;
    }

    @Override
    public void enableElement() {
        if(mRootView != null) {
            mRootView.setVisibility(View.VISIBLE);
        }

        mEnabled = true;
    }
}
