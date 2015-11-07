package com.dataplayed.nerdlabcontroller.app.util;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.VelocityTracker;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dataplayed.nerdlabcontroller.app.R;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import netP5.NetAddress;
import oscP5.OscBundle;
import oscP5.OscEventListener;
import oscP5.OscMessage;
import oscP5.OscP5;
import oscP5.OscStatus;


public class MainActivity extends Activity implements SurfaceHolder.Callback, SensorEventListener {
    private SurfaceHolder holder;
    private PlaySurfaceView surface;
    private TextView scoreBox;
    private TextView playerBox;
    private TextView instructionBox;
    private ImageView gestureView;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    MediaRecorder audio;
    private boolean usingAudio;

    private float x,y,z;
    private float last_x, last_y, last_z;
    long lastUpdate;
    int SHAKE_THRESHOLD = 800;

    public int width,height;
    final public double ACCELEROMETER_PADDING = .15;
    final public int GAME_STATE_NO_SERVER_CONNECTION = -1;
    final public int GAME_STATE_WAITING                 = 0;
    final public int GAME_STATE_PAUSED                  = 1;
    final public int GAME_STATE_PLAYING                 = 2;
    final public int GAME_STATE_SHOW_MESSAGE            = 3;
    final public int GAME_STATE_IN_PROGRESS_CANT_JOIN   = 4;

    final public int GAME_CONTROL_MOVE              = 0;
    final public int GAME_CONTROL_AUDIO             = 1;
    final public int GAME_CONTROL_ACCEL             = 2;
    final public int GAME_CONTROL_TAP               = 3;
    final public int GAME_CONTROL_ROTATE            = 4;
    final public int GAME_CONTROL_ROTATE_RELEASE    = 5;
    final public int GAME_CONTROL_NOTHING           = 6;

    final public int REACTION_PULSE                 = 0;
    final public int REACTION_ROLL_CALL             = 1;

    final public int IMAGE_SET_SQUARE       = 0;
    final public int IMAGE_SET_ABSTRACT     = 1;
    final public int IMAGE_SET_EVACUATE     = 2;
    final public int IMAGE_SET_TANKS        = 3;
    final public int IMAGE_SET_SUBMARINES   = 4;
    final public int IMAGE_SET_HUMANS       = 5;


    final int PORT = 9000;
    public int playerNumber;
    public int playerAvatar;
    public int gameState;
    public int gameControl;
    public String scoreName;
    public String instructions;
    public String message;
    public String status;
    public boolean usingAtlantisImageSet;
    public int score;
    public float holdingTime;
    public int color_r;
    public int color_g;
    public int color_b;
    public OscP5 oscP5;
    NetAddress myRemoteLocation;
    DialogInterface.OnClickListener dialogClickListener;
    Timer timer;

    public int screenWidth;// = displaymetrics.widthPixels;
    public int screenHeight;// = displaymetrics.heightPixels;



    private VelocityTracker mVelocityTracker = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        Bundle extras = getIntent().getExtras();
        String player = extras.getString("player");
        String server = extras.getString("server");
        DisplayMetrics displaymetrics = new DisplayMetrics();

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }


        screenWidth = displaymetrics.widthPixels;
        screenHeight = displaymetrics.heightPixels;
        gameState = GAME_STATE_NO_SERVER_CONNECTION;
        playerBox = (TextView)findViewById(R.id.playerNameView);
        playerBox.setText(player);
        scoreBox = (TextView)findViewById(R.id.scoreView);
        instructionBox = (TextView)findViewById(R.id.instructionTextView);
        gestureView = (ImageView)findViewById(R.id.gestureImage);
        new ConnectToNERDLab().execute(player, server);
        new SetupHardware().execute();
        surface=(PlaySurfaceView)findViewById(R.id.surface);
        surface.addTapListener(onTap);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        surface.imageSet = IMAGE_SET_HUMANS;
        dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        if(usingAudio) {
                            timer.cancel();
                            usingAudio = false;
                            surface.audioEnabled = false;
                        }

                        oscP5.stop();
                        finish();

                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (surface.allowChangesToCanvas && gameControl == GAME_CONTROL_ACCEL) {

            long curTime = System.currentTimeMillis();
            // only allow one update every 100ms.
            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;
                 x = sensorEvent.values[0];
                 y = sensorEvent.values[1];
                 z = sensorEvent.values[2];

                float speed = Math.abs(x+y+z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    Log.d("sensor", "shake detected w/ speed: " + speed);
                    surface.rotation = surface.rotation + speed;
//                    surface.setRotation(surface.rotation + speed);
                    surface.x = surface.getWidth()/2;
                    surface.y = surface.getHeight()/2;
                    surface.updateBitmap();
                    OscBundle bndl = new OscBundle();
                    OscMessage msg;
                    msg = new OscMessage("/shake");
                    msg.add(playerNumber);
                    msg.add(speed);
                    bndl.add(msg);
                    oscP5.send(bndl, myRemoteLocation);

                }
                last_x = x;
                last_y = y;
                last_z = z;
            }




        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    private class SetupHardware extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            if (audio == null) {
                audio = new MediaRecorder();
                audio.setAudioSource(MediaRecorder.AudioSource.MIC);
                audio.setOutputFormat(MediaRecorder.AudioEncoder.AMR_NB);
                audio.setAudioEncoder(MediaRecorder.OutputFormat.THREE_GPP);
                audio.setOutputFile("/dev/null");
                try {
                    audio.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            audio.start();
            usingAudio = false;


            return "Started";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }


    }

    // The definition of our task class
    private class ConnectToNERDLab extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            notification("Connecting to NERDLab...", Toast.LENGTH_SHORT);
        }

        @Override
        protected String doInBackground(String... params) {
            String player=params[0];
            String server=params[1];
            System.out.println("Starting Up OSC");
            oscP5 = new OscP5(this,9001);
            NERDLabListener t = new NERDLabListener();
            oscP5.addListener(t);
            myRemoteLocation = new NetAddress(server, PORT);

            joinGame(player);
            return "Connected";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }


    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            OscBundle bundle = new OscBundle();
            OscMessage msg = new OscMessage("/hello");
            bundle.add(msg);
            oscP5.send(bundle, myRemoteLocation);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
        // TODO Auto-generated method stub
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        System.out.println("Creating Surface");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surface=(PlaySurfaceView)findViewById(R.id.surface);
        surface.removeTapListener(onTap);
        oscP5.stop();
        audio.release();
        audio = null;
    }

    public PlaySurfaceView.TapListener onTap=
            new PlaySurfaceView.TapListener() {
                public void onTap(MotionEvent event) {
                    int index = event.getActionIndex();
                    int action = event.getAction();
                    int pointerId = event.getPointerId(index);
                    switch(action) {
                        case MotionEvent.ACTION_DOWN:
                            x=(int)event.getX();
                            y=(int)event.getY();
                            last_x = x;
                            last_y = y;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            x = (int) event.getX();
                            y = (int) event.getY();

                            break;
                    }

                    if (surface.allowChangesToCanvas) {
                        OscBundle bndl = new OscBundle();
                        OscMessage msg;
                        switch (gameControl) {
                            case GAME_CONTROL_MOVE:
                                surface.x = x;
                                surface.y = y;
                                float dx = last_x - x;
                                float dy = last_y - y;
                                msg = new OscMessage("/move");
                                msg.add(playerNumber);
                                msg.add(dx);
                                msg.add(dy);
                                bndl.add(msg);
                                oscP5.send(bndl, myRemoteLocation);
                                break;
                            case GAME_CONTROL_ROTATE_RELEASE:
                            case GAME_CONTROL_ROTATE:

                                surface.rotation = last_x - x;
//                                surface.setRotation(last_x - x);
                                surface.x = surface.getWidth()/2;
                                surface.y = surface.getHeight()/2;
                                msg = new OscMessage("/rotate");
                                msg.add(playerNumber);
                                msg.add(surface.rotation);
                                bndl.add(msg);
                                oscP5.send(bndl, myRemoteLocation);

                                break;

                            case GAME_CONTROL_TAP:

                                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                    msg = new OscMessage("/tap");
                                    msg.add(playerNumber);
                                    bndl.add(msg);
                                    oscP5.send(bndl, myRemoteLocation);
                                    surface.holding = true;
                                    surface.x = surface.getWidth()/2;
                                    surface.y = surface.getHeight()/2;
                                }
                                if (event.getAction() == MotionEvent.ACTION_UP) {
                                    holdingTime = event.getDownTime();
                                    // /release/playernum/holdtime
                                    msg = new OscMessage("/release");
                                    msg.add(playerNumber);
                                    msg.add(holdingTime);
                                    bndl.add(msg);
                                    oscP5.send(bndl, myRemoteLocation);
                                    holdingTime = 0;
                                    surface.holding = false;
                                }

                                break;
                        }
                    }
                    else {

                        System.out.println("Never got the ok on using controls");
                    }
                }
            };

    public void oscEvent (OscMessage theOscMessage) {

        if (theOscMessage.checkAddrPattern("/reset")) {
            System.out.println("got reset message");

            OscBundle bndl = new OscBundle();
            OscMessage resetMessage = new OscMessage("/alive");
            bndl.add(resetMessage);
            oscP5.send(bndl, myRemoteLocation);
        }

        if (theOscMessage.checkAddrPattern("/rejoin")) {
            System.out.println("GOT REJOIN MESSAGE");
            playerNumber = theOscMessage.get(0).intValue();
            gameState = theOscMessage.get(1).intValue();
            gameControl = theOscMessage.get(2).intValue();
            if (theOscMessage.get(3).intValue() == 0) {
                surface.allowChangesToCanvas = false;
            }
            else {
                surface.allowChangesToCanvas = true;
            }
            color_r = theOscMessage.get(4).intValue();
            color_g = theOscMessage.get(5).intValue();
            color_b = theOscMessage.get(6).intValue();
            playerAvatar = theOscMessage.get(8).intValue();
            surface.imageSet = theOscMessage.get(7).intValue();

        }



    }

    public void updatePlayerImage(int set, int image_number) {

        if (set == IMAGE_SET_TANKS) {
            System.out.println("Using Tank Image");
            surface.bitmapId = R.drawable.tank;
        }
        else {
            System.out.println("Not playing tanks, using abstract images");


        }
    }
    private void joinGame(String player) {
        OscBundle bndl = new OscBundle();
        OscMessage joinMessage = new OscMessage("/join");
        joinMessage.add(player);
        bndl.add(joinMessage);
        oscP5.send(bndl, myRemoteLocation);
    }
    public void quitButtonOnClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to quit?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();

    }

    public void notification(CharSequence text, int duration) {
        final CharSequence t = text;
        final int dur = duration;
        runOnUiThread(new Runnable() {
            public void run() {
                if (dur == Toast.LENGTH_LONG) {
                    Toast toast = Toast.makeText(getApplicationContext(), t, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP, 0, 0);
                    toast.show();

                }
                else {
                    Toast toast = Toast.makeText(getApplicationContext(), t, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.TOP, 0, 0);
                    toast.show();

                }

            }
        });

     }
    public float map(float x, float a, float b, float c, float d) {
        float output;
        output = (x-a)/(b-a) * (d-c) + c;
        return output;
    }

    public class stopReacting extends TimerTask {
        public void run() {
            surface.reacting = false;
        }

    }
    public class MicCheck extends TimerTask {
        public void run() {

            surface.amplitude =  map(audio.getMaxAmplitude(), 0,32767,0,2);
            runOnUiThread(new Runnable() {
                public void run() {
                    surface.updateBitmap();
                }
            });


                OscBundle bndl = new OscBundle();
                OscMessage msg;
                msg=new

                OscMessage("/sound");

                msg.add(playerNumber);
                msg.add(surface.amplitude);
                bndl.add(msg);
                oscP5.send(bndl,myRemoteLocation);


                Log.v("MIC","Amplitude: "+

                map(surface.amplitude, 0,32767,0,2)

                );
                //SEND OSC MESSAGE
            }
        }


    public class NERDLabListener implements OscEventListener {
        public void oscEvent(OscMessage event) {
            System.out.println("Event: " + event.toString());
            if (event.checkAddrPattern("/set")) {
                //SET MESSAGE

                if(event.get(0).stringValue().equals("reaction")) {
                    surface.reacting = true;
                    Timer reactionTimer = new Timer();
                    reactionTimer.schedule(new stopReacting(), 50);

                    runOnUiThread(new Runnable() {
                        public void run() {
                            surface.updateBitmap();

                        }
                    });
                }

                if(event.get(0).stringValue().equals("score")) {
                    score = event.get(1).intValue();
                    final String scoreText = scoreName + ": " + score;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scoreBox.setText(scoreText);
                        }
                    });
                }

                if (event.get(0).stringValue().equals("control_enabled")) {
                    final int visibility;
                    if (event.get(1).intValue() == 1) {
                        visibility = View.VISIBLE;
                    }
                    else {
                        visibility = View.INVISIBLE;
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gestureView.setVisibility(visibility);
                        }
                    });
                }
                if (event.get(0).stringValue().equals("control")) {
                    System.out.println("CONTROL CHANGE");
                    gameControl = event.get(1).intValue();
                    System.out.println("CONTROL NOW " + gameControl);
                    System.out.println("Setting Gesture Control");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TypedArray gestureImages;
                            gestureImages = getResources().obtainTypedArray(R.array.gestures);
                            Bitmap bImage = BitmapFactory.decodeResource(getResources(), gestureImages.getResourceId(gameControl, 0));
                            gestureView.setImageBitmap(bImage);
                            System.out.println("Updating Bitmap");
                            gestureView.refreshDrawableState();
                            surface.rotation = 0;
                            System.out.println("Refreshing");
                            if (gameControl == GAME_CONTROL_AUDIO && usingAtlantisImageSet) {
                                surface.bitmapId  = R.drawable.mic;

                            }
                            if (gameControl == GAME_CONTROL_ACCEL && usingAtlantisImageSet) {
                                surface.bitmapId  = R.drawable.bell;
                                surface.x = surface.getWidth()/2;
                                surface.y = surface.getHeight()/2;


                            }

                            if (gameControl == GAME_CONTROL_ROTATE && usingAtlantisImageSet) {
                                surface.bitmapId = R.drawable.wheel;
                                surface.x = surface.getWidth()/2;
                                surface.y = surface.getHeight()/2;

                            }

                            if (gameControl == GAME_CONTROL_TAP && usingAtlantisImageSet) {
                                surface.bitmapId = R.drawable.waterpump;
                                surface.x = surface.getWidth()/2;
                                surface.y = surface.getHeight()/2;

                            }

                            surface.updateBitmap();
                            if (gameControl != GAME_CONTROL_NOTHING || usingAtlantisImageSet == false) {
                                gestureView.setVisibility(View.VISIBLE);
                            }
                            else {
                                gestureView.setVisibility(View.INVISIBLE);

                            }

                        }
                    });
                if (gameControl != GAME_CONTROL_TAP) {
                    surface.holding = false;
                }

                if (usingAudio && gameControl != GAME_CONTROL_AUDIO) {
                        timer.cancel();
                        surface.audioEnabled = false;
                        System.out.println("Stopping Audio");
                        usingAudio = false;

                }
                    System.out.println("Checked if audio was playing");

                if (gameControl == GAME_CONTROL_AUDIO) {
                        surface.audioEnabled = true;
                        if (!usingAudio) {
                            usingAudio = true;

                        }
                    timer = new Timer();
                    timer.scheduleAtFixedRate(new MicCheck(), 0, 500);
                    System.out.println("Setting Audio Timer");
                        }
                else {
                        System.out.println("Not Using Audio");
                }
                    System.out.println("Checked for audio settings");
                    surface.x = screenWidth/2;
                    surface.y = screenHeight/2;
                    System.out.println("Updated X and Y");
                }

                if (event.get(0).stringValue().equals("outgamemessage")) {
                    System.out.println("settings instructions to " + event.get(1).stringValue());

                    instructions = event.get(1).stringValue();
                    runOnUiThread(new Runnable()
                    {
                        public void run()
                        {
                            instructionBox.setText(instructions);
                        }
                    });


                }

            }




            if (event.checkAddrPattern("/start")) {
                surface.x = screenWidth/2;
                surface.y = screenHeight/2;
                System.out.println("GOT START MESSAGE");
                playerNumber = event.get(0).intValue();
                surface.imageSet = event.get(1).intValue();
                playerAvatar = event.get(2).intValue();

                TypedArray images;
                switch(surface.imageSet) {
                    case IMAGE_SET_ABSTRACT:
                        images = getResources().obtainTypedArray(R.array.abstract_shapes);
                        usingAtlantisImageSet = false;
                        break;
                    case IMAGE_SET_EVACUATE:
                        usingAtlantisImageSet = true;
                        images = getResources().obtainTypedArray(R.array.evacuation);
                        break;
                    case IMAGE_SET_SUBMARINES:
                        usingAtlantisImageSet = false;
                        System.out.println("Setting Submarine Images");
                        images = getResources().obtainTypedArray(R.array.submarines);
                        break;
                    default:
                        images = getResources().obtainTypedArray(R.array.abstract_shapes);
                        System.out.println("Couldn't Find Image Set for " + surface.imageSet);
                        usingAtlantisImageSet = false;
                        break;
                }
                surface.bitmapId  = images.getResourceId(playerAvatar, 0);
                if (surface.imageSet == IMAGE_SET_EVACUATE) {
                    //start with the pump
                    surface.bitmapId = R.drawable.waterpump;

                }
                surface.updateBitmap();
                scoreName = event.get(3).stringValue();
                score = event.get(4).intValue();
                color_r = event.get(5).intValue();
                color_g = event.get(6).intValue();
                color_b = event.get(7).intValue();

                surface.playerColor = new Paint();
                ColorFilter filter = new LightingColorFilter(Color.rgb(color_r, color_g, color_b), 1);
                surface.playerColor.setColorFilter(filter);

                gameControl = event.get(8).intValue();
                final int visibility;
                if (gameControl != GAME_CONTROL_NOTHING) {
                    surface.allowChangesToCanvas = true;
                    visibility = View.INVISIBLE;
                }
                else {
                    visibility = View.VISIBLE;
                }
                runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        scoreBox.setText(scoreName + ": " + score);
                        gestureView.setVisibility(visibility);
                    }
                });


                if (gameControl == GAME_CONTROL_AUDIO) {
                    surface.audioEnabled = true;
                    if (!usingAudio) {
                        System.out.println("Starting Audio");
                        usingAudio = true;

                    }
                    timer = new Timer();
                    timer.scheduleAtFixedRate(new MicCheck(), 0, 500);
                    System.out.println("Setting Audio Timer");

                }



            }



        }
        public void oscStatus(OscStatus oscStatus) {
            System.out.println("Status: " + oscStatus.id());
        }
    }
}
