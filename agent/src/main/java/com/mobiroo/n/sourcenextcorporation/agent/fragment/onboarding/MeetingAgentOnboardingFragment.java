package com.mobiroo.n.sourcenextcorporation.agent.fragment.onboarding;

import android.Manifest;
import android.accounts.Account;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.item.MeetingAgent;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.util.UserCalendarUtils;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.item.onboarding.ConfigurationDataInterface;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.Usage;

import java.util.ArrayList;

/**
 * Created by krohnjw on 4/23/2014.
 */
public class MeetingAgentOnboardingFragment extends BaseOnboardingFragment {

    public static class MeetingAgentConfigurationData implements ConfigurationDataInterface {

        @Override
        public String[] getPrefs() {
            return calendarsToSend.toArray(new String[calendarsToSend.size()]);
        }
    }

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_CALENDAR, Manifest.permission.GET_ACCOUNTS
    };

    private static ListView mList;
    private static ArrayList<String> calendarsToSend;
    private Calendar[] mCalendarList;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((LinearLayout) view.findViewById(R.id.onboarding_content)).addView(View.inflate(getActivity(), R.layout.fragment_onboarding_meeting, null));
        ((TextView) view.findViewById(R.id.intro)).setText(Html.fromHtml(buildIntroString(R.string.onboarding_meeting_intro)));
        view.findViewById(R.id.intro).setOnClickListener(this);
        calendarsToSend = new ArrayList<String>();

        // Check for permissions before retrieving calendars
        if(Utils.isPermissionGranted(getActivity(), Manifest.permission.READ_CALENDAR)
                && Utils.isPermissionGranted(getActivity(), Manifest.permission.GET_ACCOUNTS)) {
            buildCalendarList();
        } else {
            Utils.requestPermissions(getActivity(),
                    new String[] { Manifest.permission.READ_CALENDAR, Manifest.permission.GET_ACCOUNTS },
                    Constants.PERMISSIONS_REQUEST_READ_CALENDAR);
        }
    }

    @Override
    protected String getLeftButtonText() {
        return String.format(getString(R.string.onboarding_process), getString(R.string.meeting_step), getString(R.string.onboarding_total_steps));
    }

    @Override
    protected String getRightButtonText() {
        return getString(R.string.next);
    }

    @Override
    protected int getRightButtonBackground() {
        return R.drawable.onboarding_button_solid_clickable;
    }

    @Override
    protected int getLeftButtonBackground() {
        return R.drawable.onboarding_transparent;
    }

    @Override
    protected boolean isLeftButtonClickable() {
        return false;
    }

    @Override
    protected boolean isRightButtonClickable() {
        return true;
    }

    @Override
    protected int getRightButtonTextColor() {
        return Color.WHITE;
    }

    @Override
    protected int getLeftButtonTextColor() {
        return R.color.onboarding_sub_text;
    }

    @Override
    protected void leftClick() {
        /* Do nothing */
    }

    @Override
    protected void rightClick() {
        mCallback.updateConfiguration(MeetingAgent.HARDCODED_GUID, new MeetingAgentConfigurationData());
        super.rightClick();
    }

    @Override
    protected void showHelp() {
        Usage.logEvent(getActivity(), Usage.Events.ONBOARDING_MEETING_LEARN_MORE, false);
        showHelp(getString(R.string.onboarding_meeting_help));
    }

    private void buildCalendarList() {
        mCalendars = new ArrayList<Calendar>();

        Account[] accounts = MeetingAgent.getAccounts(getActivity());
        for (int i = 0; i < accounts.length; i++) {
            Account a = accounts[i];
            MeetingAgent.SubCalendar[] calendars = UserCalendarUtils.getCalendarsForAccount(getActivity(), accounts[i]);
            mCalendars.add(new CalendarTitle(getActivity(), a.name));
            for (MeetingAgent.SubCalendar c : calendars) {
                SubCalendar s = new SubCalendar(getActivity(), c.name, a.name, c.id);
                mCalendars.add(s);
                calendarsToSend.add(s.getPref()  + AgentPreferences.STRING_SPLIT + "false");
            }
        }

        mList = (ListView) getView().findViewById(R.id.calendar_list);
        mCalendarList = mCalendars.toArray(new Calendar[mCalendars.size()]);
        mList.setAdapter(new CalendarAdapter(getActivity(), mCalendarList));
        mList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                SparseBooleanArray list = mList.getCheckedItemPositions();
                boolean checked = list.get(position);
                Calendar item = (Calendar) adapterView.getAdapter().getItem(position);
                if (item instanceof SubCalendar) {
                    SubCalendar c = (SubCalendar) adapterView.getAdapter().getItem(position);
                    c.selected = checked;
                    if (!c.isMain) {
                        Usage.logEvent(getActivity(), Usage.Events.ONBOARDING_CALENDAR_SELECTED, false);
                    }
                    view.findViewById(R.id.check).setBackgroundColor(getResources().getColor(checked ? R.color.onboarding_check_checked : R.color.onboarding_check_unchecked));
                    calendarsToSend.remove(c.getPref() + AgentPreferences.STRING_SPLIT + !checked);
                    calendarsToSend.add(c.getPref() + AgentPreferences.STRING_SPLIT + checked);
                }
            }
        });

        for (int i =0; i < mCalendarList.length; i++) {
            Calendar c = mCalendarList[i];
            if (c instanceof SubCalendar) {
                if (c.toString().equals(getString(R.string.main_calendar))) {
                    ((SubCalendar) c).selected = true;
                    mList.setItemChecked(i, true);
                }
            }
        }

    }

    private ArrayList<Calendar> mCalendars;

    private interface Calendar {
        public View getView(int position, View convertView, ViewGroup parent);
    }

    private class SubCalendar implements Calendar {
        private Context context;
        public String name; // Calendar name
        public String account; // Account name
        public String id; // Calendar id
        public String pref;
        private boolean isMain;
        public boolean selected;

        public SubCalendar(Context context, String name, String account, String id) {
            this.context = context;
            this.name = name;
            this.account = account;
            this.id = id;
            pref = AgentPreferences.MEETING_ACCOUNTS + account + "_" + id;
            isMain = this.account.equals(this.name);
            selected = false; // isMain;
        }

        @Override
        public String toString() {
            return (isMain) ? getActivity().getString(R.string.main_calendar) : name;
        }

        public String getPref() {
            return MeetingAgent.HARDCODED_GUID + AgentPreferences.STRING_SPLIT + pref;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = View.inflate(context, R.layout.onboarding_calendar_list_item, null);

            TextView name = (TextView) convertView.findViewById(R.id.name);

            name.setText(toString());
            ((ImageView) convertView.findViewById(R.id.check)).setBackgroundResource(selected
                ? R.drawable.onboarding_check_checked
                : R.drawable.onboarding_check_unchecked);

            return convertView;
        }
    }


    private class CalendarTitle implements Calendar {
        public String name;
        private Context context;

        public CalendarTitle(Context context, String name) {
            this.context = context;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = View.inflate(context, R.layout.list_item_onboarding_calendar_account, null);

            ((TextView) convertView.findViewById(R.id.text)).setText(name);

            return convertView;
        }

        public boolean isEnabled() {
            return false;
        }
    }

    private class CalendarAdapter extends ArrayAdapter<Calendar> {

        public CalendarAdapter(Context context, Calendar[] objects) {
            super(context, R.layout.onboarding_calendar_list_item, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getItem(position).getView(position, convertView, parent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case Constants.PERMISSIONS_REQUEST_READ_CALENDAR: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {

                    for(int grantResult : grantResults) {
                        if(grantResult != PackageManager.PERMISSION_GRANTED) {
                            // permission denied
                            return;
                        }
                    }

                    // All permissions granted
                    buildCalendarList();
                }
            }

        }

    }
}
