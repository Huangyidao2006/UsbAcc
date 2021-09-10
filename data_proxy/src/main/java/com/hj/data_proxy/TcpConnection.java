package com.hj.data_proxy;

import com.myusb.proxy.proto.Proxy;

/**
 * Created by huangjian at 21-9-8 18:09
 */
public class TcpConnection extends NetConnection {
    TcpConnection(UsbDataProxy proxy, int id) {
        super(proxy, id);
    }

    @Override
    public int connect(String ip, int port) {
        if (mIsConnected) {
            return ErrorCode.ERROR_INVALID_OPERATION;
        }

        int ret = mUsbDataProxy.sendProxyMsg(mId, Proxy.ConnType.TCP, Proxy.MsgType.CONNECT, ip, port,
                    0, 0, "", null);
        if (ErrorCode.SUCCESS == ret) {
            mIsConnected = true;
        }

        return ret;
    }

    @Override
    public int send(byte[] data) {
        if (!mIsConnected || mIsClosed) {
            return ErrorCode.ERROR_INVALID_OPERATION;
        }

        int ret = mUsbDataProxy.sendProxyMsg(mId, Proxy.ConnType.TCP, Proxy.MsgType.SEND, "", 0,
                0, 0, "", data);
        return ret;
    }

    @Override
    public void close() {
        if (!mIsConnected) {
            return;
        }

        if (mIsClosed) {
            return;
        }

        mIsConnected = false;
        mIsClosed = true;

        mUsbDataProxy.sendProxyMsg(mId, Proxy.ConnType.TCP, Proxy.MsgType.CLOSE, "", 0,
                0, 0, "", null);
    }
}
