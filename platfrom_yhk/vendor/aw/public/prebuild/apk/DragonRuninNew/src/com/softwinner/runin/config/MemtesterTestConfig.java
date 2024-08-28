package com.softwinner.runin.config;

import com.softwinner.runin.Settings;
import com.softwinner.runin.config.interfaces.IConfiguration;

/**
 * @author zengsc
 * @version date 2020--5-13
 */
public class MemtesterTestConfig implements IConfiguration {
	private final String NAME = "MemtesterTest";
	private final String[] ATTRS = { Settings.ATTR_REPEAT_COUNT,Settings.ATTR_MEMSIZE };
	private final NodeProperties[] NODES = null;

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
