package com.softwinner.runin.config;

import com.softwinner.runin.config.interfaces.IConfiguration;

/**
 * @author zengsc
 * @version date 2013-6-7
 */
public class BlankTestConfig implements IConfiguration {
	private final String NAME = "BlankTest";
	private final String[] ATTRS = null;
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
