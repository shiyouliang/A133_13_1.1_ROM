package com.android.launcher3;

//add by heml,for AllApp NavBar and Statusbar color
public class AllAppColorController {

    public interface AllAppColorControllerListener {
        void setNavBarColor(boolean isBlack);
    }

    private static AllAppColorController instance = null;

    private AllAppColorController() {
    }

    public static AllAppColorController getInstance() {
        if (instance == null) {
            instance = new AllAppColorController();
        }
        return instance;
    }


    private AllAppColorControllerListener mListener;

    public void setListener(AllAppColorControllerListener listener) {
        mListener = listener;
    }

    public void setNavBarColor(boolean isBlack) {
        if (mListener != null) {
            mListener.setNavBarColor(isBlack);
        }
    }
}