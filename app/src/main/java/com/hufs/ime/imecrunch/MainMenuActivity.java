package com.hufs.ime.imecrunch;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainMenuActivity extends AppCompatActivity {

    Button menuActivity, menuEmotion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        getSupportActionBar().hide();

        menuActivity = (Button) findViewById(R.id.btn_menu_activity);
        menuEmotion = (Button) findViewById(R.id.btn_menu_emotion);


        menuActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainMenuActivity.this, ActionRecognitionActivity.class));
            }
        });

        menuEmotion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainMenuActivity.this, EmotionRecognitionActivity.class));
            }
        });
    }
}
