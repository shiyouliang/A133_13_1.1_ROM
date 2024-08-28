package com.softwinner.camera.views;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.text.PrecomputedText;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.softwinner.camera.R;
import com.softwinner.camera.data.CameraData;

import java.util.ArrayList;

import static android.util.Log.getStackTraceString;


import com.softwinner.camera.MyHelperManager;//add by heml for Rotate Text Direction

public class SlideTabHost extends FrameLayout implements MyHelperManager.MyHelperListener {//add by heml for Rotate Text Direction
    private static final String TAG = "SlideTabHost";
    private static final long ANIM_DURATION = 200;
    private static Object sLockobject = new Object();
    private Context mContext;
    private LinearLayout mTabsContainer;
    private int mTabCount;
    private int mSlectedTab;
    private ObjectAnimator mSlidingAnimator;
    private int mScreenWidth;
    private int mCommingPositon = -1;
    private int mCurrentPosition = -1;
    private int mTextPaddingLeft;
    private int mTextPaddingRight;
    private int slidetab_left; //add by heml,for Arab language
    private Paint mTextPaint;
    private ArrayList<String> mModeNames;
    private int[] mModeWidths;
    private boolean mFling = false;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    //add by heml for Rotate Text Direction
    private RotateTextView tabView=null;
    private RotateTextView autoTab=null;
    private RotateTextView squareTab=null;
    private RotateTextView videoTab=null;
    private static MyHelperManager mMyHelperManager = MyHelperManager.getInstance();

    private Runnable mAnimateToTabRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (sLockobject) {
                if (mCurrentPosition < mSlectedTab) {
                    mCommingPositon = mCurrentPosition + 1;
                } else if (mCurrentPosition > mSlectedTab) {
                    mCommingPositon = mCurrentPosition - 1;
                } else {
                    mFling = false;
                    return;
                }
            }
            animateToTabPosition(mCommingPositon, ANIM_DURATION);

        }
    };

    private OnTabSelectedListener mOnTabSelectedListener;

    public interface OnTabSelectedListener {

        void onTabSelected(int position);
    }

    public SlideTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mTabsContainer = new LinearLayout(context);
        LayoutInflater.from(context).inflate(R.layout.slidetabhost, this, true);
        mTabsContainer = (LinearLayout) findViewById(R.id.tabs_container);
        mScreenWidth = CameraData.getInstance().getSreenWidth();
        mTextPaddingLeft = mContext.getResources().getDimensionPixelOffset(R.dimen.mode_text_padding);
        mTextPaddingRight = mContext.getResources().getDimensionPixelOffset(R.dimen.mode_text_padding);
        slidetab_left = mContext.getResources().getDimensionPixelOffset(R.dimen.slidetab_left);//add by heml,for Arab language

        mTextPaint = new Paint();
        int color = context.getColor(R.color.colorAccent);
        mTextPaint.setColor(color);
        mTextPaint.setTextSize(mContext.getResources().getDimension(R.dimen.mode_text_fontsize));
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mMyHelperManager.setListener(this);//add by heml for Rotate Text Direction
    }

    public void setTitles(ArrayList<String> titles, String defaultTitle) {
        if (titles != null) {
            mTabsContainer.removeAllViews();
            mModeNames = titles;
            calculateModeLengths();
            mTabCount = titles.size();
            int selectedIndex = -1;
            for (int i = 0; i < titles.size(); i++) {
                Tab tab = new Tab();
                tab.position = i;
                tab.text = titles.get(i);
                addTab(tab);
                if (titles.get(i).equals(defaultTitle)) {
                    selectedIndex = i;
                }
            }

            if (selectedIndex != -1) {
                setSelectedTab(selectedIndex, true);
            }
        }
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        mOnTabSelectedListener = listener;
    }
    //add by heml for Rotate Text Direction
    @Override
    public void setOrientationChanged(int orientation) {
        if(orientation==270||orientation==90) {
            return;
        }
        squareTab.onOrientationChanged(orientation);
        autoTab.onOrientationChanged(orientation);
        videoTab.onOrientationChanged(orientation);
    }
    //add by heml for Rotate Text Direction

    private void addTab(Tab tab) {
        //add by heml for Rotate Text Direction
        tabView= (RotateTextView) View.inflate(mContext, R.layout.tab_layout, null);
        tabView.setText(tab.text);
        if(tab.position==0) {
            squareTab=tabView;
        } else if(tab.position==1) {
            autoTab=tabView;
        } else {
            videoTab=tabView;
        }
        //add by heml for Rotate Text Direction
        final int positon = tab.position;
        tabView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!CameraData.getInstance().isImageCaptureIntent()&&!CameraData.getInstance().isVideoCaptureIntent() && CameraData.getInstance().getCanChangeMode()&&CameraData.getInstance().getIsPreviewDone()) {
                    setSelectedTab(positon, true);
                    if (mOnTabSelectedListener != null) {
                        mOnTabSelectedListener.onTabSelected(positon);
                    }
                }
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(mTextPaddingLeft,0,mTextPaddingRight,0);
        mTabsContainer.addView(tabView,layoutParams);
    }

    private void calculateModeLengths() {
        int totalModes = mModeNames.size();
        mModeWidths = new int[totalModes];
        for (int i = 0; i < totalModes; i++) {
            mModeWidths[i] = (int) mTextPaint.measureText(mModeNames.get(i));
            Log.i(TAG, "calculateModeIndex: " + i + " Lengths: " + mModeWidths[i]);
        }
    }

    public void moveLeft() {
        if (mSlectedTab > 0) {
            mFling = true;
            setSelectedTab(mSlectedTab - 1, true);
        }
    }

    public void moveRight() {
        if (mSlectedTab < mTabCount - 1) {
            mFling = true;
            setSelectedTab(mSlectedTab + 1, true);
        }
    }

    public int getTabCount() {
        return mTabCount;
    }

    public int getSelectedTab() {
        return mSlectedTab;
    }

    public void setSelectedTab(int position, boolean animToSelected) {
        Log.i(TAG, "setSelectedTab: " + position);
        synchronized (sLockobject) {
            if (position < mTabCount && position >= 0) {
                mSlectedTab = position;
                if (animToSelected) {
                    if (!mFling || mSlidingAnimator == null || (mSlidingAnimator != null && !mSlidingAnimator.isRunning())) {
                        animateToTabPosition(mSlectedTab, ANIM_DURATION);
                    }
                } else {
                    float halfOfCurrentModeLength = (mModeWidths[position] +
                                                     mTextPaddingLeft + mTextPaddingRight) / 2f;
                    float leftSideLength = getPreviousModeLength(position - 1) + halfOfCurrentModeLength;
                    mTabsContainer.setTranslationX(mScreenWidth / 2f - leftSideLength);
                    if (mCurrentPosition >= 0 && mCurrentPosition < mTabCount) {
                        mTabsContainer.getChildAt(mCurrentPosition).setSelected(false);
                    }

                    mCurrentPosition = position;
                    mTabsContainer.getChildAt(position).setSelected(true);
                }
            }
        }
    }

    private void animateToTabPosition(int position, long duration) {
        if (position >= 0 && position < mTabCount) {
            if (mCurrentPosition >= 0 && mCurrentPosition < mTabCount) {
                mTabsContainer.getChildAt(mCurrentPosition).setSelected(false);
            }
            mTabsContainer.getChildAt(position).setSelected(true);
            mCommingPositon = position;
            float halfOfCurrentModeLength = (mModeWidths[position] +
                                             mTextPaddingLeft + mTextPaddingRight) / 2f;
            float leftSideLength = getPreviousModeLength(position - 1) + halfOfCurrentModeLength;
            //add by heml,for Arab language
            if(slidetab_left==20) {
                if(position==2) {
                    mSlidingAnimator = ObjectAnimator.ofFloat(mTabsContainer,
                                       "translationX", mTabsContainer.getTranslationX(),128);
                } else if(position==0) {
                    mSlidingAnimator = ObjectAnimator.ofFloat(mTabsContainer,
                                       "translationX", mTabsContainer.getTranslationX(),-128);
                } else {
                    mSlidingAnimator = ObjectAnimator.ofFloat(mTabsContainer,
                                       "translationX", mTabsContainer.getTranslationX(),0);
                }
            } else {
                mSlidingAnimator = ObjectAnimator.ofFloat(mTabsContainer,
                                   "translationX", mTabsContainer.getTranslationX(),mScreenWidth / 2f - leftSideLength);
            }
            //add by heml,for Arab language

            mSlidingAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mCurrentPosition = mCommingPositon;
                    synchronized (sLockobject) {
                        if (mCurrentPosition != mSlectedTab) {
                            mHandler.post(mAnimateToTabRunnable);
                        } else {
                            mFling = false;
                        }
                    }

                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            mSlidingAnimator.setDuration(duration);
            mSlidingAnimator.start();
        }
    }


    private int getPreviousModeLength(int previousMode) {
        int length = 0;
        if (previousMode == -1) {
            return length;
        }
        length = mTextPaddingLeft + mModeWidths[previousMode] + mTextPaddingRight;
        length += getPreviousModeLength(previousMode - 1);
        return length;
    }

    static class Tab {
        CharSequence text;
        int position = -1;
    }


}
