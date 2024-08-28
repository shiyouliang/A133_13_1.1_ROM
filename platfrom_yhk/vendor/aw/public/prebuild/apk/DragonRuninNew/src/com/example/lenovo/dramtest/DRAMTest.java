package com.example.lenovo.dramtest;

import android.util.Log;

/* loaded from: classes.dex */
public class DRAMTest implements Runnable {
    private long correctData;
    private int currentCycles;
    private int errorCount;
    private long errorData;
    private long fileSize;
    private final String name;
    private boolean running = false;
    private Status status = Status.IDLE;
    private int totalCycles;

    /* loaded from: classes.dex */
    public enum Status {
        IDLE,
        TESTING,
        FINISH
    }

    private native boolean compareData(long j);

    private native void destoryMemoryTest(long j);

    private native long getCorrectData(long j);

    private native long getErrorData(long j);

    private native long getMemoryTestAddress(long j);

    private native void writeData(long j);

    public DRAMTest(String str) {
        this.name = str;
    }

    static {
        System.loadLibrary("native-lib");
    }

    public void stop() {
        this.running = false;
    }

    public boolean isError() {
        return this.errorCount > 0;
    }

    public int getErrorCount() {
        return this.errorCount;
    }

    public CharSequence getCyclesInfo() {
        return this.currentCycles + "/" + this.totalCycles;
    }

    public void setTotalCycles(int i) {
        this.totalCycles = i;
    }

    public String getErrorData() {
        long j = this.errorData;
        return j == 0 ? "" : Long.toHexString(j);
    }

    public long getFileSize() {
        return this.fileSize;
    }

    public void setFileSize(long j) {
        this.fileSize = j;
    }

    public Status getStatus() {
        return this.status;
    }

    @Override // java.lang.Runnable
    public void run() {
        int i;
        String str = this.name;
        this.status = Status.IDLE;
        this.errorCount = 0;
        this.running = true;
        this.currentCycles = 0;
        Log.d(str, "getMemoryTestAddress" + this.fileSize);
        long memoryTestAddress = getMemoryTestAddress(this.fileSize);
        Log.d(str, "memoryTestAddress " + memoryTestAddress);
        while (this.running && (i = this.currentCycles) < this.totalCycles) {
            this.currentCycles = i + 1;
            this.status = Status.TESTING;
            writeData(memoryTestAddress);
            if (!compareData(memoryTestAddress)) {
                this.errorCount++;
                this.errorData = getErrorData(memoryTestAddress);
                this.correctData = getCorrectData(memoryTestAddress);
            }
        }
        destoryMemoryTest(memoryTestAddress);
        Log.d(str, "FINISH ");
        this.status = Status.FINISH;
    }

    public void start() {
        new Thread(this).start();
    }
}