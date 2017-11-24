package com.mobiroo.n.sourcenextcorporation.agent.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.compat.AppIssue;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.activity.MainActivity;
import com.mobiroo.n.sourcenextcorporation.agent.billing.IabClient;
import com.mobiroo.n.sourcenextcorporation.agent.compat.AppIssueDetector;
import com.mobiroo.n.sourcenextcorporation.agent.compat.AppIssueDialogFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.receiver.AgentStatusChangedReceiver;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.tasks.AgentTaskCollection;
import com.mobiroo.n.sourcenextcorporation.agent.util.tasks.AgentTasksHelper;

import java.util.ArrayList;
import java.util.List;


public class AgentsFragment extends Fragment {

    public static final int DEFAULT_LIMIT = 20;

    protected int mLimit = DEFAULT_LIMIT;

    protected ArrayList<AgentDisplay> mAgentDisplayList;

    protected LinearLayout mLoading;
    protected ScrollView mContent;
    protected LinearLayout mAgentsList;
    protected LinearLayout mAgentWarnings;

    protected AgentTaskCollection mTaskCollection;

    @Override
    public void onResume() {
        super.onResume();

        getActivity().registerReceiver(mAgentChangedReceiver, AgentStatusChangedReceiver.getFilterAllActions());

        mLoading.setVisibility(View.VISIBLE);
        mContent.setVisibility(View.GONE);
        mAgentsList.setVisibility(View.GONE);

        new AgentsCursorTask(getActivity()).execute();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTaskCollection.cancelTasks();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mAgentChangedReceiver);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTaskCollection = new AgentTaskCollection();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_agents, container,
                false);

        mLoading = (LinearLayout) view.findViewById(R.id.loading);
        mContent = (ScrollView) view.findViewById(R.id.content_scroll);
        mAgentsList = (LinearLayout) view.findViewById(R.id.agentsList);
        mAgentWarnings = (LinearLayout) view.findViewById(R.id.agentsWarning);

        AppIssue[] issues = AppIssueDetector.getIssues(getActivity());

        if (issues.length == 0) {
            mAgentWarnings.setVisibility(View.GONE);
        } else {
            for (final AppIssue issue : issues) {
                View v = inflater.inflate(R.layout.list_item_issue, null);
                v.findViewById(R.id.container).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        AppIssueDialogFactory.getKeepSaveDialogForIssue(issue, getActivity(), new Runnable() {
                            @Override
                            public void run() {
                                v.setVisibility(View.GONE);
                                if (AppIssueDetector.getIssues(getActivity()).length == 0) {
                                    mAgentWarnings.setVisibility(View.GONE);
                                }
                            }

                        }).show();
                    }

                });

                TextView issueTitle = (TextView) v.findViewById(R.id.issue_title);
                issueTitle.setText(issue.getTitle());

                mAgentWarnings.addView(v);
            }
        }

        return view;
    }

    protected void statusLongClicked(final String agentGuid) {
        if (!IabClient.checkLocalUnlockOrTrial(getActivity())) {
            return;
        }

        Agent agent = AgentFactory.getAgentFromGuid(getActivity(),
                agentGuid);
        if (!agent.isInstalled()) {
            return;
        }

        if (agent.isActive()) {
            AlertDialog.Builder bld =
                    new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.agent_confirm_manual_stop)
                            .setTitle(R.string.agent_confirm)
                            .setIcon(R.drawable.ic_launcher)
                            .setPositiveButton(R.string.btn_stop,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int whichButton) {
                                            AgentTasksHelper.manualActivateDeactivate(getActivity(), agentGuid, false, true, mTaskCollection);
                                        }
                                    }).setNegativeButton(android.R.string.no, null);

            bld.show();
            return;
        }

        if (agent.isPaused()) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.agent_confirm_manual_unpause)
                    .setTitle(R.string.agent_confirm)
                    .setIcon(R.drawable.ic_launcher)
                    .setPositiveButton(R.string.yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                    AgentTasksHelper.manualActivateDeactivate(getActivity(), agentGuid, true, true, mTaskCollection);
                                }
                            }).setNegativeButton(android.R.string.no, null)
                    .show();
            return;
        }

        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.agent_confirm_manual_start)
                .setTitle(R.string.agent_confirm)
                .setIcon(R.drawable.ic_launcher)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                AgentTasksHelper.manualActivateDeactivate(getActivity(), agentGuid, true, false, mTaskCollection);
                            }
                        }).setNegativeButton(android.R.string.no, null)
                .show();

    }

    public final class AgentsCursorTask extends
            AsyncTask<String, Integer, List<Agent>> {
        protected int mAgentsLimit;
        protected Context mContext;

        public AgentsCursorTask(Context context) {
            this(context, DEFAULT_LIMIT);
        }

        public AgentsCursorTask(Context context, int limit) {
            mContext = context;
            mAgentsLimit = limit;
        }

        @Override
        protected void onPreExecute() {
            mTaskCollection.addTask(this);
        }

        @Override
        protected List<Agent> doInBackground(String... params) {
            Logger.d("AgentsFragment: Loading agents from db");
            List<Agent> agents = AgentFactory.getAllAgents(mContext);
            return agents;
        }

        @Override
        protected void onPostExecute(List<Agent> result) {
            if (this.isCancelled()) return;

            AgentsFragment.this.updateAgentsView(result);
            mTaskCollection.completeTask(this);
        }

    }

    private AgentStatusChangedReceiver mAgentChangedReceiver = new AgentStatusChangedReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            new AgentsCursorTask(getActivity()).execute();
        }

    };

    protected void updateAgentsView(List<Agent> agents) {
        if (agents == null) {
            Logger.e("in updateAgentsView result should never be null.");
            return;
        }

        Context c = getActivity();

        if (c == null) {
            Logger.e("in updateAgentsView c should never be null.");
            return;
        }

        mAgentDisplayList = new ArrayList<AgentDisplay>();

        mAgentsList.removeAllViews();

        for (Agent agent : agents) {
            AgentDisplay ad = new AgentDisplay();

//            if (agent!=null && agent.isInstalled()) {
//                ad.mIcon = agent.getColorIconId();
//                ad.mEnabled = true;
//            } else {
//                ad.mEnabled = false;
//                ad.mIcon = agent.getIconId();
//            }

            ad.mIcon = agent.getColorIconId();
            ad.mEnabled = true;

            ad.mConfigActivity = agent.getConfigActivity();
            ad.mAgentGuid = agent.getGuid();
            ad.mName = agent.getName();
            ad.setStatus();

            mAgentDisplayList.add(ad);
            mAgentsList.addView(ad.getView(getActivity(), mAgentsList));
        }

        mLoading.setVisibility(View.GONE);
        mContent.setVisibility(View.VISIBLE);
        mAgentsList.setVisibility(View.VISIBLE);
    }

    public class AgentDisplay {
        // information
        public String mName;
        public String mAgentGuid;
        public boolean mEnabled;
        public int mIcon;
        public String mStatus;
        public LinearLayout mAgentDisplay;
        public int mBackgroundResource;
        public int mAgentBoxBackgroundResource;
        public int mStatusColor;

        public Class<?> mConfigActivity;

        protected boolean mShowStatus;

        // view elements
        public ImageView mViewIcon;
        public TextView mViewName;
        public TextView mViewRegularStatus;
        public LinearLayout mViewRegularStatusBar;
        public LinearLayout mViewDetails;
        public LinearLayout mViewBackgroundBox;

        public void setStatus() {
            mShowStatus = true;
            Agent agent = AgentFactory.getAgentFromGuid(getActivity(), mAgentGuid);

            mStatusColor = R.color.TextColor;

            if (mEnabled) {
                mIcon = agent.getColorIconId();
                String formatText;
                mAgentBoxBackgroundResource = R.drawable.agent_box_clickable;

                if (agent.isActive()) { // is active
                    formatText = getResources().getString(
                            R.string.agent_status_activated);

                    mBackgroundResource = R.drawable.agent_status_started;
                    mStatusColor = R.color.status_started;
                } else if (agent.isPaused() && (agent.needsUIPause())) {
                    formatText = getResources().getString(
                            R.string.agent_status_paused);
                    mBackgroundResource = R.drawable.agent_status_paused;
                    mStatusColor = R.color.status_paused;
                } else {
                    mShowStatus = false;
                    formatText = getResources().getString(
                            R.string.agent_status_installed);
                    mBackgroundResource = R.drawable.agent_status_enabled;
                    mStatusColor = R.color.status_enabled;
                }


                mStatus = formatText;
            } else {
                mIcon = agent.getIconId();
                mAgentBoxBackgroundResource = R.drawable.agent_box_disabled_clickable;
                mStatus = getResources().getString(R.string.agent_not_enabled);
                mBackgroundResource = R.drawable.agent_status_disabled;
                mStatusColor = R.color.status_disabled;
            }
        }

        public void updateView() {
            mViewIcon.setImageResource(mIcon);
            mViewName.setText(mName);
            mViewRegularStatusBar.setVisibility(mShowStatus ? View.VISIBLE : View.GONE);
            mViewRegularStatus.setText(mStatus);
            mViewRegularStatus.setTextColor(getResources().getColor(mStatusColor));
            mViewRegularStatusBar.setBackgroundResource(mBackgroundResource);
            mViewBackgroundBox.setBackgroundResource(mAgentBoxBackgroundResource);
        }


        public View getView(Context context, View parent) {
            View view = View.inflate(context, R.layout.list_item_agent_base, null);

            mViewIcon = (ImageView) view.findViewById(R.id.icon);
            mViewName = (TextView) view.findViewById(R.id.name);
            mViewRegularStatus = (TextView) view.findViewById(R.id.regular_status);
            mViewRegularStatusBar = (LinearLayout) view.findViewById(R.id.regular_status_bar);

            mViewDetails = (LinearLayout) view.findViewById(R.id.container);
            mViewBackgroundBox = (LinearLayout) view.findViewById(R.id.backgroundBox);

            mViewBackgroundBox.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (IabClient.checkLocalUnlockOrTrial(getActivity())) {
                        Intent intent = new Intent(getActivity(), mConfigActivity);
                        intent.putExtra(MainActivity.EXTRA_AGENT_GUID, mAgentGuid);
                        getActivity().startActivityForResult(intent,
                                MainActivity.REQUEST_AGENT_CONFIG);
                    }
                    return;
                }
            });

            mViewRegularStatusBar.setOnClickListener(new OnClickListener() {
                @SuppressLint("NewApi")
                @Override
                public void onClick(View v) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                        mViewBackgroundBox.callOnClick();
                    } else {
                        mViewBackgroundBox.performClick();
                    }
                }
            });

            mViewBackgroundBox.setLongClickable(true);
            mViewBackgroundBox.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    statusLongClicked(mAgentGuid);
                    return true;
                }

            });

            mViewRegularStatusBar.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    statusLongClicked(mAgentGuid);
                    return true;
                }

            });

            updateView();

            return view;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MainActivity.REQUEST_AGENT_CONFIG:
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case Constants.PERMISSIONS_REQUEST_MEETING:
            case Constants.PERMISSIONS_REQUEST_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {

                    for(int grantResult : grantResults) {
                        if(grantResult != PackageManager.PERMISSION_GRANTED) {
                            // permission denied
                            return;
                        }
                    }

                    // All permissions granted
//                    mSelectedAd.openAgent();
                }

                break;
        }
    }
}
