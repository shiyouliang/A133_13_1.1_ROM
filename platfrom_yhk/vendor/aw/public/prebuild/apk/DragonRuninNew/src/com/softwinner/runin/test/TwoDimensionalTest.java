package com.softwinner.runin.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.softwinner.log.RuninLog;
import com.softwinner.runin.Settings;
import com.softwinner.runin.test.base.DurationTest;
import com.softwinner.xml.Node;

/**
 * 2D测试
 * @author zengsc
 * @version date 2013-5-16
 */
public class TwoDimensionalTest extends DurationTest {
	private static final String TAG = "Runin-TwoDimensionalTest";
	private static final String BROADCAST_RECEIVER_ACTION = Settings.PACKAGE_NAME + ".twodimensionaltest";
	private static final String DEFAULT_PATH = "pic";
	private String[] mDefaultSources;
	private Object[] mSources;
	private Map<String, WeakReference<Bitmap>> mBitmapCache = new HashMap<String, WeakReference<Bitmap>>();
	private int mIndex;
	private ImageView mView;
	private int mPicDelayMillis = 2000;
	private Handler mPicHandler = new Handler();
	private Runnable mPicRunnable = new Runnable() {
		@Override
		public void run() {
			playOne();
			mPicHandler.postDelayed(this, mPicDelayMillis);
		}
	};
	private int mCounter;

	@Override
	protected boolean onStart(Node node) {
		Log.i(TAG, "start TwoDimensionalTest");
		int delay = node.getAttributeIntegerValue(Settings.ATTR_DELAY);
		if (delay > 0)
			mPicDelayMillis = delay;
		RuninLog.append("2d start duration:" + mDuration + "ms delay:" + mPicDelayMillis + "ms");
		mCounter = 1;
		List<String> sources = new ArrayList<String>();
		int n = node.getNNodes();
		for (int i = 0; i < n; i++) {
			Node tmp = node.getNode(i);
			if (Settings.NODE_SOURCE.equals(tmp.getName())) {
				String path = tmp.getValue().trim();
				File file = new File(path);
				if (!file.exists())
					continue;
				if (file.isDirectory()) {
					for (File subFile : file.listFiles()) {
						if (subFile.isFile())
							sources.add(subFile.getAbsolutePath());
					}
				} else {
					sources.add(path);
				}
			}
		}
		if (sources.size() == 0) {
			Log.i(TAG, "load defualt pictures.");
			try {
				mDefaultSources = mContext.getAssets().list(DEFAULT_PATH);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		mSources = sources.toArray();
		sources.clear();
		sources = null;
		if (mSources.length == 0 && mDefaultSources.length == 0) {
			Log.e(TAG, "no 2d resource");
			return false;
		}
		mIndex = 0;
		mView = new ImageView(mContext);
		mStage.addView(mView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		mPicHandler.post(mPicRunnable);
		return true;
	}

	@Override
	protected void onStop() {
		Log.i(TAG, "stop TwoDimensionalTest");
		RuninLog.append("2d stop");
		mPicHandler.removeCallbacks(mPicRunnable);
	}

	@Override
	protected String getBroadcastReceiverAction() {
		return BROADCAST_RECEIVER_ACTION;
	}

	/**
	 * 播放视频
	 */
	private void playOne() {
		if (mSources == null)
			return;
		Log.i(TAG, "index " + mIndex);
		RuninLog.append("2d pass " + mCounter++ + "x");
		if (mSources.length > 0) {
			String path = (String) mSources[mIndex];
			Log.i(TAG, "path " + path);
			mIndex = (mIndex + 1) % mSources.length;
			mView.setImageBitmap(getBitmap(path));
		} else {
			String path = DEFAULT_PATH + File.separator + mDefaultSources[mIndex];
			Log.i(TAG, "path " + path);
			mIndex = (mIndex + 1) % mDefaultSources.length;
			mView.setImageBitmap(getAssetsBitmap(path));
		}
		mView.postInvalidate();
	}

	private Bitmap getBitmap(String path) {
		Bitmap bitmap = null;
		if (mBitmapCache.containsKey(path))
			bitmap = mBitmapCache.get(path).get();
		if (bitmap != null)
			return bitmap;
		mBitmapCache.remove(path);
		// 重新读入图片，读取缩放后的bitmap，注意这次要把options.inJustDecodeBounds 设为 false
		bitmap = BitmapFactory.decodeFile(path);
		mBitmapCache.put(path, new WeakReference<Bitmap>(bitmap));
		return bitmap;
	}

	private Bitmap getAssetsBitmap(String path) {
		Bitmap bitmap = null;
		if (mBitmapCache.containsKey(path))
			bitmap = mBitmapCache.get(path).get();
		if (bitmap != null)
			return bitmap;
		mBitmapCache.remove(path);
		InputStream is = null;
		try {
			is = mContext.getAssets().open(path);
			bitmap = BitmapFactory.decodeStream(is);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		mBitmapCache.put(path, new WeakReference<Bitmap>(bitmap));
		return bitmap;
	}
}
