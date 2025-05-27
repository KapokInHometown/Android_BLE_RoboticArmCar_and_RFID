package com.example.admin.mybledemo.ui;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.Utils;
import com.example.admin.mybledemo.adapter.DeviceInfoAdapter;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.BleRequestImpl;
import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.callback.BleMtuCallback;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotifyCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.utils.ByteUtils;

public class CarAndRfidActivity extends AppCompatActivity {
    private static final String CAR_NAME = "BT18-T";
    private static final String CAR_MAC = "98:DA:F0:00:E2:DC";
    private static final String RFID_NAME = "WULIANKEJI";
    private static final String RFID_MAC = "98:DA:C0:00:41:44";

    private static final UUID CAR_SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");
    private static final UUID CAR_NOTIFY_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB");
    private static final UUID CAR_WRITE_UUID = UUID.fromString("0000FFE2-0000-1000-8000-00805F9B34FB");
    private static final UUID RFID_SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");
    private static final UUID RFID_NOTIFY_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB");
    private static final UUID RFID_WRITE_UUID = UUID.fromString("0000FFE2-0000-1000-8000-00805F9B34FB");

    private Ble<BleDevice> ble;
    private BleDevice carDevice;
    private BleDevice rfidDevice;
    private TextView tvCarStatus;
    private TextView tvRfidStatus;
    private TextView tvCarData;
    private TextView tvRfidData;
    private Button btnCarAction;
    private Button btnRfidAction;
    private Handler handler = new Handler();
    private boolean carFound = false;
    private boolean rfidFound = false;

    private ActionBar actionBar;

    private DeviceInfoAdapter adapter;

    private List<BluetoothGattService> gattServices;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_and_rfid);
        tvCarStatus = findViewById(R.id.tv_car_status);
        tvRfidStatus = findViewById(R.id.tv_rfid_status);
        tvCarData = findViewById(R.id.tv_car_data);
        tvRfidData = findViewById(R.id.tv_rfid_data);
        btnCarAction = findViewById(R.id.btn_car_action);
        btnRfidAction = findViewById(R.id.btn_rfid_action);
        ble = Ble.getInstance();
        gattServices = new ArrayList<>();
        adapter = new DeviceInfoAdapter(this, gattServices);
        startScanAndConnect();
        btnCarAction.setOnClickListener(v -> {
            // 发送指令1：小车+RFID
            handler.post(() -> sendCmdToCar("233030365031303030543030303021"));
            handler.postDelayed(() -> sendCmdToCar("233030375031373030543030303021"), 300);
            handler.postDelayed(() -> sendCmdToCar("233030385031303030543030303021"), 600);
            handler.postDelayed(() -> sendCmdToCar("233030395031333030543030303021"), 900);
        });
        btnRfidAction.setOnClickListener(v -> {
            // 发送指令2：只小车
            handler.post(() -> sendCmdToCar("233030365031353030543030303021"));
            handler.postDelayed(() -> sendCmdToCar("233030375031353030543030303021"), 300);
            handler.postDelayed(() -> sendCmdToCar("233030385031353030543030303021"), 600);
            handler.postDelayed(() -> sendCmdToCar("233030395031353030543030303021"), 900);

        });
    }

    private void startScanAndConnect() {
        tvCarStatus.setText("小车扫描中...");
        tvRfidStatus.setText("RFID扫描中...");
        ble.startScan(scanCallback);
        handler.postDelayed(() -> {
            if (!carFound) tvCarStatus.setText("小车未发现");
            if (!rfidFound) tvRfidStatus.setText("RFID未发现");
            ble.stopScan();
        }, 10000);
    }

    private final BleScanCallback<BleDevice> scanCallback = new BleScanCallback<BleDevice>() {
        @Override
        public void onLeScan(BleDevice device, int rssi, byte[] scanRecord) {
            if (!carFound && device.getBleAddress().equalsIgnoreCase(CAR_MAC)) {
                carFound = true;
                carDevice = device;
                tvCarStatus.setText("小车已发现，连接中...");
                ble.connect(carDevice, connectCallback);
            }
            if (!rfidFound && device.getBleAddress().equalsIgnoreCase(RFID_MAC)) {
                rfidFound = true;
                rfidDevice = device;
                tvRfidStatus.setText("RFID已发现，连接中...");
                ble.connect(rfidDevice, connectCallback);
            }
            if (carFound && rfidFound) {
                ble.stopScan();
            }
        }
    };



    private BleConnectCallback<BleDevice> connectCallback = new BleConnectCallback<BleDevice>() {
//        @Override
//        public void onConnectionChanged(BleDevice device) {
//            Log.e("DeviceInfoActivity", "onConnectionChanged: " + device.getConnectionState()+Thread.currentThread().getName());
//            if (device.isConnected()) {
//                actionBar.setSubtitle("已连接");
//            }else if (device.isConnecting()){
//                actionBar.setSubtitle("连接中...");
//            }
//            else if (device.isDisconnected()){
//                actionBar.setSubtitle("未连接");
//            }
//        }

        @Override
        public void onConnectionChanged(BleDevice device) {
            runOnUiThread(() -> {
                if (device.getBleAddress().equalsIgnoreCase(CAR_MAC)) {
                    if (device.isConnected()) {
                        tvCarStatus.setText("小车已连接");
                    } else if (device.isConnecting()) {
                        tvCarStatus.setText("小车连接中...");
                    } else {
                        tvCarStatus.setText("小车未连接");
                    }
                } else if (device.getBleAddress().equalsIgnoreCase(RFID_MAC)) {
                    if (device.isConnected()) {
                        tvRfidStatus.setText("RFID已连接");
                    } else if (device.isConnecting()) {
                        tvRfidStatus.setText("RFID连接中...");
                    } else {
                        tvRfidStatus.setText("RFID未连接");
                    }
                }
            });
//            // 连接成功后自动开启通知监听
//            if (device.isConnected()) {
//                BleLog.i("DeviceInfoActivity", "1");
//
//                ConnectReady(device);
//
//                BleLog.i("DeviceInfoActivity", "11");
//
//            }
        }

        @Override
        public void onConnectFailed(BleDevice device, int errorCode) {
            runOnUiThread(() -> {
                if (device.getBleAddress().equalsIgnoreCase(CAR_MAC)) {
                    tvCarStatus.setText("小车连接失败，错误码:" + errorCode);
                } else if (device.getBleAddress().equalsIgnoreCase(RFID_MAC)) {
                    tvRfidStatus.setText("RFID连接失败，错误码:" + errorCode);
                }
            });
        }

        public void ConnectReady(BleDevice device) {

            BleLog.i("DeviceInfoActivity", "2");

            //设置MTU长度，以免设备回传长度超过20字节被自动丢弃
            ble.setMTU(ble.getConnectedDevices().get(0).getBleAddress(), 96, new BleMtuCallback<BleDevice>() {
                @Override
                public void onMtuChanged(BleDevice device, int mtu, int status) {
                    super.onMtuChanged(device, mtu, status);
                    Utils.showToast("最大支持MTU：" + mtu);
                }
            });

            BleLog.i("DeviceInfoActivity", "3");

            //连接成功后，设置通知
            ble.enableNotify(device, true, new BleNotifyCallback<BleDevice>() {
                @Override
                public void onChanged(BleDevice device, BluetoothGattCharacteristic characteristic) {
                    BleLog.i("DeviceInfoActivity", "4");
                    UUID uuid = characteristic.getUuid();
                    BleLog.e("DeviceInfoActivity", "onChanged==uuid:" + uuid.toString());
                    BleLog.e("DeviceInfoActivity", "onChanged==data:" + ByteUtils.toHexString(characteristic.getValue()));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.showToast(String.format("收到设备通知数据: %s", ByteUtils.toHexString(characteristic.getValue())));
                        }
                    });
                }

                @Override
                public void onNotifySuccess(BleDevice device) {
                    BleLog.i("DeviceInfoActivity", "44");
                    super.onNotifySuccess(device);
                    BleLog.i("DeviceInfoActivity", "55");
                    BleLog.e("DeviceInfoActivity", "onNotifySuccess: "+device.getBleName());
                }

                @Override
                public void onNotifyFailed(BleDevice device, int failedCode) {
                    BleLog.i("DeviceInfoActivity", "444");
                    super.onNotifyFailed(device, failedCode);
                    BleLog.i("DeviceInfoActivity", "555");
                    BleLog.e("DeviceInfoActivity", "onNotifyFailed: "+failedCode);
                }

            });
            BleLog.i("DeviceInfoActivity", "4444");
        }

        public void StartNotify(BleDevice device) {
            if (device.isConnected()) {
                    BleLog.i("DeviceInfoActivity", "1");

                    ConnectReady(device);

                    BleLog.i("DeviceInfoActivity", "11");

            }
        }

        @Override
        public void onServicesDiscovered(BleDevice device, BluetoothGatt gatt) {
            super.onServicesDiscovered(device, gatt);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    BleLog.i("DeviceInfoActivity", "onServicesDiscovered1");
                    gattServices.addAll(gatt.getServices());
                    BleLog.i("DeviceInfoActivity", "onServicesDiscovered2");
                    adapter.notifyDataSetChanged();
                    BleLog.i("DeviceInfoActivity", "onServicesDiscovered3");
                    StartNotify(device);
                    BleLog.i("DeviceInfoActivity", "onServicesDiscovered4");
                }
            });

        }

    };

    private void sendCmdToCar(String hex) {
        if (carDevice != null && carDevice.isConnected()) {
            BleLog.i("Ble", "准备向小车发送指令: " + hex);
            ble.writeByUuid(carDevice, ByteUtils.hexStr2Bytes(hex), CAR_SERVICE_UUID, CAR_WRITE_UUID, new BleWriteCallback<BleDevice>() {
                @Override
                public void onWriteSuccess(BleDevice device, BluetoothGattCharacteristic characteristic) {
                    BleLog.i("Ble", "小车指令写入成功");
                }
                @Override
                public void onWriteFailed(BleDevice device, int failedCode) {
                    Log.e("Ble", "小车指令写入失败: " + failedCode);
                }
            });
        } else {
            BleLog.e("Ble", "小车未连接，无法发送指令");
        }
    }

    private void sendCmdToRfid(String hex) {
        if (rfidDevice != null && rfidDevice.isConnected()) {
            BleLog.i("Ble", "准备向RFID发送指令: " + hex);
            ble.writeByUuid(rfidDevice, ByteUtils.hexStr2Bytes(hex), RFID_SERVICE_UUID, RFID_WRITE_UUID, new BleWriteCallback<BleDevice>() {
                @Override
                public void onWriteSuccess(BleDevice device, BluetoothGattCharacteristic characteristic) {
                    BleLog.i("Ble", "RFID指令写入成功");
                }
                @Override
                public void onWriteFailed(BleDevice device, int failedCode) {
                    Log.e("Ble", "RFID指令写入失败: " + failedCode);
                }
            });
        } else {
            BleLog.e("Ble", "RFID未连接，无法发送指令");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ble.stopScan();
        if (carDevice != null) {
            if (carDevice.isConnecting()) {
                ble.cancelConnecting(carDevice);
            } else if (carDevice.isConnected()) {
                ble.disconnect(carDevice);
            }
        }
        if (rfidDevice != null) {
            if (rfidDevice.isConnecting()) {
                ble.cancelConnecting(rfidDevice);
            } else if (rfidDevice.isConnected()) {
                ble.disconnect(rfidDevice);
            }
        }
        ble.cancelCallback(connectCallback);
    }
}

