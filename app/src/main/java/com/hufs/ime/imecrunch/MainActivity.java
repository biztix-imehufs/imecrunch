package com.hufs.ime.imecrunch;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandPendingResult;
import com.microsoft.band.ConnectionState;

public class MainActivity extends AppCompatActivity implements Runnable {
    public static BandInfo[] pairedBands;
    private BandClient bandClient;
    private BandPendingResult<ConnectionState> pendingResult;

    public static String fwVersion;
    public static String hwVersion;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pairedBands = BandClientManager.getInstance().getPairedBands();
        bandClient = BandClientManager.getInstance().create(getBaseContext(), pairedBands[0]);

        if (pairedBands.length == 0) {
            Toast.makeText(getBaseContext(), "No paired bands", Toast.LENGTH_SHORT).show();
        } else {
            /**
             * Code for starting sensor subscribing
             */
            pendingResult = bandClient.connect();
            Thread connectionThread = new Thread(this);
            connectionThread.start();

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if(id == R.id.action_info) {
            if (pairedBands.length > 0) {
                startActivity(new Intent(getBaseContext(), DeviceInfoActivity.class));
            } else {
                Toast.makeText(getBaseContext(), "No paired bands", Toast.LENGTH_SHORT).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void run() {
        try {
            ConnectionState state = pendingResult.await();

            if (state == ConnectionState.CONNECTED) {
                // on success
                fwVersion = bandClient.getFirmwareVersion().await();
                hwVersion = bandClient.getHardwareVersion().await();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), "Connected to band", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // on failed
            }
        }catch (InterruptedException ex) {
            // handle InterruptedException
        }catch (BandException ex) {
            // handle BandException
        }
    }
}
