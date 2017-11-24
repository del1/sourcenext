package com.mobiroo.n.sourcenextcorporation.agent.util;


public class BluetoothWrapper {
	public String name;
	public String mac;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mac == null) ? 0 : mac.hashCode());
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
		BluetoothWrapper other = (BluetoothWrapper) obj;
		if (mac == null) {
			if (other.mac != null)
				return false;
		} else if (!mac.equals(other.mac))
			return false;
		return true;
	}

	public BluetoothWrapper(String serialized) {
		String[] values = serialized.split(AgentPreferences.STRING_SPLIT);
		name = values[0];
		mac = values[1];
	}
	
	public BluetoothWrapper() {
	}

	@Override
	public String toString() {
		return name + AgentPreferences.STRING_SPLIT + mac;
	}
}
