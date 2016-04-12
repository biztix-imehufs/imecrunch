package com.hufs.ime.imecrunch;

import android.Manifest;
import android.app.Activity;
import android.app.VoiceInteractor;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionMenu;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandPendingResult;
import com.microsoft.band.ConnectionState;

import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.*;

import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements HeartRateConsentListener {
    public static BandInfo[] pairedBands;
    private BandClient bandClient;
    private BandPendingResult<ConnectionState> pendingResult;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private TextView textStatus;
    private TextView textSensors;
    private TextView textGyro;
    private TextView textHeart;
    private com.github.clans.fab.FloatingActionButton btnWalking,  btnRunning;
    private FloatingActionMenu menu;

    public static String fwVersion;
    public static String hwVersion;

    private BandHeartRateEventListener hearListener;
    private HeartRateConsentListener heartConsentListener;
    private BandAccelerometerEventListener accelerometerListener;
    private BandGsrEventListener gsrListener;
    private BandGyroscopeEventListener gyroListener;

    private ArrayList<Double> accelXList = new ArrayList<>();
    private ArrayList<Double> accelYList = new ArrayList<>();
    private ArrayList<Double> accelZList = new ArrayList<>();

    private ArrayList<Double> gyroAccelXList = new ArrayList<>();
    private ArrayList<Double> gyroAngularXList = new ArrayList<>();

    public static double gyroAccelX;

    public int counter = 1;



    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (bandClient == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                appendToUI("Band isn't paired with your phone.\n");
                return false;
            }
            bandClient = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == bandClient.getConnectionState()) {
            if (bandClient.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {

            }else {
                bandClient.getSensorManager().requestHeartRateConsent(this, this);
            }
            return true;
        }

        appendToUI("Band is connecting...\n");
        setGyroText("Connecting...");
        return ConnectionState.CONNECTED == bandClient.connect().await();
    }

    private void appendToUI(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textSensors.setText(string);
            }
        });
    }

    private void setGyroText(final String n){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textGyro.setText(n);
            }
        });
    }

    private void setHeartText(final String n){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textHeart.setText(n);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textStatus = (TextView) findViewById(R.id.txt_status);
        textSensors = (TextView) findViewById(R.id.text_sensor);
        textGyro = (TextView)findViewById(R.id.text_gyro);
        textHeart = (TextView)findViewById(R.id.text_heart);

        pairedBands = BandClientManager.getInstance().getPairedBands();

        verifyStoragePermissions(this);

        // start timer counter
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                counter++;
                if(counter >= 11) {
                    counter = 1;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //textStatus.setText(""+counter);
                    }
                });
            }
        }, 0, 100);

        accelerometerListener = new BandAccelerometerEventListener() {
            @Override
            public void onBandAccelerometerChanged(BandAccelerometerEvent event) {
                accelXList.add(new Double(event.getAccelerationX()));
                accelYList.add(new Double(event.getAccelerationY()));
                accelZList.add(new Double(event.getAccelerationZ()));
                if (counter == 10) {
                    Double[] dAccelX = accelXList.toArray(new Double[accelXList.size()]);
                    Double[] dAccelY = accelYList.toArray(new Double[accelYList.size()]);
                    Double[] dAccelZ = accelZList.toArray(new Double[accelZList.size()]);

                    double accelXMean = StdStats.mean(ArrayUtils.toPrimitive(dAccelX));
                    double accelYMean = StdStats.mean(ArrayUtils.toPrimitive(dAccelY));
                    double accelZMean = StdStats.mean(ArrayUtils.toPrimitive(dAccelZ));

                    // update public static band info
                    BandPublicInfo.currentAccelerometer[0] = accelXMean;
                    BandPublicInfo.currentAccelerometer[1] = accelYMean;
                    BandPublicInfo.currentAccelerometer[2] = accelZMean;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textStatus.setText(""+BandPublicInfo.currentAccelerometer[0]);
                        }
                    });
                }
            }
        };

        gsrListener = new BandGsrEventListener() {
            @Override
            public void onBandGsrChanged(BandGsrEvent bandGsrEvent) {
                appendToUI(String.valueOf(bandGsrEvent.getResistance()));
            }
        };

        gyroListener = new BandGyroscopeEventListener() {
            @Override
            public void onBandGyroscopeChanged(BandGyroscopeEvent bandGyroscopeEvent) {

                gyroAngularXList.add(new Double(bandGyroscopeEvent.getAngularVelocityX()));

                if (counter == 10) {
                    Double[] dGyroAccelX = gyroAccelXList.toArray(new Double[gyroAccelXList.size()]);
                    Double[] dGyroAngularX = gyroAngularXList.toArray(new Double[gyroAngularXList.size()]);
                    final double gyroAccelXMean = StdStats.mean(ArrayUtils.toPrimitive(dGyroAngularX));

                    setGyroText(String.valueOf(gyroAccelXMean) + "\n" + gyroAngularXList.size());
                    gyroAngularXList.clear();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (Math.abs(gyroAccelXMean) > 10) {
                                //textStatus.setText("Status: Moving");
                            } else {
                                //textStatus.setText("Status: Stay still");
                            }
                        }
                    });
                }
                /*
                setGyroText("X accel: " + bandGyroscopeEvent.getAccelerationX() + "\n" +
                        "Y accel: " + bandGyroscopeEvent.getAccelerationY() + "\n" +
                        "Z accel: " + bandGyroscopeEvent.getAccelerationZ() + "\n\n" +
                        "X angular: " + bandGyroscopeEvent.getAngularVelocityX() + "\n" +
                        "Y angular: " + bandGyroscopeEvent.getAngularVelocityY() + "\n" +
                        "Z angular: " + bandGyroscopeEvent.getAngularVelocityZ() + "\n\n");
                        */
            }
        };

        hearListener = new BandHeartRateEventListener() {
            @Override
            public void onBandHeartRateChanged(BandHeartRateEvent bandHeartRateEvent) {
                setHeartText("Heart Rate: " + bandHeartRateEvent.getHeartRate() + "\n" +
                "Heart Quality: " + bandHeartRateEvent.getQuality().name());
            }
        };
        heartConsentListener = new HeartRateConsentListener() {
            @Override
            public void userAccepted(boolean b) {
                if (b) {

                }
            }
        };

        menu = (FloatingActionMenu) findViewById(R.id.fab_menu);
        btnWalking = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fab_walking);
        btnWalking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, StartLabelingActivity.class);
                i.putExtra("LABEL", "WALKING");
                startActivity(i);
                menu.close(true);
            }
        });



        new AccelerometerSubscriptionTask().execute();
        new GsrSubscriptionTask().execute();
        new GyroSubscriptionTask().execute();

        // sensors which need user consent
        new HrSubscriptionTask().execute();
    }

    public void userAccepted(boolean consentGiven) {

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
        } else if (id == R.id.action_info) {
            if (pairedBands.length > 0) {
                startActivity(new Intent(getBaseContext(), DeviceInfoActivity.class));
            } else {
                Toast.makeText(getBaseContext(), "No paired bands", Toast.LENGTH_SHORT).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bandClient != null) {
            try {
                bandClient.getSensorManager().unregisterAccelerometerEventListener(accelerometerListener);
            } catch (BandIOException e) {
                appendToUI(e.getMessage());
            }
        }
    }

    protected void onDestroy() {
        if (bandClient != null) {
            try {
                bandClient.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }

    private class AccelerometerSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void[] params) {
            try {
                if (getConnectedBandClient()) {
                    bandClient.getSensorManager().registerAccelerometerEventListener(accelerometerListener, SampleRate.MS128);
                } else {
                }
            } catch (BandException e) {
            } catch (Exception e) {
            }
            return null;
        }
    }

    private class GsrSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void[] params) {
            try {
                if (getConnectedBandClient()) {
                    bandClient.getSensorManager().registerGsrEventListener(gsrListener, GsrSampleRate.MS5000);
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
            } catch (Exception e) {
            }
            return null;
        }
    }

    private class GyroSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void[] params) {
            try {
                if (getConnectedBandClient()) {
                    bandClient.getSensorManager().registerGyroscopeEventListener(gyroListener, SampleRate.MS128);
                } else {
                }
            } catch (BandException e) {
            } catch (Exception e) {
            }
            return null;
        }
    }

    private class HrSubscriptionTask  extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void[] params) {
            try {
                if (getConnectedBandClient()) {
                    bandClient.getSensorManager().registerHeartRateEventListener(hearListener);
                } else {
                }
            } catch (BandException e) {
            } catch (Exception e) {
            }
            return null;
        }
    }
}
