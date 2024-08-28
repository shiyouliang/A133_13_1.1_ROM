package com.softwinner.camera;
//add by heml,for kill awcamera
public class PermissionLayoutManager {

    public interface PermissionLayoutManagerListener {
        void finishCamera();
    }

    private static PermissionLayoutManager instance = null;

    private PermissionLayoutManager() {
    }

    public static PermissionLayoutManager getInstance() {
		
        if (instance == null) {
            instance = new PermissionLayoutManager();
        }
        return instance;
    }


    private PermissionLayoutManagerListener mListener;

    public void setListener(PermissionLayoutManagerListener listener) {
        mListener = listener;
    }

    public void finishCamera() {
        if (mListener != null) {
            mListener.finishCamera();
        }
    }

}
