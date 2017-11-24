package com.mobiroo.n.sourcenextcorporation.agent.fragment;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.SimpleCursorLoader;
import com.mobiroo.n.sourcenextcorporation.agent.item.DbAgent;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentStateListener;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.activity.AgentConfigurationActivity;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.util.TaskDatabaseHelper;

import java.util.HashMap;

public class FeedFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private FeedCursorAdapter mAdapter;
	protected static String AGENT_IDENTIFIER = "AGENT";
	public static String AGENT_GUID = "agent_guid";

	private View mContent;
	private View mEmpty;
	
	private String mAgentClassName;
	protected String mAgentGuid;
			
	protected void refreshView() {
		if(getActivity()!=null) {
			mAdapter = new FeedCursorAdapter(getActivity(), null, 0, mAgentClassName);
        	setListAdapter(mAdapter);
        
        	getLoaderManager().restartLoader(0, null, this);
		}
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		String agentGuid = null;
		Bundle args = getArguments();

		if (args != null) {
			agentGuid = args.getString(AGENT_GUID);
			mAgentGuid = agentGuid;

			AgentConfigurationActivity activity = (AgentConfigurationActivity) getActivity();
	    	activity.addAgentStateListener(new FeedFragmentAgentListener());
		}
		
		if(agentGuid != null) {
			DbAgent agent = (DbAgent) AgentFactory.getAgentFromGuid(getActivity(), agentGuid);
			mAgentClassName = agent.getStaticClass();
		} else {
			mAgentClassName = null;
		}
		
		View view = inflater.inflate(R.layout.fragment_my_feed, container, false);
		
		return view;
	}
	
    @Override
    public void onResume() {
        super.onResume();
    	refreshView();
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
    	mContent = view.findViewById(R.id.content);
    	mEmpty = view.findViewById(R.id.noFeedText);
    	 
        getListView().setDivider(getResources().getDrawable(R.drawable.agent_background_card_spacer_small));
        getListView().setDividerHeight(4);
        getListView().setSelector(new ColorDrawable(Color.TRANSPARENT));
    }

    
	public static final class FeedCursorLoader extends SimpleCursorLoader {

		String mAgentClassName;
		public FeedCursorLoader(Context context, String agentClassName) {
			super(context);
			mAgentClassName = agentClassName;
			
		}
		
		@Override
		public Cursor loadInBackground() {
            SQLiteDatabase db = TaskDatabaseHelper.getInstance(mContext).getReadableDatabase();
            Cursor cursor = null;
            if(mAgentClassName == null) {
            cursor = db.rawQuery("SELECT " +
                    TaskDatabaseHelper.TABLE_USAGE + "." + TaskDatabaseHelper.FIELD_ID + " AS _id, " + 
            		TaskDatabaseHelper.TABLE_USAGE + "." + TaskDatabaseHelper.FIELD_GUID + ", " + 
                    TaskDatabaseHelper.FIELD_REASON + ", " + 
                    TaskDatabaseHelper.FIELD_STATS + ", " + 
                    TaskDatabaseHelper.FIELD_TIME_EXECUTED + ", " +
                    TaskDatabaseHelper.TABLE_AGENTS + "." + TaskDatabaseHelper.FIELD_STATIC_CLASS + ", " +
                    TaskDatabaseHelper.TABLE_AGENTS + "." + TaskDatabaseHelper.FIELD_NAME +
                    " FROM " + TaskDatabaseHelper.TABLE_USAGE + ", " + TaskDatabaseHelper.TABLE_AGENTS +
                    " WHERE " + TaskDatabaseHelper.TABLE_USAGE + "." + TaskDatabaseHelper.FIELD_GUID + " = " +
                    TaskDatabaseHelper.TABLE_AGENTS + "." + TaskDatabaseHelper.FIELD_GUID +
                    " ORDER BY " + TaskDatabaseHelper.TABLE_USAGE + "." + TaskDatabaseHelper.FIELD_ID + " DESC LIMIT 30", 
                    null);
            } else {
            	cursor = db.rawQuery("SELECT " +
                        TaskDatabaseHelper.TABLE_USAGE + "." + TaskDatabaseHelper.FIELD_ID + " AS _id, " + 
                		TaskDatabaseHelper.TABLE_USAGE + "." + TaskDatabaseHelper.FIELD_GUID + ", " + 
                        TaskDatabaseHelper.FIELD_REASON + ", " + 
                        TaskDatabaseHelper.FIELD_STATS + ", " + 
                        TaskDatabaseHelper.FIELD_TIME_EXECUTED + ", " +
                        TaskDatabaseHelper.TABLE_AGENTS + "." + TaskDatabaseHelper.FIELD_STATIC_CLASS + ", " +
                        TaskDatabaseHelper.TABLE_AGENTS + "." + TaskDatabaseHelper.FIELD_NAME +
                        " FROM " + TaskDatabaseHelper.TABLE_USAGE + ", " + TaskDatabaseHelper.TABLE_AGENTS +
                        " WHERE " + TaskDatabaseHelper.TABLE_USAGE + "." + TaskDatabaseHelper.FIELD_GUID + " = " +
                        TaskDatabaseHelper.TABLE_AGENTS + "." + TaskDatabaseHelper.FIELD_GUID +
                        " AND "  + TaskDatabaseHelper.TABLE_AGENTS + "." + TaskDatabaseHelper.FIELD_STATIC_CLASS + " = " +
                        "'" + mAgentClassName + "'" +
                        " ORDER BY " + TaskDatabaseHelper.TABLE_USAGE + "." + TaskDatabaseHelper.FIELD_ID + " DESC LIMIT 30", 
                        null);
            	return cursor;
            }
            MatrixCursor end = new MatrixCursor(new String[] { "_id", "fieldguid", "reason", "stats", "executed", "classname" });
            try {
				end.addRow(new String[] { "-1", "", getContext().getResources().getString(R.string.agent_was_installed), "", Long.toString(getContext().getPackageManager().getPackageInfo("com.mobiroo.n.sourcenextcorporation.agent", 0).firstInstallTime), AGENT_IDENTIFIER });
			} catch (NameNotFoundException e) {
				end.addRow(new String[] { "-1", "", getContext().getResources().getString(R.string.agent_was_installed), "", Long.toString(System.currentTimeMillis()), AGENT_IDENTIFIER });
			}

            Cursor[] cursors = { cursor, end};

            Cursor extendedCursor = new MergeCursor(cursors);
            
			return extendedCursor;
		}

	}
	
	public static final class FeedCursorAdapter extends CursorAdapter {
		HashMap<String, Integer> mAgentIconHash;
		String mAgentClassName;
		Context mContext;
		
		public FeedCursorAdapter(Context context, Cursor c, int flags, String agentClassName) {
            super(context, c, flags);
 			mAgentIconHash = new HashMap<String, Integer>();
 			mAgentClassName = agentClassName;
 			mContext = context;
		}
		
		protected class FeedViewHolder {
			public ImageView mIcon;
	        public TextView mMessage;
	        public TextView mStats;
	        public TextView mDate;
	        
	        public LinearLayout mTop;
	        public LinearLayout mBottom;
		}
		
		@Override
		public View newView(Context context, Cursor c, ViewGroup parent) {
			View view;

			view = LayoutInflater.from(context).inflate(R.layout.list_item_feed, parent, false);
			
			FeedViewHolder fvh = new FeedViewHolder();
			fvh.mIcon = (ImageView) view.findViewById(R.id.icon);
			fvh.mDate = (TextView) view.findViewById(R.id.date);
			fvh.mMessage = (TextView) view.findViewById(R.id.message);
			fvh.mStats = (TextView) view.findViewById(R.id.stats);

			fvh.mTop = (LinearLayout) view.findViewById(R.id.containerTop);
			fvh.mBottom = (LinearLayout) view.findViewById(R.id.containerBottom);

			view.setTag(fvh);

			return view;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor c) {
			String reason = c.getString(2);
			String stats = c.getString(3);
			long executedAt = c.getLong(4);
			String staticClassName = c.getString(5);
			
			int iconResId;

			if(mAgentIconHash.containsKey(staticClassName)) {
				iconResId = mAgentIconHash.get(staticClassName).intValue();
			} else if (DbAgent.isStatic(staticClassName)) {
				try {
					Agent agent = (Agent) Class.forName("com.mobiroo.n.sourcenextcorporation.agent.item." + staticClassName).getConstructor().newInstance();
					iconResId = agent.getColorIconId();
					mAgentIconHash.put(staticClassName, iconResId);
				} catch (Exception e) {
					Logger.e(e.getClass().getName(), e.getMessage(), e);
					return;
				}
			} else { //else if (staticClassName.equals(AGENT_IDENTIFIER)) {
				iconResId = R.drawable.ic_launcher_nob;
			}
			
			FeedViewHolder fvh = (FeedViewHolder) view.getTag();
			fvh.mIcon.setImageResource(iconResId);
			
			fvh.mTop.setVisibility((c.isFirst())?(View.VISIBLE):(View.GONE));
			fvh.mBottom.setVisibility((c.isLast())?(View.VISIBLE):(View.GONE));

			
            String lastUsedText = (String) DateUtils.getRelativeDateTimeString(context, executedAt,
                    DateUtils.SECOND_IN_MILLIS, DateUtils.DAY_IN_MILLIS, 0);
			fvh.mDate.setText(lastUsedText);
			
			fvh.mMessage.setText(reason);
			if ((stats != null) && (! stats.equals(""))){
				fvh.mStats.setVisibility(View.VISIBLE);
				fvh.mStats.setText(stats);
			} else {
				fvh.mStats.setVisibility(View.GONE);
			}
		}
	}
	        

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new FeedCursorLoader(getActivity(), mAgentClassName);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    	Logger.i("FeedFragment.onLoadFinished() cursor size: " + data.getCount());
    	showData(data);
    }
    public void onLoaderReset(Loader<Cursor> loader) {
    	Logger.i("FeedFragment.onLoaderReset() called");
    	mAdapter.swapCursor(null);
    }
  
    private void showData(Cursor data) {
        if ((data != null) && (data.getCount() > 0)) {
            mAdapter.swapCursor(data);
            mContent.setVisibility(View.VISIBLE);
            mEmpty.setVisibility(View.GONE);
        } else {
            mAdapter.swapCursor(null);
            mContent.setVisibility(View.GONE);
            mEmpty.setVisibility(View.VISIBLE);
        }
    }
    


	public class FeedFragmentAgentListener implements AgentStateListener {

		@Override
		public void agentEnabled() {
			refreshView();
		}

		@Override
		public void agentDisabled() {
			refreshView();
		}

		@Override
		public void agentPaused() {
			refreshView();
		}

		@Override
		public void agentStarted() {
			refreshView();
		}
		
		@Override
		public void agentFinished() {
			refreshView();
		}

	}


}
