package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.dependancy;

/**
 * Created by omarseyal on 4/4/14.
 */
public interface ConditionalElement {
    public boolean getState();
    public void addChildCheck(ChildCheck c);
}
