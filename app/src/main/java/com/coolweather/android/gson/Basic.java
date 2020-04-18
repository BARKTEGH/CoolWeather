package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

public class Basic {

    @SerializedName("location")
    public String location;

    @SerializedName("cid")
    public String cid;

    public String lat;

    public String lon;

    public String parent_city;

    public String admin_area;

    public String cnty;

    public String tz;
}
