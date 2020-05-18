package com.example.sensor;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.time.LocalTime;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor temperature;
    private MqttHelper mqttHelper;

    private long timeLastPublish = System.currentTimeMillis();

    private long delayBetweenPublish = 2000;

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        temperature = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mqttHelper = new MqttHelper(getApplicationContext());
        mqttHelper.mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.w("Debug","Connected");
            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                if(topic.equals("intervalle")) {
                    if (isNumeric(mqttMessage.toString())) {
                        delayBetweenPublish = Long.parseLong(mqttMessage.toString());
                        System.out.println("Nouveau délai entre deux mesures : " + delayBetweenPublish);
                    }

                } else if(topic.equals("alert")) {
                    System.out.println("ALERTE : " + mqttMessage.toString());
                } else if(topic.equals("action")) {
                    switch (mqttMessage.toString()) {
                        case "stop":
                            System.out.println("ARRETEZ TOUT");
                            stopSensor();
                            break;
                        case "start":
                            System.out.println("GO GO GO !!");
                            startSensor();
                            break;
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                Log.w("Debug", "Valeur envoyée");
            }
        });

        startSensor();
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        Float vitesse = event.values[0];
        long currentTime = System.currentTimeMillis();
        if(timeLastPublish < (currentTime - delayBetweenPublish)) {
            timeLastPublish = currentTime;
            Log.w("Mesure", vitesse.toString());
            mqttHelper.publish(vitesse);
        }
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        stopSensor();
    }

    protected void stopSensor() {
        sensorManager.unregisterListener(this);
    }

    protected void startSensor() {
        sensorManager.registerListener(this, temperature, SensorManager.SENSOR_DELAY_UI);
    }

    public static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }
}