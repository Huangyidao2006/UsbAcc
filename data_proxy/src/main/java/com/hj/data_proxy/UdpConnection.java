package com.hj.data_proxy;

import com.myusb.proxy.proto.Proxy;

/**
 * Created by huangjian at 21-9-8 18:13
 */
public class UdpConnection extends NetConnection {
    UdpConnection(UsbDataProxy proxy, int id) {
        super(proxy, id);
    }

    @Override
    public int sendTo(byte[] data, String ip, int port) {
        if (mIsClosed) {
            return ErrorCode.ERROR_INVALID_OPERATION;
        }

        int ret = mUsbDataProxy.sendProxyMsg(mId, Proxy.ConnType.UDP, Proxy.MsgType.SEND, ip, port,
                0, 0, "", data);
        return ret;
    }

    @Override
    public void close() {
        if (mIsClosed) {
            return;
        }

        mIsClosed = true;
        mUsbDataProxy.sendProxyMsg(mId, Proxy.ConnType.UDP, Proxy.MsgType.CLOSE, "", 0,
                0, 0, "", null);
    }
}
