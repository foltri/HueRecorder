package com.philips.lighting.huerecorder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
import com.google.gson.stream.JsonWriter;
import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHMessageType;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResource;
import com.philips.lighting.model.PHBridgeResourcesCache;
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
    private ArrayList<controlFrame> lastLightFrame = new ArrayList<controlFrame>();
    private boolean recording = false;
    private ArrayList<Integer> timeSinceLastFrame = new ArrayList<Integer>();
    private boolean firstSample = false;

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

        // Register the PHSDKListener to receive callbacks from the bridge.
        phHueSDK.getNotificationManager().registerSDKListener(listener1);

        final EditText newAnimationName;
        newAnimationName = (EditText) findViewById(R.id.newAnimationName);

        final Button startButton;
        startButton = (Button) findViewById(R.id.buttonStart);
        startButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                savedFrames.clear();
                lastLightFrame.clear();
                startButton.setClickable(false);
                recording = true;   //todo interrupt problem
                firstSample = true;

                timeSinceLastFrame.clear();

                for(int i = 0;i < bridge.getResourceCache().getAllLights().size();i++) {
                    timeSinceLastFrame.add(0);
                    controlFrame tmp = new controlFrame(i+1, 0,0,0,0);
                    lastLightFrame.add(tmp);
                }
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

                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                try {
                    writeJsonStream(outputStream, savedFrames);
                } catch (IOException e) {
                    e.printStackTrace();
                }

//                try {
//                    fw = new FileWriter(file);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                //String json = JsonWriter.objectToJson(savedFrames.get(0));
//
//                Gson gson = new Gson();
//                String json = gson.toJson(savedFrames.get(0));
//
//                for(int i = 1;i < savedFrames.size();i++) {
//                    json = json.concat(gson.toJson(savedFrames.get(i)));
//               }
//
//                try {
//                    fw.write(json);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                try {
//                    fw.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
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
                if (myTimer != null) myTimer.cancel();
                recording = false;
                startButton.setClickable(true);
                saveButton.setClickable(true);
            }

        });

    }

    private void TimerMethod()
    {
        //This method is called directly by the timer
        //and runs in the same thread as the timer.

        PHBridgeResourcesCache cache = phHueSDK.getSelectedBridge().getResourceCache();
        int numberOfLamps = cache.getAllLights().size();

        for(int i = 0;i<numberOfLamps;i++) {
            //controlFrame tmp = new controlFrame(i+1, cache.getAllLights().get(i).getLastKnownLightState().getHue(), cache.getAllLights().get(i).getLastKnownLightState().getBrightness(),
            //100, 0);

            timeSinceLastFrame.set(i, timeSinceLastFrame.get(i) + 1);

            //debug
           Log.w(TAG, i + ": " + String.valueOf(cache.getAllLights().get(i).getLastKnownLightState().getHue()));
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

    public void writeJsonStream(OutputStream out, ArrayList<controlFrame> frames) throws IOException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        writer.beginArray();

        Gson gson = new Gson();
        for (controlFrame frame : frames) {
            gson.toJson(frame, controlFrame.class, writer);
        }
        writer.endArray();
        writer.close();
    }

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

        public void onCacheUpdated(List cacheNotificationsList, PHBridge bridge) {
            // Here you receive notifications that the BridgeResource Cache was updated. Use the PHMessageType to
            // check which cache was updated, e.g.
            if (cacheNotificationsList.contains(PHMessageType.LIGHTS_CACHE_UPDATED)) {
                System.out.println("Lights Cache Updated ");
            }
        }

        @Override
        public void onSuccess() {
        }

        @Override
        public void onStateUpdate(Map<String, String> arg0, List<PHHueError> arg1) {
            //Log.w(TAG, "Light has updated");
        }

        @Override
        public void onError(int arg0, String arg1) {}

        @Override
        public void onReceivingLightDetails(PHLight arg0) {
            int i = 0;
        }

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

            phHueSDK.disableAllHeartbeat();

            phHueSDK.disconnect(bridge);
            super.onDestroy();
        }
    }

    // Local SDK Listener
    private PHSDKListener listener1 = new PHSDKListener() {

        @Override
        public void onAccessPointsFound(List accessPoint) {
            // Handle your bridge search results here.  Typically if multiple results are returned you will want to display them in a list
            // and let the user select their bridge.   If one is found you may opt to connect automatically to that bridge.
        }

        @Override
        public void onCacheUpdated(List cacheNotificationsList, PHBridge bridge) {
            // Here you receive notifications that the BridgeResource Cache was updated. Use the PHMessageType to
            // check which cache was updated, e.g.
            //Log.w(TAG, "haho");

            if (cacheNotificationsList.contains(PHMessageType.LIGHTS_CACHE_UPDATED)) {
                System.out.println("Lights Cache Updated ");

                if(recording) {
                    List<PHLight> ligthCache = phHueSDK.getSelectedBridge().getResourceCache().getAllLights();
                    ArrayList<controlFrame> changedLights = new ArrayList<controlFrame>();

                    if(firstSample) {
                        myTimer = new Timer();
                        myTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                TimerMethod();
                            }
                        }, 0, 100);
                    }
                    for (int i = 0; i < ligthCache.size(); i++) {
                        if (lastLightFrame.get(i).getHue() != ligthCache.get(i).getLastKnownLightState().getHue() || lastLightFrame.get(i).getBri() != ligthCache.get(i).getLastKnownLightState().getBrightness()) {

                            //.getTransitionTime always gives null!!!!???
                            int upTime = 200;
                            if(timeSinceLastFrame.get(i) > lastLightFrame.get(i).getTransitionTime()) {
                                upTime = timeSinceLastFrame.get(i) - lastLightFrame.get(i).getTransitionTime();
                            }

                            //Only save frames from the second samples when there's correct upTime
                            if(!firstSample) {
                                controlFrame savableFrame = new controlFrame(i + 1, lastLightFrame.get(i).getHue(), lastLightFrame.get(i).getBri(), upTime*100, 0);//lastLightFrame.get(i).getTransitionTime(), upTime);
                                changedLights.add(savableFrame);
                            }

                            //UpTime can only be calculated when the next frame arrives
                            int hue = 0;
                            int bri = 0;
                            int transitionTime = 0;
                            if(ligthCache.get(i).getLastKnownLightState().getHue() != null) hue = ligthCache.get(i).getLastKnownLightState().getHue();
                            if(ligthCache.get(i).getLastKnownLightState().getBrightness() != null) bri = ligthCache.get(i).getLastKnownLightState().getBrightness();
                            //.getTransitionTime always gives null!!!!???
                            if(ligthCache.get(i).getLastKnownLightState().getTransitionTime() != null) transitionTime = ligthCache.get(i).getLastKnownLightState().getTransitionTime();
                            controlFrame tmp = new controlFrame(i + 1, hue, bri, transitionTime, 0);

                            timeSinceLastFrame.set(i, 0);
                            lastLightFrame.set(i, tmp);
                        }
                    }

                    if (firstSample) firstSample = false;


                    for(controlFrame frame:changedLights) {
                        savedFrames.add(frame);
                    }

                }
            }
        }

        @Override
        public void onBridgeConnected(PHBridge b) {
            //phHueSDK.setSelectedBridge(b);
            //phHueSDK.enableHeartbeat(b, PHHueSDK.HB_INTERVAL);
            // Here it is recommended to set your connected bridge in your sdk object (as above) and start the heartbeat.
            // At this point you are connected to a bridge so you should pass control to your main program/activity.
            // Also it is recommended you store the connected IP Address/ Username in your app here.  This will allow easy automatic connection on subsequent use.
        }

        @Override
        public void onAuthenticationRequired(PHAccessPoint accessPoint) {
            //phHueSDK.startPushlinkAuthentication(accessPoint);
            // Arriving here indicates that Pushlinking is required (to prove the User has physical access to the bridge).  Typically here
            // you will display a pushlink image (with a timer) indicating to to the user they need to push the button on their bridge within 30 seconds.
        }

        @Override
        public void onConnectionResumed(PHBridge bridge) {

        }

        @Override
        public void onConnectionLost(PHAccessPoint accessPoint) {
            // Here you would handle the loss of connection to your bridge.
        }

        @Override
        public void onError(int code, final String message) {
            // Here you can handle events such as Bridge Not Responding, Authentication Failed and Bridge Not Found
        }

        @Override
        public void onParsingErrors(List parsingErrorsList) {
            // Any JSON parsing errors are returned here.  Typically your program should never return these.
        }
    };
}
