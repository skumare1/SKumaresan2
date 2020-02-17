package com.example.skumaresan2;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;


public class MainActivity extends Activity {
    GraphView graph1, graph2, graph3;
    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();
    boolean timerRunning = false;
    boolean graphRestarted = false;
    boolean graphVisible = false;
    float[] values1, values2, values3; //Values that will be plotted on the 3 graphs
    float[] zeroValues;
    String[] horlabels; //Labels on the x-axis of the graph1
    AccelerometerService mAccelerometerService;
    float[] data = new float[]{0,0,0};
    PatientDatabaseHelper2 db;
    boolean dbHelperCreated = false;
    EditText patientId, patientAge, patientName;
    RadioGroup sex;
    String tableName="";
    Button mDownloadButton, mUploadButton, mStopButton;
    int databaseVersion = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Storage permissions
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_PERMISSION_STORAGE = 100;
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

            for (String str : permissions) {
                if (checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                   requestPermissions(permissions, REQUEST_CODE_PERMISSION_STORAGE);
                    //return;
                }
            }
        }

        //Start a bound service to receive accelerometer data
        Intent intent = new Intent (this, AccelerometerService.class);
        bindService(intent,connection,BIND_AUTO_CREATE);
        Log.d("Service", "Requested to bind service");

        //Initialize View components
        setContentView(R.layout.activity_main);
        graph1 = findViewById(R.id.customGraph1); graph2 = findViewById(R.id.customGraph2); graph3 = findViewById(R.id.customGraph3);
        graph1.setTitle("X-values");graph2.setTitle("Y-values");graph3.setTitle("Z-values");
        patientId = findViewById(R.id.editTextPatientId);
        patientAge = findViewById(R.id.editTextPatientAge);
        patientName = findViewById(R.id.editTextPatientName);
        mDownloadButton = findViewById(R.id.downloadButton);
        mUploadButton = findViewById(R.id.uploadButton);
        mStopButton = findViewById(R.id.stopButton);
        sex = findViewById(R.id.radioSex);

        mUploadButton.setEnabled(false); //Keep the upload button disabled until some value is plotted on the graph.
        //mDownloadButton.setEnabled(false); //Keep the download button disabled until the DB is uploaded.
        mStopButton.setEnabled(false); //Keep the stop button disabled until something is drawn on the screen.

        zeroValues = new float[]{0,0,0,0,0,0,0,0,0,0};

        //Re-instate saved values, if they exist
        if(savedInstanceState!=null){
            Log.d("Config:","Reinstating values!");
            //Obtain values from saved instance state
            horlabels = savedInstanceState.getStringArray("horlabels");
            values1 = savedInstanceState.getFloatArray("values1"); values2 = savedInstanceState.getFloatArray("values2"); values3 = savedInstanceState.getFloatArray("values3");
            graphRestarted = savedInstanceState.getBoolean("graphRestarted");
            graphVisible = savedInstanceState.getBoolean("graphVisible");
            if (graphVisible) {
                timerRunning=false;
                graph1.setValues(values1); graph2.setValues(values2); graph3.setValues(values3);

                runClicked(graph1); //To simulate as if "Run" button was just clicked
            }else {
                graph1.setValues (zeroValues); graph2.setValues (zeroValues); graph2.setValues (zeroValues);
                graph1.invalidate(); graph2.invalidate();graph3.invalidate();
            }
            //Set x-axis values
            graph1.setHorlabels(horlabels); graph2.setHorlabels(horlabels); graph3.setHorlabels(horlabels);

        } else{
            //Initialize variables for the graph1 for the first run
            values1 = new float[]{0,0,0,0,0,0,0,0,0,0};
            values2 = new float[]{0,0,0,0,0,0,0,0,0,0};
            values3 = new float[]{0,0,0,0,0,0,0,0,0,0};
            //horlabels = new String[]{"-9",  "-8", "-7", "-6", "-5", "-4", "-3", "-2", "-1", "0"};
            horlabels = new String[]{"",  "", "", "", "", "", "", "", "", "0"};
        }
    }

    //Handle configuration changes by saving the values on the x-axis and plotted values on the graph1
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArray("horlabels",horlabels);
        outState.putFloatArray("values1", values1); outState.putFloatArray("values2", values2); outState.putFloatArray("values3", values3);
        outState.putBoolean("graphVisible", graphVisible);
        outState.putBoolean("graphRestarted", graphRestarted);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);

    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AccelerometerService.LocalBinder binder = (AccelerometerService.LocalBinder) service;
            mAccelerometerService = binder.getService();
            Log.d("Service","mAccelerometer has been set.");
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
    }

    //"RUN" button clicked
    public void runClicked(View view){
        //Validate patient data
        if (!patientId.getText().toString().equals("") && !patientAge.getText().toString().equals("")){
            int pId = Integer.parseInt(patientId.getText().toString());
            int pAge = Integer.parseInt(patientAge.getText().toString());
            String pName = patientName.getText().toString();
            pName.trim();

            if (pId>-1 && pAge >0 && pAge<150 && !pName.equals("")){ //Patient data validations


                String pSex;
                int selectedId = sex.getCheckedRadioButtonId();
                if (selectedId == R.id.radioMale)
                    pSex = "male";
                else
                    pSex = "female";

                String constructedTableName= pName + "_" + pId + "_" + pAge + "_" + pSex; //Construct table name

                if (!constructedTableName.equals(tableName)) {
                    tableName = constructedTableName;
                    Log.d("Main", "Creating table " + tableName);

//                    PatientDownloadedDatabaseHelper p = new PatientDownloadedDatabaseHelper(getApplicationContext(), tableName, "SKumaresan2Downloaded.db");
//                    if (!dbHelperCreated){
//                        Log.d("Database", "New db helper");
//                        db = new PatientDatabaseHelper(getApplicationContext(), tableName, p.getDatabaseVersion()); //Create database with constructed table name
//                    }else {
//                        Log.d("Database", "Reusing db helper");
//                        db.onUpgrade(db.getDatabase(), db.getDatabaseVersion(), db.getDatabaseVersion() + 1);
//                    }
                    //db.onUpgrade(db.getDatabase(), db.getDatabaseVersion(), p.getDatabaseVersion() + 1);

                    //databaseVersion++;

                    db = new PatientDatabaseHelper2(getApplicationContext(), tableName);
                    db.createTable();
                }

                //Activate the 3 graphs
                if (!timerRunning) {
                    timerRunning=true;
                    graphVisible=true;
                    graphRestarted=true; //Denotes that the graphs were just restarted (just started for the very first event) by clicking "RUN".
                    startTimer();
                }

                mUploadButton.setEnabled(true); //Enable upload button for user to upload database
                mStopButton.setEnabled(true); //Enable stop button

            }else
                Toast.makeText(getApplicationContext(), "Patient data incomplete or invalid. Try again!", Toast.LENGTH_SHORT).show();
        }else
            Toast.makeText(getApplicationContext(), "Patient data incomplete or invalid. Try again!", Toast.LENGTH_SHORT).show();
    }

    //"STOP" button clicked
    public void stopClicked(View view){
        stopTimer();
        graph1.setValues (zeroValues); graph2.setValues (zeroValues); graph3.setValues (zeroValues);//Clear graphs, i.e., plot 0-values on graphs
        graph1.invalidate(); graph2.invalidate(); graph3.invalidate(); //Force re-draw of Graph View
        graphVisible = false;
    }

    public void startTimer() {
        timer = new Timer();
        setupTimer();//Setup the TimerTask
        timer.schedule(timerTask, 0, 1000);//Setup timer to trigger every 1 second
        Log.d("TIMER", "Timer started!");
    }

    public void stopTimer() {
        timerRunning=false;
        //Ensure the timer is NOT null before trying to cancel it.
        if (timer != null) {
            Log.d("TIMER", "Timer is being stopped.");
            timer.cancel();
            timer = null;
        }
    }

    public void setupTimer() {
        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        Log.d("Main","--------------------------------------------------------");
                        if (mAccelerometerService!=null) {
                            data = mAccelerometerService.getAccelerometerData(); //Get the latest x,y,z values from accelerometer sensor
                            Log.d("Main", "x:" + data[0] + "   y:" + data[1] + "   z:" + data[2]);
                            if (db!=null)
                                db.insertRow(data);
                        }
                        //If the graph1 was restarted by clicking "Run" (after a "STOP"), do NOT generate new values for the 1st cycle so that the last values are retained on resume.
                        if (!graphRestarted) {
                            //Update the values that will be plotted on the graph1
                            updateGraphValues();
                            //Update the x-axis labels on the graph1
                            updateGraphAxisLabels();
                            //Re-draw the graph1 (custom view) at every timer-interval, without rendering the entire activity.
                            graph1.invalidate(); graph2.invalidate(); graph3.invalidate();
                        }else {
                            graphRestarted = false;
                            graph1.setValues(values1); graph2.setValues(values2); graph3.setValues(values3);
                            graph1.invalidate(); graph2.invalidate(); graph3.invalidate();
                        }
                    }
                });
            }
        };
    }

    //Keep updating the values1 to be plotted on the graph1 by shifting the y-axis values to the left, to simulate animation
    private void updateGraphValues(){
        for (int i=0;i<9;i++)
            values1[i]= values1[i+1];
        values1[9]=data[0];
        graph1.setValues(values1); //Update the values to be plotted in the GraphView view.
        String s="";
        for (int i =0;i<10;i++)
            s+= Float.toString(values1[i])+",";
        Log.d("Values1",s);

        for (int i=0;i<9;i++)
            values2[i]= values2[i+1];
        values2[9]=data[1];
        graph2.setValues(values2); //Update the values to be plotted in the GraphView view.
        s="";
        for (int i =0;i<10;i++)
            s+= Float.toString(values2[i])+",";
        Log.d("Values2",s);

        for (int i=0;i<9;i++)
            values3[i]= values3[i+1];
        values3[9]=data[2];
        graph3.setValues(values3); //Update the values to be plotted in the GraphView view.
        s="";
        for (int i =0;i<10;i++)
            s+= Float.toString(values3[i])+",";
        Log.d("Values3",s);
    }

    //Keep shifting the x-axis labels of the graph1 to the left, to simulate animation, as the time progresses
    private void updateGraphAxisLabels(){
        for (int i=0;i<9;i++)
            horlabels[i]=horlabels[i+1];
        horlabels[9]=Integer.toString(Integer.parseInt(horlabels[8])+1);
        graph1.setHorlabels(horlabels); graph2.setHorlabels(horlabels); graph3.setHorlabels(horlabels);
    }
//
//    public void savePatient(View view) {
//        if (!patientId.getText().toString().equals("") && !patientAge.getText().toString().equals("")){
//            int pId = Integer.parseInt(patientId.getText().toString());
//            int pAge = Integer.parseInt(patientAge.getText().toString());
//            String pName = patientName.getText().toString();
//            pName.trim();
//
//
//            if (pId>-1 && pAge >0 && pAge<150 && !pName.equals("")){ //Patient data validations
//                String pSex;
//                int selectedId = sex.getCheckedRadioButtonId();
//                if (selectedId == R.id.radioMale)
//                    pSex = "male";
//                else
//                    pSex = "female";
//
//                String tableName= pName + "_" + pId + "_" + pAge + "_" + pSex;
//                Log.d ("Main", "Creating table "+tableName);
//                db = new PatientDatabaseHelper(getApplicationContext(),tableName);
//                runClicked(graph1);
//            }else
//                Toast.makeText(getApplicationContext(), "Patient data incomplete or invalid. Try again!", Toast.LENGTH_SHORT).show();
//        }else
//            Toast.makeText(getApplicationContext(), "Patient data incomplete or invalid. Try again!", Toast.LENGTH_SHORT).show();
//    }

    public void uploadClicked(View view) {

        String currentDBPath = "/data/data/" + getApplicationContext().getPackageName() + "/databases/"+db.getDatabaseName();

        File dbFile = new File(currentDBPath);
        Uri fileUri = Uri.fromFile(dbFile);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        UploadFilesFlask uff = new UploadFilesFlask(MainActivity.this);
        uff.sendFileToLocalServer(currentDBPath, "application");

        //TODO: Fix this... it is called too early
        if (uff.getUploadStatus()) //If upload was successful, enable Download button
           mDownloadButton.setEnabled(true);
    }

    Timer timer2=null;
    public void downloadClicked(View view) throws InterruptedException {
        //timer2 is used to check (ever 0.2 seconds) if the download has been completed.
        //This check is to ensure that timer2 is started only if it is NOT already running.
        if (timer2==null) {
            //mDownloadButton.setEnabled(false);//Disable download button until the download completes

            String currentDBPath = "/data/data/" + getApplicationContext().getPackageName() + "/databases/" + "SKumaresan2Downloaded.db";
            File dbFile = new File(currentDBPath);
            Uri fileUri = Uri.fromFile(dbFile);
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());

            final UploadFilesFlask uff = new UploadFilesFlask(MainActivity.this);
            uff.getFileFromServer(currentDBPath, "application");
            //Thread.sleep(2000);

            //Ensure that the download is complete, before trying to access the database.
            //To do so, start a timer that checks if the download is complete, every 0.2 seconds
            timer2 = new Timer();

            TimerTask timerTask2 = new TimerTask() {
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            //Check if the database has been copied over from server
                            if (uff.getDownloadStatus()) {
                                PatientDownloadedDatabaseHelper pddh = new PatientDownloadedDatabaseHelper(getApplicationContext(), tableName, "SKumaresan2Downloaded.db");
                                float[][] data;
                                data = pddh.fetchLastTenRows();
                                for (int i = 0; i < data[0].length; i++) {
                                    for (int j = 0; j < data[1].length; j++) {
                                        switch (j) {
                                            case 0:
                                                values1[9-i] = data[i][j];
                                                break;
                                            case 1:
                                                values2[9-i] = data[i][j];
                                                break;
                                            case 2:
                                                values3[9-i] = data[i][j];
                                                break;
                                        }
                                    }
                                }
                                graph1.setValues(values1);
                                graph2.setValues(values2);
                                graph3.setValues(values3);
                                graph1.invalidate();
                                graph2.invalidate();
                                graph3.invalidate();
                                Log.d("Download", "Graphs redrawn based on downloaded database.");

                                uff.setDownloadStatus(false); //To ensure that the graph is not refreshed multiple times.
                                mDownloadButton.setEnabled(true); //Re-enable the download button
                            }
                        }
                    }); //Runnable ends
                }
            };
            timer2.schedule(timerTask2, 0, 200);//Setup timer to trigger every 0.2 seconds
        }else{
            Log.d("Download", "Timer 2 NOT being started again since it is already running.");
        }
    }

    public void stopTimer2() {
        //Ensure the timer is NOT null before trying to cancel it.
        if (timer2 != null) {
            Log.d("TIMER", "Timer is being stopped.");
            timer2.cancel();
            timer2 = null;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permission, int[] grantResults) {
        switch(requestCode){
            case 200:
                boolean writeAccepted = grantResults[0]==PackageManager.PERMISSION_GRANTED;
                break;
        }
    }


}
