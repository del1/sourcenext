package com.mobiroo.n.sourcenextcorporation.agent.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.util.tasks.AgentTaskCollection;

import java.util.List;


public class AgentToggleWidgetConfigureActivity extends Activity {
	int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	ListView mAgentListView;
    protected AgentTaskCollection mTaskCollection;

	private class ViewHolder {
		public TextView nameView;
		public ImageView iconView;
		public String agentGuid;
	}

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTaskCollection.cancelTasks();
    }

    @Override
    public void onCreate(Bundle bundle) {
    	super.onCreate(bundle);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

        mTaskCollection = new AgentTaskCollection();

    	setContentView(R.layout.app_widget_configure);
    	mAgentListView = (ListView) findViewById(R.id.agent_list_view);
    	mAgentListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				ViewHolder holder = (ViewHolder) view.getTag();
				Context context = AgentToggleWidgetConfigureActivity.this;
				AgentToggleWidgetProvider.setAgentGuidForWidgetId(context, holder.agentGuid, mAppWidgetId);
				
				AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
	            AgentToggleWidgetProvider.updateAppWidgets(context, appWidgetManager, mAppWidgetId);
	            
	            Intent resultValue = new Intent();
	            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
	            setResult(RESULT_OK, resultValue);
	            finish();
			}
    		
    	});
    	
    	Intent intent = getIntent();
    	Bundle extras = intent.getExtras();
    	if (extras != null) {
    	    mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    	}
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
        
        new LoadAgentsTask().execute();
    }
    
    
	private class LoadAgentsTask extends AsyncTask<Void, Void, List<Agent>> {

        @Override
        protected void onPreExecute() {
            mTaskCollection.addTask(this);
        }

		@Override
		protected List<Agent> doInBackground(Void... params) {
			List<Agent> installedAgents = AgentFactory.getInstalledAgents(AgentToggleWidgetConfigureActivity.this);
			return installedAgents;
		}

		@Override
		protected void onPostExecute(List<Agent> results) {
			super.onPostExecute(results);
            if(isCancelled())
                return;

			AgentArrayAdapter agentArrayAdapter = new AgentArrayAdapter(AgentToggleWidgetConfigureActivity.this, R.id.agent_list_view, results);
			mAgentListView.setAdapter(agentArrayAdapter);
		}
	}
	
	private class AgentArrayAdapter extends ArrayAdapter<Agent> {
		private List<Agent> mResults;
		private Context mContext;

		public AgentArrayAdapter(Context context, int resource, List<Agent> objects) {
			super(context, resource, objects);
			mResults = objects;
			mContext = context;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
			
			if (convertView == null) {
				LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = li.inflate(R.layout.widget_agent_toggle_configure_row, null);
				holder = new ViewHolder();
				holder.nameView = (TextView) convertView.findViewById(R.id.agent_name);
				holder.iconView = (ImageView) convertView.findViewById(R.id.agent_icon);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			Agent agent = mResults.get(position);
            holder.nameView.setText(agent.getName());
            holder.iconView.setImageResource(agent.getColorIconId());
            holder.agentGuid = agent.getGuid();
            
	        return convertView;
	    }
		
	}

}
