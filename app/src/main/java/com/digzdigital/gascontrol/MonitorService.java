package com.digzdigital.gascontrol;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.digzdigital.gascontrol.event.EventType;
import com.digzdigital.gascontrol.event.MessageEvent;

import org.greenrobot.eventbus.EventBus;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;


public class MonitorService extends Service implements BluetoothSPP.OnDataReceivedListener, BluetoothSPP.BluetoothConnectionListener, BluetoothSPP.AutoConnectionListener {

    private static final int NOTIFY_ID = 1;
    private final IBinder monitorBind = new MonitorBinder();
    private Notification.Builder builder = null;
    private BluetoothSPP bluetoothSPP;
    private String address;
    private Ringtone alarm;
    private boolean alarmState = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        address = intent.getStringExtra("device");
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return monitorBind;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothSPP = new BluetoothSPP(this);

    }

    @Override
    public boolean onUnbind(Intent intent) {

        return false;
    }

    @Override
    public void onDataReceived(byte[] data, String message) {

        if (message.equals("B") || message.equals("b")) {
            if (!alarmState){
                postMessage(EventType.MESSAGE_RECEIVED);
                playAlarm();
                alarmState = true;

            }

        }
    }

    private void showNotification() {
        Notification notification = builder.build();
        startForeground(NOTIFY_ID, notification);
    }

    private void dismissNotification() {
        stopForeground(true);
    }

    @Override
    public void onDestroy() {
        stopBluetooth();
        dismissNotification();
    }

    private void buildNotification() {
        PendingIntent pendingIntent = createPendingIntent();
        builder = createNotificationBuilder(pendingIntent);
    }

    private PendingIntent createPendingIntent() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Notification.Builder createNotificationBuilder(PendingIntent pendingIntent) {
        builder = new Notification.Builder(this);
        return builder.setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("Gas Monitor")
                .setOngoing(true)
                .setContentTitle("Gas Monitor");
    }

    private void editNotificationBuilder(String contentText) {
        if (builder == null) buildNotification();
        builder.setContentText(contentText);
    }

    public void startBluetooth() {
        bluetoothSPP.setupService();
        bluetoothSPP.startService(BluetoothState.DEVICE_OTHER);
        bluetoothSPP.connect(address);
        bluetoothSPP.setBluetoothConnectionListener(this);
        bluetoothSPP.setOnDataReceivedListener(this);
        // bluetoothSPP.autoConnect("HC-05");
        bluetoothSPP.setAutoConnectionListener(this);
        if (builder == null) buildNotification();
        editNotificationBuilder("Setting up monitoring");
        showNotification();


    }

    public void stopBluetooth() {
        bluetoothSPP.stopService();
        dismissNotification();
        editNotificationBuilder("Monitoring suspended");
        showNotification();

    }

    private void playAlarm() {
        Uri alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        this.alarm = RingtoneManager.getRingtone(getApplicationContext(), alarm);
        this.alarm.play();
    }

    public void stopAlarm() {
        alarm.stop();
        bluetoothSPP.send("B", false);
    }

    @Override
    public void onDeviceConnected(String name, String address) {
        alarmState = false;
        if (builder == null) buildNotification();
        editNotificationBuilder("Monitoring gas installation");
        showNotification();

        postMessage(EventType.CONNECTED);
    }

    @Override
    public void onDeviceDisconnected() {
        if (builder == null) buildNotification();
        editNotificationBuilder("Waiting for device connection");
        showNotification();
        postMessage(EventType.DISCONNECTED /*"Device disconnected"*/);
    }

    @Override
    public void onDeviceConnectionFailed() {

        postMessage(EventType.CONNECTION_FAILED /*"Device connection failed"*/);
    }

    private void postMessage(EventType message) {
        EventBus.getDefault().post(new MessageEvent(message));
    }

    @Override
    public void onAutoConnectionStarted() {
        postMessage(EventType.AUTO_CONNECTED);

    }

    @Override
    public void onNewConnection(String name, String address) {
        postMessage(EventType.NEW_CONNECTION);

    }

    public class MonitorBinder extends Binder {
        MonitorService getService() {
            return MonitorService.this;
        }
    }


}
