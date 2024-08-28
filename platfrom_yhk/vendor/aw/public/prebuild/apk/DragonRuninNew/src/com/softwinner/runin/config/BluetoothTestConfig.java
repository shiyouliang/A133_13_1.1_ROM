package com.softwinner.runin.config;

import com.softwinner.runin.Settings;
import com.softwinner.runin.config.interfaces.IConfiguration;

/**
 * @author zengsc
 * @version date 2013-6-7
 */
public class BluetoothTestConfig implements IConfiguration {
	private final String NAME = "BluetoothTest";
	private final String[] ATTRS = { Settings.ATTR_OPEN_DURATION, Settings.ATTR_CLOSE_DURATION };
	private final NodeProperties NODE = new NodeProperties() {

		@Override
		public boolean multi() {
			return true;
		}

		@Override
		public TYPE getType() {
			return IConfiguration.NodeProperties.TYPE.FOREGROUNDTEST;
		}

		@Override
		public String getName() {
			return Settings.NODE_WHILE;
		}
	};
	private final NodeProperties[] NODES = { NODE };

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String[] attrs() {
		return ATTRS;
	}

	@Override
	public NodeProperties[] nodes() {
		return NODES;
	}
}
