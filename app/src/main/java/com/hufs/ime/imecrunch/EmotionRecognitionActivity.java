package com.hufs.ime.imecrunch;

import android.content.Intent;
import android.graphics.Color;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.clans.fab.FloatingActionMenu;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandRRIntervalEvent;
import com.microsoft.band.sensors.BandRRIntervalEventListener;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;
import com.microsoft.band.sensors.GsrSampleRate;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import listview.MovementSensorItem;
import listview.MovementSensorListAdapter;

public class EmotionRecognitionActivity extends AppCompatActivity implements HeartRateConsentListener {

    public static BandInfo[] pairedBands;
    private BandClient bandClient;

    private HeartRateConsentListener heartConsentListener;

    private BandGsrEventListener gsrListener;
    private BandHeartRateEventListener heartRateEventListener;
    private BandRRIntervalEventListener rrIntervalEventListener;
    private BandSkinTemperatureEventListener skinTemperatureEventListener;
    private CSVWriter csvWriter;
    private Timer writeCSVTimer;
    private boolean recording = false;
    private File f;
    private String labelType;
    private FileWriter fileWriter;
    private FloatingActionMenu menu;

    int currentHeartRate, currentGsr;
    double currentRRInterval, currentSkinTemperature;
    private MenuItem m;

    private GraphView rrIntervalGraphView, gsrGraphView;
    private LineGraphSeries<DataPoint> rrIntervalSeries, gsrSeries;

    private com.github.clans.fab.FloatingActionButton btnHappy, btnSad, btnNeutral;
    private int rrSeriesCounter, gsrSeriesCounter;

    double totalRR = 0;
    int rrCounter = 0;

    double rrThreshold = 0.05;

    private void addRrIntervalPointToGraph() {
        rrIntervalSeries.appendData(new DataPoint(rrSeriesCounter, currentRRInterval), true, 40);
        rrSeriesCounter++;
    }

    private void addGsrPointToGraph() {
        gsrSeries.appendData(new DataPoint(gsrSeriesCounter, currentGsr), true, 40);
        gsrSeriesCounter++;
    }

    public void beginSynchronization() {

        RequestQueue queue = Volley.newRequestQueue(this);

        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String status = android.os.Environment.getExternalStorageState();
        String fileName = "emotion.csv";


        final String filePath = baseDir + File.separator + fileName;
        try {
            CSVReader reader = new CSVReader(new FileReader(filePath));
            String[] row;

            while ((row = reader.readNext()) != null) {
                String param = Arrays.toString(row).replace("[", "").replace("]", "").replace(", ", ",");
                param = URLEncoder.encode(param, "utf-8");
                String url = "http://biztix.hufs.ac.kr/sa/sa-crunch-connector-emotion.jsp?sensor=" + param;
                StringRequest req = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
//                        Toast.makeText(getBaseContext(), "One or more data were failed to synchronize", Toast.LENGTH_SHORT).show();
                    }
                });

                queue.add(req);

            }

            Toast.makeText(getBaseContext(), "Synchronization performed", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion_recognition);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("Emotion Recognition");

        /**
         * LIST VIEW OF SENSORS
         */
        final ArrayList<MovementSensorItem> movementSensorItems = new ArrayList<>();
        MovementSensorItem s = new MovementSensorItem();
        s.setSensorName("Heart rate");
        s.setSensorValue("-");
        movementSensorItems.add(s);
        MovementSensorItem s2 = new MovementSensorItem();
        s2.setSensorName("Gsr");
        s2.setSensorValue("-");
        movementSensorItems.add(s2);
        MovementSensorItem s3 = new MovementSensorItem();
        s3.setSensorName("RR Interval");
        s3.setSensorValue("-");
        movementSensorItems.add(s3);
        MovementSensorItem s4 = new MovementSensorItem();
        s4.setSensorName("Skin Temperature");
        s4.setSensorValue("-");
        movementSensorItems.add(s4);
        MovementSensorItem s5 = new MovementSensorItem();
        s5.setSensorName("Status");
        s5.setSensorValue("-");
        movementSensorItems.add(s5);

        /**
         * graphview settings
         */

        rrIntervalGraphView = (GraphView) findViewById(R.id.graph_emotion);
        rrIntervalSeries = new LineGraphSeries<>();
        rrIntervalSeries.setTitle("RR-Interval");
        rrIntervalGraphView.getViewport().setXAxisBoundsManual(true);
        rrIntervalGraphView.getViewport().setMaxX(40);
        rrIntervalGraphView.getViewport().setMinX(0);
        rrIntervalGraphView.getViewport().setYAxisBoundsManual(true);
        rrIntervalGraphView.getViewport().setMaxY(1.5);
        rrIntervalGraphView.getViewport().setMinY(0);
        rrIntervalGraphView.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        rrIntervalGraphView.getLegendRenderer().setVisible(true);
        rrIntervalGraphView.getLegendRenderer().setBackgroundColor(Color.WHITE);
        rrIntervalGraphView.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.BOTTOM);
        rrIntervalGraphView.addSeries(rrIntervalSeries);

        gsrGraphView = (GraphView) findViewById(R.id.graph_emotion_gsr);
        gsrSeries = new LineGraphSeries<>();
        gsrSeries.setTitle("GSR");
        gsrGraphView.getViewport().setXAxisBoundsManual(true);
        gsrGraphView.getViewport().setMaxX(40);
        gsrGraphView.getViewport().setMinX(0);
        gsrGraphView.addSeries(gsrSeries);

        writeCSVTimer = new Timer();
        writeCSVTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (recording) {
                    String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
                    String status = android.os.Environment.getExternalStorageState();
                    String fileName = "emotion.csv";

                    final String filePath = baseDir + File.separator + fileName;
                    f = new File(filePath);

                    // write csv over time
                    if (f.exists() && !f.isDirectory()) {
                        try {
                            fileWriter = new FileWriter(filePath, true);
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

                    /**
                     * ACTUAL CSV WRITING
                     */
                    String[] row = {new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                            String.valueOf(currentHeartRate), String.valueOf(currentGsr), String.valueOf(currentRRInterval), String.valueOf(currentSkinTemperature),
                            labelType};
                    csvWriter.writeNext(row);
                    try {
                        csvWriter.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }, 0, 1000);

        final ListView sensorListView = (ListView) findViewById(R.id.list_view_sensors);
        sensorListView.setAdapter(new MovementSensorListAdapter(this, movementSensorItems));
        gsrListener = new BandGsrEventListener() {
            @Override
            public void onBandGsrChanged(final BandGsrEvent bandGsrEvent) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentGsr = bandGsrEvent.getResistance();
                        movementSensorItems.get(1).setSensorValue("" + bandGsrEvent.getResistance());
                        addGsrPointToGraph();
//                        ((BaseAdapter) sensorListView.getAdapter()).notifyDataSetChanged();
                    }
                });
            }
        };

        heartRateEventListener = new BandHeartRateEventListener() {
            @Override
            public void onBandHeartRateChanged(final BandHeartRateEvent bandHeartRateEvent) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentHeartRate = bandHeartRateEvent.getHeartRate();
                        movementSensorItems.get(0).setSensorValue("" + bandHeartRateEvent.getHeartRate());
//                        ((BaseAdapter) sensorListView.getAdapter()).notifyDataSetChanged();
                    }
                });
            }
        };


        rrIntervalEventListener = new BandRRIntervalEventListener() {
            @Override
            public void onBandRRIntervalChanged(final BandRRIntervalEvent bandRRIntervalEvent) {

                runOnUiThread(new Runnable() {


                    @Override
                    public void run() {
                        currentRRInterval = bandRRIntervalEvent.getInterval();
                        movementSensorItems.get(2).setSensorValue("" + bandRRIntervalEvent.getInterval());
//                        ((BaseAdapter) sensorListView.getAdapter()).notifyDataSetChanged();
                        addRrIntervalPointToGraph();

                        rrCounter++;
                        totalRR += bandRRIntervalEvent.getInterval();

                        if (rrCounter == 5) {
                            TextView tv = (TextView) findViewById(R.id.text_rr_interval);
                            tv.setText("" + (totalRR / rrCounter));

                            TextView tvEs = (TextView) findViewById(R.id.text_emotion_state);
                            ImageView imEmotionState = (ImageView) findViewById(R.id.image_emotion_state);
                            if (totalRR / rrCounter <= 0.70 + rrThreshold) {
                                tvEs.setText("HAPPY");
                                imEmotionState.setBackgroundDrawable(getResources().getDrawable(R.drawable.happy));
                            } else if(totalRR / rrCounter >= 1.00 + rrThreshold) {
                                tvEs.setText("SAD");
                                imEmotionState.setBackgroundDrawable(getResources().getDrawable(R.drawable.sad));
                            } else {
                                tvEs.setText("NEUTRAL");
                                imEmotionState.setBackgroundDrawable(getResources().getDrawable(R.drawable.neutral));
                            }
                            rrCounter = 0;
                            totalRR = 0;


                        }
                    }
                });
            }
        };

        skinTemperatureEventListener = new BandSkinTemperatureEventListener() {
            @Override
            public void onBandSkinTemperatureChanged(final BandSkinTemperatureEvent bandSkinTemperatureEvent) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentSkinTemperature = bandSkinTemperatureEvent.getTemperature();
                        movementSensorItems.get(3).setSensorValue("" + bandSkinTemperatureEvent.getTemperature());
//                        ((BaseAdapter) sensorListView.getAdapter()).notifyDataSetChanged();
                    }
                });
            }
        };

        menu = (FloatingActionMenu) findViewById(R.id.fab_menu);
        btnHappy = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fab_happy);
        btnHappy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                labelType = "HAPPY";
                recording = true;
                m.setVisible(true);
                menu.close(true);
                menu.hideMenu(true);
            }
        });

        btnSad = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fab_sad);
        btnSad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                labelType = "SAD";
                recording = true;
                m.setVisible(true);
                menu.close(true);
                menu.hideMenu(true);
            }
        });

        btnNeutral= (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fab_neutral);
        btnNeutral.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                labelType = "NEUTRAL";
                recording = true;
                m.setVisible(true);
                menu.close(true);
                menu.hideMenu(true);
            }
        });


        new GsrSubscriptionTask().execute();
        new HrSubscriptionTask().execute();
        new RRIntervalSubscriptionTask().execute();
        new SkinTemperatureSubscriptionTask().execute();
    }

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (bandClient == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                //appendToUI("Band isn't paired with your phone.\n");
                return false;
            }
            bandClient = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == bandClient.getConnectionState()) {
            if (bandClient.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {

            } else {
                bandClient.getSensorManager().requestHeartRateConsent(this, this);
            }

            return true;
        }

        //appendToUI("Band is connecting...\n");
        //setGyroText("Connecting...");
        return ConnectionState.CONNECTED == bandClient.connect().await();
    }

    @Override
    public void userAccepted(boolean b) {

    }

    private class HrSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void[] params) {
            try {
                if (getConnectedBandClient()) {
                    bandClient.getSensorManager().registerHeartRateEventListener(heartRateEventListener);
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
                    //appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
            } catch (Exception e) {
            }
            return null;
        }
    }

    private class RRIntervalSubscriptionTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    bandClient.getSensorManager().registerRRIntervalEventListener(rrIntervalEventListener);
                } else {
                    //appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
            } catch (Exception e) {
            }
            return null;
        }
    }

    private class SkinTemperatureSubscriptionTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    bandClient.getSensorManager().registerSkinTemperatureEventListener(skinTemperatureEventListener);
                } else {
                }
            } catch (BandException e) {
            } catch (Exception e) {
            }
            return null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        if (bandClient != null) {
//            try {
//                bandClient.getSensorManager().unregisterGsrEventListener(gsrListener);
//                bandClient.getSensorManager().unregisterHeartRateEventListener(heartRateEventListener);
//                bandClient.getSensorManager().unregisterRRIntervalEventListener(rrIntervalEventListener);
//                bandClient.getSensorManager().unregisterSkinTemperatureEventListener(skinTemperatureEventListener);
//            } catch (BandIOException e) {
//            }
//        }
    }

    @Override
    protected void onDestroy() {
        if (bandClient != null) {
            try {
                bandClient.disconnect().await();
                bandClient.getSensorManager().unregisterGsrEventListener(gsrListener);
                bandClient.getSensorManager().unregisterHeartRateEventListener(heartRateEventListener);
                bandClient.getSensorManager().unregisterRRIntervalEventListener(rrIntervalEventListener);
                bandClient.getSensorManager().unregisterSkinTemperatureEventListener(skinTemperatureEventListener);
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }

//        updateSensorTimer.cancel();
//        writeCSVTimer.cancel();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_emotion, menu);

        m = menu.findItem(R.id.action_stop_recording);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            return true;
        } else
        */
        if (id == R.id.action_info) {
            if (pairedBands.length > 0) {
                startActivity(new Intent(getBaseContext(), DeviceInfoActivity.class));
            } else {
                Toast.makeText(getBaseContext(), "No paired bands", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.action_stop_recording) {
            recording = false;
            m.setVisible(false);
            menu.showMenu(true);
        } else if (id == R.id.action_sync) {
             beginSynchronization();
//            Toast.makeText(this, "test", Toast.LENGTH_SHORT).show();
        }

        return super.onOptionsItemSelected(item);
    }
}
