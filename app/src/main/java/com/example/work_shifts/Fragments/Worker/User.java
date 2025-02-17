package com.example.work_shifts.Fragments.Worker;

public class User {
    private String companyName;
    private String email;
    private String phone;
    private boolean isAdmin;

    public User() {
    }

    public User(String companyName, String email, String phone, boolean isAdmin) {
        this.companyName = companyName;
        this.email = email;
        this.phone = phone;
        this.isAdmin = isAdmin;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public boolean getIsAdmin() {
        return isAdmin;
    }
}
