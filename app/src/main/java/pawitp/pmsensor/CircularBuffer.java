package pawitp.pmsensor;

public class CircularBuffer {

    byte[] mArray;
    int mCurrentIndex;

    public CircularBuffer(int size) {
        mArray = new byte[size];
    }

    public void insert(byte ch) {
        mCurrentIndex = (mCurrentIndex + 1) % mArray.length;
        mArray[mCurrentIndex] = ch;
    }

    public int get(int i) {
        return mArray[(mCurrentIndex + i + 1) % mArray.length];
    }

}
