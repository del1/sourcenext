package com.mobiroo.n.sourcenextcorporation.agent.util;

/**
 * Created by Pritam Kadam on 27/06/2017.
 */
public class ValidatorUtility {

    /**
     * function to validate if the string is blank or not
     * @param str
     * @return
     */
    public static boolean isBlank(String str){
        return (str == null || str.equalsIgnoreCase(""));
    }

}
