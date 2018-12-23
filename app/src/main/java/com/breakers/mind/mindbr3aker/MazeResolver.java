package com.breakers.mind.mindbr3aker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import it.unive.dais.legodroid.lib.EV3;
import it.unive.dais.legodroid.lib.comm.BluetoothConnection;
import it.unive.dais.legodroid.lib.plugs.LightSensor;
import it.unive.dais.legodroid.lib.plugs.TachoMotor;
import it.unive.dais.legodroid.lib.plugs.UltrasonicSensor;

public class MazeResolver extends AppCompatActivity {

    private EV3 ev3;

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

    private LightSensor lightSensor;

    private int speedL = 10;
    private int speedR = speedL-1;

    private TextView log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maze_resolver);

        log = findViewById(R.id.log_txt);

        connectToEv3();
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
        lightSensor = api.getLightSensor(lightSensorPort);
        UltrasonicSensor ultrasonicSensor = api.getUltrasonicSensor(ultrasonicSensorPort);
        smallMotor = api.getTachoMotor(smallMotorPort);
        rightMotor = api.getTachoMotor(rightMotorPort);
        leftMotor = api.getTachoMotor(leftMotorPort);

        try {
            int i = 0;
            while (!api.ev3.isCancelled()) {    // loop until cancellation signal is fired
                try {
                    //LIGHT SENSOR
                    Future<LightSensor.Color> colf = lightSensor.getColor();
                    LightSensor.Color col = colf.get();
                    //runOnUiThread(() -> color = col);

                    followLineToColor(api, LightSensor.Color.RED);
                } catch (IOException | InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }


        } finally {

        }
    }

    //fa seguire la linea nera al robot fino al colore specificato come parametro
    private void followLineToColor(EV3.Api api, LightSensor.Color destColor){
        if(lightSensor!=null){
            AtomicReference<LightSensor.Color> lastColor = new AtomicReference<>();
            lastColor.set(LightSensor.Color.BLACK);
            try {
                setMotorsSpeed(speedL, speedR);
                while (!api.ev3.isCancelled()) {    // loop until cancellation signal is fired
                    //LIGHT SENSOR
                    Future<LightSensor.Color> colf = lightSensor.getColor();
                    LightSensor.Color col = colf.get();
                    runOnUiThread(() -> {
                        if (col == LightSensor.Color.WHITE && lastColor.get() !=LightSensor.Color.WHITE) {
                            lastColor.set(LightSensor.Color.WHITE);
                            int tempSpeed = speedL;
                            speedL = speedR;
                            speedR = tempSpeed;
                            try {
                                setMotorsSpeed(speedL, speedR);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else {
                            lastColor.set(col);
                            if(col == destColor){
                                try {
                                    setMotorsSpeed(0, 0);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } finally {

            }
        }
    }


    private void setMotorsSpeed(int l, int r) throws IOException {
        if(leftMotor != null && rightMotor!=null){
            leftMotor.setSpeed(l);
            leftMotor.start();
            rightMotor.setSpeed(r);
            rightMotor.start();
        }
    }
}
