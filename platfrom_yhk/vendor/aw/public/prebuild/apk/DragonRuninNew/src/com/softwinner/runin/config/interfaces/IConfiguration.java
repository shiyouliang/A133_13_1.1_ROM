package com.softwinner.runin.config.interfaces;

/**
 * @author zengsc
 * @version date 2013-6-7
 */
public interface IConfiguration {
	/**
	 * test name
	 */
	public String name();

	/**
	 * configuration attrs
	 */
	public String[] attrs();

	/**
	 * configuration node
	 */
	public NodeProperties[] nodes();

	/**
	 * node properties
	 * @author zengsc
	 * @version date 2013-6-7
	 */
	public interface NodeProperties {
		public enum TYPE {
			STRING, PATH, PICTURE, VIDEO, MUISC, FOREGROUNDTEST
		}

		/**
		 * node name
		 */
		public String getName();

		/**
		 * can multi
		 */
		public boolean multi();

		/**
		 * type
		 */
		public TYPE getType();
	}
}
