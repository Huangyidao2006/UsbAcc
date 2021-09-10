package com.hj.data_proxy;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;

import com.google.protobuf.ByteString;
import com.hj.data_proxy.buffer.DataBuffer;
import com.hj.data_proxy.log.CatLogger;
import com.hj.data_proxy.utils.DataUtil;
import com.myusb.proxy.proto.Proxy;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by huangjian at 21-9-8 15:48
 */
public class UsbDataProxy {
    private static final String TAG = "UsbDataProxy";

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private static final String FRAME_BEGIN = "#frame-begin#";

    private static final int FRAME_BEGIN_LEN = 13;

    private static final int FRAME_HEADER_LEN = 17;

    private static int sCONNECTION_ID = 0;

    private static int sMSG_ID = 0;

    private final Object mIdSynObj = new Object();

    private Context mContext;

    private UsbManager mUsbManager;

    private UsbListener mUsbListener;

    private ParcelFileDescriptor mFileDescriptor;

    private boolean mIsConnected;

    private static UsbDataProxy sINSTANCE;

    public interface UsbListener {
        void onAccessoryAttached(UsbAccessory accessory);
        void onAccessoryDetached(UsbAccessory accessory);
        void onPermissionResult(UsbAccessory accessory, boolean isGranted);
        void onConnected(UsbAccessory accessory);
        void onDisconnected();
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

            if (ACTION_USB_PERMISSION.equals(action)) {
                boolean isGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                if (isGranted) {    // try to connect
                    connect();
                }

                if (mUsbListener != null) {
                    mUsbListener.onPermissionResult(accessory, isGranted);
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
                if (mUsbListener != null) {
                    mUsbListener.onAccessoryAttached(accessory);
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                // disconnect
                disconnect();

                if (mUsbListener != null) {
                    mUsbListener.onAccessoryDetached(accessory);
                }
            }
        }
    };

    private PendingIntent mPermissionIntent;

    private void registerReceivers() {
        mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        mContext.registerReceiver(mReceiver, filter);
    }

    private void unregisterReceivers() {
        mContext.unregisterReceiver(mReceiver);
    }

    private UsbDataProxy(Context context, UsbListener listener) {
        mContext = context.getApplicationContext();
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        mUsbListener = listener;

        registerReceivers();
    }

    public static UsbDataProxy createInstance(Context context, UsbListener listener) {
        if (sINSTANCE == null) {
            sINSTANCE = new UsbDataProxy(context, listener);
        }

        return sINSTANCE;
    }

    public static UsbDataProxy getInstance() {
        return sINSTANCE;
    }

    public int connect() {
        if (mIsConnected) {
            return ErrorCode.ERROR_INVALID_OPERATION;
        }

        UsbAccessory[] accArray = mUsbManager.getAccessoryList();
        if (accArray == null) {
            return ErrorCode.ERROR_USB_NO_DEV;
        }

        UsbAccessory curUsb = accArray[0];
        if (!mUsbManager.hasPermission(curUsb)) {
            mUsbManager.requestPermission(curUsb, mPermissionIntent);

            return ErrorCode.ERROR_USB_NO_PERMISSION;
        }

        mFileDescriptor = mUsbManager.openAccessory(curUsb);

        mFos = new FileOutputStream(mFileDescriptor.getFileDescriptor());

        mRecvThread = new RecvThread();
        mRecvThread.start();

        mIsConnected = true;

        if (mUsbListener != null) {
            mUsbListener.onConnected(curUsb);
        }

        return ErrorCode.SUCCESS;
    }

    public void disconnect() {
        if (!mIsConnected) {
            return;
        }

        closeAllNetConn();

        if (mRecvThread != null) {
            mRecvThread.stopRun();
        }

        if (mFileDescriptor != null) {
            try {
                mFileDescriptor.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mUsbListener != null) {
            mUsbListener.onDisconnected();
        }

        mIsConnected = false;
    }

    public void destroy() {
        disconnect();

        unregisterReceivers();

        sINSTANCE = null;
    }

    private static final int MAX_USB_FRAME_LEN = 16384;

    private RecvThread mRecvThread;

    // 1M的帧数据缓存
    private DataBuffer mFrameBuffer = new DataBuffer(1024 * 1024);

    private class RecvThread extends Thread {
        private boolean mStop = false;

        public void stopRun() {
            mStop = true;
        }

        @Override
        public void run() {
            CatLogger.i(TAG, "receive thread started");

            try {
                InputStream ins = new FileInputStream(mFileDescriptor.getFileDescriptor());
                byte[] buffer = new byte[MAX_USB_FRAME_LEN];
                byte[] frameHeaderBuffer = new byte[FRAME_HEADER_LEN];
                int frameDataLen = 0;

                while (!mStop) {
                    int len = ins.read(buffer);
                    CatLogger.d(TAG, "received usb data, len=%d", len);

                    if (len > 0) {
                        mFrameBuffer.push(buffer, 0, len);
                        while (true) {
                            int readLen = mFrameBuffer.read(frameHeaderBuffer, 0, frameHeaderBuffer.length);
                            CatLogger.d(TAG, "read frame buffer, readLen=%d", readLen);

                            if (FRAME_HEADER_LEN == readLen) {
                                CatLogger.d(TAG, "read a frame header");

                                // 读取帧头长度的数据，判断是否为帧头
                                if (DataUtil.byteCompare(FRAME_BEGIN.getBytes(), frameHeaderBuffer, FRAME_BEGIN_LEN)) {
                                    // 获取帧数据长度
                                    frameDataLen = DataUtil.toInt(frameHeaderBuffer, FRAME_BEGIN_LEN);

                                    CatLogger.d(TAG, "frameDataLen=%d", frameDataLen);

                                    if (FRAME_HEADER_LEN + frameDataLen <= mFrameBuffer.size()) {
                                        // 足够一帧数据，取出来用protobuf解析
                                        mFrameBuffer.pop(FRAME_HEADER_LEN);
                                        byte[] frameData = mFrameBuffer.pop(frameDataLen);

                                        // 解析并处理
                                        Proxy.ProxyMsg proxyMsg = Proxy.ProxyMsg.parseFrom(frameData);
                                        processReceivedMsg(proxyMsg);
                                    } else {
                                        // 不够一帧，应继续收数据
                                        break;
                                    }
                                } else {
                                    // 不是帧头，出错了
                                    CatLogger.e(TAG, "wrong frame header");

                                    mFrameBuffer.clear();
                                    break;
                                }
                            } else {
                                // 不够帧头
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }

                ins.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            CatLogger.i(TAG, "receive thread stopped");
        }
    }

    private void processReceivedMsg(Proxy.ProxyMsg proxyMsg) {
        int msgId = proxyMsg.getMsgId();
        int connId = proxyMsg.getConnId();
        int ackId = proxyMsg.getAckId();
        Proxy.MsgType msgType = proxyMsg.getMsgType();

        CatLogger.d(TAG, "processReceivedMsg, msgId=%d, ackId=%d, connId=%d, msgType=%s, ip=%s, port=%d, " +
                        "arg1=%d, arg2=%d, arg3=%s, arg4_len=%d",
                msgId, ackId, connId, msgType.toString(), proxyMsg.getIp(), proxyMsg.getPort(),
                proxyMsg.getData().getArg1(), proxyMsg.getData().getArg2(), proxyMsg.getData().getArg3(),
                proxyMsg.getData().getArg4() == null ? 0 : proxyMsg.getData().getArg4().toByteArray().length);

        if (Proxy.MsgType.RESULT == msgType) {
            MsgResult msgResult = mMsgResultMap.get(ackId);
            if (msgResult != null) {
                msgResult.mRet = proxyMsg;

                msgResult.mLock.lock();
                msgResult.mCond.signal();
                msgResult.mLock.unlock();
            }
        } else if (Proxy.MsgType.ERROR == msgType) {
            NetConnection conn = mConnMap.get(connId);
            if (conn != null) {
                NetConnection.ConnectionListener listener = conn.getListener();
                if (listener != null) {
                    listener.onError(proxyMsg.getData().getArg1(), proxyMsg.getData().getArg3());
                }
            }

            MsgResult msgResult = mMsgResultMap.get(ackId);
            if (msgResult != null) {
                msgResult.mRet = proxyMsg;

                msgResult.mLock.lock();
                msgResult.mCond.signal();
                msgResult.mLock.unlock();
            }
        } else if (Proxy.MsgType.RECV == msgType) {
            NetConnection conn = mConnMap.get(connId);
            if (conn != null) {
                NetConnection.ConnectionListener listener = conn.getListener();
                if (listener != null) {
                    listener.onRecv(proxyMsg.getData().getArg4().toByteArray(),
                            proxyMsg.getIp(), proxyMsg.getPort());
                }
            }
        }
    }

    private FileOutputStream mFos;

    private synchronized int writeToUsb(byte[] data) {
        int offset = 0;

        while (offset != data.length) {
            int left = data.length - offset;
            int sendLen = Math.min(MAX_USB_FRAME_LEN, left);

            try {
                mFos.write(data, offset, sendLen);
                offset += sendLen;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

        return offset;
    }

    private void closeAllNetConn() {
        mMsgResultMap.clear();
        mConnMap.clear();

        // connId为-1，表示全局消息，并不是针对某条连接
        sendProxyMsg(-1, Proxy.ConnType.TCP, Proxy.MsgType.CLOSE_ALL,
                "", 0, 0, 0, null, null);
    }

    private boolean sendProxyMsg(Proxy.ProxyMsg proxyMsg) {
        byte[] frameData = proxyMsg.toByteArray();
        byte[] framePacket = new byte[FRAME_HEADER_LEN + frameData.length];

        System.arraycopy(FRAME_BEGIN.getBytes(), 0, framePacket, 0, FRAME_BEGIN.length());
        byte[] dataLenBytes = DataUtil.toBytes(frameData.length);
        System.arraycopy(dataLenBytes, 0, framePacket, FRAME_BEGIN.length(), dataLenBytes.length);
        System.arraycopy(frameData, 0, framePacket, FRAME_HEADER_LEN, frameData.length);

        int ret = writeToUsb(framePacket);
        CatLogger.d(TAG, "writeToUsb, ret=%d", ret);

        return ret == framePacket.length;
    }

    int sendProxyMsg(int connId, Proxy.ConnType connType, Proxy.MsgType msgType,
                     String ip, int port, int arg1, int arg2, String arg3, byte[] arg4) {
        if (!mIsConnected) {
            return ErrorCode.ERROR_USB_DISCONNECTED;
        }

        int msgId = getMsgId();
        int ackId = -1;

        CatLogger.d(TAG, "sendProxyMsg, msgId=%d, ackId=%d, connId=%d, msgType=%s, ip=%s, port=%d, " +
                        "arg1=%d, arg2=%d, arg3=%s, arg4_len=%d",
                msgId, ackId, connId, msgType.toString(), ip, port,
                arg1, arg2, arg3 == null ? "" : arg3, arg4 == null ? 0 : arg4.length);

        Proxy.MsgData.Builder msgDataBuilder = Proxy.MsgData.newBuilder();
        msgDataBuilder.setArg1(arg1).setArg2(arg2).setArg3(arg3 == null ? "" : arg3);
        if (arg4 != null) {
            msgDataBuilder.setArg4(ByteString.copyFrom(arg4));
        }

        Proxy.MsgData msgData = msgDataBuilder.build();

                Proxy.ProxyMsg.Builder builder = Proxy.ProxyMsg.newBuilder();
        builder.setMsgId(msgId)
                .setAckId(ackId)
                .setConnId(connId)
                .setConnType(connType)
                .setMsgType(msgType)
                .setIp(ip)
                .setPort(port)
                .setData(msgData);
        Proxy.ProxyMsg proxyMsg = builder.build();
        boolean isSent = sendProxyMsg(proxyMsg);
        int ret = 0;

        if (isSent) {
            if (Proxy.MsgType.CLOSE_ALL != msgType) {
                ret = syncGetResult(msgId);
            }
        } else {
            ret = ErrorCode.ERROR_USB_SEND;
        }

        if (Proxy.MsgType.CLOSE == msgType) {
            mConnMap.remove(connId);
        }

        CatLogger.d(TAG, "sendProxyMsg, ret=%d", ret);

        return ret;
    }

    private int syncGetResult(int msgId) {
        int ret;
        MsgResult msgResult = new MsgResult();
        msgResult.mMsgId = msgId;
        msgResult.mLock = new ReentrantLock();
        msgResult.mCond = msgResult.mLock.newCondition();
        mMsgResultMap.put(msgId, msgResult);

        msgResult.mLock.lock();

        try {
            // 等待结果返回
            msgResult.mCond.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            msgResult.mLock.unlock();
        }

        // 从map中去掉
        mMsgResultMap.remove(msgId);

        if (msgResult.mRet != null) {
            if (msgResult.mRet.getMsgType() != Proxy.MsgType.RESULT) {
                ret = ErrorCode.ERROR_FAILED;
            } else {
                ret = msgResult.mRet.getData().getArg1();
            }
        } else {
            ret = ErrorCode.ERROR_FAILED;
        }
        return ret;
    }

    private int getConnId() {
        synchronized (mIdSynObj) {
            int id = sCONNECTION_ID;
            sCONNECTION_ID = (sCONNECTION_ID + 1) % Integer.MAX_VALUE;

            return id;
        }
    }

    private int getMsgId() {
        synchronized (mIdSynObj) {
            int id = sMSG_ID;
            sMSG_ID = (sMSG_ID + 1) % Integer.MAX_VALUE;

            return id;
        }
    }

    private class MsgResult {
        public int mMsgId;
        public ReentrantLock mLock;
        public Condition mCond;
        public Proxy.ProxyMsg mRet;
    }

    private ConcurrentHashMap<Integer, MsgResult> mMsgResultMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, NetConnection> mConnMap = new ConcurrentHashMap<>();

    public static final int CONN_TYPE_TCP = 1;
    public static final int CONN_TYPE_UDP = 2;

    public NetConnection createNetConn(int connType, int port) {
        if (!mIsConnected) {
            return null;
        }

        NetConnection conn = null;

        int connId = getConnId();
        int ret = 0;

        switch (connType) {
            case CONN_TYPE_TCP: {
                ret = sendProxyMsg(connId, Proxy.ConnType.TCP, Proxy.MsgType.CREATE, "", port,
                        0, 0, null, null);
                if (ErrorCode.SUCCESS == ret) {
                    conn = new TcpConnection(this, connId);
                }
            } break;

            case CONN_TYPE_UDP: {
                ret = sendProxyMsg(connId, Proxy.ConnType.UDP, Proxy.MsgType.CREATE, "", port,
                        0, 0, null, null);
                if (ErrorCode.SUCCESS == ret) {
                    conn = new UdpConnection(this, connId);
                }
            } break;
        }

        // 将连接存入Map
        if (conn != null) {
            mConnMap.put(connId, conn);
        }

        return conn;
    }
}
