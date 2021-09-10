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

    public int push(byte[] data, int offset, int len) {
        int left = left();
        if (left == 0) {
            return 0;
        }

        int total = 0;
        int writeLen = Math.min(left, len);

        if (mEnd + writeLen > mCapacity + 1) {
            int partLen = mCapacity - mEnd + 1;
            total += push(data, offset, partLen);
            writeLen -= partLen;

            if (writeLen != 0) {
                total += push(data, offset + partLen, writeLen);
            }
        } else {
            if (writeLen != 0) {
                System.arraycopy(data, offset, mBuffer, mEnd, writeLen);
                mEnd = (mEnd + writeLen) % (mCapacity + 1);
                total += writeLen;
            }
        }

        return total;
    }

    private int read(int srcStart, byte[] dst, int offset, int len) {
        int readLen = Math.min(size(), len);
        int total = 0;

        if (srcStart + readLen > mCapacity + 1) {
            int partLen = mCapacity - srcStart + 1;
            readLen -= read(srcStart, dst, offset, partLen);
            total += partLen;

            srcStart = (srcStart + partLen) % (mCapacity + 1);

            if (readLen != 0) {
                total += read(srcStart, dst, offset + partLen, readLen);
            }
        } else {
            if (readLen != 0) {
                System.arraycopy(mBuffer, srcStart, dst, offset, readLen);
                total += readLen;
            }
        }

        return total;
    }

    public int read(byte[] dst, int offset, int len) {
        return read(mStart, dst, offset, len);
    }

    public byte[] pop(int len) {
        int realLen = Math.min(size(), len);
        if (realLen == 0) {
            return null;
        }

        byte[] buffer = new byte[realLen];
        read(buffer, 0, realLen);
        mStart = (mStart + realLen) % (mCapacity + 1);

        return buffer;
    }

    public void clear() {
        mStart = 0;
        mEnd = 0;
    }
}
