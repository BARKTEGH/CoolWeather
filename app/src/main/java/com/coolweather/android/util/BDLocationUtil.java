package com.coolweather.android.util;

import android.content.Context;

import com.baidu.location.BDLocation;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;


public class BDLocationUtil {

    private static LocationClientOption option = new LocationClientOption();
    private static String district;

    //当出现无法定位或者定位失败情况时，定位县城返回ERROR
    public static final String DISTRICT_ERROR_LOCATION = "ERROR";

    public static String getLocationDistrict(Context context){
        //重新初始化
        district = null;
        option.setIsNeedAddress(true);
        BMapLocationHelper helper = BMapLocationHelper.create(context, option, new CityCallback());
        SDKInitializer.initialize(context);
        helper.locStart();
        helper.locStop();
        return district;
    }

    public static class CityCallback extends BMapLocationHelper.LocationCallBack {

        @Override
        public void onReceiveLocation(int statusCode, BDLocation bdLocation, String errMsg) {
            System.out.println(bdLocation.getLocType());
           district = bdLocation.getDistrict();
        }
    }
}
