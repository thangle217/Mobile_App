package com.example.myapplication.demo;

import java.util.ArrayList;
import java.util.List;

public final class DemoSeed {
    private DemoSeed() {}

    public static List<DemoRecord> rooms() {
        List<DemoRecord> data = new ArrayList<>();
        data.add(new DemoRecord("P101", "Phong Deluxe 101", "Da thue", "4.500.000d/thang", "Tang 1 - Nha tro Binh Minh"));
        data.add(new DemoRecord("P102", "Phong Studio 102", "Con trong", "3.800.000d/thang", "Tang 1 - Nha tro Binh Minh"));
        data.add(new DemoRecord("P203", "Phong Gac 203", "Dang sua", "3.200.000d/thang", "Tang 2 - Nha tro Hoa Sen"));
        return data;
    }

    public static List<DemoRecord> tenants() {
        List<DemoRecord> data = new ArrayList<>();
        data.add(new DemoRecord("NT001", "Nguyen Minh Anh", "P101", "0988 123 456", "Dang thue tu 01/05/2026"));
        data.add(new DemoRecord("NT002", "Tran Gia Bao", "P203", "0977 222 555", "Cho cap nhat hop dong"));
        return data;
    }

    public static List<DemoRecord> invoices() {
        List<DemoRecord> data = new ArrayList<>();
        data.add(new DemoRecord("HD0526-101", "Hoa don P101", "Chua thanh toan", "5.120.000d", "Tien phong + dien + nuoc + internet"));
        data.add(new DemoRecord("HD0526-203", "Hoa don P203", "Thanh toan mot phan", "3.760.000d", "Da xac nhan 2.000.000d"));
        return data;
    }

    public static List<DemoRecord> notices() {
        List<DemoRecord> data = new ArrayList<>();
        data.add(new DemoRecord("TB01", "Hoa don thang 05 da duoc tao", "Moi", "He thong", "Nguoi thue vui long thanh toan truoc ngay 25"));
        data.add(new DemoRecord("TB02", "Yeu cau sua chua dang xu ly", "Da doc", "Chu tro", "Se kiem tra phong P101 vao chieu nay"));
        return data;
    }
}