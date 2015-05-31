package com.philips.lighting.huerecorder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResource;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

/**
 * MyApplicationActivity - The starting point for creating your own Hue App.  
 * Currently contains a simple view with a button to change your lights to random colours.  Remove this and add your own app implementation here! Have fun!
 *
 * @author SteveyO
 *
 */
public class MyApplicationActivity extends Activity {
    private PHHueSDK phHueSDK;
    private static final int MAX_HUE=65535;
    public static final String TAG = "HueRecorder";
    private Timer myTimer;
    private ArrayList<controlFrame> savedFrames = new ArrayList<controlFrame>();

    public final class controlFrame
    {
        private final int lightIndex;
        private final int hue;
        private final int bri;
        private final int transitionTime;
        private final int upTime;

        public controlFrame(int lightIndex, int hue, int bri, int transitionTime, int upTime)
        {
            this.lightIndex = lightIndex;
            this.hue = hue;
            this.bri = bri;
            this.transitionTime = transitionTime;
            this.upTime = upTime;
        }

        public int getLightIndex()
        {
            return lightIndex;
        }

        public int getHue() {
            return hue;
        }

        public int getBri() {
            return bri;
        }

        public int getTransitionTime() {
            return transitionTime;
        }

        public int getUpTime() {
            return upTime;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);
        setContentView(R.layout.activity_main);
        phHueSDK = PHHueSDK.create();
        final PHBridge bridge = phHueSDK.getSelectedBridge();

        final EditText newAnimationName;
        newAnimationName = (EditText) findViewById(R.id.newAnimationName);

        final Button startButton;
        startButton = (Button) findViewById(R.id.buttonStart);
        startButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                myTimer = new Timer();
                myTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        TimerMethod(bridge);
                    }

                }, 0, 100);
                startButton.setClickable(false);
                savedFrames.clear();
            }

        });

        final Button saveButton;
        saveButton = (Button) findViewById(R.id.buttonSave);
        saveButton.setClickable(false);
        saveButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File (sdCard.getAbsolutePath() + "/Hue animations");
                dir.mkdirs();
                File file = new File(dir, String.valueOf(newAnimationName.getText() + ".txt"));
                FileWriter fw = null;

                try {
                    fw = new FileWriter(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //String json = JsonWriter.objectToJson(savedFrames.get(0));

                Gson gson = new Gson();
                String json = gson.toJson(savedFrames.get(0));

                for(int i = 1;i < savedFrames.size();i++) {
                    json = json.concat(gson.toJson(savedFrames.get(i)));
               }

                try {
                    fw.write(json);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Toast.makeText(getApplicationContext(), "Animation saved",
                        Toast.LENGTH_SHORT).show();
                saveButton.setClickable(false);
            }

        });

        Button stopButton;
        stopButton = (Button) findViewById(R.id.buttonStop);
        stopButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                myTimer.cancel();

                startButton.setClickable(true);
                saveButton.setClickable(true);
            }

        });

    }

    private void TimerMethod(PHBridge bridge)
    {
        //This method is called directly by the timer
        //and runs in the same thread as the timer.
        int numberOfLamps = bridge.getResourceCache().getAllLights().size();

        for(int i = 0;i<numberOfLamps;i++) {
            controlFrame tmp = new controlFrame(i+1, bridge.getResourceCache().getAllLights().get(1).getLastKnownLightState().getHue(), bridge.getResourceCache().getAllLights().get(1).getLastKnownLightState().getBrightness(),
            100, 0);
            //No getOn() method?!
            savedFrames.add(tmp);

            //debug
            //Log.w(TAG, String.valueOf(savedFrames.size()));
        }

        //We call the method that will work with the UI
        //through the runOnUiThread method.
        this.runOnUiThread(Timer_Tick);
    }

    private Runnable Timer_Tick = new Runnable() {
        public void run() {

            //This method runs in the same thread as the UI.

            //Do something to the UI thread here

        }
    };

    public void randomLights() {
        PHBridge bridge = phHueSDK.getSelectedBridge();

        List<PHLight> allLights = bridge.getResourceCache().getAllLights();
        Random rand = new Random();

        for (PHLight light : allLights) {
            PHLightState lightState = new PHLightState();
            //lightState.setTransitionTime(2);
            lightState.setHue(rand.nextInt(MAX_HUE));

            // To validate your lightstate is valid (before sending to the bridge) you can use:  
            // String validState = lightState.validateState();
            bridge.updateLightState(light, lightState, listener);
            //  bridge.updateLightState(light, lightState);   // If no bridge response is required then use this simpler form.
        }
    }
    // If you want to handle the response from the bridge, create a PHLightListener object.
    PHLightListener listener = new PHLightListener() {

        @Override
        public void onSuccess() {
        }

        @Override
        public void onStateUpdate(Map<String, String> arg0, List<PHHueError> arg1) {
            Log.w(TAG, "Light has updated");
        }

        @Override
        public void onError(int arg0, String arg1) {}

        @Override
        public void onReceivingLightDetails(PHLight arg0) {}

        @Override
        public void onReceivingLights(List<PHBridgeResource> arg0) {}

        @Override
        public void onSearchComplete() {}
    };

    @Override
    protected void onDestroy() {
        PHBridge bridge = phHueSDK.getSelectedBridge();
        if (bridge != null) {

            if (phHueSDK.isHeartbeatEnabled(bridge)) {
                phHueSDK.disableHeartbeat(bridge);
            }

            phHueSDK.disconnect(bridge);
            super.onDestroy();
        }
    }
}
