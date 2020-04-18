package com.coolweather.android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.coolweather.android.db.ChoosedCounty;
import com.coolweather.android.util.BDLocationUtil;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

import static org.litepal.LitePalApplication.getContext;

public class CountyChoosedActivity extends AppCompatActivity {

    private Button addCountyButton;
    private ListView listView;
    private Button locationCityButton;
    private ArrayAdapter<String> adapter;
    private List<String> countyList = new ArrayList<>();
    private List<ChoosedCounty> countyListDB = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_county_choosed);
        queryChoosedCountyList();
        addCountyButton = (Button) findViewById(R.id.add_county_button);
        listView = (ListView) findViewById(R.id.county_list_choosed_view);
        locationCityButton = (Button) findViewById(R.id.location_city_button);

        String locationCityName = BDLocationUtil.getLocationDistrict(getApplicationContext());
        if (locationCityName == null){
            locationCityName = "北京";
            locationCityButton.setText(locationCityName+ "--重置位置");
        }else {
            locationCityButton.setText(locationCityName+"--定位");
        }
        final String finalLocationCityName = locationCityName;
        locationCityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CountyChoosedActivity.this, WeatherActivity.class);
                intent.putExtra("county_name", finalLocationCityName);
                startActivity(intent);
                finish();
            }
        });

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
                Intent intent = new Intent(getContext(), WeatherActivity.class);
                intent.putExtra("county_name",s);
                startActivity(intent);
                finish();
            }
        });



    }

    private void queryChoosedCountyList(){
        countyListDB = DataSupport.findAll(ChoosedCounty.class);
        System.out.println(countyListDB.size());
            for (ChoosedCounty county:countyListDB){
                countyList.add(county.getCountyName());
            }
//        }
    }




}
