package com.coolweather.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.coolweather.android.db.ChoosedCounty;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.BDLocationUtil;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateBingPic();
        //更新天气
        String districtLocation = BDLocationUtil.getLocationDistrict(getApplicationContext());
        updateWeather(districtLocation);
        List<ChoosedCounty> list = DataSupport.findAll(ChoosedCounty.class);
        for(ChoosedCounty county:list){
            updateWeather(county.getCountyName());
        }

        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int time = 8 * 60 * 60 * 1000;
        long triggerTime = SystemClock.elapsedRealtime()+time;
        Intent intentA = new Intent(this, AutoUpdateService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intentA, 0);
        manager.cancel(pendingIntent);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void updateWeather(final String name){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherUrl =  "https://free-api.heweather.net/s6/weather/?location="
                +name+"&key="+Utility.getProperties(getApplicationContext(), "hefengKEY");
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String reponseText = response.body().string();
                Weather weather = Utility.handleWeatherResponse(reponseText);
                if (weather!=null  && "ok".equals(weather.status)){
                    SharedPreferences.Editor edit = PreferenceManager.
                            getDefaultSharedPreferences(AutoUpdateService.this).edit();
                    edit.putString(name, reponseText);
                    edit.apply();
                }
            }
        });
    }

    private void updateBingPic(){
        String requestBingPicUrl = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPicUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String pic = response.body().string();
                SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                edit.putString("bing_pic", pic);
                edit.apply();
            }
        });
    }
}
