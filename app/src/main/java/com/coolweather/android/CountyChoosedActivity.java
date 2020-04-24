package com.coolweather.android;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.coolweather.android.db.ChoosedCounty;
import com.coolweather.android.util.BDLocationUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.litepal.LitePalApplication.getContext;

public class CountyChoosedActivity extends AppCompatActivity {

    private static final String TAG = "CountyChoosedActivity";
    private SharedPreferences prefs;
    private Button addCountyButton;
    private ListView listView;
    private Button locationCityButton;
    private ArrayAdapter<String> adapter;
    private List<String> countyList = new ArrayList<>();
    private List<ChoosedCounty> countyListDB = new ArrayList<>();

    private LocationClient locationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_county_choosed);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        queryChoosedCountyList();
        addCountyButton = (Button) findViewById(R.id.add_county_button);
        listView = (ListView) findViewById(R.id.county_list_choosed_view);
        locationCityButton = (Button) findViewById(R.id.location_city_button);

        startLocation();

        addCountyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), ChooseAreaActivity.class);
                startActivity(intent);
                finish();
            }
        });

        adapter = new ArrayAdapter<String>(CountyChoosedActivity.this,
                android.R.layout.simple_list_item_1, countyList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String s = countyList.get(position);
                SharedPreferences.Editor edit = prefs.edit();
                edit.putString("curCounty", s);
                edit.apply();
                Intent intent = new Intent(getContext(), WeatherActivity.class);
                startActivity(intent);
                finish();
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                //定义AlertDialog.Builder对象，当长按列表项的时候弹出确认删除对话框
                AlertDialog.Builder builder=new AlertDialog.Builder(CountyChoosedActivity.this);
                builder.setMessage("确定删除?");
                builder.setTitle("提示");

                //添加AlertDialog.Builder对象的setPositiveButton()方法
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String countyName= countyList.get(position);
                        if(countyList.remove(position)!=null){
                            DataSupport.deleteAll(ChoosedCounty.class,
                                    "countyName = ?", countyName);
                            System.out.println("success");
                        }else {
                            System.out.println("failed");
                        }
                        adapter.notifyDataSetChanged();
                        Toast.makeText(getBaseContext(), "删除列表项", Toast.LENGTH_SHORT).show();
                    }
                });

                //添加AlertDialog.Builder对象的setNegativeButton()方法
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                AlertDialog alertDialog = builder.create();
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.show();
                return true;

            }
        });

    }

    private void startLocation(){
        locationClient = new LocationClient(getApplicationContext());
        locationClient.registerLocationListener(new BDLocationListener() {
            @Override
            public void onReceiveLocation(final BDLocation location) {
                String district = location.getDistrict();
                if (district == null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(CountyChoosedActivity.this,
                                    "无法获取定位，重置定位为北京",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    district = "北京";
                    locationCityButton.setText( district+"--重新定位");
                }else {
                    locationCityButton.setText( district+"--定位");
                }
                locationClient.stop();
                final String finalDistrict = district;
                locationCityButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SharedPreferences.Editor edit = prefs.edit();
                        Utility.saveChoosedCounty(finalDistrict);
                        edit.putString("curCounty", finalDistrict);
                        edit.apply();
                        Intent intent = new Intent(CountyChoosedActivity.this, WeatherActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        });
        LocationClientOption option = new LocationClientOption();
        option.setIsNeedAddress(true);
        locationClient.setLocOption(option);
        locationClient.start();
    }


    private void queryChoosedCountyList(){
        countyListDB = DataSupport.findAll(ChoosedCounty.class);
        for (ChoosedCounty county:countyListDB){
            countyList.add(county.getCountyName());
        }
    }


}
