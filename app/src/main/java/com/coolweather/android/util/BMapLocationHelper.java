package com.coolweather.android.util;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;


public class BMapLocationHelper {

    private static LocationClient locationClient;
    private static final int LOCATION_SUCCESS = 1;
    static final int LOCATION_FAIL = -1;
    private LocationClient mLocationClient;
    private MyLocationListener myListener = new MyLocationListener();
    private LocationCallBack callBack;


    private BMapLocationHelper(LocationCallBack callBack) {

        this.callBack = callBack;
    }

    public static BMapLocationHelper create(@NonNull Context context, @NonNull LocationClientOption option, @NonNull LocationCallBack callBack) {
        BMapLocationHelper bMapLocationHelper = new BMapLocationHelper(callBack);
        LocationClient client = new LocationClient(context);
        client.setLocOption(option);
        //声明LocationClient类
        client.registerLocationListener(bMapLocationHelper.myListener);
        bMapLocationHelper.mLocationClient = client;
        return bMapLocationHelper;
    }

    /**
     * 开始定位
     */
    public void locStart() {
        mLocationClient.start();
    }

    /**
     * 停止定位
     */
    public void locStop() {
        mLocationClient.stop();
    }

    public void locReStart() {
        mLocationClient.restart();
    }

    public LocationClient getmLocationClient() {
        return mLocationClient;
    }

    /**
     * 地图定位结果监听类
     */
    public class MyLocationListener extends BDAbstractLocationListener {
        private static final String TAG = "MyLocationListener";

        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null) return;
            int locType = location.getLocType();
            int status = LOCATION_SUCCESS;
            if (locType != 61 && locType != 161 && locType != 66) status = LOCATION_FAIL;
            String errMsg = getLocationResultMsg(locType);
            callBack.onReceiveLocation(status, location, errMsg);
        }

        @Override
        public void onLocDiagnosticMessage(int i, int i1, String s) {
            Log.i(TAG, "onLocDiagnosticMessage: " + i + "diaType:" + i1);
            callBack.onLocDiagnosticMessage(i, i1, getLocDiagnosticMessage(i, i1));
            super.onLocDiagnosticMessage(i, i1, s);
        }
    }


    /**
     * 回调类
     */
    public abstract static class LocationCallBack {

        /**
         * 定位的结果
         *
         * @param statusCode 状态码,1:定位成功，-1定位失败
         * @param bdLocation 定位成功时返回的定位结果对象
         * @param errMsg     定位失败时的错误信息，成功时则为null
         */
        public abstract void onReceiveLocation(int statusCode, BDLocation bdLocation, String errMsg);

        /**
         * 错误的状态码
         * <a>http://lbsyun.baidu.com/index.php?title=android-locsdk/guide/addition-func/error-code</a>
         * <p>
         * 回调定位诊断信息，开发者可以根据相关信息解决定位遇到的一些问题
         *
         * @param locType           当前定位类型
         * @param diagnosticType    诊断类型（1~9）
         * @param diagnosticMessage 具体的诊断信息释义
         */
        public void onLocDiagnosticMessage(int locType, int diagnosticType, String diagnosticMessage) {
        }
    }

    /**
     * 错误的状态码
     * <a>http://lbsyun.baidu.com/index.php?title=android-locsdk/guide/addition-func/error-code</a>
     *
     * @param locType 当前定位类型
     * @return String 定位成功或失败的信息
     */
    private String getLocationResultMsg(int locType) {
        switch (locType) {
            case 61:
                return "GPS定位结果，GPS定位成功";
            case 62:
                return "无法获取有效定位依据，定位失败，请检查运营商网络或者WiFi网络是否正常开启，尝试重新请求定位";
            case 63:
                return "网络异常，没有成功向服务器发起请求，请确认当前测试手机网络是否通畅，尝试重新请求定位";
            case 66:
                return "离线定位结果。通过requestOfflineLocaiton调用时对应的返回结果";
            case 67:
                return "离线定位失败";
            case 161:
                return "网络定位结果，网络定位成功";
            case 162:
                return "请求串密文解析失败，一般是由于客户端SO文件加载失败造成，请严格参照开发指南或demo开发，放入对应SO文件";
            case 167:
                return "服务端定位失败，请您检查是否禁用获取位置信息权限，尝试重新请求定位";
            case 505:
                return "AK不存在或者非法，请按照说明文档重新申请AK";
            default:
                return "";
        }
    }

    /**
     * @param locType        当前定位类型
     * @param diagnosticType 诊断类型（1~9）
     * @return String
     */
    private String getLocDiagnosticMessage(int locType, int diagnosticType) {

        switch (locType) {
            case 62:
                switch (diagnosticType) {
                    case 4:
                        return "定位失败，无法获取任何有效定位依据";
                    case 5:
                        return "定位失败，无法获取有效定位依据，请检查运营商网络或者Wi-Fi网络是否正常开启，尝试重新请求定位";
                    case 6:
                        return "定位失败，无法获取有效定位依据，请尝试插入一张sim卡或打开Wi-Fi重试";
                    case 7:
                        return "定位失败，飞行模式下无法获取有效定位依据，请关闭飞行模式重试";
                    case 9:
                        return "定位失败，无法获取任何有效定位依据";
                }
            case 67:
                if (diagnosticType == 3) return "定位失败，请您检查您的网络状态";

            case 161:
                switch (diagnosticType) {
                    case 1:
                        return "定位失败，建议您打开GPS";
                    case 2:
                        return "定位失败，建议您打开Wi-Fi";
                }
            case 167:
                if (diagnosticType == 8) return "定位失败，请确认您定位的开关打开状态，是否赋予APP定位权限";

            default:
                return "未知错误";
        }
    }

}
