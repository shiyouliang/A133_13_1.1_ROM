package com.softwinner.camera;
//add by heml for Rotate Text Direction
public class MyHelperManager {

    public interface MyHelperListener {
        void setOrientationChanged(int orientation);
    }

    private static MyHelperManager instance = null;

    private MyHelperManager() {
    }

    public static MyHelperManager getInstance() {
        if (instance == null) {
            instance = new MyHelperManager();
        }
        return instance;
    }


    private MyHelperListener mListener;

    public void setListener(MyHelperListener listener) {
        mListener = listener;
    }

    public void setOrientationChanged(int orientation) {
        if (mListener != null) {
            mListener.setOrientationChanged(orientation);
        }
    }

}
