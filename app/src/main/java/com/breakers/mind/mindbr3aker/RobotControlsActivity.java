package com.breakers.mind.mindbr3aker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
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

public class RobotControlsActivity extends AppCompatActivity {

    private static String DEVICE_NAME = "EV3";
    private static String TAG = "RobotControlsActivity";
    private static EV3.OutputPort rightMotorPort = EV3.OutputPort.A;
    private static EV3.OutputPort leftMotorPort = EV3.OutputPort.B;
    private static EV3.OutputPort smallMotorPort = EV3.OutputPort.C;
    private static EV3.InputPort lightSensorPort = EV3.InputPort._1;
    private static EV3.InputPort ultrasonicSensorPort = EV3.InputPort._2;

    private EV3 ev3;

    private JoystickView movementJoystick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_controls);

        movementJoystick = findViewById(R.id.movement_joystick);



/*
        try {
            //connessione al dispositivo
            ev3 = new EV3(new BluetoothConnection(DEVICE_NAME).connect());

            //run main
            try {
                ev3.run(this::legomain);
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            Log.e(TAG, "Cannot connect to the device");
            e.printStackTrace();
        }*/
    }

    private void legomain(EV3.Api api) {
        //inizializzazione riferimenti ai componenti del robot utilizzati
        LightSensor lightSensor = api.getLightSensor(lightSensorPort);
        UltrasonicSensor ultrasonicSensor = api.getUltrasonicSensor(ultrasonicSensorPort);
        TachoMotor smallMotor = api.getTachoMotor(smallMotorPort);
        TachoMotor rightMotor = api.getTachoMotor(rightMotorPort);
        TachoMotor LeftMotor = api.getTachoMotor(leftMotorPort);


    }
}
