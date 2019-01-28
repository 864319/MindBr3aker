package com.breakers.mind.mindbr3aker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
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
    private int direction;/*0-Nord
                            1-Est
                            2-Sud
                            3-Ovest*/
    private int speedL = 10;
    private int speedR = -2;

    private int startRow = 0;
    private int startColumn = 0;

    private LightSensor.Color cellCenter = LightSensor.Color.RED;
    private LightSensor.Color rotColor = LightSensor.Color.GREEN;
    private LightSensor.Color finishColor = LightSensor.Color.BROWN;

    private float obsMinDist = 20;

    private int headRotPower = 50;

    private TextView log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maze_resolver);

        log = findViewById(R.id.log_txt);

        direction = 0;

        inizializeMaze(rows, columns);

        Button startBtn = findViewById(R.id.start);
        startBtn.setOnClickListener(v -> connectToEv3());
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

        while(!solved)
            mainTask(api);
    }

    private void mainTask(EV3.Api api){
        if(lightSensor!=null){
            //vado avanti fino alla prossima cella
            followLineToColor(api, cellCenter);
            if(solved)return;
            //controllo se è già stata visitata
            //se è già stata visitata controllo se è un vicolo cieco
            if(maze[row][column].isVisited()==0) {
                controlObstacles(api);
                maze[row][column].setVisited();
                if(!right && !left && !forward)maze[row][column].setBlind(true);
            }else{
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
            }

            //calcolo nuova direzione
            int newDirection = chooseNextCell(row, column);
            switch (newDirection){
                case 0:
                    row = row+1;
                    break;

                case 1:
                    column = column + 1;
                    break;

                case 2:
                    row = row-1;
                    break;

                case 3:
                    column = column - 1;
                    break;
            }

            //vado avanti fino al punto di rotazione
            followLineToColor(api, rotColor);

            //gira
            while (direction!=newDirection){
                rotate(api, 1);
                direction = (direction+1)%4;
            }


        }

    }

    private void inizializeMaze(int r, int c){
        maze = new Cell[rows][columns];
        for(int i = 0; i < r; i++)
            for(int j = 0; j < c; j++)
                maze[i][j] = new Cell(i, j);
        maze[0][0].setDirections((direction+2)%4, false);
        row = startRow+1;
        column = startColumn;
        solved = false;
    }

    private void updateMaze(int d, boolean f, boolean r, boolean l, int row, int col){
        if(d==0) {
            maze[row][col].setDirections(0, f);
            maze[row][col].setDirections(1, r);
            maze[row][col].setDirections(3, l);
        }
        if(d==1) {
            maze[row][col].setDirections(2, r);
            maze[row][col].setDirections(1, f);
            maze[row][col].setDirections(0, l);
        }
        if(d==2) {
            maze[row][col].setDirections(3, l);
            maze[row][col].setDirections(1, r);
            maze[row][col].setDirections(2, f);
        }
        if(d==3) {
            maze[row][col].setDirections(3, f);
            maze[row][col].setDirections(0, r);
            maze[row][col].setDirections(2, l);
        }
    }


    private int chooseNextCell(int row, int col){
        if(maze[row][col].getDirections()[direction])
            return direction;
        else
            if(maze[row][col].getDirections()[(direction+3)%4])
                return (direction+3)%4;
            else
                if(maze[row][col].getDirections()[(direction+1)%4])
                    return (direction+1)%4;
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
                AtomicBoolean line = new AtomicBoolean(false);

                speedLeft = speedL;
                speedRight = speedR;

                while (!destination.get()) {    // loop until cancellation signal is fired
                    //LIGHT SENSOR
                    Future<LightSensor.Color> colf = lightSensor.getColor();
                    LightSensor.Color col = colf.get();
                    runOnUiThread(() -> {
                        if (col == LightSensor.Color.WHITE && line.get()) {
                            line.set(false);

                            int tempSpeed = speedLeft;
                            speedLeft = speedRight;
                            speedRight = tempSpeed;
                            try {
                                setMotorsSpeed(speedLeft, speedRight);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else if(col != LightSensor.Color.WHITE){
                            line.set(true);
                            //if(col == finishColor)solved=true;
                            if(col == destColor){
                                destination.set(true);
                                try {
                                    setMotorsSpeed(0, 0);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }/*else if(col==finishColor){
                                destination.set(true);
                                solved = true;
                                try {
                                    setMotorsSpeed(0, 0);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }*/
                        }
                    });
                }

            } catch (IOException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private void rotate(EV3.Api api, int direction){
        if(lightSensor!=null){
            try {
                setMotorsSpeed(speedL*direction, speedL*(-direction));
                AtomicBoolean line = new AtomicBoolean(true);
                AtomicBoolean line2 = new AtomicBoolean(false);
                while(!line2.get()){
                    Future<LightSensor.Color> colf = lightSensor.getColor();
                    LightSensor.Color col = colf.get();
                    runOnUiThread(() -> {
                        if(col == LightSensor.Color.WHITE && line.get()){
                            line.set(false);
                        }else if(col != LightSensor.Color.WHITE && !line.get()){
                            line2.set(true);
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

    private void controlObstacles(EV3.Api api){
        if(ultrasonicSensor!=null && smallMotor!=null){
            try {
                smallMotor.resetPosition();
                AtomicReference<Future<Float>> distance = new AtomicReference<>(ultrasonicSensor.getDistance());
                final CountDownLatch latch = new CountDownLatch(1);
                runOnUiThread(() -> {
                    try {
                        float distForward = distance.get().get(); //<- distanza davanti
                        forward=distForward>obsMinDist;
                        //gira testa a destra e aspetta il completamento del movimento
                        smallMotor.setStepSpeed(headRotPower,90,0,0,true);
                        smallMotor.waitCompletion();

                        distance.set(ultrasonicSensor.getDistance());
                        runOnUiThread(() -> {
                            try {
                                float distRight = distance.get().get();
                                right = distRight > obsMinDist;

                                //gira testa a sinistra e aspetta il completamento del movimento
                                smallMotor.setStepSpeed(-headRotPower,180,0,0,true);
                                smallMotor.waitCompletion();

                                distance.set(ultrasonicSensor.getDistance());
                                runOnUiThread(() -> {
                                    try {
                                        float distLeft = distance.get().get();
                                        left = distLeft>obsMinDist;

                                        //torna nella posizione iniziale
                                        smallMotor.setStepSpeed(headRotPower,90,0,0,true);
                                        smallMotor.waitCompletion();

                                        //decido direzione in cui girare
                                        updateMaze(direction, forward, right, left, row, column);
                                        latch.countDown();

                                    } catch (IOException | InterruptedException | ExecutionException e) {
                                        e.printStackTrace();
                                    }
                                });

                            } catch (IOException | InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (IOException | InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
