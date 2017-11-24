package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;

import java.util.ArrayList;
import java.util.Calendar;

public class AgentDayOfWeekSetting extends AgentUIElement {

    private ArrayList<Integer>  mDaysSelected = new ArrayList<Integer>(0);
    private Context             mContext;
    
    private static ArrayList<Integer> mDefaultDays 
        = new ArrayList<Integer>() {
            private static final long serialVersionUID = -4115864455202667307L;
            {
                add(Calendar.MONDAY);
                add(Calendar.TUESDAY);
                add(Calendar.WEDNESDAY);
                add(Calendar.THURSDAY);
                add(Calendar.FRIDAY);
            }
        };
    
    public static enum Days {
        SUNDAY(Calendar.SUNDAY),
        MONDAY(Calendar.MONDAY),
        TUESDAY(Calendar.TUESDAY),
        WEDNESDAY(Calendar.WEDNESDAY),
        THURSDAY(Calendar.THURSDAY),
        FRIDAY(Calendar.FRIDAY),
        SATURDAY(Calendar.SATURDAY),
        UNDEFINED(-1);
        
        private int mValue;
        
        Days(int value) {
            mValue = value;
        }
        
        public int getValue() {
            return mValue;
        }
        
        @Override
        public String toString() {
            return String.valueOf(getValue());
        }
        
        public static Days getEnum(String value) throws NumberFormatException {
            int i = Integer.parseInt(value);
            return getEnum(i);
        }
        
        public static Days getEnum(int value) {
            for (Days v: values()) {
                if (value == v.getValue()) {
                    return v;
                }
            }
            return Days.UNDEFINED;
        }
        
        public static int getViewIdForDay(int value) {
            
            switch (value) {
                case Calendar.MONDAY:
                    return R.id.monday;
                case Calendar.TUESDAY:
                    return R.id.tuesday;
                case Calendar.WEDNESDAY:
                    return R.id.wednesday;
                case Calendar.THURSDAY:
                    return R.id.thursday;
                case Calendar.FRIDAY:
                    return R.id.friday;
                case Calendar.SATURDAY:
                    return R.id.saturday;
                case Calendar.SUNDAY:
                    return R.id.sunday;
                default:
                    return -1;
                    
            }
        }
    }
        
    public static ArrayList<Integer> getDefaultDays() {
        return mDefaultDays;
    }
    
    public AgentDayOfWeekSetting(AgentConfigurationProvider aca, String prefName, String days) {
    	mEnabled = true;
        mPrefName = prefName;
        mAgentConfigure = aca;
        setDaysSelected(days);
    }
    
    public AgentDayOfWeekSetting(AgentConfigurationProvider aca, String prefName, String days, ArrayList<Integer> defaultDays) {
    	mEnabled = true;
        mPrefName = prefName;
        mAgentConfigure = aca;
        mDefaultDays = defaultDays;
        setDaysSelected(days);
    }
    
    public AgentDayOfWeekSetting(AgentConfigurationProvider aca, String prefName, int[] days) {
    	mEnabled = true;
        mPrefName = prefName;
        mAgentConfigure = aca;
        setDaysSelected(days);
    }
    
    public AgentDayOfWeekSetting(AgentConfigurationProvider aca, String prefName, int[] days, ArrayList<Integer> defaultDays) {
    	mEnabled = true;
        mPrefName = prefName;
        mAgentConfigure = aca;
        mDefaultDays = defaultDays;
        setDaysSelected(days);
    }
    
    @Override
    public SettingType getType() {
        return SettingType.DAY_OF_WEEK;
    }
    
    @Override
    public int getName() {
        return R.string.days_of_week_description_calendar;
    }
    
    @Override
    public View getView(Context context) {
        mContext = context;
        View v = View.inflate(context, R.layout.list_item_day_of_week_picker, null);
        
        mDayViews = new ArrayList<TextView>();
        
        // Assign click listeners internally so they aren't scoped to the activity
        setupTextView(context, (TextView) v.findViewById(R.id.sunday));
        setupTextView(context, (TextView) v.findViewById(R.id.monday));
        setupTextView(context, (TextView) v.findViewById(R.id.tuesday));
        setupTextView(context, (TextView) v.findViewById(R.id.wednesday));
        setupTextView(context, (TextView) v.findViewById(R.id.thursday));
        setupTextView(context, (TextView) v.findViewById(R.id.friday));
        setupTextView(context, (TextView) v.findViewById(R.id.saturday));
        
        // Highlight selected days
        if (mDaysSelected.size() == 0) {
            mDaysSelected = mDefaultDays;
        }

        
        for (int day: mDaysSelected) {
            TextView t = (TextView)v.findViewById(Days.getViewIdForDay(day));
            if (t != null) {
            	t.setBackgroundResource(R.drawable.item_multiselect_selected_clickable);
                t.setTextColor(context.getResources().getColor(R.color.time_selected));
                t.setTypeface(null, Typeface.BOLD);
            } 

        }
        
        if(mEnabled) {
        	enable();
        } else {
        	disable();
        }
        
        return v;
    }

    protected ArrayList<TextView> mDayViews;
    
    protected void disableDayViews() {
        for(TextView dayView : mDayViews) {
        	dayView.setEnabled(false);
        	dayView.setTextColor(mContext.getResources().getColor(R.color.time_selected));
        	dayView.setTypeface(null, Typeface.BOLD);
        	dayView.setBackgroundResource(R.drawable.item_multiselect_disabled);
        }
    }

    protected void enableDayViews() {
        for(TextView dayView : mDayViews) {
        	int day = getDayFromId(dayView.getId());
        	dayView.setEnabled(true);
        	
            if (!mDaysSelected.contains(day)) {
                dayView.setTextColor(mContext.getResources().getColor(R.color.time_unselected));
                dayView.setTypeface(null, Typeface.NORMAL);
                dayView.setBackgroundResource(R.drawable.item_multiselect_unselected_clickable);
            } else {
                dayView.setTextColor(mContext.getResources().getColor(R.color.time_selected));
                dayView.setTypeface(null, Typeface.BOLD);
                dayView.setBackgroundResource(R.drawable.item_multiselect_selected_clickable);
            }
        }
    }

    
    private void setupTextView(Context context, TextView v) {
        v.setOnClickListener(dayClicked);
        v.setTextColor(context.getResources().getColor(R.color.time_unselected));
        v.setTypeface(null, Typeface.NORMAL);
        v.setBackgroundResource(R.drawable.item_multiselect_unselected_clickable);
        
        mDayViews.add(v);
    }
    
    private OnClickListener dayClicked = new OnClickListener() {
        @Override
        public void onClick(View v) {
            TextView t = (TextView) v;
            int day = getDayFromId(v.getId());
            if (mDaysSelected.contains(day)) {
                mDaysSelected.remove((Object) day);
                t.setTextColor(mContext.getResources().getColor(R.color.time_unselected));
                t.setTypeface(null, Typeface.NORMAL);
                t.setBackgroundResource(R.drawable.item_multiselect_unselected_clickable);
            } else {
                mDaysSelected.add(day);
                t.setTextColor(mContext.getResources().getColor(R.color.time_selected));
                t.setTypeface(null, Typeface.BOLD);
                t.setBackgroundResource(R.drawable.item_multiselect_selected_clickable);
            }
            
            mAgentConfigure.updateSetting(mPrefName, getDaysSelectedAsString());
        }

    };
    
    private int getDayFromId(int id) {
        switch (id) {
            case R.id.sunday:
                return Days.SUNDAY.getValue();
            case R.id.monday:
                return Days.MONDAY.getValue();
            case R.id.tuesday:
                return Days.TUESDAY.getValue();
            case R.id.wednesday:
                return Days.WEDNESDAY.getValue();
            case R.id.thursday:
                return Days.THURSDAY.getValue();
            case R.id.friday:
                return Days.FRIDAY.getValue();
            case R.id.saturday:
                return Days.SATURDAY.getValue();
            default:
                return Days.UNDEFINED.getValue();
        }
    }

    public ArrayList<Integer> getDaysSelected() {
        return mDaysSelected;
    }
    
    public String getDaysSelectedAsString() {
        return TextUtils.join(",", mDaysSelected);
    }
    
    public void setDaysSelected(String days) {
        if (days == null) {
            return;
        }
        
        
        String[] values = days.split(",");
        mDaysSelected = new ArrayList<Integer>(days.length());
        
        if (days.trim().isEmpty()) {
        	return;
        }
        
        for (String value: values) {
            mDaysSelected.add(Integer.parseInt(value));
        }
    }
    
    public void setDaysSelected(int[] days) {
        for (int value: days) {
            mDaysSelected.add(value);
        }
    }

    
    boolean mEnabled;
    
	@Override
	public void disableElement() {
		mEnabled = false;
		if(mDayViews != null) {
			disableDayViews();
		}
	}

	@Override
	public void enableElement() {
		mEnabled = true;
		if(mDayViews != null) {
			enableDayViews();
		}
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

}
