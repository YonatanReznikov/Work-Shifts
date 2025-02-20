package com.example.work_shifts.Fragments.Worker;

public class Shift {
    private String day;
    private String time;
    private String workerName;

    public Shift(String day, String time, String workerName) {
        this.day = day;
        this.time = time;
        this.workerName = workerName;
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
}

