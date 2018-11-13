package com.breakers.mind.mindbr3aker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import it.unive.dais.legodroid.lib.EV3;

public class RobotControlsActivity extends AppCompatActivity {

    private EV3 ev3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_controls);
    }
}
