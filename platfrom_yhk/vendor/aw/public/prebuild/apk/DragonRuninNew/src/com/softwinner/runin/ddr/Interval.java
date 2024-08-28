package com.softwinner.runin.ddr;

import android.os.Handler;
import android.os.Looper;

/* loaded from: classes.dex */
public class Interval {
    private final long interval;
    private final Runnable runnable;
    private final long startDelay;
    private boolean start = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable task = new Runnable() { // from class: com.clock.pt1.keeptesting.ddr.Interval.1
        @Override // java.lang.Runnable
        public void run() {
            if (Interval.this.start) {
                Interval.this.runnable.run();
            }
            Interval.this.handler.postDelayed(Interval.this.task, Interval.this.interval);
        }
    };

    public Interval(long j, long j2, Runnable runnable) {
        this.interval = j;
        this.startDelay = j2;
        this.runnable = runnable;
    }

    public void start() {
        this.handler.removeCallbacks(this.task);
        this.start = true;
        this.handler.postDelayed(this.task, this.startDelay);
    }

    public void stop() {
        this.start = false;
        this.handler.removeCallbacks(this.task);
    }
}