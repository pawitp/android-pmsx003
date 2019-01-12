package pawitp.pmsensor;

import android.util.Log;

public class SensorReader {

    private static final String TAG = "SensorReader";

    private static int DATA_SIZE = 32;

    private CircularBuffer mBuffer = new CircularBuffer(DATA_SIZE);
    private SensorOutput mStdOutput = new SensorOutput(0, 0, 0);
    private SensorOutput mAtmOutput = new SensorOutput(0, 0, 0);

    public void insert(byte[] data) {
        for (byte ch : data) {
            mBuffer.insert(ch);
            parseBuffer();
        }
    }

    public String getCurrentBuffer() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < DATA_SIZE; i++) {
            sb.append(mBuffer.get(i));
            sb.append(" ");
        }
        return sb.toString();
    }

    public SensorOutput getStdOutput() {
        return mStdOutput;
    }

    public SensorOutput getAtmOutput() {
        return mAtmOutput;
    }

    private void parseBuffer() {
        if (mBuffer.get(0) == 66 && mBuffer.get(1) == 77) {
            int check = 0;
            for (int i = 0; i < DATA_SIZE - 2; i++) {
                check += (mBuffer.get(i) & 0xff);
            }

            int deviceCheck = getData(DATA_SIZE - 2);
            if (deviceCheck == check) {
                mStdOutput = new SensorOutput(getData(4), getData(6), getData(8));
                mAtmOutput = new SensorOutput(getData(10), getData(12), getData(14));
            } else {
                Log.w(TAG, "Failed checksum device: " + deviceCheck + " calc: " + check);
            }
        }
    }

    private int getData(int index) {
        return ((mBuffer.get(index) & 0xff) << 8) | (mBuffer.get(index + 1) & 0xff);
    }

}
