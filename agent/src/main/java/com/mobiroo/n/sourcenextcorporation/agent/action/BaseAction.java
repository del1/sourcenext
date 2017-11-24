package com.mobiroo.n.sourcenextcorporation.agent.action;

import android.content.Context;
import android.database.Cursor;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.util.TaskDatabaseHelper;


public abstract class BaseAction {
	public static final int OPERATION_TRIGGER = 1;
	public static final int OPERATION_UNTRIGGER = -1;
	
	protected Context mContext;
	
	protected String mActionClassName;
	protected int mId;
	protected String mAgentGuid;
	protected int mActionNumber;
	protected String mActionCommand;

	protected Agent mAgent;
	
	public static BaseAction getActionFromCursor(Context context, Cursor cursor) {
		String actionClassName = cursor.getString(cursor.getColumnIndex(TaskDatabaseHelper.FIELD_ACTION_CLASS));
		
		if ((actionClassName == null) || (! actionClassName.endsWith("Action"))) {
			return null;
		}
		
		BaseAction action = null;		
		try {
			action = (BaseAction) Class.forName("com.mobiroo.n.sourcenextcorporation.agent.action." + actionClassName).getConstructor().newInstance();
		} catch (Exception e) {
			Logger.e(e.getClass().getName(), e.getMessage(), e);
			return null;
		}
		
		action.mActionClassName = actionClassName;
		action.mId = cursor.getInt(0);
		action.mContext = context;
		action.mAgentGuid = cursor.getString(cursor.getColumnIndex(TaskDatabaseHelper.FIELD_GUID));
		action.mActionNumber = cursor.getInt(cursor.getColumnIndex(TaskDatabaseHelper.FIELD_ACTION_NUMBER));
		action.mActionCommand = cursor.getString(cursor.getColumnIndex(TaskDatabaseHelper.FIELD_COMMAND));
		
		action.mAgent = AgentFactory.getAgentFromGuid(action.mContext, action.mAgentGuid);
		
		return action;
	}
	
	
	public void trigger(int triggerType, Object extraInfo) {		
		performActions(OPERATION_TRIGGER, triggerType, extraInfo);
	}
	
	public void untrigger(int triggerType, Object extraInfo) {
		performActions(OPERATION_UNTRIGGER, triggerType, extraInfo);
	}
	
	public String actionClassName() {
		return mActionClassName;
	}
	
	protected abstract void performActions(int operation, int triggerType, Object extraInfo);	

}
