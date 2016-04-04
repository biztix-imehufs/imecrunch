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
    public static BandInfo[] pairedBands;
    BandClient bandClient;

    TextView textBandInformation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        getSupportActionBar().hide();

        pairedBands = BandClientManager.getInstance().getPairedBands();
        bandClient = BandClientManager.getInstance().create(getBaseContext(), pairedBands[0]);

        textBandInformation = (TextView) findViewById(R.id.text_band_information);

        if (pairedBands.length > 0) {
            textBandInformation.setText(pairedBands[0].getName());
        } else {
            Toast.makeText(getBaseContext(), "No paired bands", Toast.LENGTH_SHORT).show();
        }
    }
}
