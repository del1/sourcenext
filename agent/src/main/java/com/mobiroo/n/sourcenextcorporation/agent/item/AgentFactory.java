package com.mobiroo.n.sourcenextcorporation.agent.item;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentPermission;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.util.TaskDatabaseHelper;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class AgentFactory {

	public static Agent getAgentFromGuid(Context context, String guid) {
		if (guid == null) {return null;}

		Cursor cur = getAgentCursorByGuid(context, guid);
		if (cur == null) {
			return null;
		}

		Agent agent = getAgentFromCursor(context, cur);
		cur.close();
		return agent;
	}

	public static Cursor getAgentCursorByGuid(Context context, String guid) {
		SQLiteDatabase db = TaskDatabaseHelper.getInstance(context).getReadableDatabase();
		Cursor cur = TaskDatabaseHelper.getAgentByGuid(db, guid);
		if (! cur.moveToFirst()) {
			cur.close();
			return null;
		}
		return cur;
	}



	public static List<Agent> getAllAgents(Context context) {
		return getAgents(context, false);
	}
	public static List<Agent> getInstalledAgents(Context context) {
		return getAgents(context, true);
	}

	public static List<Agent> getActiveAgents(Context context) {
		List<Agent> installed = getInstalledAgents(context);
		List<Agent> active = new ArrayList<Agent>();
		for (Agent agent: installed) {
			if (agent.isActive()) {
				active.add(agent);
			}
		}

		return active;

	}

	
	// ---------------------------------------------------
	// Private methods below
	// ---------------------------------------------------
	
	
	private static Agent getStaticAgentClass(Context context, DbAgent dba) {
		StaticAgent agent = null;
		try {
			agent = (StaticAgent) Class.forName("com.mobiroo.n.sourcenextcorporation.agent.item."+dba.getStaticClass()).newInstance();
			agent.setDataFromDbAgent(dba);
		} catch (Exception e) {
			Logger.e(e.getClass().getName(), e.getMessage(), e);
			agent = null;
		}
		return agent;
	}

	private static List<Agent> getAgents(Context context, boolean installedOnly) {
		ArrayList<Agent> results = new ArrayList<Agent>();

		SQLiteDatabase db = TaskDatabaseHelper.getInstance(context).getReadableDatabase();
		Cursor cur = installedOnly ? TaskDatabaseHelper.getInstalledAgents(db, 1000) : TaskDatabaseHelper.getAllAgents(db, 1000);
		if (! cur.moveToFirst()) {
			cur.close();
			return results;
		}

		do {
            Agent agent = getAgentFromCursor(context, cur);
			results.add(getAgentFromCursor(context, cur));
		} while (cur.moveToNext());

		cur.close();

		return results;
	}

    private static class ConcreteDbAgent extends DbAgent {
        private ConcreteDbAgent(Context context, Cursor cur) {
            mContext = context;
            super.syncFieldsFromCursor(context, cur);
        }

        public Class<?> getConfigActivity() { return null;};
        public AgentPermission[] getTriggerArray() {return null;};
    }

	private static Agent getAgentFromCursor(Context context, Cursor cursor) {
		DbAgent dba = new ConcreteDbAgent(context, cursor);

		if (dba.isStatic()) {
			return getStaticAgentClass(context, dba);
		}
        Assert.fail("Should not have non-static agents yet.");
		return dba;
	}

}
