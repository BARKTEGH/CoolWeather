package com.coolweather.android.util;

import android.content.Context;
import android.text.TextUtils;
import com.coolweather.android.db.ChoosedCounty;
import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.gson.Weather;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.crud.DataSupport;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Utility {

    /**
     * 解析处理服务器返回的省级数据
     */
    public static boolean handleProvinceResponse(String response){
        if (!TextUtils.isEmpty(response)){
            try{
                JSONArray allProvinces = new JSONArray(response);
                for (int i = 0; i < allProvinces.length(); i++) {
                    JSONObject provinceObject = allProvinces.getJSONObject(i);
                    Province province = new Province();
                    province.setProvinceName(provinceObject.getString("name"));
                    province.setProvinceCode(provinceObject.getInt("id"));
                    province.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return false;
    }

    /**
     * 解析处理服务器返回的市ji数据
     * @param response
     * @param provinceId
     * @return
     */
    public static boolean handleCityResponse(String response, int provinceId){
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allCities = new JSONArray(response);
                for (int i = 0; i < allCities.length(); i++) {
                    JSONObject cityObject = allCities.getJSONObject(i);
                    City city = new City();
                    city.setCityName(cityObject.getString("name"));
                    city.setCityCode(cityObject.getInt("id"));
                    city.setProvinceId(provinceId);
                    city.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的县级数据
    */
    public static boolean handleCountyResponse(String response, int cityId) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allCounties = new JSONArray(response);
                for (int i = 0; i < allCounties.length(); i++) {
                    JSONObject countyObject = allCounties.getJSONObject(i);
                    County county = new County();
                    county.setCountyName(countyObject.getString("name"));
                    county.setWeatherId(countyObject.getString("weather_id"));
                    county.setCityId(cityId);
                    county.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static Weather handleWeatherResponse(String response){
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather6");
            String weatherContent = jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherContent, Weather.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static final Map<String, String> myMap;
    static
    {
        myMap = new HashMap<String, String>();
        myMap.put("comf","舒适度指数");
        myMap.put("cw","洗车指数");
        myMap.put("drsg","穿衣指数");
        myMap.put("flu","感冒指数");
        myMap.put("ptfc","交通指数");
        myMap.put("trav","旅游指数");
        myMap.put("sport","运动指数");
        myMap.put("uv","紫外线指数");
        myMap.put("air","空气污染扩散条件指数");
        myMap.put("ac","空调开启指数");
        myMap.put("ag","过敏指数");
        myMap.put("gl","太阳镜指数");
        myMap.put("mu","化妆指数");
        myMap.put("airc","晾晒指数");
        myMap.put("fsh","钓鱼指数");
        myMap.put("spi","防晒指数");
    }

    public static String translate(String type){
        return myMap.get(type);
    }


    public static void saveChoosedCounty(String countyName){
        //去重
        List<ChoosedCounty> all = DataSupport.findAll(ChoosedCounty.class);
        for (ChoosedCounty county: all){
            if (countyName.equals(county.getCountyName())){
                return;
            }
        }
        ChoosedCounty county = new ChoosedCounty(countyName);
        county.save();
    }

    public static String getProperties(Context c, String key){
        Properties props = new Properties();
        try {
            //方法一：通过activity中的context攻取setting.properties的FileInputStream
            //注意这地方的参数appConfig在eclipse中应该是appConfig.properties才对,但在studio中不用写后缀
            //InputStream in = c.getAssets().open("appConfig.properties");
            InputStream in = c.getAssets().open("config");
            //方法二：通过class获取setting.properties的FileInputStream
            //InputStream in = PropertiesUtill.class.getResourceAsStream("/assets/  setting.properties "));
            props.load(in);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return props.getProperty(key);
    }

}

