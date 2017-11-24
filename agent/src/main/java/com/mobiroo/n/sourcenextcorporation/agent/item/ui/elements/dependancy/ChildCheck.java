package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.dependancy;

import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentUIElement;

public interface ChildCheck {
    public void addConditional(ConditionalElement element);
    public void addConsequence(AgentUIElement element);

    public void onConditionalChanged(ConditionalElement element);
}
