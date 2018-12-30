package com.breakers.mind.mindbr3aker;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import it.unive.dais.legodroid.lib.EV3;
import it.unive.dais.legodroid.lib.comm.BluetoothConnection;
import it.unive.dais.legodroid.lib.plugs.LightSensor;
import it.unive.dais.legodroid.lib.plugs.TachoMotor;
import it.unive.dais.legodroid.lib.plugs.UltrasonicSensor;

import static java.lang.StrictMath.abs;

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
    private UltrasonicSensor ultrasonicSensor;

    private Cell maze[][];
    private int rows = 4;
    private int columns = 4;
    private int row;
    private int column;
    private boolean forward;
    private boolean right;
    private boolean left;
    private boolean solved;
    private int direction;
    private int speedL = 12;
    private int speedR = speedL-11;

    private int headRotationPower = 50;

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
        ultrasonicSensor = api.getUltrasonicSensor(ultrasonicSensorPort);
        smallMotor = api.getTachoMotor(smallMotorPort);
        rightMotor = api.getTachoMotor(rightMotorPort);
        leftMotor = api.getTachoMotor(leftMotorPort);
        maze = new Cell[rows][columns];
        direction = 0;

        inizializeMaze(rows, columns);

        try {
            int i = 0;
            while (!api.ev3.isCancelled() && solved) {    // loop until cancellation signal is fired
                try {
                    //LIGHT SENSOR
                    Future<LightSensor.Color> colf = lightSensor.getColor();
                    LightSensor.Color col = colf.get();

                    switch (col){
                        case RED:

                            if(maze[row][column].isVisited()==0) {
                                if (ultrasonicSensor.getDistance().get() < 15) {
                                    forward = false;
                                } else
                                    forward = true;

                                //moveHeadLeft(api, 1);
                                if (ultrasonicSensor.getDistance().get() < 15)
                                    right = false;
                                else
                                    right = true;

                                //moveHeadLeft(api, -1);
                                //moveHeadLeft(api, -1);
                                if (ultrasonicSensor.getDistance().get() < 15)
                                    left = false;
                                else
                                    left = true;

                                maze[row][column].setVisited();
                                updateMaze(direction, forward, right, left, row, column);
                                followLineToColor(api, LightSensor.Color.GREEN);
                            }
                            else{
                                if(!maze[row][column].isBlind()){
                                    boolean dir[] = maze[row][column].getDirections();
                                    int walls = 0;
                                    for(int j = 0; j < dir.length; j++) {
                                        if (!dir[j])
                                            walls++;
                                        else{
                                            if(j==0)
                                                if(maze[row+1][column].isBlind())
                                                    walls++;
                                            if(j==1)
                                                if(maze[row][column+1].isBlind())
                                                    walls++;
                                            if(j==2)
                                                if(maze[row-1][column].isBlind())
                                                    walls++;
                                            if(j==3)
                                                if(maze[row][column-1].isBlind())
                                                    walls++;
                                        }
                                    }
                                    if(walls==3)
                                        maze[row][column].setBlind(true);

                                }
                                followLineToColor(api, LightSensor.Color.GREEN);
                            }

                            break;

                        case BLACK:
                            followLineToColor(api, LightSensor.Color.RED);
                            break;

                        //case WHITE:

                        case GREEN:
                            int newDirection = -1;
                            boolean found = false;
                            while(!found) {
                                newDirection = chooseNextCell(row, column);
                                if((newDirection==0 && maze[row+1][column].isVisited()==0) ||
                                    (newDirection==1 && maze[row][column+1].isVisited()==0) ||
                                        (newDirection==2 && maze[row-1][column].isVisited()==0) ||
                                            (newDirection==3 && maze[row][column-1].isVisited()==0))
                                                found = true;

                            }

                            if(newDirection==direction)
                                followLineToColor(api, LightSensor.Color.RED);
                            else
                                if(abs(newDirection-direction)==0)
                                    maze[row][column].setBlind(true);
                                else
                                    if(abs(newDirection-direction)==3)
                                        rotate(api, (newDirection-direction)/(-3));
                                    else
                                        if(abs(newDirection-direction)==2){
                                            rotate(api, 1);
                                            rotate(api, 1);
                                        }
                                        else
                                            if(abs(newDirection-direction)==1)
                                                rotate(api, newDirection-direction);

                            direction = newDirection;

                            break;

                        case BLUE:
                            for(int z = 1; z < 9; z++)
                                rotate(api, 1);

                            solved = true;
                            break;
                    }
                    //runOnUiThread(() -> color = col);


                    moveHeadLeft();
                    try {
                        //set time in mili
                        Thread.sleep(2000);

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    moveHeadLeft();

                } catch (IOException | InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

                break;
            }


        } finally {

        }
    }

    private void inizializeMaze(int r, int c){
        for(int i = 0; i < r; i++)
            for(int j = 0; j < c; j++)
                maze[i][j] = new Cell(i, j);
        maze[0][0].setDirections((direction+2)%4, false);
        row = 0;
        column = 0;
        solved = false;
    }

    private void updateMaze(int d, boolean f, boolean r, boolean l, int row, int col){
        if(d==1) {
            maze[row][col].setDirections(0, f);
            maze[row][col].setDirections(1, r);
            maze[row][col].setDirections(3, l);
        }
        if(d==2) {
            maze[row][col].setDirections(2, r);
            maze[row][col].setDirections(1, f);
            maze[row][col].setDirections(0, l);
        }
        if(d==4) {
            maze[row][col].setDirections(2, l);
            maze[row][col].setDirections(0, r);
            maze[row][col].setDirections(3, f);
        }
        if(d==3) {
            maze[row][col].setDirections(2, f);
            maze[row][col].setDirections(3, r);
            maze[row][col].setDirections(1, l);
        }
    }


    private int chooseNextCell(int row, int col){
        if(maze[row][col].getDirections()[direction])
            return direction;
        else
            if(maze[row][col].getDirections()[(direction+1)%4])
                return (direction+1)%4;
            else
                if(maze[row][col].getDirections()[(direction+3)%4])
                    return (direction+3)%4;
                else
                    return (direction+2)%4;
    }

    int speedLeft = speedL;
    int speedRight = speedR;
    //fa seguire la linea nera al robot fino al colore specificato come parametro
    private void followLineToColor(EV3.Api api, LightSensor.Color destColor){
        if(lightSensor!=null){
            try {
                AtomicBoolean destination = new AtomicBoolean(false);
                setMotorsSpeed(speedL, speedR);
                AtomicBoolean white = new AtomicBoolean(false);

                speedLeft = speedL;
                speedRight = speedR;

                while (!api.ev3.isCancelled() && !destination.get()) {    // loop until cancellation signal is fired
                    //LIGHT SENSOR
                    Future<LightSensor.Color> colf = lightSensor.getColor();
                    LightSensor.Color col = colf.get();
                    runOnUiThread(() -> {
                        if (col != LightSensor.Color.WHITE && !white.get()) {
                            white.set(true);

                            int tempSpeed = speedLeft;
                            speedLeft = speedRight;
                            speedRight = tempSpeed;
                            try {
                                setMotorsSpeed(speedLeft, speedRight);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else {
                            white.set(false);
                            if(col == destColor){
                                destination.set(true);
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

    private void rotate(EV3.Api api, int direction){
        if(lightSensor!=null){
            try {
                setMotorsSpeed(speedL*direction, speedL*(-direction));
                AtomicBoolean white = new AtomicBoolean(false);
                AtomicBoolean white2 = new AtomicBoolean(false);
                while(!white.get() || !white2.get()){
                    Future<LightSensor.Color> colf = lightSensor.getColor();
                    LightSensor.Color col = colf.get();
                    runOnUiThread(() -> {
                       if(col == LightSensor.Color.WHITE && !white.get()){
                           white.set(true);

                       }
                       if(white.get() && col != LightSensor.Color.WHITE){
                           white2.set(true);
                           try {
                               setMotorsSpeed(0, 0);
                           } catch (IOException e) {
                               e.printStackTrace();
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

    private void moveHeadLeft(){
        try {
            smallMotor.setStepSpeed(-headRotationPower,90,0,0, false);
            smallMotor.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void controlObstacles(EV3.Api api){
        try {
            smallMotor.setStepPower(-headRotationPower,90,0,0, false);
            smallMotor.start();
            AtomicReference<Float> pos = new AtomicReference<>((float) 0);
            while(pos.get()>-90){
                runOnUiThread(()->{
                    try {
                        pos.set(smallMotor.getPosition().get());
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

            smallMotor.setStepPower(headRotationPower,180,0,0, false);
            smallMotor.start();
            while(pos.get()<90){
                runOnUiThread(()->{
                    try {
                        pos.set(smallMotor.getPosition().get());
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            smallMotor.setStepPower(-headRotationPower,90,0,0, false);
            smallMotor.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
