package com.hufs.ime.imecrunch;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;

public class DeviceInfoActivity extends AppCompatActivity {


    TextView textBandInformation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        getSupportActionBar().hide();

        textBandInformation = (TextView) findViewById(R.id.text_band_information);
        textBandInformation.setText("Name: " + MainActivity.pairedBands[0].getName() + "\n" +
            "Firmware: " + MainActivity.fwVersion + "\n" + "Hardware: " + MainActivity.hwVersion);

    }
}
