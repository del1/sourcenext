package com.mobiroo.n.sourcenextcorporation.agent.util;

import junit.framework.Assert;

import java.util.HashMap;

/**
 * Created by omarseyal on 4/5/14.
 */
public class HashedNumberDispenser {
    int value;
    HashMap<String, Integer> strings;
    boolean locked;

    public HashedNumberDispenser() {
        value = 0;
        strings = new HashMap<String, Integer>();
        locked = false;
    }

    public int generate(String key) {
        Assert.assertEquals(locked, false);

        if(key != null) {
            strings.put(key, new Integer(value));
        }

        int return_value = value;
        value += 1;
        return return_value;
    }

    public void lock() {
        locked = true;
    }

    public int fetch(String key) {
        if(!strings.containsKey(key))
            return -1;

        return strings.get(key).intValue();
    }
}
