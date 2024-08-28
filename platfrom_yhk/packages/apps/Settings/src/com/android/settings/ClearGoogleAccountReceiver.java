package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.app.ActivityManager;
import android.util.Log;
import android.service.oemlock.OemLockManager;
import android.service.persistentdata.PersistentDataBlockManager;
import java.io.UnsupportedEncodingException;
import android.widget.Toast;
import android.os.Handler;
//add by heml,for clear Google account
public class ClearGoogleAccountReceiver extends BroadcastReceiver {
    private ActivityManager mAm; //add by heml,for kill awcamera
    @Override
    public void onReceive(Context context, Intent intent) {
	    mAm = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE); //add by heml,for kill awcamera
        final PersistentDataBlockManager pdbManager = (PersistentDataBlockManager)
                context.getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);
        final String action = intent.getAction();
        android.util.Log.d("heml","ClearGoogleAccountReceiver action="+action);
        if ("com.android.yhk.clear.google.wipe".equals(action)) {
            pdbManager.wipe();
        } else if("com.android.yhk.clear.google.read".equals(action)) {
            android.util.Log.d("heml","ClearGoogleAccountReceiver read="+pdbManager.read().length);
        } else if("com.android.yhk.clear.google.write".equals(action)) {
            byte[] b="0".getBytes();
            pdbManager.write(b);
            pdbManager.wipe();
            if(pdbManager.read().length==1) {
                //android.util.Log.d("heml","ClearGoogleAccountReceiver write1 length="+pdbManager.read().length);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context,context.getResources().getText(R.string.lockpassword_clear_label)+" "+context.getResources().getText(R.string.done),Toast.LENGTH_LONG).show();
                    }
                },5000);

            } else {
                //android.util.Log.d("heml","ClearGoogleAccountReceiver write2 length="+pdbManager.read().length);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context,context.getResources().getText(R.string.lockpassword_clear_label)+" "+context.getResources().getText(R.string.done),Toast.LENGTH_LONG).show();
                    }
                },5000);
            }
        //add by heml,for kill awcamera
        }else if("com.android.kill_awcamera".equals(action)){
		    String[] packageName = intent.getStringArrayExtra("PACKAGE_NAME");
            if(mAm!=null){
			    for(int i=0;i<packageName.length;i++){
                    mAm.forceStopPackage(packageName[i]);
                    mAm.killBackgroundProcesses(packageName[i]);
		        }
            }
		}
		
		
    }

}
