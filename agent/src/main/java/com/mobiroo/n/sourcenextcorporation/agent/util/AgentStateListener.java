package com.mobiroo.n.sourcenextcorporation.agent.util;

public interface AgentStateListener {
	public void agentEnabled();	
	public void agentDisabled();
	public void agentPaused();
	public void agentStarted();
	public void agentFinished();
}
