package com.hufs.ime.imecrunch;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class DeviceInfoActivity extends AppCompatActivity {


    TextView textBandInformation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        getSupportActionBar().hide();

        textBandInformation = (TextView) findViewById(R.id.text_band_information);
        textBandInformation.setText("Name: " + ActionRecognitionActivity.pairedBands[0].getName() + "\n" +
            "Firmware: " + ActionRecognitionActivity.fwVersion + "\n" + "Hardware: " + ActionRecognitionActivity.hwVersion);

    }
}
