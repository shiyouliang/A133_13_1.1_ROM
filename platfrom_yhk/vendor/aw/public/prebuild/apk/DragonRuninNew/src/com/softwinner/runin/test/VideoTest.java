package com.softwinner.runin.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.Toast;

import com.softwinner.log.RuninLog;
import com.softwinner.runin.R;
import com.softwinner.runin.Settings;
import com.softwinner.runin.test.base.DurationTest;
import com.softwinner.xml.Node;

/**
 * 视频测试
 * @author zengsc
 * @version date 2013-5-15
 */
public class VideoTest extends DurationTest implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
	private static final String TAG = "Runin-VideoTest";
	private static final String BROADCAST_RECEIVER_ACTION = Settings.PACKAGE_NAME + ".durationtest";
	private static final String DEFAULT_VIDEO = "video/sample";
	private Object[] mSources;
	private int mIndex;
	private int mCounter; // counter 4log

	private MediaPlayer mediaPlayer;
	private SurfaceView surfaceView;

	@SuppressWarnings("deprecation")
	@Override
	protected boolean onStart(Node node) {
		Log.i(TAG, "start VideoTest");
		RuninLog.append("video start duration:" + mDuration + "ms");
		mCounter = 1;
		mIndex = 0;
		// list video file
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
			Log.i(TAG, "no custom video resource");
			String tmp = copyVideo();
			sources.add(tmp);
		}
		mSources = sources.toArray();
		sources.clear();
		sources = null;
		if (mSources.length == 0) {
			Toast.makeText(mContext, R.string.video_no_exist, Toast.LENGTH_LONG).show();
			Log.e(TAG, "no video resource");
			return false;
		}

		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);

		surfaceView = new SurfaceView(mContext);
		mStage.addView(surfaceView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		surfaceView.getHolder().setFixedSize(176, 144);
		surfaceView.getHolder().setKeepScreenOn(true);
		surfaceView.getHolder().addCallback(new SurfaceCallback());
		return true;
	}

	private final class SurfaceCallback implements SurfaceHolder.Callback {
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			playOne();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
		}
	}

	private String copyVideo() {
		String cachePath = mContext.getCacheDir().getAbsolutePath() + "/sample";
		File file = new File(cachePath);
		if (!file.exists()) {
			InputStream is = null;
			OutputStream os = null;
			byte[] buffer = new byte[8192];
			int len;
			try {
				is = mContext.getAssets().open(DEFAULT_VIDEO);
				os = new BufferedOutputStream(new FileOutputStream(file));
				while ((len = is.read(buffer)) != -1) {
					os.write(buffer, 0, len);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (is != null)
						is.close();
					if (os != null)
						os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		file.setReadable(true, false);
		return cachePath;
	}

	@Override
	protected void onStop() {
		if (mediaPlayer != null) {
			if (mediaPlayer.isPlaying())
				mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
		mSources = null;
		Log.i(TAG, "stop VideoTest");
		RuninLog.append("video stop");
	}

	@Override
	protected String getBroadcastReceiverAction() {
		return BROADCAST_RECEIVER_ACTION;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// playOne();
		mVideoHandler.postDelayed(mVideoRunnable, 100);
		String pass = "video pass " + mCounter++ + "x";
		Log.i(TAG, pass);
		RuninLog.append(pass);
	}

	/**
	 * 播放视频
	 */
	private void playOne() {
		if (mSources == null)
			return;
		Log.i(TAG, "index " + mIndex);
		String path = (String) mSources[mIndex];
		Log.i(TAG, "path " + path);
		mIndex = (mIndex + 1) % mSources.length;

		try {
			mediaPlayer.reset();
			mediaPlayer.setDataSource(path);
			mediaPlayer.setDisplay(surfaceView.getHolder());
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.prepare();
			mediaPlayer.start();
		} catch (Exception e) {
			e.printStackTrace();
			RuninLog.append("video start error");
			Log.e(TAG, "video start error, will restart in 0.5s");
			mVideoHandler.postDelayed(mVideoRunnable, 500);
		}
	}

	private Handler mVideoHandler = new Handler();
	private Runnable mVideoRunnable = new Runnable() {
		@Override
		public void run() {
			playOne();
		}
	};

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.e(TAG, "video play error, will restart in 0.5s");
		mVideoHandler.postDelayed(mVideoRunnable, 500);
		return true;
	}
}
