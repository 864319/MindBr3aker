package com.breakers.mind.mindbr3aker;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import io.github.controlwear.virtual.joystick.android.JoystickView;
import it.unive.dais.legodroid.lib.EV3;
import it.unive.dais.legodroid.lib.comm.BluetoothConnection;
import it.unive.dais.legodroid.lib.plugs.LightSensor;
import it.unive.dais.legodroid.lib.plugs.Plug;
import it.unive.dais.legodroid.lib.plugs.TachoMotor;
import it.unive.dais.legodroid.lib.plugs.UltrasonicSensor;

public class RobotControlsActivity extends AppCompatActivity {

    private static String DEVICE_NAME = "EV3";
    private static String TAG = "RobotControlsActivity";
    private static EV3.OutputPort rightMotorPort = EV3.OutputPort.A;
    private static EV3.OutputPort leftMotorPort = EV3.OutputPort.D;
    private static EV3.OutputPort smallMotorPort = EV3.OutputPort.B;
    private static EV3.InputPort lightSensorPort = EV3.InputPort._3;
    private static EV3.InputPort ultrasonicSensorPort = EV3.InputPort._4;

    private TachoMotor rightMotor;
    private TachoMotor leftMotor;
    private TachoMotor smallMotor;

    private int max_speed = 100;
    private int head_speed = 10;

    //ho creto una TextView di log per mandare dei messaggi a video
    private TextView log;


    private ImageView colorFeed;
    private TextView distanceFeed;

    private EV3 ev3;

    private JoystickView movementJoystick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_controls);

        colorFeed = findViewById(R.id.color_feedback);
        distanceFeed = findViewById(R.id.distance_txt);
        log = findViewById(R.id.log);

        movementJoystick = findViewById(R.id.movement_joystick);
        movementJoystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                   leftJoysttick(angle, strength);
            }
        });

        findViewById(R.id.right_btn).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        try {
                            smallMotor.setSpeed(head_speed);
                            smallMotor.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        try {
                            smallMotor.brake();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                }
                return true;
            }
        });

        findViewById(R.id.left_btn).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        try {
                            smallMotor.setSpeed(-head_speed);
                            smallMotor.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        try {
                            smallMotor.brake();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                }
                return true;
            }
        });

        connectToEv3();
    }

    private void leftJoysttick(int angle, int strength){
        if(rightMotor!=null && leftMotor!=null){
            try {
                if(angle>180){
                    angle -= 180;
                    strength = -strength;
                }
                float angleSpeed = (float) angle /180.f;

                int leftSpeed = (int) (max_speed*((float)strength)/100.f);
                leftSpeed *= 1-angleSpeed;
                log.setText(angle+"   Left: "+leftSpeed);
                leftMotor.setPower(leftSpeed);
                leftMotor.start();

                int rightSpeed = (int) (max_speed*((float)strength)/100.f);
                rightSpeed *= angleSpeed;
                log.append(" - Right:"+rightSpeed);
                rightMotor.setPower(rightSpeed);
                rightMotor.start();
            } catch (IOException e) {
                log.append("Errore metododo di accelerazione.");
                e.printStackTrace();
            }
        }
    }

    private void connectToEv3(){
        try {
            //connessione al dispositivo
            ev3 = new EV3(new BluetoothConnection(DEVICE_NAME).connect());
            if(ev3!=null){
                log.setText("Connected");
                try {
                    ev3.run(this::legomain);
                } catch (EV3.AlreadyRunningException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Cannot connect to the device ");
            log.setText("Cannot connect to the device");
            e.printStackTrace();
        }

    }

    private final void legomain(EV3.Api api) {
        //inizializzazione riferimenti ai componenti del robot utilizzati
        LightSensor lightSensor = api.getLightSensor(lightSensorPort);
        UltrasonicSensor ultrasonicSensor = api.getUltrasonicSensor(ultrasonicSensorPort);
        smallMotor = api.getTachoMotor(smallMotorPort);
        rightMotor = api.getTachoMotor(rightMotorPort);
        leftMotor = api.getTachoMotor(leftMotorPort);

        try {

            smallMotor.resetPosition();

            while (!api.ev3.isCancelled()) {    // loop until cancellation signal is fired
                try {
                    //LIGHT SENSOR
                    Future<LightSensor.Color> colf = lightSensor.getColor();
                    LightSensor.Color col = colf.get();
                    runOnUiThread(() -> colorFeed.setBackgroundColor(col.toARGB32()));

                    //ULTRASONIC SENSOR
                    Future<Float> distance = ultrasonicSensor.getDistance();
                    runOnUiThread(() -> {
                        try {
                            distanceFeed.setText(String.valueOf(distance.get()));
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (IOException | InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }

    }

}
