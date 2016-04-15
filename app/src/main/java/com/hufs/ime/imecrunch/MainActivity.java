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
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ListView;
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
import com.opencsv.CSVWriter;

import org.apache.commons.lang.ArrayUtils;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements HeartRateConsentListener {
    public static BandInfo[] pairedBands;
    private BandClient bandClient;
    private BandPendingResult<ConnectionState> pendingResult;

    public static boolean recording = false;

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
    private com.github.clans.fab.FloatingActionButton btnWalking,  btnRunning, btnIdle;
    private FloatingActionMenu menu;

    public static String fwVersion;
    public static String hwVersion;

    private BandHeartRateEventListener hearListener;
    private HeartRateConsentListener heartConsentListener;
    private BandAccelerometerEventListener accelerometerListener;
    private BandGsrEventListener gsrListener;
    private BandGyroscopeEventListener gyroListener;
    private BandDistanceEventListener distanceListener;

    private double[] currentAccelerometer = new double[3];
    private double[] currentGyroAccel = new double[3];
    private double[] currentGyroAngularVelocity = new double[3];

    private ArrayList<Double> accelXList = new ArrayList<>();
    private ArrayList<Double> accelYList = new ArrayList<>();
    private ArrayList<Double> accelZList = new ArrayList<>();

    private ArrayList<Double> gyroAccelXList = new ArrayList<>();
    private ArrayList<Double> gyroAccelYList = new ArrayList<>();
    private ArrayList<Double> gyroAccelZList = new ArrayList<>();
    
    private ArrayList<Double> gyroAngularXList = new ArrayList<>();
    private ArrayList<Double> gyroAngularYList = new ArrayList<>();
    private ArrayList<Double> gyroAngularZList = new ArrayList<>();
    

    public static double gyroAccelX;

    public int counter = 1;

    private MenuItem m;

    File f;
    CSVWriter csvWriter;
    FileWriter fileWriter;
    String labelType;

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
//                textSensors.setText(string);
            }
        });
    }

    private void setGyroText(final String n){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                textGyro.setText(n);
            }
        });
    }

    private void setHeartText(final String n){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                textHeart.setText(n);
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

    Timer updateSensorTimer, writeCSVTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /**
         * LIST VIEW OF SENSORS
         */
        final ArrayList<SensorItem> sensorItems = new ArrayList<>();
        SensorItem s = new SensorItem();
        s.setSensorName("Accelerometer");
        s.setSensorValue("X:\nY:\nZ:");
        sensorItems.add(s);
        SensorItem s2 = new SensorItem();
        s2.setSensorName("Gyroscope");
        s2.setSensorValue("Angular Velocity\nX:\nY:\nZ:\n\nAcceleration\nX:\nY:\nZ:");
        sensorItems.add(s2);


        final ListView sensorListView = (ListView) findViewById(R.id.list_view_sensors);
        sensorListView.setAdapter(new SensorListAdapter(this, sensorItems));


//        textStatus = (TextView) findViewById(R.id.txt_status);
        /*textSensors = (TextView) findViewById(R.id.text_sensor);
        textGyro = (TextView)findViewById(R.id.text_gyro);
        textHeart = (TextView)findViewById(R.id.text_heart);
        */

        pairedBands = BandClientManager.getInstance().getPairedBands();

        verifyStoragePermissions(this);

        // start timer counter
        // global value storage occured here
        updateSensorTimer = new Timer();
        updateSensorTimer.scheduleAtFixedRate(new TimerTask() {
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

        /**
         * Begin recording csv
         */
        writeCSVTimer = new Timer();
        writeCSVTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (recording) {
                    String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
                    String status = android.os.Environment.getExternalStorageState();
                    String fileName = "imecrunch.csv";


                    switch (labelType) {
                        case "WALKING":
                            // textLabelType.setText("Walking");
                            //fileName = "walking.csv";
                            break;
                        case "RUNNING":
                            //fileName = "running.csv";
                            break;
                    }
                    final String filePath = baseDir + File.separator + fileName;
                    f = new File(filePath);

                    // write csv over time
                    if (f.exists() && !f.isDirectory()){
                        try {
                            fileWriter = new FileWriter(filePath,   true);
                            csvWriter = new CSVWriter(fileWriter);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            csvWriter = new CSVWriter(new FileWriter(filePath));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    String[] row = {String.valueOf(currentAccelerometer[0]), String.valueOf(currentAccelerometer[1]), String.valueOf(currentAccelerometer[2]),
                            String.valueOf(currentGyroAngularVelocity[0]), String.valueOf(currentGyroAngularVelocity[1]), String.valueOf(currentGyroAngularVelocity[2]),
                            labelType};
                    csvWriter.writeNext(row);
                    try {
                        csvWriter.close();
                    }catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }, 0, 1000);

        accelerometerListener = new BandAccelerometerEventListener() {
            @Override
            public void onBandAccelerometerChanged(final BandAccelerometerEvent event) {
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
                    currentAccelerometer[0] = accelXMean; currentAccelerometer[1] = accelYMean; currentAccelerometer[2] = accelZMean;

                    accelXList.clear(); accelYList.clear(); accelZList.clear();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //s2.setSensorValue("Angular Velocity\nX:\nY:\nZ:\n\nAcceleration\nX:\nY:\nZ:");
                        sensorItems.get(0).setSensorValue(String.format("X: %f\nY: %f\nZ: %f", currentAccelerometer[0], currentAccelerometer[1], currentAccelerometer[2]));
                        ((BaseAdapter) sensorListView.getAdapter()).notifyDataSetChanged();

                    }
                });
            }
        };

        distanceListener = new BandDistanceEventListener() {
            @Override
            public void onBandDistanceChanged(final BandDistanceEvent bandDistanceEvent) {
               runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
//                       textStatus.setText(bandDistanceEvent.getMotionType().toString());
                   }
               });
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
                gyroAngularYList.add(new Double(bandGyroscopeEvent.getAngularVelocityY()));
                gyroAngularZList.add(new Double(bandGyroscopeEvent.getAngularVelocityZ()));
                
                gyroAccelXList.add(new Double(bandGyroscopeEvent.getAccelerationX()));
                gyroAccelYList.add(new Double(bandGyroscopeEvent.getAccelerationY()));
                gyroAccelZList.add(new Double(bandGyroscopeEvent.getAccelerationZ()));

                if (counter == 10) {
                    Double[] dGyroAccelX = gyroAccelXList.toArray(new Double[gyroAccelXList.size()]);
                    Double[] dGyroAccelY = gyroAccelYList.toArray(new Double[gyroAccelYList.size()]);
                    Double[] dGyroAccelZ = gyroAccelZList.toArray(new Double[gyroAccelZList.size()]);
                    
                    Double[] dGyroAngularX = gyroAngularXList.toArray(new Double[gyroAngularXList.size()]);
                    Double[] dGyroAngularY = gyroAngularYList.toArray(new Double[gyroAngularYList.size()]);
                    Double[] dGyroAngularZ = gyroAngularZList.toArray(new Double[gyroAngularZList.size()]);
                    
                    final double gyroAngularXMean = StdStats.mean(ArrayUtils.toPrimitive(dGyroAngularX));
                    final double gyroAngularYMean = StdStats.mean(ArrayUtils.toPrimitive(dGyroAngularY));
                    final double gyroAngularZMean = StdStats.mean(ArrayUtils.toPrimitive(dGyroAngularZ));
                    
                    final double gyroAccelXMean = StdStats.mean(ArrayUtils.toPrimitive(dGyroAccelX));
                    final double gyroAccelYMean = StdStats.mean(ArrayUtils.toPrimitive(dGyroAccelY));
                    final double gyroAccelZMean = StdStats.mean(ArrayUtils.toPrimitive(dGyroAccelZ));

                    currentGyroAngularVelocity[0] = gyroAngularXMean; currentGyroAngularVelocity[1] = gyroAngularYMean; currentGyroAngularVelocity[2] = gyroAngularZMean;
                    currentGyroAccel[0] = gyroAccelXMean;currentGyroAccel[1] = gyroAccelYMean;currentGyroAccel[2] = gyroAccelZMean;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            sensorItems.get(1).setSensorValue(String.format("Angular Velocity\nX: %f\nY: %f\nZ: %f\n\nAcceleration\nX: %f\nY: %f\nZ: %f",
                                    currentGyroAngularVelocity[0], currentGyroAngularVelocity[1],currentGyroAngularVelocity[2],
                                    currentGyroAccel[0], currentGyroAccel[1], currentGyroAccel[2]));
                        }
                    });
                    
                    gyroAngularXList.clear();gyroAngularYList.clear();gyroAngularZList.clear();
                    gyroAccelXList.clear();gyroAccelYList.clear();gyroAccelZList.clear();
                }
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
                labelType = "WALKING";
                recording = true;
                m.setVisible(true);
                menu.close(true);
                menu.hideMenu(true);
                sensorItems.get(0).setSensorValue(String.format("Angular Velocity\nX:%f\nY:\nZ:\n\nAcceleration\nX:\nY:\nZ:", currentAccelerometer[0]));
                sensorListView.invalidate();
            }
        });

        btnRunning = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fab_running);
        btnRunning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                labelType = "RUNNING";
                recording = true;
                m.setVisible(true);
                menu.close(true);
                menu.hideMenu(true);
            }
        });

        btnIdle = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fab_idle);
        btnIdle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                labelType = "IDLE";
                recording = true;
                m.setVisible(true);
                menu.close(true);
                menu.hideMenu(true);
            }
        });

        new AccelerometerSubscriptionTask().execute();
        new GsrSubscriptionTask().execute();
        new GyroSubscriptionTask().execute();
        new DistanceSubscriptionTask().execute();

        // sensors which need user consent
        new HrSubscriptionTask().execute();
    }

    public void userAccepted(boolean consentGiven) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        m = menu.findItem(R.id.action_stop_recording);

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
        } else if(id == R.id.action_stop_recording) {
            recording = false;
            m.setVisible(false);
            menu.showMenu(true);
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

        updateSensorTimer.cancel();
        writeCSVTimer.cancel();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        updateSensorTimer.cancel();
        writeCSVTimer.cancel();
        super.onBackPressed();
    }

    private class AccelerometerSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void[] params) {
            try {
                if (getConnectedBandClient()) {
                    bandClient.getSensorManager().registerAccelerometerEventListener(accelerometerListener, SampleRate.MS16);
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
                    bandClient.getSensorManager().registerGyroscopeEventListener(gyroListener, SampleRate.MS16);
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

    private class DistanceSubscriptionTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                bandClient.getSensorManager().registerDistanceEventListener(distanceListener);
            } catch (Exception e) {
            }
            return null;
        }
    }

}
