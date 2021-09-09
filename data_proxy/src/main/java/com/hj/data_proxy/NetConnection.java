package com.hj.data_proxy;

/**
 * Created by huangjian at 21-9-8 18:00
 */
public abstract class NetConnection {
    protected int mId;
    protected int mMsgId = 1;
    protected ConnectionListener mListener;
    protected UsbDataProxy mUsbDataProxy;

    public interface ConnectionListener {
        void onRecv(byte[] data, String ip, int port);
        void onError(int error, String des);
    }

    NetConnection(UsbDataProxy proxy, int id) {
        mUsbDataProxy = proxy;
        mId = id;
    }

    public int getId() {
        return mId;
    }

    public void setListener(ConnectionListener listener) {
        mListener = listener;
    }

    ConnectionListener getListener() {
        return mListener;
    }

    public int connect(String ip, int port) {
        return ErrorCode.ERROR_INVALID_OPERATION;
    }

    public int send(byte[] data) {
        return ErrorCode.ERROR_INVALID_OPERATION;
    }

    public int sendTo(byte[] data, String ip, int port) {
        return ErrorCode.ERROR_INVALID_OPERATION;
    }

    public abstract void close();
}
