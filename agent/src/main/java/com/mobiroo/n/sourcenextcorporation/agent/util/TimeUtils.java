package com.mobiroo.n.sourcenextcorporation.agent.util;

import java.util.Date;

/**
 * Created by omarseyal on 5/5/14.
 */
public class TimeUtils {
    public static final long MINUTE = 60*1000;
    public static final long HOUR = 60*MINUTE;
    public static final long DAY = 24*HOUR;
    public static final long WEEK = 7*DAY;

    public static boolean isOlderThan(long timeIn, long compare) {
        long time = timeIn*1000;
        Date d = new Date();

        long diff = d.getTime() - time;

        if (diff <= compare) {
            return false;
        }

        return true;
    }

    public static String getShortTimeString(long timeIn) {
        long time = timeIn*1000;
        Date d = new Date();

        long diff = d.getTime() - time;

        if (diff < (MINUTE * 60)) {
            if (diff / MINUTE <= 1) {
                return "1m";
            } else {
                return "" + (diff / MINUTE) + "m";
            }
        }

        if (diff < (HOUR * 24)) {
            if (diff / HOUR <= 1) {
                return "1h";
            } else {
                return "" + (diff / HOUR) + "h";
            }
        }

        if (diff < (DAY * 7)) {
            if (diff / DAY <= 1) {
                return "1d";
            } else {
                return "" + (diff / DAY) + "d";
            }
        }

        if (diff <= WEEK) {
            return "1w";
        } else {
            return "" + (diff / WEEK) + "w";
        }
    }

}
