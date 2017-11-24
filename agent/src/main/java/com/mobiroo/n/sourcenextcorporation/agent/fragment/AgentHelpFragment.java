package com.mobiroo.n.sourcenextcorporation.agent.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.R;

public class AgentHelpFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help,  null);
        ((TextView) view.findViewById(R.id.help_drive_list)).setText(Html.fromHtml(getString(R.string.help_drive_list)));
        ((TextView) view.findViewById(R.id.help_meeting_list)).setText(Html.fromHtml(getString(R.string.help_meeting_list)));
        ((TextView) view.findViewById(R.id.help_battery_list)).setText(Html.fromHtml(getString(R.string.help_battery_list)));
        ((TextView) view.findViewById(R.id.help_sleep_list)).setText(Html.fromHtml(getString(R.string.help_sleep_list)));
        return view;
    }
}
