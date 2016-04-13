package com.hufs.ime.imecrunch;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.util.Timer;
import java.util.TimerTask;

public class StartLabelingActivity extends AppCompatActivity {
    TextView textLabelType, textTimer;
    Button btnStartTraining;

    boolean clicked = false;
    String labelType;


    Timer timer;
    TimerTask timerTask;
    long counter = 0;

    File f;
    CSVWriter csvWriter;
    FileWriter fileWriter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_labeling);
        labelType = getIntent().getStringExtra("LABEL");

        getSupportActionBar().hide();

        textLabelType = (TextView) findViewById(R.id.txt_label_type);
        textTimer = (TextView) findViewById(R.id.txt_timer);
        textTimer.setText("");
        btnStartTraining = (Button) findViewById(R.id.btn_start_training);

        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String status = android.os.Environment.getExternalStorageState();
        String fileName = "others.csv";
        switch (labelType) {
            case "WALKING":
                textLabelType.setText("Walking");
                fileName = "walking.csv";
                break;
            case "RUNNING":
                fileName = "running.csv";
                break;
        }
        final String filePath = baseDir + File.separator + fileName;

        f = new File(filePath);

//        Toast.makeText(getBaseContext(), baseDir, Toast.LENGTH_SHORT).show();
//        Toast.makeText(getBaseContext(), status, Toast.LENGTH_SHORT).show();



        timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textLabelType.setText(String.valueOf(BandPublicInfo.getCurrentAccelerometer()[0]));
                        if (clicked) {
//                            textTimer.setText("Elapsed: " + ((float)(counter)/10) + " seconds");
//                            textTimer.setText("X: " + BandPublicInfo.currentAccelerometer[0]);
                            textTimer.setText("X: " + BandPublicInfo.getCurrentAccelerometer()[0]);
                        }
                    }
                });

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

//                            String[] row = {String.valueOf(BandPublicInfo.currentAccelerometer[0]), String.valueOf(BandPublicInfo.currentAccelerometer[1]), String.valueOf(BandPublicInfo.currentAccelerometer[2])};
                String[] row = {String.valueOf(BandPublicInfo.getCurrentAccelerometer()[0]), String.valueOf(BandPublicInfo.getCurrentAccelerometer()[1]), String.valueOf(BandPublicInfo.getCurrentAccelerometer()[2])};
//                            Toast.makeText(getBaseContext(), ""+BandPublicInfo.currentAccelerometer[0], Toast.LENGTH_SHORT).show();
                csvWriter.writeNext(row);
                try {
                    csvWriter.close();
                }catch (Exception e) {
                    e.printStackTrace();
                }

                counter++;

            }
        };
        timer = new Timer();
        timer.schedule(timerTask, 0, 1000);



        btnStartTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clicked = !clicked;
                if (clicked) {
                    counter = 0;
                    btnStartTraining.setText("Stop Training");

                } else {
                    btnStartTraining.setText("Start Training");
                }
            }
        });
    }
}
