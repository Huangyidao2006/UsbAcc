package com.hj.data_proxy.buffer;

/**
 * Created by huangjian at 21-9-8 16:46
 */
public class DataBuffer {
    private int mStart = 0;
    private int mEnd = 0;
    private int mCapacity = 0;

    private byte[] mBuffer;

    public DataBuffer(int capacity) {
        mCapacity = capacity;
        mBuffer = new byte[mCapacity + 1];
    }

    public int capacity() {
        return mCapacity;
    }

    public int size() {
        return (mEnd - mStart + mCapacity + 1) % (mCapacity + 1);
    }

    public int left() {
        return capacity() - size();
    }

    public int push(byte[] data, int len) {
        int left = left();
        if (left == 0) {
            return 0;
        }

        int copyLen = Math.min(left, len);
        if (mEnd + copyLen > mCapacity) {
            int copyLen1 = mCapacity - mEnd;
            int copyLen2 = copyLen - copyLen1;

            System.arraycopy(data, 0, mBuffer, mEnd, copyLen1);
            mEnd = (mEnd + copyLen1) % (mCapacity + 1);
            System.arraycopy(data, copyLen1, mBuffer, mEnd, copyLen2);
            mEnd = (mEnd + copyLen2) % (mCapacity + 1);
        } else {
            System.arraycopy(data, 0, mBuffer, mEnd, copyLen);
        }

        return copyLen;
    }

    public int read(byte[] dst) {
        int readLen = Math.min(size(), dst.length);

        if (mStart + readLen > mCapacity) {
            int readLen1 = mCapacity - mStart;
            int readLen2 = readLen - readLen1;

            System.arraycopy(mBuffer, mStart, dst, 0, readLen1);
            int pos = (mStart + readLen1) % (mCapacity + 1);
            System.arraycopy(mBuffer, pos, dst, readLen1, readLen2);
        } else {
            System.arraycopy(mBuffer, mStart, dst, 0, readLen);
        }

        return readLen;
    }

    public byte[] pop(int len) {
        int readLen = Math.min(size(), len);
        if (readLen == 0) {
            return null;
        }

        byte[] buffer = new byte[len];
        read(buffer);
        mStart = (mStart + len) % (mCapacity + 1);

        return buffer;
    }

    public void clear() {
        mStart = 0;
        mEnd = 0;
    }
}
