package com.mobiroo.n.sourcenextcorporation.agent.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.TimePickerFragment;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.TimePickerFragment.OnCompleteListener;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentTimeRange;

public class AgentTimeRangeEditor extends Activity {

	public final static int MODE_ALL = 0;
	public final static int MODE_SINGLE = 1;
	
	public final static String TIME_ERROR = "error";
	public final static String TIME_RANGE = "range";
	public final static String TIME_ID = "id";
	public final static String MODE = "mode";

	String mSerializedRange;
	AgentTimeRange mAgentTimeRange;
	int mId;
	Spinner mStartDay, mEndDay;
	TextView mStartTime, mEndTime;
	
	int mMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_time_range_editor);

		mMode = getIntent().getIntExtra(MODE, MODE_SINGLE);
		
		if (savedInstanceState != null) {
			mSerializedRange = savedInstanceState.getString(TIME_RANGE);
			mId = savedInstanceState.getInt(TIME_ID);
		} else {
			if (getIntent().hasExtra(TIME_ID)) {
				mId = getIntent().getExtras().getInt(TIME_ID, -1);
				mSerializedRange = getIntent().getExtras().getString(
						TIME_RANGE, "");
			} else {
				mId = -1;
				mSerializedRange = AgentTimeRange
						.getDefaultSingleDayString(this);
			}
		}

		mAgentTimeRange = new AgentTimeRange();

		mAgentTimeRange.deserialize(mSerializedRange);

		mStartDay = (Spinner) findViewById(R.id.start_day);
		mEndDay = (Spinner) findViewById(R.id.end_day);

		if(mMode == MODE_ALL) {
			mStartDay.setVisibility(View.GONE);
			mEndDay.setVisibility(View.GONE);
		}

		String[] days = AgentTimeRange.getDayStringArray(this);

		ArrayAdapter<String> endDayArrayAadapter = new ArrayAdapter<String>(
				this, android.R.layout.simple_spinner_item, days);
		endDayArrayAadapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); 
		
		mStartDay.setAdapter(endDayArrayAadapter);
		mStartDay.setOnItemSelectedListener(mOnStartDaySelectedListener);

		ArrayAdapter<String> startDayArrayAadapter = new ArrayAdapter<String>(
				this, android.R.layout.simple_spinner_item, days);
		startDayArrayAadapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		mEndDay.setAdapter(startDayArrayAadapter);

		for (int i = 0; i < days.length; i++) {
			if (days[i].equals(mAgentTimeRange.getEndDayString())) {
				mEndDay.setSelection(i);
			}

			if (days[i].equals(mAgentTimeRange.getStartDayString())) {
				mStartDay.setSelection(i);
			}
		}

		mStartTime = (TextView) findViewById(R.id.start_time);
		mEndTime = (TextView) findViewById(R.id.end_time);

		mStartTime.setText(mAgentTimeRange.getDisplayStartTime(this));
		mEndTime.setText(mAgentTimeRange.getDisplayEndTime(this));

		mStartTime.setPaintFlags(mStartTime.getPaintFlags()
				| Paint.UNDERLINE_TEXT_FLAG);
		mEndTime.setPaintFlags(mEndTime.getPaintFlags()
				| Paint.UNDERLINE_TEXT_FLAG);

		mStartTime.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showTimeDialog(true);
			}

		});

		mEndTime.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showTimeDialog(false);
			}

		});

		findViewById(R.id.cancel_button).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						mAgentTimeRange.setStartDay((String) mStartDay
								.getSelectedItem());
						mAgentTimeRange.setEndDay((String) mEndDay
								.getSelectedItem());
						Intent returnData = new Intent();
						returnData.putExtra(TIME_RANGE,
								mAgentTimeRange.serialize());
						returnData.putExtra(TIME_ID, mId);
						setResult(RESULT_CANCELED, returnData);
						AgentTimeRangeEditor.this.finish();
					}

				});

		findViewById(R.id.save_button).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						mAgentTimeRange.setStartDay((String) mStartDay
								.getSelectedItem());
						mAgentTimeRange.setEndDay((String) mEndDay
								.getSelectedItem());
						
						if ((mMode != MODE_ALL) && (mAgentTimeRange.getTimeLength() > (24 * 60))) {
							new AlertDialog.Builder(AgentTimeRangeEditor.this)
									.setIcon(R.drawable.ic_launcher)
									.setTitle(R.string.range_is_long_title)
									.setMessage(R.string.range_is_long_warning)
									.setPositiveButton(
											R.string.yes,
											new DialogInterface.OnClickListener() {
												@Override
												public void onClick(
														DialogInterface dialog,
														int which) {
													saveAndFinish();
												}
											})
									.setNegativeButton(
											R.string.no,
											new DialogInterface.OnClickListener() {
												@Override
												public void onClick(
														DialogInterface dialog,
														int which) {
													// do nothing
												}
											}).show();
						} else {
							saveAndFinish();
						}
					}
				});

	}

	protected void saveAndFinish() {
		Intent returnData = new Intent();
		returnData.putExtra(TIME_RANGE, mAgentTimeRange.serialize());
		returnData.putExtra(TIME_ID, mId);
		returnData.putExtra(MODE, mMode);

		setResult(RESULT_OK, returnData);

		AgentTimeRangeEditor.this.finish();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		mAgentTimeRange.setStartDay((String) mStartDay.getSelectedItem());
		mAgentTimeRange.setEndDay((String) mEndDay.getSelectedItem());

		outState.putString(TIME_RANGE, mAgentTimeRange.serialize());
		outState.putInt(TIME_ID, mId);
	}

	void showTimeDialog(boolean start) {
		TimePickerFragment newFragment = new TimePickerFragment();
		Bundle args = new Bundle();
		args.putString("time", (start) ? mAgentTimeRange.getStartTime()
				: mAgentTimeRange.getEndTime());
		newFragment.setArguments(args);
		newFragment.setOnCompleteListener((start) ? mStartTimeCompleteListener
				: mEndTimeCompleteListener);
		newFragment.show(getFragmentManager(), "timePicker");
	}

	OnItemSelectedListener mOnStartDaySelectedListener = new OnDaySelectedListener(
			false);

	public class OnDaySelectedListener implements OnItemSelectedListener {

		boolean mChangeStart;

		public OnDaySelectedListener(boolean changeStart) {
			mChangeStart = changeStart;
		}

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			int startIndex = mStartDay.getSelectedItemPosition();
			int endIndex = mEndDay.getSelectedItemPosition();
			int totalDays = AgentTimeRange.getDayStringArray(AgentTimeRangeEditor.this).length;

			if (startIndex == endIndex) // same item_multiselect_selected
			{
				boolean startTimeAfterStopTime = mAgentTimeRange
						.getStartTimeInMinutes() > mAgentTimeRange.getEndTimeInMinutes();

				if (!startTimeAfterStopTime)
					return;
			}

			if (endIndex == (startIndex + 1) % totalDays) // next item_multiselect_selected
				return;

			if (mChangeStart) {
				mStartDay.setSelection((endIndex - 1) % totalDays);
			} else {
				mEndDay.setSelection((startIndex + 1) % totalDays);
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
		}

	}

	OnCompleteListener mStartTimeCompleteListener = new StartTimeCompleteListener();
	OnCompleteListener mEndTimeCompleteListener = new EndTimeCompleteListener();

	public class StartTimeCompleteListener implements OnCompleteListener {
		@Override
		public void onComplete(String time) {
			mAgentTimeRange.setStartTimeOfDay(time);
			mStartTime.setText(mAgentTimeRange
					.getDisplayStartTime(AgentTimeRangeEditor.this));
		}
	}

	public class EndTimeCompleteListener implements OnCompleteListener {
		@Override
		public void onComplete(String time) {
			mAgentTimeRange.setEndTimeOfDay(time);
			mEndTime.setText(mAgentTimeRange
					.getDisplayEndTime(AgentTimeRangeEditor.this));
		}
	}

}
