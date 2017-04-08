package com.digzdigital.gascontrol;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.digzdigital.gascontrol.MonitorService.MonitorBinder;
import com.digzdigital.gascontrol.databinding.ActivityMainBinding;
import com.digzdigital.gascontrol.event.MessageEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityMainBinding binding;
    private MonitorService monitorService;
    private Intent monitorIntent;
    private BluetoothSPP bluetoothSPP;
    private boolean serviceConnected = false;


    private ServiceConnection monitorConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MonitorBinder monitorBinder = (MonitorBinder) service;

            monitorService = monitorBinder.getService();
            serviceConnected = true;
            Toast.makeText(MainActivity.this, "Connected to bluetooth service", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceConnected = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        bluetoothSPP = new BluetoothSPP(this);
        if (!bluetoothSPP.isBluetoothAvailable()) {
            Snackbar.make(binding.activityMain, "This device doesn't support bluetooth", Snackbar.LENGTH_LONG).show();
            binding.connectDevice.setEnabled(false);
        }
        binding.toggleMonitorButton.setOnClickListener(this);
        binding.connectDevice.setOnClickListener(this);
        binding.stopAlarm.setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (bluetoothSPP.isBluetoothAvailable() && !bluetoothSPP.isBluetoothEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, BluetoothState.REQUEST_ENABLE_BT);
            return;
        }
        binding.connectDevice.setEnabled(true);
        EventBus.getDefault().register(this);
    }

    private void startServiceIntent(String address) {
        if (monitorIntent == null) {
            monitorIntent = new Intent(this, MonitorService.class);
            monitorIntent.putExtra("device", address);
            bindService(monitorIntent, monitorConnection, Context.BIND_AUTO_CREATE);
            startService(monitorIntent);
        }
    }

    @Override
    protected void onStop() {
        bluetoothSPP.stopService();
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        destroyService();
        super.onDestroy();
    }

    private void destroyService() {
        if (monitorIntent != null) stopService(monitorIntent);
        monitorService = null;
    }

    private void pickDevice() {
        Intent intent = new Intent(this, DeviceList.class);
        startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK) {
                String address = data.getExtras().getString("device_address");
                startServiceIntent(address);
            }
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bluetoothSPP.setupService();
                bluetoothSPP.startService(BluetoothState.DEVICE_OTHER);
            }
        } else {
            //DO something else of user doesn't choose any device
            Snackbar.make(binding.activityMain, "No device chosen", Snackbar.LENGTH_LONG).show();
        }
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.toggleMonitorButton:
                if (binding.toggleMonitorButton.isChecked() && serviceConnected) {
                    monitorService.startBluetooth();
                    setMonitorStateText("Monitoring on");
                }
                if (!binding.toggleMonitorButton.isChecked() && serviceConnected) {
                    monitorService.stopBluetooth();
                    setMonitorStateText("Monitoring off");
                }
                if (binding.toggleMonitorButton.isChecked() && !serviceConnected) {
                    binding.toggleMonitorButton.setChecked(false);
                    setMonitorStateText("Monitoring off");
                    Toast.makeText(this, "Pick a device first", Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.connectDevice:
                startServiceIntent("20:16:05:05:48:17");
                // pickDevice();
                break;
            case R.id.stopAlarm:
                try {
                    monitorService.stopAlarm();

                }catch (Exception ignore){

                }
                break;
        }
    }

    private void setMonitorStateText(String text) {
        binding.monitorStateText.setText(text);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        switch (event.eventType){
            case CONNECTED:
                Snackbar.make(binding.activityMain, "Device connected", Snackbar.LENGTH_LONG).show();
                break;
            case DISCONNECTED:
                Snackbar.make(binding.activityMain, "Device disconnected", Snackbar.LENGTH_LONG).show();
                break;
            case CONNECTION_FAILED:
                Snackbar.make(binding.activityMain, "Device connection failed", Snackbar.LENGTH_LONG).show();
                break;
            case AUTO_CONNECTED:
                Snackbar.make(binding.activityMain, "Device auto connected", Snackbar.LENGTH_LONG).show();
                break;
            case NEW_CONNECTION:
                Snackbar.make(binding.activityMain, "Device new connection", Snackbar.LENGTH_LONG).show();
                break;
            case MESSAGE_RECEIVED:
                Snackbar.make(binding.activityMain, "Incoming Message From Microcontroller", Snackbar.LENGTH_LONG).show();
                break;
        }
    }
}
