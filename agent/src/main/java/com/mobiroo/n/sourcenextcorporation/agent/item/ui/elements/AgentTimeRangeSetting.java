package com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentTimeRange;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.activity.choosers.AgentTimeRangeChooserActivity;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.SettingSaver;

import java.util.List;

public class AgentTimeRangeSetting extends AgentUIElement implements
		SettingSaver {

	protected String mTimeRangesString;
	protected int mDescriptionResource;
	protected LinearLayout mTimeRangesContainer;

	public AgentTimeRangeSetting(AgentConfigurationProvider aca,
			String prefName, String timeRangeString, int description) {
		mDescriptionResource = description;
		mPrefName = prefName;
		mAgentConfigure = aca;
		mTimeRangesString = timeRangeString;
	}

	@Override
	public View getView(Context context) {
		View v = View.inflate(context, R.layout.list_item_time_range_picker,
				null);
		((TextView) v.findViewById(R.id.range_description))
				.setText(mDescriptionResource);

		mTimeRangesContainer = (LinearLayout) v
				.findViewById(R.id.time_ranges_container);

		mTimeRangesContainer.removeAllViews();
		addTimeRangesToView(context, mTimeRangesContainer, mTimeRangesString);

		return v;
	}

	protected void addTimeRangesToView(Context context, LinearLayout view,
			String input) {
		List<AgentTimeRange> ranges = AgentTimeRange.getTimeRanges(input);

        //addDaysOfWeekToView(context, view);

		for (AgentTimeRange range : ranges) {
			addTimeRangeToView(context, view, range);
		}

		if (ranges.size() == 0) {
			addNoTimeRangesMessage(context, view);
		}

		addTimeEdit(context, view);

	}

	protected void addNoTimeRangesMessage(Context context, LinearLayout view) {
		View v = View.inflate(context, R.layout.list_item_text_plain, null);
		((TextView) v.findViewById(R.id.text))
				.setText(R.string.no_times_setting);

		view.addView(v);
	}

	protected void addTimeEdit(Context context, LinearLayout view) {
		View v = View.inflate(context, R.layout.list_item_text_plain_padded,
				null);

		((TextView) v.findViewById(R.id.text)).setText(R.string.edit_times);

        v.setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(
                                AgentTimeRangeSetting.this.mAgentConfigure
                                        .getActivity(),
                                AgentTimeRangeChooserActivity.class);
                        intent.putExtra(
                                AgentTimeRangeChooserActivity.TIME_RANGES,
                                mTimeRangesString);
                        AgentTimeRangeSetting.this.mAgentConfigure
                                .startUpdateSettingActivityForResult(intent,
                                        getActivityResultKey());
                    }

                });

		view.addView(v);
	}

    protected void addDaysOfWeekToView(Context context, LinearLayout view) {
        View v = View.inflate(context, R.layout.list_item_day_of_week_picker, null);
        view.addView(v);

        setupTextView(context, (TextView) v.findViewById(R.id.sunday));
        setupTextView(context, (TextView) v.findViewById(R.id.monday));
        setupTextView(context, (TextView) v.findViewById(R.id.tuesday));
        setupTextView(context, (TextView) v.findViewById(R.id.wednesday));
        setupTextView(context, (TextView) v.findViewById(R.id.thursday));
        setupTextView(context, (TextView) v.findViewById(R.id.friday));
        setupTextView(context, (TextView) v.findViewById(R.id.saturday));

    }

    private void setupTextView(Context context, TextView v) {
        //v.setOnClickListener(dayClicked);
        v.setTextColor(context.getResources().getColor(R.color.time_unselected));
        v.setTypeface(null, Typeface.NORMAL);
        v.setBackgroundResource(R.drawable.item_multiselect_unselected_clickable);

        //mDayViews.add(v);
    }

	protected void addTimeRangeToView(Context context, LinearLayout view,
			AgentTimeRange range) {
		View v = View
				.inflate(context, R.layout.list_item_time_range_item, null);

		String[] rangeStrings = range.getDisplayStringRange(context);
		((TextView) v.findViewById(R.id.range_start_value))
				.setText(rangeStrings[0]);
		((TextView) v.findViewById(R.id.range_start_value))
				.setTypeface(Typeface.MONOSPACE);
		((TextView) v.findViewById(R.id.range_end_value))
				.setText(rangeStrings[1]);
		((TextView) v.findViewById(R.id.range_end_value))
				.setTypeface(Typeface.MONOSPACE);

		view.addView(v);
	}

	
	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public SettingType getType() {
		return SettingType.TIME_RANGE_GROUP;
	}

	@Override
	public void disableElement() {
		// TODO add disable / enable support
	}

	@Override
	public void enableElement() {
		// TODO add disable / enable support
	}

	@Override
	public int getName() {
		return R.string.time_ranges_item;
	}

	@Override
	public int getActivityResultKey() {
		return mPrefName.hashCode() & 32767;
	}

	@Override
	public void saveSetting(Intent data) {
		mTimeRangesString = data.getExtras().getString(
				AgentTimeRangeChooserActivity.TIME_RANGES);
		mTimeRangesContainer.removeAllViews();
		addTimeRangesToView(mAgentConfigure.getActivity(),
				mTimeRangesContainer, mTimeRangesString);
		mAgentConfigure.updateSetting(mPrefName, mTimeRangesString);
	}
}
