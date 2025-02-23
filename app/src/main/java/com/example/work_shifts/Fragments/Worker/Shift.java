package com.example.work_shifts.Fragments.Worker;

public class Shift {
    private String day;
    private String sTime;
    private String fTime;
    private String workerName;
    private String workerId;
    private String weekType;

    public Shift() {}

    public Shift(String day, String sTime, String fTime, String workerName, String workerId, String weekType) {
        this.day = day;
        this.sTime = sTime;
        this.fTime = fTime;
        this.workerName = workerName;
        this.workerId = workerId;
        this.weekType = weekType;
    }

    public String getDay() {
        return day;
    }

    public String getsTime() {
        return sTime;
    }

    public String getfTime() {
        return fTime;
    }

    public String getWorkerName() {
        return workerName;
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getWeekType() {
        return weekType;
    }
}
