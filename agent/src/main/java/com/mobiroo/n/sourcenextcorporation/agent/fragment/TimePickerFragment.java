package com.mobiroo.n.sourcenextcorporation.agent.fragment;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;
import android.widget.Toast;

import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.R;

public class TimePickerFragment extends DialogFragment implements
		TimePickerDialog.OnTimeSetListener {

	String mTimeString;
	
	public TimePickerFragment() {
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the current time as the default values for the picker
		int[] time = Utils.createTimeOfDayFromString(getArguments().getString("time"));
		int hour = time[0];
		int minute = time[1];

		mTimeString = null;
		
		// Create a new instance of TimePickerDialog and return it
		TimePickerDialog tp = new TimePickerDialog(getActivity(), this, hour, minute,
				DateFormat.is24HourFormat(getActivity()));
		return tp;
	}

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		mTimeString = Utils.createTimeOfDayString(hourOfDay, minute);
	}
	
	public static interface OnCompleteListener {
	    public abstract void onComplete(String time);
	}

	private OnCompleteListener mListener;

	public void setOnCompleteListener(OnCompleteListener listener) {
		mListener = listener;
	}
	
	public void onPause() {
		super.onPause();
		if(mTimeString != null) {
			if(this.mListener != null) {
				this.mListener.onComplete(mTimeString);
			} else {
				Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.save_error), Toast.LENGTH_SHORT).show();
			}
		}
	}
}