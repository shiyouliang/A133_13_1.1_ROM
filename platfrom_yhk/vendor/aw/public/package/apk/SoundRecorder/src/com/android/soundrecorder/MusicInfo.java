package com.android.soundrecorder;

import android.widget.TextView;

import java.io.File;

public class MusicInfo {
    private String fileName = null;
    private String fileTime = null;
    private String filePath = null;

    public MusicInfo(String fileName, String fileTime, String filePath) {
        this.fileName = fileName;
        this.fileTime = fileTime;
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileTime() {
        return fileTime;
    }

    public void setFileTime(String fileTime) {
        this.fileTime = fileTime;
    }
}
