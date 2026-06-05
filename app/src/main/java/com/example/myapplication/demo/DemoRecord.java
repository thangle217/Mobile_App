package com.example.myapplication.demo;

public class DemoRecord {
    public final String code;
    public final String title;
    public final String status;
    public final String value;
    public final String note;

    public DemoRecord(String code, String title, String status, String value, String note) {
        this.code = code;
        this.title = title;
        this.status = status;
        this.value = value;
        this.note = note;
    }
}