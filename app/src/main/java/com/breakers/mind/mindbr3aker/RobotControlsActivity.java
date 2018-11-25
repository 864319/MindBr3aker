package com.breakers.mind.mindbr3aker;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.sql.Time;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import io.github.controlwear.virtual.joystick.android.JoystickView;
import it.unive.dais.legodroid.lib.EV3;
import it.unive.dais.legodroid.lib.comm.BluetoothConnection;
import it.unive.dais.legodroid.lib.plugs.GyroSensor;
import it.unive.dais.legodroid.lib.plugs.LightSensor;
import it.unive.dais.legodroid.lib.plugs.TachoMotor;
import it.unive.dais.legodroid.lib.plugs.TouchSensor;
import it.unive.dais.legodroid.lib.plugs.UltrasonicSensor;
import it.unive.dais.legodroid.lib.util.Prelude;

public class RobotControlsActivity extends AppCompatActivity {

    private static String DEVICE_NAME = "EV3";
    private static String TAG = "RobotControlsActivity";
    private static EV3.OutputPort rightMotorPort = EV3.OutputPort.A;
    private static EV3.OutputPort leftMotorPort = EV3.OutputPort.D;
    private static EV3.OutputPort smallMotorPort = EV3.OutputPort.C;
    private static EV3.InputPort lightSensorPort = EV3.InputPort._1;
    private static EV3.InputPort ultrasonicSensorPort = EV3.InputPort._2;

    private TachoMotor rightMotor;
    private TachoMotor leftMotor;

    private int max_speed = 30;

    //ho creto una TextView di log per mandare dei messaggi a video
    private TextView log;

    private EV3 ev3;

    private JoystickView movementJoystick;

    long timer = 0;
    long refreshRate = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_controls);

        timer = Calendar.getInstance().getTimeInMillis();

        movementJoystick = findViewById(R.id.movement_joystick);
        movementJoystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                long time = Calendar.getInstance().getTimeInMillis();
                if(time>=timer){
                    timer+=refreshRate;

                    if(angle<=180 && rightMotor!=null && leftMotor!=null){
                        try {
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
            }
        });

        log = findViewById(R.id.log);

        try {
            //connessione al dispositivo
            ev3 = new EV3(new BluetoothConnection(DEVICE_NAME).connect());
            if(ev3!=null){
                log.setText("Connected");
            }


            //run main
            try {
                ev3.run(this::legomain);
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            Log.e(TAG, "Cannot connect to the device ");
            log.setText("Cannot connect to the device");
            e.printStackTrace();
        }

        findViewById(R.id.forward_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(leftMotor!=null && rightMotor!=null){
                        leftMotor.setPower(60);
                        //rightMotor.setPower(60);
                        leftMotor.start();
                        //rightMotor.start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        findViewById(R.id.backward_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(leftMotor!=null && rightMotor!=null){
                        leftMotor.setPower(-60);
                        //rightMotor.setPower(-60);
                        leftMotor.start();
                        //rightMotor.start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }



        });
    }

    private void legomain(EV3.Api api) throws IOException {
        //inizializzazione riferimenti ai componenti del robot utilizzati
        LightSensor lightSensor = api.getLightSensor(lightSensorPort);
        UltrasonicSensor ultrasonicSensor = api.getUltrasonicSensor(ultrasonicSensorPort);
        TachoMotor smallMotor = api.getTachoMotor(smallMotorPort);
        rightMotor = api.getTachoMotor(rightMotorPort);
        leftMotor = api.getTachoMotor(leftMotorPort);
    }

    @Override
    public void onBackPressed() {
        try {
            rightMotor.setSpeed(0);
            leftMotor.setSpeed(0);
            rightMotor.start();
            leftMotor.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ev3.cancel();
        super.onBackPressed();
    }
}
