package pawitp.pmsensor;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.HashMap;

public class MainActivity extends Activity implements UsbSerialInterface.UsbReadCallback {

    UsbManager mUsbManager;
    UsbSerialDevice mSerial;
    TextView mStatus;
    TextView mBuffer;
    TextView mTextPm1_0;
    TextView mTextPm2_5;
    TextView mTextPm10;
    TextView mTextAqiPm2_5;
    TextView mTextAqiPm10;
    Switch mDataSwitch;
    final SensorReader mSensorReader = new SensorReader();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        updateUi();
    }

    public void start(View view) {
        UsbDevice device = findUsbDevice("CP2102");
        if (device != null) {
            if (!mUsbManager.hasPermission(device)) {
                mUsbManager.requestPermission(device, PendingIntent.getActivity(this, 0, new Intent("test"), 0));
            } else {
                UsbDeviceConnection usbConnection = mUsbManager.openDevice(device);
                mSerial = UsbSerialDevice.createUsbSerialDevice(device, usbConnection);
                mSerial.open();
                mSerial.setBaudRate(9600);
                mSerial.setDataBits(UsbSerialInterface.DATA_BITS_8);
                mSerial.setParity(UsbSerialInterface.PARITY_NONE);
                mSerial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                mSerial.read(this);
                mStatus.setText("Reading from " + device.getProductName());
            }
        } else {
            mStatus.setText("No device found");
        }
    }

    public void stop(View view) {
        mSerial.syncClose();
        mStatus.setText("Stopped");
    }

    private void dataSwitch() {
        if (mDataSwitch.isChecked()) {
            mDataSwitch.setText("Using ATM");
        } else {
            mDataSwitch.setText("Using STD");
        }
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

    private UsbDevice findUsbDevice(String name) {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        for (UsbDevice dev : deviceList.values()) {
            if (dev.getProductName().contains(name)) {
                return dev;
            }
        }
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
