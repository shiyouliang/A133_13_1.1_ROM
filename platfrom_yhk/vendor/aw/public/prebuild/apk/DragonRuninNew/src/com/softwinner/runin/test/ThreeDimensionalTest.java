package com.softwinner.runin.test;

import android.util.Log;
import android.view.ViewGroup;

import com.softwinner.log.RuninLog;
import com.softwinner.runin.Settings;
import com.softwinner.runin.test.base.DurationTest;
import com.softwinner.xml.Node;

/**
 * 3D测试
 *
 * @author zengsc
 * @version date 2013-5-20
 */
public class ThreeDimensionalTest extends DurationTest {
    private static final String TAG = "Runin-ThreeDimensionalTest";
    private static final String BROADCAST_RECEIVER_ACTION = Settings.PACKAGE_NAME + ".threedimensionaltest";

    private ThreeDimensionalView mThreeDimensionalView;

    @Override
    protected boolean onStart(Node node) {
        Log.i(TAG, "start ThreeDimensionalTest");
        RuninLog.append("3d start duration:" + mDuration + "ms");
        mThreeDimensionalView = new ThreeDimensionalView(mContext);
        mStage.addView(mThreeDimensionalView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return true;
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "stop ThreeDimensionalTest");
        RuninLog.append("3d stop");
        if (mThreeDimensionalView != null) {
            mThreeDimensionalView.onPause();
            mThreeDimensionalView = null;
        }
    }

    @Override
    protected String getBroadcastReceiverAction() {
        return BROADCAST_RECEIVER_ACTION;
    }

}
