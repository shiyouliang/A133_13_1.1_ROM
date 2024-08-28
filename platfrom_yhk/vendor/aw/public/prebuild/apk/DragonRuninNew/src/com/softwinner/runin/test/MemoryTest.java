package com.softwinner.runin.test;

import java.util.Arrays;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.softwinner.log.RuninLog;
import com.softwinner.runin.R;
import com.softwinner.runin.Settings;
import com.softwinner.runin.test.interfaces.ITest;
import com.softwinner.xml.Node;

/**
 * 内存测试
 * @author zengsc
 * @version date 2013-5-19
 */
public class MemoryTest implements ITest {
	static final String TAG = "Runin-MemoryTest";
	static final boolean DEBUG = false;

	protected Context mContext;
	protected ViewGroup mStage;

	private boolean mIsRunning = false;
	private boolean mResult = false;

	protected StopCallback mStopCallback;

	private BufferTask mBufferTask;
	private ProgressBar mProgressBar;
	private TextView mResultTextView;
	private Handler mUIHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0) {
				mProgressBar.setMax(msg.arg2);
				mProgressBar.setProgress(0);
				mResultTextView.setText(R.string.memory_testing);
			} else if (msg.what == 1) {
				mProgressBar.setProgress(msg.arg1);
				mProgressBar.setMax(msg.arg2);
				String tmp;
				if (msg.arg1 >= msg.arg2) {
					tmp = "Memory Test Complete.";
				} else {
					tmp = "Buffer Compare Count:" + msg.arg1;
				}
				mResultTextView.setText(tmp);
			} else if (msg.what == 3) {
				Log.i(TAG, "Memory background copy stop.");
				mResult = true;
				stop();
				if (mStopCallback != null)
					mStopCallback.onStop(MemoryTest.this);
			}
		};
	};

	/**
	 * 内存复制线程
	 * @author zengsc
	 * @version date 2013-5-19
	 */
	class BufferTask extends AsyncTask<Void, Void, Void> {
		private final int SIZE = 1024;
		private final int mBufferSize = 1024;
		private final int mRepeatCount;
		private final byte[] mBuffer;
		private final byte[] mRawdata; // 原始数据
		private final byte[] mTmpBuffer; // 临时数据

		BufferTask(int repeatCount) {
			mRepeatCount = repeatCount;
			mBuffer = new byte[mBufferSize * SIZE];
			mRawdata = new byte[mBufferSize];
			mTmpBuffer = new byte[mBufferSize];
			// 置位
			Arrays.fill(mRawdata, (byte) 200);
			mUIHandler.sendMessage(mUIHandler.obtainMessage(0, 0, mRepeatCount));
		}

		@Override
		protected Void doInBackground(Void... params) {
			Log.i(TAG, "Memory background copy start.");
			int num = 0;
			int counter = 0;
			System.arraycopy(mRawdata, 0, mBuffer, 0, mBufferSize);
			if (DEBUG)
				Log.d(TAG, "background running:" + isRunning());
			while (isRunning()) {
				if (num == 0) {
					// 将mBuffer前mBufferSize字节拷到mTmpBuffer中
					System.arraycopy(mBuffer, 0, mTmpBuffer, 0, mBufferSize);
					// mTmpBuffer与原始数据比较
					boolean result = Arrays.equals(mRawdata, mTmpBuffer);
					if (result) {
						if (DEBUG)
							Log.d(TAG, "Memory Copy Result:" + result);
						if (counter % (mRepeatCount / 10) == 0)
							RuninLog.append("memory pass:" + (counter + 1) + "x");
						mUIHandler.sendMessage(mUIHandler.obtainMessage(1, counter, mRepeatCount));
					} else {
						Log.w(TAG, "Memory Copy Result:" + result);
						RuninLog.append("memory fail:" + counter + "x");
					}
					counter++;
					// 测试次数超过设定次数退出循环
					if (counter >= mRepeatCount) {
						RuninLog.append("memory complete:" + mRepeatCount + "x");
						break;
					}
				}
				int next = (num + 1) % SIZE;
				// 将数据拷到下一个mBufferSize
				System.arraycopy(mBuffer, num * mBufferSize, mBuffer, next * mBufferSize, mBufferSize);
				num = next;
			}
			if (isRunning()) {
				Log.i(TAG, "Memory background copy succeed.");
				mUIHandler.sendEmptyMessageDelayed(3, 3000);
			}
			return null;
		}
	}

	@Override
	public void create(Context context, ViewGroup stage) {
		mContext = context;
		mStage = stage;
	}

	@Override
	public void start(Node node) {
		Log.i(TAG, "start MemoryTest");
		mIsRunning = true;
		mResult = false;
		View.inflate(mContext, R.layout.memory_test, mStage);
		mProgressBar = (ProgressBar) mStage.findViewById(R.id.memory_buffer_progressbar);
		mProgressBar.setMax(0);
		mProgressBar.setProgress(0);
		mResultTextView = (TextView) mStage.findViewById(R.id.memory_result_textview);
		mResultTextView.setText(R.string.memory_testing);
		mResultTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
		int repeatCount = node.getAttributeIntegerValue(Settings.ATTR_REPEAT_COUNT);
		if (repeatCount == 0)
			repeatCount = 100;
		mBufferTask = new BufferTask(repeatCount);
		RuninLog.append("memory start repeat:" + repeatCount);
		mBufferTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void stop() {
		Log.i(TAG, "stop MemoryTest");
		RuninLog.append("memory stop");
		if (mBufferTask != null) {
			mBufferTask.cancel(false);
			mBufferTask = null;
		}
		mIsRunning = false;
	}

	@Override
	public void destory() {
		mContext = null;
		mStage = null;
	}

	@Override
	public boolean isRunning() {
		return mIsRunning;
	}

	@Override
	public void setOnStopCallback(StopCallback callback) {
		mStopCallback = callback;
	}

	@Override
	public String getResult() {
		return mResult ? "Pass" : "Fail";
	}

	@Override
	public int getIsTestNext() {
		return 0;
	}
}
