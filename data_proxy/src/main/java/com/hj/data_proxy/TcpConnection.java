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
        int ret = mUsbDataProxy.sendProxyMsg(mId, Proxy.ConnType.TCP, Proxy.MsgType.CONNECT, ip, port,
                    0, 0, "", null);
        return ret;
    }

    @Override
    public int send(byte[] data) {
        int ret = mUsbDataProxy.sendProxyMsg(mId, Proxy.ConnType.TCP, Proxy.MsgType.SEND, "", 0,
                0, 0, "", data);
        return ret;
    }

    @Override
    public void close() {
        mUsbDataProxy.sendProxyMsg(mId, Proxy.ConnType.TCP, Proxy.MsgType.CLOSE, "", 0,
                0, 0, "", null);
    }
}
