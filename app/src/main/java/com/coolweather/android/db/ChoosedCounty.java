package com.coolweather.android.db;

import org.litepal.crud.DataSupport;

public class ChoosedCounty extends DataSupport {
    private String countyName;

    public ChoosedCounty(String countyName) {
        this.countyName = countyName;
    }

    public ChoosedCounty() {
    }

    public String getCountyName() {
        return countyName;
    }

    public void setCountyName(String countyName) {
        this.countyName = countyName;
    }
}
