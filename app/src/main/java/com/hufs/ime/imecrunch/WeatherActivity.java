package com.hufs.ime.imecrunch;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.kweather.kweatheropenapi.KOpenApi_Client;
import com.kweather.kweatheropenapi.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WeatherActivity extends AppCompatActivity {
    private KOpenApi_Client kOpenApiClient;
    private String APPID = "ac35c8d9e7412f6928fc504a6e0afa73";

    private JsonObjectRequest req;
    private String url = "http://api.openweathermap.org/data/2.5/weather?q=yongin&APPID=ac35c8d9e7412f6928fc504a6e0afa73";
    private RequestQueue queue;

    private String weatherText;

    TextView textWeather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        textWeather = (TextView) findViewById(R.id.text_weather);
        textWeather.setText("Fetching weather data...");

        queue = Volley.newRequestQueue(this);
        req = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String city, humidity, weather="";
                    Double temp;
                    city = response.getString("name");
                    humidity = ((JSONObject) response.get("main")).getString("humidity");
                    temp = Double.valueOf(((JSONObject) response.get("main")).getString("temp")) - 273; // Kelvin to celsius

                    for (int i = 0; i < (response.getJSONArray("weather")).length(); i++) {
                        weather += ((JSONObject)response.getJSONArray("weather").get(i)).getString("main");
                        if (i < (response.getJSONArray("weather")).length()-1)
                            weather += ", ";
                    }

                    weatherText = String.format("City: %s\nTemperature: %f\nWeather: %s\nHumidity: %s", city, temp, weather, humidity);
                    textWeather.setText(weatherText);
                }catch (JSONException e) {
                    Log.e("ERROR", e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getBaseContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(req);
    }
}
