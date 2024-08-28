package com.softwinner.runin.test.interfaces;

import android.content.Context;
import android.view.ViewGroup;

import com.softwinner.xml.Node;

/**
 * 测试接口
 * @author zengsc
 * @version date 2013-5-15
 */
public interface ITest {
	/**
	 * 创建
	 * @param stage 显示区域
	 */
	public void create(Context context, ViewGroup stage);

	/**
	 * 开始
	 */
	public void start(Node node);

	/**
	 * 停止
	 */
	public void stop();

	/**
	 * 销毁
	 */
	public void destory();

	/**
	 * 正在运行
	 */
	public boolean isRunning();

	/**
	 * 设置测试结束回调
	 */
	public void setOnStopCallback(StopCallback callback);

	/**
	 * 测试结束回调
	 * @author zengsc
	 * @version date 2013-5-16
	 */
	public interface StopCallback {
		/**
		 * 测试结束
		 */
		public void onStop(ITest test);
	}

	/**
	 * 返回测试结果
	 */
	public String getResult();

	public int getIsTestNext();
}
