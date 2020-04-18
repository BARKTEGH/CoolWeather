package com.coolweather.android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Lifestyle;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;


import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private LinearLayout lifestyleLayout;
    private ImageView bingPIcImg;

    private ViewPager viewPager;

    public SwipeRefreshLayout swipeRefreshLayout;

    //当前显示的城市
    private String countyName;

//    public DrawerLayout drawerLayout;
    private Button navButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        //实现状态栏配色统一
        if (Build.VERSION.SDK_INT>=21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);

        // 初始化各控件
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        lifestyleLayout  = (LinearLayout) findViewById(R.id.lifestyle_layout);
        bingPIcImg = (ImageView) findViewById(R.id.bing_pic_img);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        navButton = (Button) findViewById(R.id.nav_button);

        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences
                (this);
        //传入的县名字，调用都需要传入
        countyName = getIntent().getStringExtra("county_name");
        String weatherString = prefs.getString(countyName, null);
        if (weatherString != null ) {
            // 有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
        } else {
            // 无缓存时去服务器查询天气
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(countyName);
        }
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(countyName);
            }
        });
        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null){
            Glide.with(this).load(bingPic).into(bingPIcImg);
        }else {
            loadBingPIc();
        }

        //添加城市
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WeatherActivity.this, CountyChoosedActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * 根据城市名来查询天气
     * 支持中英文与拼音，
     * @param city
     */
    public void requestWeather(final String city){
        String weatherUrl = "https://free-api.heweather.net/s6/weather/?location="
                +city+"&key=a8f7289cd0d044529f56932856345b60";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败",
                                Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather!=null && "ok".equals(weather.status)){
                            @SuppressLint("CommitPrefEdits")
                            SharedPreferences.Editor edit = PreferenceManager.
                                    getDefaultSharedPreferences(WeatherActivity.this).
                                    edit();
                            edit.putString(city, responseText);
                            edit.apply();
                            showWeatherInfo(weather);
                        }else {
                            Toast.makeText(WeatherActivity.this,"获取天气信息失败",
                                    Toast.LENGTH_SHORT).show();
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
        loadBingPIc();
    }

    /**
     * 处理并展示Weather实体类数据信息
     * @param weather
     */
    private void showWeatherInfo(Weather weather){
        String cityName = weather.basic.location;
        String updateTime = weather.update.loc.split(" ")[1];
        String degree = weather.now.tmp+"℃";
        String weatherInfo = weather.now.cond_txt;

        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);

        forecastLayout.removeAllViews();
        for (Forecast forecast:weather.daily_forecast){
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,
                    forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);

            dateText.setText(forecast.date);
            infoText.setText(forecast.cond_txt_d);
            minText.setText(forecast.tmp_min);
            maxText.setText(forecast.tmp_max);
            forecastLayout.addView(view);
        }
        lifestyleLayout.removeAllViews();
        for (Lifestyle lifestyle:weather.lifestyles){
            View view = LayoutInflater.from(this).inflate(R.layout.lifestyle_item,
                    lifestyleLayout, false);
            TextView lifestyle_text_text = (TextView) view.findViewById(R.id.lifestyle_text_text);
            TextView lifestyle_type_text = (TextView) view.findViewById(R.id.lifestyle_type_text);

            lifestyle_type_text.setText(Utility.translate(lifestyle.type) +" "+ lifestyle.brf);
            lifestyle_text_text.setText(lifestyle.txt);
            lifestyleLayout.addView(view);
        }
        weatherLayout.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }

    private void loadBingPIc(){
        String requestBingPicUrl = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPicUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String pic = response.body().string();
                SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                edit.putString("bing_pic", pic);
                edit.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(pic).into(bingPIcImg);
                    }
                });
            }
        });


    }
}
