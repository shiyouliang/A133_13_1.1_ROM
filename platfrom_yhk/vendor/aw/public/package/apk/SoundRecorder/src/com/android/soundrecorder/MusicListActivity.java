package com.android.soundrecorder;

import android.app.AlertDialog;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.KeyEvent;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.DialogInterface;

import android.app.Activity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicListActivity extends Activity {
    private List<File> musicFiles;
    private String sdCardPath = Environment.getExternalStorageDirectory() + "/Recordings";
    private boolean isEmpty = true;
    private TextView empty = null;
    private ListView listView = null;
    private MusicAdapter musicAdapter = null;

    private List<MusicInfo> fileMusicInfo = null;
    private TextView countTime=null;
    private TextView fileName=null;
    private TextView fullTime=null;
    private TextView closeDialog=null;
    private ProgressBar mProgressBar=null;

    private boolean isCloseClick=false;
    private MediaPlayer mediaPlayer = null;
    private Handler mhandler =  new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String messageData = (String) msg.obj;
            switch (msg.what) {
            case 0:
                if(countTime!=null) {
                    int time=msg.arg2;
                    countTime.setText(HelpUtils.formatMilliseconds(time));
                }
                break;
            case 1:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(fileMusicInfo==null) {
                            empty.setVisibility(View.VISIBLE);
                            listView.setVisibility(View.GONE);
                        } else {
                            empty.setVisibility(View.GONE);
                            listView.setVisibility(View.VISIBLE);
                        }
                    }
                });
                break;
            case 2:
                if(fileMusicInfo==null) {
                    return;
                }
                musicAdapter = new MusicAdapter(MusicListActivity.this, fileMusicInfo);
                listView.setAdapter(musicAdapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        showDialog(fileMusicInfo.get(position).getFilePath(), fileMusicInfo.get(position).getFileName(), fileMusicInfo.get(position).getFileTime());
                    }
                });
				 // 设置长按对话框  
				listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {  
					@Override  
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {    
					AlertDialog.Builder builder = new AlertDialog.Builder(MusicListActivity.this,AlertDialog.THEME_HOLO_LIGHT);  
					builder.setTitle("Options");  
					builder.setItems(R.array.options, new DialogInterface.OnClickListener() {  
						public void onClick(DialogInterface dialog, int which) {  
							switch (which) {
								case 0: //删除操作  
								  DeleteshowDialog(position);
									break;   
							}  
							// 重置列表视图以显示新的数据...  
							musicAdapter.notifyDataSetChanged();  
						}  
					});  
					builder.create().show(); //显示对话框...  
					return true; 
				}
				});
                break;
            case 3:
                if (musicAdapter != null) {
                    musicAdapter.notifyDataSetChanged();
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_list);
        musicFiles = new ArrayList<File>();
        empty = findViewById(R.id.empty);

        listView = (ListView) findViewById(R.id.music_list);
        MyThread myThread = new MyThread();
        myThread.start();
    }

    public class MyThread extends Thread {
        @Override
        public void run() {
            getMusicList();
            sendMsg(2);
            sendMsg(1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendMsg(3);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
	
	private void DeleteshowDialog(int position) {
		AlertDialog.Builder builder = new AlertDialog.Builder(MusicListActivity.this,AlertDialog.THEME_HOLO_LIGHT);  
		builder.setTitle("Delete");
		builder.setMessage("Are you sure you want to delete this file?");  
		builder.setPositiveButton("yes", new DialogInterface.OnClickListener() {  
			@Override  
			public void onClick(DialogInterface dialog, int which) {  
				// 删除文件操作... 
				try{
				File file = new File(fileMusicInfo.get(position).getFilePath()); 
					if(file.delete())
					{
						Toast.makeText(MusicListActivity.this, "Delete Success", Toast.LENGTH_SHORT).show();
						fileMusicInfo.remove(position);
						// 重置列表视图以显示新的数据...  
						musicAdapter.notifyDataSetChanged(); 										
					}
					} catch (Exception e) {
						e.printStackTrace();
					}
			}  
		});  
		builder.setNegativeButton("no", null);  
		builder.create().show();
	}

    private void showDialog(String path, String name, String time) {
        isCloseClick=false;
        final AlertDialog dialog = new AlertDialog.Builder(MusicListActivity.this,AlertDialog.THEME_HOLO_LIGHT)
        .setTitle(getResources().getString(R.string.playing_message))
        .setView(R.layout.dialog_player)
        .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                if(keyEvent.getKeyCode()==KeyEvent.KEYCODE_BACK) {
                    isCloseClick=true;
                    if (mediaPlayer != null) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                    dialog.dismiss();
                }
                return false;
            }
        });

        fileName = dialog.findViewById(R.id.file_name);
        fileName.setText(name);
        countTime = dialog.findViewById(R.id.counttime);
        countTime.setText("00:00:00");
        fullTime= dialog.findViewById(R.id.fulltime);
        fullTime.setText(time);


        closeDialog = dialog.findViewById(R.id.close_dialog);
        closeDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isCloseClick=true;
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                dialog.dismiss();
            }
        });

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mediaPlayer = new MediaPlayer();
        File file = new File(path);
        try {
            mediaPlayer.setDataSource(file.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mProgressBar = dialog.findViewById(R.id.seek_bar);
        try {
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mProgressBar.setMax(mediaPlayer.getDuration());

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                dialog.dismiss();
            }
        });
        MyCountThread mMyCountThread = new MyCountThread(mProgressBar,countTime);
        mMyCountThread.start();
    }

    public class MyCountThread extends Thread {
        TextView countTime=null;
        ProgressBar progressBar=null;
        public MyCountThread(ProgressBar progressBar,TextView countTime) {
            this.countTime=countTime;
            this.progressBar=progressBar;
        }
        @Override
        public void run() {
            int currentPosition=0;
            if(mediaPlayer==null) {
                return;
            }
            int total=mediaPlayer.getDuration();
            while(currentPosition<total) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                if(isCloseClick) {
                    return;
                }
                if(mediaPlayer==null) {
                    return;
                }
                currentPosition = mediaPlayer.getCurrentPosition();
                progressBar.setProgress(currentPosition);

                Message msg = new Message();
                msg.what=0;
                msg.arg2=currentPosition;
                mhandler.sendMessage(msg);
            }
        }
    }

    public void updateTime(TextView countTime,final long time) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                countTime.setText(HelpUtils.formatMilliseconds(time));
            }
        });

    }

    private List<MusicInfo> getMusicList() {
        fileMusicInfo = new ArrayList<MusicInfo>();
        fileMusicInfo.clear();
        final List<Map<String, Object>> listItems = new ArrayList<>();
        File musicDirectory = new File(sdCardPath);
        if (musicDirectory.exists()) {
            musicDirectory.setReadable(true);
            File[] files = musicDirectory.listFiles();
            if (files != null) {
                musicFiles.clear();
                isEmpty = true;
                for (File file : files) {
                    if ((file.getName().endsWith(".mp3") ||file.getName().endsWith(".amr")||file.getName().endsWith(".3gpp"))&& (!file.getName().contains("trashed"))) {
                        isEmpty = false;
                        musicFiles.add(file);
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(file.getAbsolutePath());
                        String time = null;
                        time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                        String time1 = HelpUtils.formatMilliseconds(Integer.parseInt(time));
                        MusicInfo musicInfo = new MusicInfo(file.getName(), "" + time1, file.getAbsolutePath());
                        fileMusicInfo.add(musicInfo);
                    }
                }
            } else {
                isEmpty=true;
            }
        } else {
            isEmpty=true;
        }
        if (isEmpty) {
            fileMusicInfo=null;
            sendMsg(1);
            return null;
        }
        sendMsg(1);
        sendMsg(2);
        return fileMusicInfo;
    }

    public void sendMsg(int value) {
        Message msg = new Message();
        msg.what=value;
        mhandler.sendMessage(msg);
    }
}