package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Weather {

    public String status;

    public Basic basic;

    public Update update;

    public Now now;

    public List<Forecast> daily_forecast;

    @SerializedName("lifestyle")
    public List<Lifestyle> lifestyles;
}
