package com.example.work_shifts.Fragments.Worker;

public class Shift {
    private String day;
    private String time;
    private String workerName;
    private String workerId;

    public Shift(String day, String time, String workerName, String workerId) {
        this.day = day;
        this.time = time;
        this.workerName = workerName;
        this.workerId = workerId;
    }

    public String getDay() {
        return day;
    }

    public String getTime() {
        return time;
    }

    public String getWorkerName() {
        return workerName;
    }

    public String getWorkerId() {
        return workerId; // Getter for workerId
    }
}
