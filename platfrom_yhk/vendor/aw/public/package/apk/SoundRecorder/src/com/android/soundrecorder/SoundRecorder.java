/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.soundrecorder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.PowerManager.WakeLock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import java.net.URI;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.net.URISyntaxException;
import android.os.Build;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.widget.Toast;

//add by heml,for file list show
import android.media.MediaPlayer;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.content.DialogInterface;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.widget.Toolbar;
//add by heml,for file list show

/**
 * Calculates remaining recording time based on available disk space and
 * optionally a maximum recording file size.
 *
 * The reason why this is not trivial is that the file grows in blocks
 * every few seconds or so, while we want a smooth countdown.
 */

class RemainingTimeCalculator {
    public static final int UNKNOWN_LIMIT = 0;
    public static final int FILE_SIZE_LIMIT = 1;
    public static final int DISK_SPACE_LIMIT = 2;

    // which of the two limits we will hit (or have fit) first
    private int mCurrentLowerLimit = UNKNOWN_LIMIT;

    private File mSDCardDirectory;

    // State for tracking file size of recording.
    private File mRecordingFile;
    private long mMaxBytes;

    // Rate at which the file grows
    private int mBytesPerSecond;

    // time at which number of free blocks last changed
    private long mBlocksChangedTime;
    // number of available blocks at that time
    private long mLastBlocks;

    // time at which the size of the file has last changed
    private long mFileSizeChangedTime;
    // size of the file at that time
    private long mLastFileSize;

    static {
        System.loadLibrary("mp3recorder_jni");
    }

    public RemainingTimeCalculator() {
        mSDCardDirectory = Environment.getExternalStorageDirectory();
    }

    /**
     * If called, the calculator will return the minimum of two estimates:
     * how long until we run out of disk space and how long until the file
     * reaches the specified size.
     *
     * @param file the file to watch
     * @param maxBytes the limit
     */

    public void setFileSizeLimit(File file, long maxBytes) {
        mRecordingFile = file;
        mMaxBytes = maxBytes;
    }

    /**
     * Resets the interpolation.
     */
    public void reset() {
        mCurrentLowerLimit = UNKNOWN_LIMIT;
        mBlocksChangedTime = -1;
        mFileSizeChangedTime = -1;
    }

    /**
     * Returns how long (in seconds) we can continue recording.
     */
    public long timeRemaining() {
        // Calculate how long we can record based on free disk space

        StatFs fs = new StatFs(mSDCardDirectory.getAbsolutePath());
        long blocks = fs.getAvailableBlocks();
        long blockSize = fs.getBlockSize();
        long now = System.currentTimeMillis();

        if (mBlocksChangedTime == -1 || blocks != mLastBlocks) {
            mBlocksChangedTime = now;
            mLastBlocks = blocks;
        }

        /* The calculation below always leaves one free block, since free space
           in the block we're currently writing to is not added. This
           last block might get nibbled when we close and flush the file, but
           we won't run out of disk. */

        // at mBlocksChangedTime we had this much time
        long result = mLastBlocks*blockSize/mBytesPerSecond;
        // so now we have this much time
        result -= (now - mBlocksChangedTime)/1000;

        if (mRecordingFile == null) {
            mCurrentLowerLimit = DISK_SPACE_LIMIT;
            return result;
        }

        // If we have a recording file set, we calculate a second estimate
        // based on how long it will take us to reach mMaxBytes.

        mRecordingFile = new File(mRecordingFile.getAbsolutePath());
        long fileSize = mRecordingFile.length();
        if (mFileSizeChangedTime == -1 || fileSize != mLastFileSize) {
            mFileSizeChangedTime = now;
            mLastFileSize = fileSize;
        }

        long result2 = (mMaxBytes - fileSize)/mBytesPerSecond;
        result2 -= (now - mFileSizeChangedTime)/1000;
        result2 -= 1; // just for safety

        mCurrentLowerLimit = result < result2
                             ? DISK_SPACE_LIMIT : FILE_SIZE_LIMIT;

        return Math.min(result, result2);
    }

    /**
     * Indicates which limit we will hit (or have hit) first, by returning one
     * of FILE_SIZE_LIMIT or DISK_SPACE_LIMIT or UNKNOWN_LIMIT. We need this to
     * display the correct message to the user when we hit one of the limits.
     */
    public int currentLowerLimit() {
        return mCurrentLowerLimit;
    }

    /**
     * Is there any point of trying to start recording?
     */
    public boolean diskSpaceAvailable() {
        StatFs fs = new StatFs(mSDCardDirectory.getAbsolutePath());
        // keep one free block
        return fs.getAvailableBlocks() > 1;
    }

    /**
     * Sets the bit rate used in the interpolation.
     *
     * @param bitRate the bit rate to set in bits/sec.
     */
    public void setBitRate(int bitRate) {
        mBytesPerSecond = bitRate/8;
    }
}

public class SoundRecorder extends Activity
        implements Button.OnClickListener, Recorder.OnStateChangedListener {
    static final String TAG = "SoundRecorder";
    static final String STATE_FILE_NAME = "soundrecorder.state";
    static final String RECORDER_STATE_KEY = "recorder_state";
    static final String SAMPLE_INTERRUPTED_KEY = "sample_interrupted";
    static final String MAX_FILE_SIZE_KEY = "max_file_size";

    static final String AUDIO_3GPP = "audio/3gpp";
    static final String AUDIO_AMR = "audio/amr";
    static final String AUDIO_MP3 = "audio/mpeg";
    static final String AUDIO_ANY = "audio/*";
    static final String ANY_ANY = "*/*";

    static final int BITRATE_AMR =  5900; // bits/sec
    static final int BITRATE_3GPP = 5900;
    static final int BITRATE_MP3 = 320000;
    private Uri mResultUri;
    private long mRecordTime;

    //add by heml,for file list show
    private MediaPlayer mediaPlayer;
    private List<File> musicFiles = new ArrayList<File>();
    private String sdCardPath = Environment.getExternalStorageDirectory()+"/Recordings";

    private boolean isSaveDone=false;
    //add by heml,for file list show
    private static final int SOUND_RECORDER_PERMISSION_REQUEST = 1;

    WakeLock mWakeLock;
    String mRequestedType = AUDIO_ANY;
    Recorder mRecorder;
    boolean mSampleInterrupted = false;
    String mErrorUiMessage = null; // Some error messages are displayed in the UI,
    // not a dialog. This happens when a recording
    // is interrupted for some reason.

    long mMaxFileSize = -1;        // can be specified in the intent
    RemainingTimeCalculator mRemainingTimeCalculator;

    String mTimerFormat;
    final Handler mHandler = new Handler();
    Runnable mUpdateTimer = new Runnable() {
        public void run() {
            updateTimerView();
        }
    };

    ImageButton mRecordButton;
    ImageButton mPlayButton;
    ImageButton mStopButton;
    ImageButton mListButton;//add by heml,for file list show

    ImageView mStateLED;
    TextView mStateMessage1;
    TextView mStateMessage2;
    ProgressBar mStateProgressBar;
    TextView mTimerView;

    LinearLayout mExitButtons;
    Button mAcceptButton;
    Button mDiscardButton;
    VUMeter mVUMeter;
    private BroadcastReceiver mSDCardMountEventReceiver = null;

    private Intent mIntent;
    private Bundle mBundle;
    private boolean isEmpty=true;//add by heml,for file list show
    @Override
    public void onCreate(Bundle icycle) {
        super.onCreate(icycle);
        mIntent = getIntent();
        mBundle = icycle;

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                                          getApplicationInfo().packageName, PackageManager.GET_PERMISSIONS);

            if (packageInfo.requestedPermissions != null) {
for (String permission : packageInfo.requestedPermissions) {
                    Log.v(TAG, "Checking permissions for: " + permission);
                    if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(packageInfo.requestedPermissions,
                                           SOUND_RECORDER_PERMISSION_REQUEST);
                        return;
                    }
                }
            }
            createContinue();
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Unable to load package's permissions", e);
            Toast.makeText(this, R.string.runtime_permissions_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setContentView(R.layout.main);
        initResourceRefs();
        updateUi();
        //add by heml,for file list show
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setElevation(0);
        //add by heml,for file list show
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if ((mRecorder ==null) || (mRecorder.sampleLength() == 0))
            return;

        Bundle recorderState = new Bundle();

        mRecorder.saveState(recorderState,mResultUri);
        recorderState.putBoolean(SAMPLE_INTERRUPTED_KEY, mSampleInterrupted);
        recorderState.putLong(MAX_FILE_SIZE_KEY, mMaxFileSize);

        outState.putBundle(RECORDER_STATE_KEY, recorderState);
    }

    /*
     * Whenever the UI is re-created (due f.ex. to orientation change) we have
     * to reinitialize references to the views.
     */
    private void initResourceRefs() {
        mRecordButton = (ImageButton) findViewById(R.id.recordButton);
        mPlayButton = (ImageButton) findViewById(R.id.playButton);
        mStopButton = (ImageButton) findViewById(R.id.stopButton);
        mListButton = (ImageButton) findViewById(R.id.listButton);//add by heml,for file list show

        mStateLED = (ImageView) findViewById(R.id.stateLED);
        mStateMessage1 = (TextView) findViewById(R.id.stateMessage1);
        mStateMessage2 = (TextView) findViewById(R.id.stateMessage2);
        mStateProgressBar = (ProgressBar) findViewById(R.id.stateProgressBar);
        mTimerView = (TextView) findViewById(R.id.timerView);

        mExitButtons = (LinearLayout) findViewById(R.id.exitButtons);
        mAcceptButton = (Button) findViewById(R.id.acceptButton);
        mDiscardButton = (Button) findViewById(R.id.discardButton);
        mVUMeter = (VUMeter) findViewById(R.id.uvMeter);

        mRecordButton.setOnClickListener(this);
        mPlayButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);
        mListButton.setOnClickListener(this);//add by heml,for file list show
        mAcceptButton.setOnClickListener(this);
        mDiscardButton.setOnClickListener(this);

        mTimerFormat = getResources().getString(R.string.timer_format);

        mVUMeter.setRecorder(mRecorder);
    }

    /*
     * Make sure we're not recording music playing in the background, ask
     * the MediaPlaybackService to pause playback.
     */
    private void stopAudioPlayback() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }
    //add by heml,for file list show
    private void openDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,AlertDialog.THEME_HOLO_DARK);
        builder.setTitle(getResources().getString(R.string.list_of_recordings));
        final List<Map<String, Object>> listItems = new ArrayList<>();
        File musicDirectory = new File(sdCardPath);
        if (musicDirectory.exists()) {
            musicDirectory.setReadable(true);
            File[] files = musicDirectory.listFiles();
            if (files != null) {
                musicFiles.clear();
                isEmpty=true;
for (File file : files) {
                    if (file.getName().endsWith(".mp3")&&(!file.getName().contains("trashed"))) {
                        isEmpty=false;
                        musicFiles.add(file);
                    }
                }
            } else {
                Toast.makeText(this,getResources().getString(R.string.not_found_recordings),Toast.LENGTH_LONG).show();
                return;
            }
        } else {
            Log.e("heml", "Music directory does not exist!  sdCardPath= "+sdCardPath);
            return;
        }
        if(isEmpty) {
            Toast.makeText(this,getResources().getString(R.string.not_found_recordings),Toast.LENGTH_LONG).show();
            return;
        }
for (File file : musicFiles) {
            Map<String, Object> item = new HashMap<>();
            item.put("title", file.getName());
            listItems.add(item);
        }
        final AlertDialog dialog = builder.setSingleChoiceItems(new SimpleAdapter(this, listItems, android.R.layout.simple_list_item_1, new String[] {"title"}, new int[] {android.R.id.text1}), -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(mediaPlayer!=null) {
                    mediaPlayer.release();
                    mediaPlayer=null;
                }
                mediaPlayer = new MediaPlayer();
                String selectedSong = listItems.get(which).get("title").toString();
                File file = new File(sdCardPath, selectedSong);
                try {
                    Log.d("hemll","file.getPath()="+file.getPath()+" "+which+" "+selectedSong);
                    mediaPlayer.setDataSource(file.getPath());
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).setPositiveButton(getResources().getString(R.string.close), null).create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if(mediaPlayer!=null) {
                    mediaPlayer.release();
                    mediaPlayer=null;
                }
            }
        });
        dialog.show();
    }

    /*
     * Handle the buttons.
     */
    public void onClick(View button) {
        if (!button.isEnabled())
            return;

        switch (button.getId()) {
        case R.id.recordButton:
            isSaveDone=false;//add by heml,for file list show
            mRemainingTimeCalculator.reset();
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                mSampleInterrupted = true;
                mErrorUiMessage = getResources().getString(R.string.insert_sd_card);
                updateUi();
            } else if (!mRemainingTimeCalculator.diskSpaceAvailable()) {
                mSampleInterrupted = true;
                mErrorUiMessage = getResources().getString(R.string.storage_is_full);
                updateUi();
            } else {
                stopAudioPlayback();

                if (AUDIO_AMR.equals(mRequestedType)) {
                    mRemainingTimeCalculator.setBitRate(BITRATE_AMR);
                    Uri uri = addToMediaDB();
                    mRecorder.startRecording(MediaRecorder.OutputFormat.AMR_NB, ".amr", this,uri);
                } else if (AUDIO_3GPP.equals(mRequestedType)) {
                    mRemainingTimeCalculator.setBitRate(BITRATE_3GPP);
                    Uri uri = addToMediaDB();
                    mRecorder.startRecording(MediaRecorder.OutputFormat.THREE_GPP, ".3gpp",
                                             this,uri);
                } else if (AUDIO_MP3.equals(mRequestedType)) {
                    mRemainingTimeCalculator.setBitRate(BITRATE_MP3);
                    Uri uri = addToMediaDB();
                    mRecorder.startRecording(MediaRecorder.OutputFormat.THREE_GPP, ".mp3",
                                             this,uri);
                } else {
                    throw new IllegalArgumentException("Invalid output file type requested");
                }

                if (mMaxFileSize != -1 && mResultUri != null ) {
                    try {
                        mRemainingTimeCalculator.setFileSizeLimit(
                            new File(new URI(mResultUri.toString())), mMaxFileSize);
                    } catch (URISyntaxException e) {
                        Log.e("Recorder","error:"+e);
                    }

                }
            }
            break;
        case R.id.playButton:
            mRecorder.startPlayback(mResultUri);
            break;
        case R.id.stopButton:
            mRecorder.stop();
            break;
        case R.id.acceptButton:
            mRecorder.stop();
            saveSample();
            //add by heml,for file list show
            mRecorder.saveFilesDone(this);
            updateUi();
            isSaveDone=true;
            mListButton.setEnabled(true);
            mListButton.setFocusable(true);
            //add by heml,for file list show
            break;
        case R.id.listButton:
            //openDialog();
            Intent intent=new Intent(this,MusicListActivity.class);
            startActivity(intent);
            break;
        case R.id.discardButton:
            mRecorder.delete(mResultUri,this);
            //add by heml,for file list show
            mListButton.setEnabled(true);
            mListButton.setFocusable(true);
            isSaveDone=false;
            mResultUri=null;
            //finish();
            //add by heml,for file list show
            break;
        }
    }

    /*
     * Handle the "back" hardware key.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            switch (mRecorder.state()) {
            case Recorder.IDLE_STATE:
                if (mRecorder.sampleLength() > 0)
                    saveSample();
                finish();
                break;
            case Recorder.PLAYING_STATE:
                mRecorder.stop();
                saveSample();
                break;
            case Recorder.RECORDING_STATE:
                mRecorder.clear();
                break;
            }
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onStop() {
        //if (mRecorder != null) {
        //    mRecorder.stop();
        //}
        super.onStop();
    }

    @Override
    protected void onPause() {
        //if (mRecorder != null) {
        //    mSampleInterrupted = mRecorder.state() == Recorder.RECORDING_STATE;
        //    mRecorder.stop();
        //}

        super.onPause();
    }

    /*
     * If we have just recorded a smaple, this adds it to the media data base
     * and sets the result to the sample's URI.
     */
    private void saveSample() {
        if (mRecorder.sampleLength() == 0)
            return;
        int code = -1;
        try {
            code = this.updateMediaDB();
        } catch(UnsupportedOperationException ex) {  // Database manipulation failure
            return;
        }
        if (code != -1) {
            setResult(RESULT_OK, new Intent().setData(mResultUri)
                      .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        }
    }

    /*
     * Called on destroy to unregister the SD card mount event receiver.
     */
    @Override
    public void onDestroy() {
        if (mSDCardMountEventReceiver != null) {
            unregisterReceiver(mSDCardMountEventReceiver);
            mSDCardMountEventReceiver = null;
        }
        //add by heml,for file list show
        if(!isSaveDone) {
            mRecorder.delete(mResultUri,this);
        }
        //add by heml,for file list show
        if (mRecorder != null) {
            mRecorder.stop();
        }

        super.onDestroy();
    }

    /*
     * Registers an intent to listen for ACTION_MEDIA_EJECT/ACTION_MEDIA_MOUNTED
     * notifications.
     */
    private void registerExternalStorageListener() {
        if (mSDCardMountEventReceiver == null) {
            mSDCardMountEventReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                        mRecorder.delete(mResultUri,context);
                    } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                        mSampleInterrupted = false;
                        updateUi();
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
            iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            iFilter.addDataScheme("file");
            registerReceiver(mSDCardMountEventReceiver, iFilter);
        }
    }

    /*
     * A simple utility to do a query into the databases.
     */
    private Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            ContentResolver resolver = getContentResolver();
            if (resolver == null) {
                return null;
            }
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (UnsupportedOperationException ex) {
            return null;
        }
    }

    /*
     * Add the given audioId to the playlist with the given playlistId; and maintain the
     * play_order in the playlist.
     */
    private void addToPlaylist(ContentResolver resolver, int audioId, long playlistId) {
        try {
            String[] cols = new String[] {
                "count(*)"
            };
            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
            Cursor cur = resolver.query(uri, cols, null, null, null);
            cur.moveToFirst();
            final int base = cur.getInt(0);
            cur.close();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, Integer.valueOf(base + audioId));
            values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioId);
            resolver.insert(uri, values);
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "Catch IllegalArgumentException: Invalid column count(*)");
        }
    }

    /*
     * Obtain the id for the default play list from the audio_playlists table.
     */
    private int getPlaylistId(Resources res) {
        Uri uri = MediaStore.Audio.Playlists.getContentUri("external");
        final String[] ids = new String[] { MediaStore.Audio.Playlists._ID };
        final String where = MediaStore.Audio.Playlists.NAME + "=?";
        final String[] args = new String[] { res.getString(R.string.audio_db_playlist_name) };
        Cursor cursor = query(uri, ids, where, args, null);
        if (cursor == null) {
            Log.v(TAG, "query returns null");
        }
        int id = -1;
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                id = cursor.getInt(0);
            }
        }
        cursor.close();
        return id;
    }

    /*
     * Create a playlist with the given default playlist name, if no such playlist exists.
     */
    private Uri createPlaylist(Resources res, ContentResolver resolver) {
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Audio.Playlists.NAME, res.getString(R.string.audio_db_playlist_name));
        Uri uri = resolver.insert(MediaStore.Audio.Playlists.getContentUri("external"), cv);
        if (uri == null) {
            new AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(R.string.error_mediadb_new_record)
            .setPositiveButton(R.string.button_ok, null)
            .setCancelable(false)
            .show();
        }
        return uri;
    }

    /*
     * Adds file and returns content uri.
     */
    private Uri addToMediaDB() {
        Resources res = getResources();
        ContentValues cv = new ContentValues();
        mRecordTime = System.currentTimeMillis();
        // long modDate = file.lastModified();
        Date date = new Date(mRecordTime);
        SimpleDateFormat formatter = new SimpleDateFormat(
            res.getString(R.string.audio_db_title_format));
        String title = formatter.format(date);
        //add by heml,for file list show
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        cv.put(MediaStore.Audio.Media.DISPLAY_NAME, sdf.format(d));
        //add by heml,for file list show

        // Lets label the recorded audio file as NON-MUSIC so that the file
        // won't be displayed automatically, except for in the playlist.
        cv.put(MediaStore.Audio.Media.IS_MUSIC, "0");
        cv.put(MediaStore.Audio.Media.DATE_ADDED, (int) (mRecordTime / 1000));
        //  cv.put(MediaStore.Audio.Media.DATE_MODIFIED, (int) (modDate / 1000));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //android Q中不再使用DATA字段，而用RELATIVE_PATH代替
            //RELATIVE_PATH是相对路径不是绝对路径
            //DCIM是系统文件夹，关于系统文件夹可以到系统自带的文件管理器中查看，不可以写没存在的名字
            cv.put(MediaStore.Audio.Media.RELATIVE_PATH, "Recordings");//add by heml,for file list show
        } else {
            //Android Q以下版本
            //  mCurrentVideoValues.put(MediaStore.Images.ImageColumns.DATA, mVideoSaveTempPath);
        }
        cv.put(MediaStore.Audio.Media.MIME_TYPE, mRequestedType);
        cv.put(MediaStore.Audio.Media.ARTIST,
               res.getString(R.string.audio_db_artist_name));
        cv.put(MediaStore.Audio.Media.ALBUM,
               res.getString(R.string.audio_db_album_name));
        Log.d(TAG, "Inserting audio record: " + cv.toString());
        ContentResolver resolver = getContentResolver();
        Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Log.d(TAG, "ContentURI: " + base);
        mResultUri = resolver.insert(base, cv);
        if (mResultUri == null) {
            new AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(R.string.error_mediadb_new_record)
            .setPositiveButton(R.string.button_ok, null)
            .setCancelable(false)
            .show();
            return null;
        }

        /* Don't need to save playlist
        if (getPlaylistId(res) == -1) {
            createPlaylist(res, resolver);
        }
        int audioId = Integer.valueOf(mResultUri.getLastPathSegment());
        addToPlaylist(resolver, audioId, getPlaylistId(res));
        */
        // Notify those applications such as Music listening to the
        // scanner events that a recorded audio file just created.
        Log.d(TAG, "mResultUri: " + base);
        return mResultUri;
    }
    private int updateMediaDB() {
        int code = -1;
        long duration = System.currentTimeMillis() - mRecordTime;
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Audio.Media.DURATION, duration);
        try {
            code = getContentResolver().update(mResultUri, cv,null,null);
        } catch (Throwable th) {
            Log.e(TAG, "Failed to write MediaStore" + th);
        }
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, mResultUri));
        return code;
    }

    /**
     * Update the big MM:SS timer. If we are in playback, also update the
     * progress bar.
     */
    private void updateTimerView() {
        Resources res = getResources();
        int state = mRecorder.state();

        boolean ongoing = state == Recorder.RECORDING_STATE || state == Recorder.PLAYING_STATE;

        long time = ongoing ? mRecorder.progress() : mRecorder.sampleLength();
        String timeStr = String.format(mTimerFormat, time/60, time%60);
        mTimerView.setText(timeStr);

        if (state == Recorder.PLAYING_STATE) {
            mStateProgressBar.setProgress((int)(100*time/mRecorder.sampleLength()));
        } else if (state == Recorder.RECORDING_STATE) {
            updateTimeRemaining();
        }

        if (ongoing)
            mHandler.postDelayed(mUpdateTimer, 1000);
    }

    /*
     * Called when we're in recording state. Find out how much longer we can
     * go on recording. If it's under 5 minutes, we display a count-down in
     * the UI. If we've run out of time, stop the recording.
     */
    private void updateTimeRemaining() {
        long t = mRemainingTimeCalculator.timeRemaining();

        if (t <= 0) {
            mSampleInterrupted = true;

            int limit = mRemainingTimeCalculator.currentLowerLimit();
            switch (limit) {
            case RemainingTimeCalculator.DISK_SPACE_LIMIT:
                mErrorUiMessage
                = getResources().getString(R.string.storage_is_full);
                break;
            case RemainingTimeCalculator.FILE_SIZE_LIMIT:
                mErrorUiMessage
                = getResources().getString(R.string.max_length_reached);
                break;
            default:
                mErrorUiMessage = null;
                break;
            }

            mRecorder.stop();
            return;
        }

        Resources res = getResources();
        String timeStr = "";

        if (t < 60)
            timeStr = String.format(res.getString(R.string.sec_available), t);
        else if (t < 540)
            timeStr = String.format(res.getString(R.string.min_available), t/60 + 1);

        mStateMessage1.setText(timeStr);
    }

    /**
     * Shows/hides the appropriate child views for the new state.
     */
    private void updateUi() {
        if (mRecorder == null) return;
        Resources res = getResources();

        switch (mRecorder.state()) {
        case Recorder.IDLE_STATE:
            if (mRecorder.sampleLength() == 0) {
                mRecordButton.setEnabled(true);
                mRecordButton.setFocusable(true);
                mPlayButton.setEnabled(false);
                mPlayButton.setFocusable(false);
                mStopButton.setEnabled(false);
                mStopButton.setFocusable(false);
                mRecordButton.requestFocus();

                mStateMessage1.setVisibility(View.INVISIBLE);
                mStateLED.setVisibility(View.INVISIBLE);
                mStateMessage2.setVisibility(View.INVISIBLE);

                mExitButtons.setVisibility(View.INVISIBLE);
                mVUMeter.setVisibility(View.VISIBLE);

                mStateProgressBar.setVisibility(View.INVISIBLE);

                setTitle(res.getString(R.string.record_your_message));
            } else {
                //add by heml,for file list show
                mRecordButton.setEnabled(false);
                mRecordButton.setFocusable(false);
                //add by heml,for file list show
                mPlayButton.setEnabled(true);
                mPlayButton.setFocusable(true);
                mStopButton.setEnabled(false);
                mStopButton.setFocusable(false);

                mStateMessage1.setVisibility(View.INVISIBLE);
                mStateLED.setVisibility(View.INVISIBLE);
                mStateMessage2.setVisibility(View.INVISIBLE);

                mExitButtons.setVisibility(View.VISIBLE);
                mVUMeter.setVisibility(View.INVISIBLE);

                mStateProgressBar.setVisibility(View.INVISIBLE);

                setTitle(res.getString(R.string.message_recorded));
            }

            if (mSampleInterrupted) {
                mStateMessage2.setVisibility(View.VISIBLE);
                mStateMessage2.setText(res.getString(R.string.recording_stopped));
                mStateLED.setVisibility(View.INVISIBLE);
            }

            if (mErrorUiMessage != null) {
                mStateMessage1.setText(mErrorUiMessage);
                mStateMessage1.setVisibility(View.VISIBLE);
            }

            break;
        case Recorder.RECORDING_STATE:
            mRecordButton.setEnabled(false);
            mRecordButton.setFocusable(false);
            mPlayButton.setEnabled(false);
            mPlayButton.setFocusable(false);
            mStopButton.setEnabled(true);
            mStopButton.setFocusable(true);
            //add by heml,for file list show
            mListButton.setEnabled(false);
            mListButton.setFocusable(false);

            mStateMessage1.setVisibility(View.VISIBLE);
            mStateLED.setVisibility(View.VISIBLE);
            mStateLED.setImageResource(R.drawable.recording_led);
            mStateMessage2.setVisibility(View.VISIBLE);
            mStateMessage2.setText(res.getString(R.string.recording));

            mExitButtons.setVisibility(View.INVISIBLE);
            mVUMeter.setVisibility(View.VISIBLE);

            mStateProgressBar.setVisibility(View.INVISIBLE);

            setTitle(res.getString(R.string.record_your_message));

            break;

        case Recorder.PLAYING_STATE:
            //add by heml,for file list show
            mListButton.setEnabled(false);
            mListButton.setFocusable(false);
            mRecordButton.setEnabled(false);
            mRecordButton.setFocusable(false);
            //add by heml,for file list show
            mPlayButton.setEnabled(false);
            mPlayButton.setFocusable(false);
            mStopButton.setEnabled(true);
            mStopButton.setFocusable(true);


            mStateMessage1.setVisibility(View.INVISIBLE);
            mStateLED.setVisibility(View.INVISIBLE);
            mStateMessage2.setVisibility(View.INVISIBLE);

            mExitButtons.setVisibility(View.VISIBLE);
            mVUMeter.setVisibility(View.INVISIBLE);

            mStateProgressBar.setVisibility(View.VISIBLE);

            setTitle(res.getString(R.string.review_message));

            break;
        }

        updateTimerView();
        mVUMeter.invalidate();
    }

    /*
     * Called when Recorder changed it's state.
     */
    public void onStateChanged(int state) {
        if (state == Recorder.PLAYING_STATE || state == Recorder.RECORDING_STATE) {
            mSampleInterrupted = false;
            mErrorUiMessage = null;
            mWakeLock.acquire(); // we don't want to go to sleep while recording or playing
        } else {
            if (mWakeLock.isHeld())
                mWakeLock.release();
        }

        updateUi();
    }

    /*
     * Called when MediaPlayer encounters an error.
     */
    public void onError(int error) {
        Resources res = getResources();

        String message = null;
        switch (error) {
        case Recorder.SDCARD_ACCESS_ERROR:
            message = res.getString(R.string.error_sdcard_access);
            break;
        case Recorder.IN_CALL_RECORD_ERROR:
            // TODO: update error message to reflect that the recording could not be
            //       performed during a call.
        case Recorder.INTERNAL_ERROR:
            message = res.getString(R.string.error_app_internal);
            break;
        }
        if (message != null) {
            new AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setPositiveButton(R.string.button_ok, null)
            .setCancelable(false)
            .show();
        }
    }

    private void createContinue() {
        if (mIntent != null) {
            String s = mIntent.getType();
            if (AUDIO_AMR.equals(s) || AUDIO_3GPP.equals(s) || AUDIO_ANY.equals(s)
                    || ANY_ANY.equals(s)) {
                mRequestedType = s;
            } else if (s != null) {
                // we only support amr and 3gpp formats right now
                setResult(RESULT_CANCELED);
                finish();
                return;
            }

            final String EXTRA_MAX_BYTES
            = android.provider.MediaStore.Audio.Media.EXTRA_MAX_BYTES;
            mMaxFileSize = mIntent.getLongExtra(EXTRA_MAX_BYTES, -1);
        }

        if (AUDIO_ANY.equals(mRequestedType) || ANY_ANY.equals(mRequestedType)) {
            mRequestedType = AUDIO_MP3;
            mRecorder = new Mp3Recorder();
            Log.v(TAG, "Default use mp3.");
        } else {
            mRecorder = new Recorder();
            Log.v(TAG, "Use Intent type success.");
        }
        mRecorder.setOnStateChangedListener(this);
        setContentView(R.layout.main);

        mRemainingTimeCalculator = new RemainingTimeCalculator();

        PowerManager pm
        = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                                   "SoundRecorder");

        initResourceRefs();

        setResult(RESULT_CANCELED);
        //registerExternalStorageListener();//add by heml,for file list show
        if (mBundle != null) {
            Bundle recorderState = mBundle.getBundle(RECORDER_STATE_KEY);
            if (recorderState != null) {
                mRecorder.restoreState(recorderState,this);
                mSampleInterrupted = recorderState.getBoolean(SAMPLE_INTERRUPTED_KEY, false);
                mMaxFileSize = recorderState.getLong(MAX_FILE_SIZE_KEY, -1);
            }
        }

        updateUi();
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == SOUND_RECORDER_PERMISSION_REQUEST) {
            boolean bAllGranted = true;
            int permissionLength = permissions.length;
            int resultLength = grantResults.length;
            if (permissionLength == 0) {
                createContinue();
                return;
            }
            if (permissionLength == resultLength) {
                for (int i = 0; i < permissionLength; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        bAllGranted = false;
                    }
                }
            } else {
                bAllGranted = false;
            }

            if (bAllGranted == true) {
                createContinue();
            } else {
                Log.v(TAG, "Permission not granted.");
                Toast.makeText(this, R.string.runtime_permissions_error, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
