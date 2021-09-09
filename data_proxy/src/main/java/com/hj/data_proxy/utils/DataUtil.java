package com.hj.data_proxy.utils;

import java.nio.ByteBuffer;

/**
 * Created by huangjian at 21-9-8 16:52
 */
public class DataUtil {
    public static byte[] toBytes(int n) {
        byte[] buffer = new byte[4];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.putInt(n);

        return byteBuffer.array();
    }

    public static int toInt(byte[] buffer, int offset) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, offset, 4);
        int n = byteBuffer.getInt();

        return n;
    }

    public static int toInt(byte[] buffer) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, 4);
        int n = byteBuffer.getInt();

        return n;
    }

    public static boolean byteCompare(byte[] buffer1, byte[] buffer2, int len) {
        if (buffer1.length < len || buffer2.length < len) {
            return false;
        }

        int i = 0;
        while (i < len && buffer1[i] == buffer2[i]) {
            i++;
        }

        return i == len;
    }
}
