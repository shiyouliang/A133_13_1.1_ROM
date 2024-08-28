package com.softwinner.runin.config;

import com.softwinner.runin.Settings;
import com.softwinner.runin.config.interfaces.IConfiguration;

/**
 * @author zengsc
 * @version date 2013-6-7
 */
public class RebootTestConfig implements IConfiguration {
	private final String NAME = "RebootTest";
	private final String[] ATTRS = { Settings.ATTR_REPEAT_COUNT };
	private final NodeProperties NODE = new NodeProperties() {

		@Override
		public boolean multi() {
			return false;
		}

		@Override
		public TYPE getType() {
			return IConfiguration.NodeProperties.TYPE.STRING;
		}

		@Override
		public String getName() {
			return Settings.NODE_REASON;
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
