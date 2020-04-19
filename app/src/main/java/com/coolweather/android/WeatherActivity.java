package com.coolweather.android;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.db.ChoosedCounty;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Lifestyle;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;
import com.coolweather.android.view.PocketSwipeRefreshLayout;


import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

@RequiresApi(api = Build.VERSION_CODES.M)
public class WeatherActivity extends AppCompatActivity {

    /**
     * 采用singleTask模式，只存在一个weather活动
     *
     */
    private static final String TAG = "WeatherActivity";

    private PocketSwipeRefreshLayout swipeRefreshLayout;
    private ImageView bingPIcImg;

    private SharedPreferences prefs;

    private ViewPager viewPager;
    private PagerAdapter adapter;
    //保存每个页面view
    private ArrayList<View> pageList = new ArrayList<>();
    //当前页索引
    private int position;
    //当前显示的城市
    private String curCountyName;

    //所有选中的城市
    private List<String> choosedCountyList = new ArrayList<>();
    //城市对应的位置
    private HashMap<String, Integer> countyIndex = new HashMap<>();


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
        prefs = PreferenceManager.getDefaultSharedPreferences
                (this);

        //下拉刷新
        swipeRefreshLayout = (PocketSwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(new PocketSwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updatePageView();
                Toast.makeText(WeatherActivity.this, "刷新",
                        Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        //tupian
        bingPIcImg = (ImageView) findViewById(R.id.bing_pic_img);
        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null){
            Glide.with(this).load(bingPic).into(bingPIcImg);
        }else {
            loadBingPIc();
        }

        choosedCountyList = initChoosedCounties();
        //初始化pager，每个页面都是一个城市的天气
        viewPager = (ViewPager) findViewById(R.id.weather_pager);
        LayoutInflater layoutInflater = getLayoutInflater();
        for (int i=0;i<choosedCountyList.size(); i++){
            View view = layoutInflater.inflate(R.layout.weather_layout, null);
            view.setTag(i);
            String countyName = choosedCountyList.get(i);
            fillView(view, countyName,true);
            pageList.add(view);
        }
        adapter = new PagerAdapter() {
            @Override
            public int getCount() {
                return pageList.size();
            }

            @Override
            public int getItemPosition(@NonNull Object object) {
                //更新其中一个页面
                View view = (View) object;
                int currentPagerIdx = viewPager.getCurrentItem();
                if (currentPagerIdx == (Integer) view.getTag()) {
                    return POSITION_NONE;
                } else {
                    return POSITION_UNCHANGED;
                }

            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                return view==object;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView(pageList.get(position));
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                container.addView(pageList.get(position));
                return pageList.get(position);
            }
        };
        viewPager.setAdapter(adapter);
        //侦测当前页面位置
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                WeatherActivity.this.position = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        curCountyName = prefs.getString("curCounty",choosedCountyList.get(0));
        position = getPositionFromName(curCountyName, choosedCountyList);
        viewPager.setCurrentItem(position);

    }

    private int getPositionFromName(String name,List<String> list){
        for (int i = 0; i < list.size(); i++) {
            if (name.equals(list.get(i))){
                return i;
            }
        }
        return -1;
    }

    public  boolean equalList(List<String> a, List<String> b) {
        if (a==b) return true;
        if (a==null || b==null) return false;
        int length = a.size();
        if (b.size() != length) return false;
        for (int i=0; i<length; i++)
            if (!a.get(i).equals(b.get(i))) return false;
        return true;
    }

    /**
     * 当重新激活后，
     * 1)比较选择城市是否发生改变，若无，显示当前选中的城市页面
     * 2 发生改变，添加或减少页面
     */
    @Override
    protected void onRestart() {
        super.onRestart();
        //当前选择的城市列表
        List<String> curList = initChoosedCounties();
        //当前变化
        if (!equalList(curList, choosedCountyList)){
            //保存以前的视图，部分没改变可以不用修改
            ArrayList<View> prePageList = new ArrayList<>(pageList);

            pageList = new ArrayList<>();
            LayoutInflater layoutInflater = getLayoutInflater();
            for (int i=0; i< curList.size();i++){
                String county = curList.get(i);
                int prePosition = getPositionFromName(county, choosedCountyList);
                if (prePosition != -1){
                    //城市没有删减，只需重新设置tag，保证下拉刷新找到对应页面
                    View view = prePageList.get(prePosition);
                    view.setTag(i);
                    pageList.add(i,view);
                }else {
                    //以前的没有，新增的城市，新增页面
                    View view = layoutInflater.inflate(R.layout.weather_layout, null);
                    view.setTag(i);
                    String countyName = curList.get(i);
                    fillView(view, countyName,true);
                    pageList.add(view);
                }
            }
            adapter.notifyDataSetChanged();
        }

        curCountyName = prefs.getString("curCounty",curList.get(0));
        position = getPositionFromName(curCountyName, curList);
        viewPager.setCurrentItem(position);
        viewPager.setOffscreenPageLimit(8);
    }


    private List<String> initChoosedCounties(){
        List<ChoosedCounty> all = DataSupport.findAll(ChoosedCounty.class);
        List<String> curList = new ArrayList<>();
        for (int i=0; i<all.size(); i++){
            curList.add(all.get(i).getCountyName());
        }
        return curList;
    }

    /**
     * 根据城市名获取内容，并填充view
     * @param countyName
     * @param isUpdate 是否更新天气数据
     */
    private void fillView(View view, String countyName, boolean isUpdate){
        if (isUpdate){
            requestWeather(view,countyName);
        }else {
            String weatherString = prefs.getString(countyName, null);
            Weather weather = null;
            if (weatherString == null){
                // 无缓存时去服务器查询天气
                requestWeather(view, countyName);
            }else {
                weather = Utility.handleWeatherResponse(weatherString);
                showWeatherInfo(view, weather);
            }
        }

    }

    /**
     * 根据城市名来查询天气
     * 支持中英文与拼音，
     * @param city
     */
    public void requestWeather(final View view, final String city){
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
                            SharedPreferences.Editor edit = prefs.edit();
                            edit.putString(city, responseText);
                            edit.apply();
                            showWeatherInfo(view, weather);
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
    private void showWeatherInfo(View view, Weather weather){
        // 初始化各控件
        ScrollView weatherLayout = (ScrollView) view.findViewById(R.id.weather_layout);
        //解决SwipeRefreshLayout与ScrollView的下拉冲突
        weatherLayout.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (swipeRefreshLayout != null){
                    swipeRefreshLayout.setEnabled(scrollY==0);
                }
            }
        });
        //添加按钮
        Button navButton = (Button) view.findViewById(R.id.nav_button);
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WeatherActivity.this,
                        CountyChoosedActivity.class);
                startActivity(intent);
            }
        });
        TextView titleCity = (TextView) view.findViewById(R.id.title_city);
        TextView titleUpdateTime = (TextView) view.findViewById(R.id.title_update_time);
        TextView degreeText = (TextView) view.findViewById(R.id.degree_text);
        TextView weatherInfoText = (TextView) view.findViewById(R.id.weather_info_text);
        LinearLayout forecastLayout = (LinearLayout) view.findViewById(R.id.forecast_layout);
        LinearLayout lifestyleLayout  = (LinearLayout) view.findViewById(R.id.lifestyle_layout);

        String cityName = weather.basic.location;
        String updateTime = weather.update.loc.split(" ")[1];
        String degree = weather.now.tmp+"℃";
        String weatherInfo = weather.now.cond_txt;
        //填充内容
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for (Forecast forecast:weather.daily_forecast){
            View viewN = LayoutInflater.from(this).inflate(R.layout.forecast_item,
                    forecastLayout, false);
            TextView dateText = (TextView) viewN.findViewById(R.id.date_text);
            TextView infoText = (TextView) viewN.findViewById(R.id.info_text);
            TextView minText = (TextView) viewN.findViewById(R.id.min_text);
            TextView maxText = (TextView) viewN.findViewById(R.id.max_text);

            dateText.setText(forecast.date);
            infoText.setText(forecast.cond_txt_d);
            minText.setText(forecast.tmp_min);
            maxText.setText(forecast.tmp_max);
            forecastLayout.addView(viewN);
        }
        lifestyleLayout.removeAllViews();
        for (Lifestyle lifestyle:weather.lifestyles){
            View viewN = LayoutInflater.from(this).inflate(R.layout.lifestyle_item,
                    lifestyleLayout, false);
            TextView lifestyle_text_text = (TextView) viewN.findViewById(R.id.lifestyle_text_text);
            TextView lifestyle_type_text = (TextView) viewN.findViewById(R.id.lifestyle_type_text);
            lifestyle_type_text.setText(Utility.translate(lifestyle.type) +" "+ lifestyle.brf);
            lifestyle_text_text.setText(lifestyle.txt);
            lifestyleLayout.addView(viewN);
        }
        weatherLayout.setVisibility(View.VISIBLE);
    }

    /**
     * 更新当前页天气
     */
    private void updatePageView(){
        View view = pageList.get(position);
        String countyName = choosedCountyList.get(position);
        fillView(view, countyName, true);
        adapter.notifyDataSetChanged();
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
