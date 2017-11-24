package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements;

import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.dependancy.ChildCheck;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.dependancy.ConditionalElement;

import java.util.ArrayList;
import java.util.List;

public abstract class AgentUIElement implements AgentListItem, ConditionalElement {
	protected int mName;
	protected String mPrefName;
	protected AgentConfigurationProvider mAgentConfigure;

	public enum SettingType {
		BOOLEAN_CHECKBOX,
		BOOLEAN_RADIO,
		BOOLEAN_SWITCH,
		PICKLIST, STRING,
		TRIGGER_TYPE,
		STRING_INFO, TIME,
		INT, TEXT, STATIC,
		DAY_OF_WEEK, TIME_RANGE_GROUP
	}
	
	public abstract SettingType getType();

    protected ArrayList<ChildCheck> mChildCheck;

    public boolean getState() { return isEnabled(); }

    public void addChildCheck(ChildCheck c) {
        if(mChildCheck == null)
            mChildCheck = new ArrayList<ChildCheck>();

        mChildCheck.add(c);
    }

    public void notifyChecksChanged() {
        if(mChildCheck == null)
            return;

        for(ChildCheck c : mChildCheck) {
            c.onConditionalChanged(this);
        }
    }
	
	public abstract void disableElement();
	
	public abstract void enableElement();
	
	public void disable() { disableElement(); notifyChecksChanged(); }
	
	public void enable() {
		enableElement(); notifyChecksChanged();
	}
	
	public int getIcon() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("getIcon() not implemented");
	}
	
	public int getName() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("getName() not implemented");
	}
	
	public int getDescription() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("getDescription() not implemented");
	}

	public List<Integer> getPickList() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("getPickList() not implemented");
	}
	
	public int getActivityResultKey() {
		return 0;
	}
}
