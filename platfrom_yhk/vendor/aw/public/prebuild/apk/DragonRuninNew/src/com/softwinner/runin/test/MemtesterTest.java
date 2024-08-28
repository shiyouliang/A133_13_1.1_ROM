package com.softwinner.runin.test;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lenovo.dramtest.DRAMTest;
import com.softwinner.log.RuninLog;
import com.softwinner.runin.R;
import com.softwinner.runin.Settings;
import com.softwinner.runin.ddr.DdrTestUtils;
import com.softwinner.runin.ddr.Interval;
import com.softwinner.runin.ddr.TestAdapter;
import com.softwinner.runin.test.interfaces.ITest;
import com.softwinner.xml.Node;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * 内存测试
 *
 * @author zengsc
 * @version date 2020-5-13
 */
public class MemtesterTest implements ITest {
    static final String TAG = "Runin-MemtesterTest";
    static final boolean DEBUG = false;

    protected Context mContext;
    protected ViewGroup mStage;

    private boolean mResult = false;

    protected StopCallback mStopCallback;

    //private BackgroundTask mBackgroundTask;
    private TextView mResultTextView;

    private Interval interval;
    private int memSize;
    private int repeatCount;
    ListView testListView;
    private DRAMTest[] tests = null;
    private TestAdapter adapter = null;
    private int currentNum = 0;
    private int isTestNext=0;


    private void yhkToast(String message) {
        Toast toast = Toast.makeText(mContext, message, Toast.LENGTH_LONG);
        LinearLayout layout = (LinearLayout) toast.getView();
        TextView tv = (TextView) layout.getChildAt(0);
        tv.setTextColor(Color.BLACK);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }


    private Handler mUIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                mResultTextView.setText((String) msg.obj + "\n");
            } else if (msg.what == 1) {
                mResultTextView.append((String) msg.obj + "\n");
            }
        }
    };

    /**
     * 内存复制线程
     *
     * @author zengsc
     * @version date 2020-5-13
     */
    class BackgroundTask extends AsyncTask<Void, Void, Void> {
        private final int mSize = 8;
        private final int mRepeatCount;

        BackgroundTask(int repeatCount) {
            mRepeatCount = repeatCount;
        }

        @Override
        protected void onPreExecute() {
            mUIHandler.sendMessage(mUIHandler.obtainMessage(0, "Begin"));
            Log.i(TAG, "Memtester background test begin.");
        }

        @Override
        protected Void doInBackground(Void... params) {
            DataOutputStream dos = null;
            BufferedReader dis = null;
            Process p = null;

            try {
                p = Runtime.getRuntime().exec("sh");
                String cmd = String.format("/system/bin/memtester %dM %d; sleep 5; exit\n", mSize, mRepeatCount);

                Log.i(TAG, "ddr test begin, cmd = " + cmd);
                dos = new DataOutputStream(p.getOutputStream());
                dis = new BufferedReader(new InputStreamReader(p.getInputStream()));
                dos.writeBytes(cmd);
                dos.flush();

                String line = null;
                while ((line = dis.readLine()) != null) {
                    if (isCancelled()) {
                        char ctrlBreak = (char) 3;
                        dos.write(ctrlBreak);
                        dos.flush();
                        dos.write(ctrlBreak);
                        dos.write('\n');
                        dos.flush();
                        break;
                    }
                    if (line == null) continue;
                    Log.d(TAG, "DDR result line:" + line);
                    if (line.startsWith("Done")) {
                        mResult = true;
                        break;
                    } else if (line.startsWith("Loop")) {
                        mUIHandler.sendMessage(mUIHandler.obtainMessage(0, line));
                    } else if (line.endsWith("ok")) {
                        line = line.substring(0, line.indexOf(':') + 1) + " ok";
                        mUIHandler.sendMessage(mUIHandler.obtainMessage(1, line));
                    }
                }

                Log.i(TAG, "ddr test end.");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (dos != null) {
                    try {
                        dos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (dis != null) {
                    try {
                        dis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (p != null) {
                    p.destroy();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.i(TAG, "Memtester background test end.");
            stop();
            if (mStopCallback != null)
                mStopCallback.onStop(MemtesterTest.this);
        }

        @Override
        protected void onCancelled(Void result) {
            Log.i(TAG, "Memtester background test cancel.");
            super.onCancelled(result);
            mResult = false;
            stop();
            if (mStopCallback != null) {
                mStopCallback.onStop(MemtesterTest.this);
            }
        }
    }

    @Override
    public void create(Context context, ViewGroup stage) {
        Log.i(TAG, "yulh MemtesterTest  create");
        mContext = context;
        mStage = stage;
        tests = new DRAMTest[DdrTestUtils.getTestCount()];
        int i = 0;
        while (true) {
            DRAMTest[] dRAMTestArr = tests;
            if (i < dRAMTestArr.length) {
                dRAMTestArr[i] = new DRAMTest("test" + i);
                i++;
            } else {
                return;
            }
        }
    }

    @Override
    public void start(Node node) {
        Log.i(TAG, "yulh MemtesterTest  start");
		RuninLog.append("memtester start  ");
        mResult = false;
        currentNum = 0;
        View.inflate(mContext, R.layout.memtester_test, mStage);
        mResultTextView = (TextView) mStage.findViewById(R.id.memtester_result_textview);
        mResultTextView.setText(R.string.memtester_testing);
        mResultTextView.setMovementMethod(ScrollingMovementMethod.getInstance());

        testListView = (ListView) mStage.findViewById(R.id.test_list);
        adapter = new TestAdapter(mContext, R.layout.item_ddr_status, Arrays.asList(tests));
        testListView.setAdapter((ListAdapter) adapter);
        if(interval!=null){
            interval.stop();
        }
        interval = new Interval(0L, 1000L, new Runnable() { // from class: com.clock.pt1.keeptesting.ddr.DdrTestActivity.3
            @Override // java.lang.Runnable
            public void run() {
                updateView();
            }
        });
        interval.start();
        memSize = node.getAttributeIntegerValue(Settings.ATTR_MEMSIZE);
        RuninLog.append("memtester start  memSize" + memSize + " MB");
        if (memSize <= 0) {
            yhkToast(mContext.getResources().getString(R.string.ddr_test_mem_size_error_text));
            return;
        }
        repeatCount = node.getAttributeIntegerValue(Settings.ATTR_REPEAT_COUNT);
        RuninLog.append("memtester start  repeatCount" + repeatCount);
        if (repeatCount <= 0) {
            yhkToast(mContext.getResources().getString(R.string.ddr_test_repeat_count_error_text));
            return;
        }
        long preferMemSize = DdrTestUtils.getPreferMemSize(mContext, this.memSize * 1024 * 1024, this.tests.length);
        for (DRAMTest dRAMTest : tests) {
            dRAMTest.setTotalCycles(repeatCount);
            dRAMTest.setFileSize(preferMemSize);
            dRAMTest.start();
        }
    }


    /* JADX INFO: Access modifiers changed from: private */
    public void updateView() {
        DRAMTest[] dRAMTestArr = this.tests;
        currentNum++;
        if (dRAMTestArr != null) {
            boolean z = false;
            boolean z2 = false;
            boolean z3 = true;
            for (DRAMTest dRAMTest : dRAMTestArr) {
                if (dRAMTest.isError()) {
                    z2 = true;
                }
                if (dRAMTest.getStatus() == DRAMTest.Status.TESTING) {
                    z = true;
                }
                if (dRAMTest.getStatus() != DRAMTest.Status.IDLE) {
                    z3 = false;
                }
            }
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            //this.startBtn.setEnabled(!z);
            if (z3) {
                mResultTextView.setText(R.string.ddr_test_no_test_running_text);
            } else if (z) {
                mResultTextView.setText(R.string.ddr_test_testing_text);
            } else if (z2) {
                mResult = false;
                isTestNext =1;
                mResultTextView.setText(R.string.ddr_test_fail_text);
                stopYhkMem();
            } else {
                mResult = true;
                isTestNext=2;
                mResultTextView.setText(R.string.ddr_test_success_text);
                stopYhkMem();
            }
        }
    }

    private void stopYhkMem() {
        Log.i(TAG, "yulh MemtesterTest stopYhkMem");
        if (mStopCallback != null) {
            mStopCallback.onStop(MemtesterTest.this);
        }
        if (interval != null && (currentNum >= repeatCount)) {
            Log.i(TAG, "yulh MemtesterTest stopYhkMem 2");
            runinLogAppend();
            interval.stop();
            interval = null;
        }
    }

    @Override
    public void stop() {
        Log.i(TAG, "yulh stop MemtesterTest");
        RuninLog.append("memtester stop");

        mResult = false;
        if (hasRunningTest()) {
            yhkToast(mContext.getResources().getString(R.string.ddr_test_stopped_text));
        } else {
            yhkToast(mContext.getResources().getString(R.string.ddr_test_no_test_running_text));
        }

        if (tests != null) {
            for (DRAMTest dRAMTest : tests) {
                dRAMTest.stop();
            }
        }
        if (interval != null) {
            Log.i(TAG, "yulh MemtesterTest stop() 2");
            runinLogAppend();
            interval.stop();
            interval = null;
        }
    }

    private void runinLogAppend(){
        if(mResult){
            RuninLog.append("memtester start  " + mContext.getResources().getString(R.string.ddr_test_success_text));
        }else {
            RuninLog.append("memtester start  " + mContext.getResources().getString(R.string.ddr_test_fail_text));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean hasRunningTest() {
        DRAMTest[] dRAMTestArr = this.tests;
        if (dRAMTestArr == null) {
            return false;
        }
        for (DRAMTest dRAMTest : dRAMTestArr) {
            if (dRAMTest.getStatus() == DRAMTest.Status.TESTING) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void destory() {
        mContext = null;
        mStage = null;

        if (tests != null) {
            for (DRAMTest dRAMTest : tests) {
                dRAMTest.stop();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return hasRunningTest();
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
        return isTestNext;
    }


}
