package com.mobiroo.n.sourcenextcorporation.agent.util;


import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;

public class WifiWrapper {
    public String name;
    public String ssid;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ssid == null) ? 0 : ssid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WifiWrapper other = (WifiWrapper) obj;
        if (ssid == null) {
            if (other.ssid != null)
                return false;
        } else if (!ssid.equals(other.ssid)) {
            return false;
        }

        return true;
    }

    public WifiWrapper(String serialized) {
        String[] values = serialized.split(AgentPreferences.STRING_SPLIT);
        try {
            name = values[0];
            ssid = values[1];
        } catch (Exception e) {
            Logger.d("Exception loading WifiWrapper with incoming data " + serialized);
        }
    }

    public WifiWrapper() {
    }

    @Override
    public String toString() {
        return name + AgentPreferences.STRING_SPLIT + ssid;
    }
}
