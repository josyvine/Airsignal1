package com.example.models;

public class CallLogItem {
    private long id;
    private String number;
    private String type; // "INCOMING", "OUTGOING", "MISSED", "AUDIO_DATA"
    private long duration; // seconds
    private long timestamp;

    public CallLogItem(long id, String number, String type, long duration, long timestamp) {
        this.id = id;
        this.number = number;
        this.type = type;
        this.duration = duration;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public String getNumber() { return number; }
    public String getType() { return type; }
    public long getDuration() { return duration; }
    public long getTimestamp() { return timestamp; }
}
