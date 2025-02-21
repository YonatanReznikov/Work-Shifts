package com.example.work_shifts.Fragments.Worker;

public class Shift {
    private String day;
    private String sTime;
    private String fTime;
    private String workerName;
    private String workerId;

    // Required empty constructor for Firebase
    public Shift() {}

    public Shift(String day, String sTime, String fTime, String workerName, String workerId) {
        this.day = day;
        this.sTime = sTime;
        this.fTime = fTime;
        this.workerName = workerName;
        this.workerId = workerId;
    }

    public String getDay() {
        return day;
    }

    public String getStartTime() {
        return sTime;
    }

    public String getEndTime() {
        return fTime;
    }

    public String getWorkerName() {
        return workerName;
    }

    public String getWorkerId() {
        return workerId;
    }

}
