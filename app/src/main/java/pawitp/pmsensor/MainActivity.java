package pawitp.pmsensor;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.HashMap;

public class MainActivity extends Activity implements UsbSerialInterface.UsbReadCallback {

    private static final String ACTION_USB_PERMISSION = "pawitp.pmsensor.USB_PERMISSION";
    private static final String TAG = "MainActivity";

    private UsbManager mUsbManager;
    private UsbSerialDevice mSerial;
    private TextView mStatus;
    private TextView mBuffer;
    private TextView mTextPm1_0;
    private TextView mTextPm2_5;
    private TextView mTextPm10;
    private TextView mTextAqiPm2_5;
    private TextView mTextAqiPm10;
    private Switch mDataSwitch;
    private PendingIntent mPermissionIntent;
    private final SensorReader mSensorReader = new SensorReader();

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    connect(device);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                stop(null);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register callback to connect to device after asking for permission
        mPermissionIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);

        setContentView(R.layout.activity_main);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mStatus = findViewById(R.id.txtStatus);
        mBuffer = findViewById(R.id.txtBuffer);
        mTextPm1_0 = findViewById(R.id.txtPm1_0);
        mTextPm2_5 = findViewById(R.id.txtPm2_5);
        mTextPm10 = findViewById(R.id.txtPm10);
        mTextAqiPm2_5 = findViewById(R.id.txtAqiPm2_5);
        mTextAqiPm10 = findViewById(R.id.txtAqiPm10);
        mDataSwitch = findViewById(R.id.dataSwitch);
        mDataSwitch.setOnCheckedChangeListener((compoundButton, b) -> dataSwitch());
        dataSwitch();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        stopRead();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        startRead();
    }

    public void start(View view) {
        UsbDevice device = findUsbDevice();
        if (device != null) {
            if (!mUsbManager.hasPermission(device)) {
                mUsbManager.requestPermission(device, mPermissionIntent);
            } else {
                connect(device);
            }
        } else {
            mStatus.setText(R.string.no_device_found);
        }
    }

    public void stop(View view) {
        stopRead();
        mSerial = null;
        mStatus.setText(R.string.stopped);
    }

    private void connect(UsbDevice device) {
        UsbDeviceConnection usbConnection = mUsbManager.openDevice(device);
        mSerial = UsbSerialDevice.createUsbSerialDevice(device, usbConnection);
        startRead();
        mStatus.setText(getString(R.string.connected_to, device.getProductName()));
    }

    private void startRead() {
        if (mSerial != null) {
            mSerial.open();
            mSerial.setBaudRate(9600);
            mSerial.setDataBits(UsbSerialInterface.DATA_BITS_8);
            mSerial.setParity(UsbSerialInterface.PARITY_NONE);
            mSerial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
            mSerial.read(this);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void stopRead() {
        if (mSerial != null) {
            mSerial.syncClose();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void dataSwitch() {
        if (mDataSwitch.isChecked()) {
            mDataSwitch.setText(R.string.using_atm);
        } else {
            mDataSwitch.setText(R.string.using_std);
        }
        updateUi();
    }

    private void updateUi() {
        synchronized (mSensorReader) {
            mBuffer.setText(mSensorReader.getCurrentBuffer());
            SensorOutput sensorOutput = mDataSwitch.isChecked() ?
                    mSensorReader.getAtmOutput() : mSensorReader.getStdOutput();
            AqiCalculator.AqiValue pm2_5Aqi = AqiCalculator.getAqi(sensorOutput.pm2_5, AqiCalculator.PM_2_5_AQI);
            AqiCalculator.AqiValue pm10Aqi = AqiCalculator.getAqi(sensorOutput.pm10, AqiCalculator.PM_10_AQI);

            mTextPm1_0.setText(String.valueOf(sensorOutput.pm1_0));

            mTextPm2_5.setText(String.valueOf(sensorOutput.pm2_5));
            mTextAqiPm2_5.setText(String.valueOf(pm2_5Aqi.value));
            mTextAqiPm2_5.setBackgroundResource(pm2_5Aqi.color);

            mTextPm10.setText(String.valueOf(sensorOutput.pm10));
            mTextAqiPm10.setText(String.valueOf(pm10Aqi.value));
            mTextAqiPm10.setBackgroundResource(pm10Aqi.color);
        }
    }

    private UsbDevice findUsbDevice() {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();

        // Try and find a known device
        for (UsbDevice dev : deviceList.values()) {
            String productName = dev.getProductName();
            if (productName != null && productName.contains("CP2102")) {
                return dev;
            }
        }

        // None found, try the first one if exists
        if (!deviceList.isEmpty()) {
            return deviceList.values().iterator().next();
        }

        // Else return null
        return null;
    }

    @Override
    public void onReceivedData(byte[] data) {
        synchronized (mSensorReader) {
            mSensorReader.insert(data);
        }
        runOnUiThread(this::updateUi);
    }

}
