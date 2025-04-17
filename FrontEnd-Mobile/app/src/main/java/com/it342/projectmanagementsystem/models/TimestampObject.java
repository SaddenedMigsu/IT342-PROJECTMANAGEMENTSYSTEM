package com.it342.projectmanagementsystem.models;

import com.google.gson.annotations.SerializedName;

public class TimestampObject {
    @SerializedName("seconds")
    private long seconds;
    
    @SerializedName("nanos")
    private int nanos;

    public static TimestampObject fromMillis(long millis) {
        TimestampObject obj = new TimestampObject();
        obj.seconds = millis / 1000;
        obj.nanos = (int) ((millis % 1000) * 1_000_000);
        return obj;
    }

    public long toMillis() {
        return seconds * 1000 + nanos / 1_000_000;
    }

    public long getSeconds() {
        return seconds;
    }

    public void setSeconds(long seconds) {
        this.seconds = seconds;
    }

    public int getNanos() {
        return nanos;
    }

    public void setNanos(int nanos) {
        this.nanos = nanos;
    }
} 