package com.mobiroo.n.sourcenextcorporation.agent.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.format.DateFormat;
import android.util.SparseArray;

import com.google.analytics.tracking.android.Log;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.item.SleepAgent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgentTimeRange implements Comparable<AgentTimeRange> {

	public static final int DEFAULT_RANGE = 1000;

	public static final String RANGE_DELIM = "-";
	public static final String DAY_DELIM = "|";

	private int startDayOfWeek;
	private String startTimeOfDay;

	private int endDayOfWeek;
	private String endTimeOfDay;

	public int getStartDay() {
		return startDayOfWeek;
	}

	public String getStartDayString() {
		return getIntegerToName().get(startDayOfWeek);
	}

	public int getEndDay() {
		return endDayOfWeek;
	}

	public String getEndDayString() {
		return getIntegerToName().get(endDayOfWeek);
	}

	
	public void setStartDay(String startDayOfWeek) {
		this.startDayOfWeek = getNameToInteger().get(startDayOfWeek);
	}

	public void setEndDay(String endDayOfWeek) {
		this.endDayOfWeek = getNameToInteger().get(endDayOfWeek);
	}

	public void setStartDay(int startDayOfWeek) {
		this.startDayOfWeek = startDayOfWeek;
	}

	public void setEndDay(int endDayOfWeek) {
		this.endDayOfWeek = endDayOfWeek;
	}
	

	public String getDisplayStartTime(Context context) {
		return getDisplayTime(startTimeOfDay, context);
	}

	public String getDisplayEndTime(Context context) {
		return getDisplayTime(endTimeOfDay, context);
	}

	public int getStartTimeInMinutes() {
		return getTimeInMinutes(startTimeOfDay);
	}

	public int getEndTimeInMinutes() {
		return getTimeInMinutes(endTimeOfDay);
	}

	public String getStartTime() {
		return startTimeOfDay;
	}

	public String getEndTime() {
		return endTimeOfDay;
	}

	
	public void setStartTimeOfDay(String startTimeOfDay) {
		this.startTimeOfDay = startTimeOfDay;
	}

	public void setEndTimeOfDay(String endTimeOfDay) {
		this.endTimeOfDay = endTimeOfDay;
	}

	

	@SuppressWarnings("deprecation")
	@SuppressLint("SimpleDateFormat")
	public int getTimeInMinutes(String time) {
		try {
			Date dateObj = (new SimpleDateFormat("H:mm")).parse(time);
			return dateObj.getHours() * 60 + dateObj.getMinutes();
		} catch (final ParseException e) {
			e.printStackTrace();
			Logger.d("getDisplayTime in locale time format failed for " + time);
			return -1;
		}
	}

	@SuppressLint("SimpleDateFormat")
	public String getDisplayTime(String time, Context context) {
		try {
			Date dateObj = (new SimpleDateFormat("H:mm")).parse(time);
			return DateFormat.getTimeFormat(context).format(dateObj);
		} catch (final ParseException e) {
			e.printStackTrace();
			Logger.d("getDisplayTime in locale time format failed for " + time);
		}

		try {
			Date dateObj = (new SimpleDateFormat("H:mm")).parse(time);
			return (new SimpleDateFormat("h:mm a")).format(dateObj);
		} catch (Exception e) {
			Logger.d("getDisplayTime in simple time format failed for " + time);
		}

		return "";
	}

	public String[] getDisplayStringRange(Context context) {
		String startDay = getIntegerToName().get(startDayOfWeek);
		String endDay = getIntegerToName().get(endDayOfWeek);

		String startTime = getDisplayTime(startTimeOfDay, context);
		String endTime = getDisplayTime(endTimeOfDay, context);

		String resultStringStart = startDay + " " + startTime;
		String resultStringEnd = endDay + " " + endTime;

		String[] resultArray = new String[2];
		resultArray[0] = resultStringStart;
		resultArray[1] = resultStringEnd;

		return resultArray;
	}

	public String serialize() {
		return "(" + startDayOfWeek + DAY_DELIM + startTimeOfDay + RANGE_DELIM
				+ endDayOfWeek + DAY_DELIM + endTimeOfDay + ")";
	}

	public boolean deserialize(String input) {
		input = input.replace("(", "");
		input = input.replace(")", "");
		String[] start_end = input.split(RANGE_DELIM);

		//Log.i("Deserializing: " + input);

		if (start_end.length != 2) {
			Log.e("AgentTimeRange string invalid: " + input + " // "
					+ start_end.length);
			return false;
		}

		String[] start = start_end[0].split("\\" + DAY_DELIM);
		String[] end = start_end[1].split("\\" + DAY_DELIM);

		if (start.length != 2) {
			Log.e("AgentTimeRange (start) string invalid: " + start_end[0]
					+ " // " + start.length);
			return false;
		}

		if (end.length != 2) {
			Log.e("AgentTimeRange (end) string invalid: " + start_end[1]
					+ " // " + end.length);
			return false;
		}

		startDayOfWeek = Integer.parseInt(start[0]);
		startTimeOfDay = start[1];

		endDayOfWeek = Integer.parseInt(end[0]);
		endTimeOfDay = end[1];

		return true;
	}

	public boolean nowInRange() {
		return nowInRange(DEFAULT_RANGE);
	}

	// toleranceMillis can be used to handle cases where alarm falls right on
	// time
	// 1000ms to be safe
	public boolean nowInRange(long toleranceMillis) {
		Calendar now = Calendar.getInstance();

		int nowDay = now.get(Calendar.DAY_OF_WEEK);
		long nowMillis = now.getTimeInMillis();

		long startMillis = Utils.getMillisFromTimeString(startTimeOfDay)
				- toleranceMillis;
		long endMillis = Utils.getMillisFromTimeString(endTimeOfDay)
				- toleranceMillis;

		if (startDayOfWeek <= endDayOfWeek) {
			if ((nowDay < startDayOfWeek) || (nowDay > endDayOfWeek)) {
				return false;
			}
		} else {
			if ((nowDay < startDayOfWeek) && (nowDay > endDayOfWeek)) {
				return false;
			}
		}

		// special case: if choosing almost whole week, e.g. Fri 10PM to Fri 9AM
		if ((nowDay == startDayOfWeek) && (startDayOfWeek == endDayOfWeek)
				&& (startMillis > endMillis)) {
			return ((nowMillis < endMillis) || (nowMillis > startMillis));
		}

		if ((nowDay == startDayOfWeek) && (nowMillis < startMillis)) {
			return false;
		}
		if ((nowDay == endDayOfWeek) && (nowMillis > endMillis)) {
			return false;
		}

		return true;
	}

	public Calendar[] getNextStartEndCalendar() {
		Calendar[] cals = new Calendar[2];
		long now = System.currentTimeMillis();

		cals[0] = Utils.getCalendarInstanceFromTimeString(startTimeOfDay);
		while (cals[0].get(Calendar.DAY_OF_WEEK) != startDayOfWeek) {
			cals[0].add(Calendar.DATE, 1);
		}
		if (cals[0].getTimeInMillis() < now) {
			cals[0].add(Calendar.DATE, 7);
		}

		cals[1] = Utils.getCalendarInstanceFromTimeString(endTimeOfDay);
		while (cals[1].get(Calendar.DAY_OF_WEEK) != endDayOfWeek) {
			cals[1].add(Calendar.DATE, 1);
		}
		if (cals[1].getTimeInMillis() < now) {
			cals[1].add(Calendar.DATE, 7);
		}

		return cals;
	}

	// utilities for deserializing lists of timeRanges
	public static List<AgentTimeRange> getTimeRanges(String input) {
		List<String> timeRangeStrings = getStringArrayOfTimeRanges(input);
		List<AgentTimeRange> allRanges = new ArrayList<AgentTimeRange>();

		if (timeRangeStrings.size() > 0) {
			for (String timeRangeString : timeRangeStrings) {
				AgentTimeRange newRange = new AgentTimeRange();
				newRange.deserialize(timeRangeString);

				allRanges.add(newRange);
			}
		}

		Collections.sort(allRanges);

		return allRanges;
	}

	protected static List<String> getStringArrayOfTimeRanges(String input) {
		List<String> allMatches = new ArrayList<String>();
		Matcher m = Pattern.compile("\\(([^\\)]*)\\)").matcher(input);

		while (m.find()) {
			String match = m.group();
			match.replace("(", "");
			match.replace(")", "");
			allMatches.add(match);
		}

		return allMatches;
	}

	// returns size of range in minutes
	public int getTimeLength() {
		int endTotalTime = getEndDay() * 24 * 60 + getEndTimeInMinutes();
		int startTotalTime = getStartDay() * 24 * 60 + getStartTimeInMinutes();

        Logger.i("end: " + getEndTimeInMinutes() + " // start:" + getStartTimeInMinutes());

		if (endTotalTime < startTotalTime) {
			endTotalTime += 7 * 24 * 60;
		}

		return endTotalTime - startTotalTime;
	}

	// static time of item_multiselect_selected methods
	public static String serializeList(List<AgentTimeRange> ranges) {
		String result = "";
		for (AgentTimeRange range : ranges) {
			result += range.serialize();
		}
		return result;
	}

	public static String getDefaultSingleDayString(Context context) {
		AgentTimeRange defaultRange = new AgentTimeRange();

		defaultRange.setStartDay(Calendar.SUNDAY);
		defaultRange.setEndDay(Calendar.MONDAY);
		defaultRange.setStartTimeOfDay(SleepAgent.DEFAULT_WEEKDAY_SLEEP_START_TIME);
		defaultRange.setEndTimeOfDay(SleepAgent.DEFAULT_WEEKDAY_SLEEP_END_TIME);

		return defaultRange.serialize();
	}

	// static utility methods

	protected static Map<String, Integer> nameToInteger;

	protected static Map<String, Integer> getNameToInteger() {
		if (nameToInteger != null)
			return nameToInteger;

		nameToInteger = Calendar.getInstance().getDisplayNames(
				Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault());
		return nameToInteger;
	}

	protected static SparseArray<String> integerToName;

	protected static SparseArray<String> getIntegerToName() {
		if (integerToName != null)
			return integerToName;

		integerToName = new SparseArray<String>();
		for (Entry<String, Integer> entry : getNameToInteger().entrySet()) {
			integerToName.put(entry.getValue(), entry.getKey());
		}
		return integerToName;
	}

	public static String[] getDayStringArray(Context context) {
		int day = Calendar.getInstance().getFirstDayOfWeek();
		String[] days = new String[getIntegerToName().size()];

		for (int i = 0; i < days.length; i++) {
			days[i] = getIntegerToName().get(day);
			day += 1;

			if (day > days.length) {
				day = 1;
			}
		}

		return days;
	}

	@Override
	public int compareTo(AgentTimeRange another) {
		Integer start = Integer.valueOf(this.getStartDay() * 24 * 60
				+ this.getStartTimeInMinutes());
		Integer end = Integer.valueOf(another.getStartDay() * 24 * 60
				+ another.getStartTimeInMinutes());

		return start.compareTo(end);
	}

}
