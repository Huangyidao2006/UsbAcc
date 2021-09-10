package com.dmai.usbacc;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.hardware.usb.UsbAccessory;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hj.data_proxy.ErrorCode;
import com.hj.data_proxy.NetConnection;
import com.hj.data_proxy.TcpConnection;
import com.hj.data_proxy.UdpConnection;
import com.hj.data_proxy.UsbDataProxy;
import com.hj.data_proxy.log.CatLogger;
import com.hj.data_proxy.log.LogLevel;
import com.hj.data_proxy.utils.DataUtil;
import com.hj.data_proxy.utils.PermissionUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "ProxyDemo";
    private static final int REQUEST_CODE_PERMISSION = 123;

    private TextView mResultTxt;
    private Button mCreateProxyBtn;
    private Button mDestroyProxyBtn;
    private Button mConnectProxyBtn;
    private Button mDisconnProxyBtn;
    private Button mCreateTcpBtn;
    private Button mCreateUdpBtn;
    private Button mConnectTcpBtn;
    private Button mSendTcpBtn;
    private Button mSendUdpBtn;
    private Button mCloseTcpBtn;
    private Button mCloseUdpBtn;
    private EditText mLocalPortEdt;
    private EditText mServerSocketTcpEdt;
    private EditText mSendContentEdt;

    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();

        CatLogger.getInstance().setPrintLogLevel(LogLevel.DEBUG);

        List<String> deniedPerms = new ArrayList<>();
        if (!hasPermissions(deniedPerms)) {
            requestPermissions(deniedPerms);
        }
    }

    private boolean hasPermissions(List<String> deniedPerms) {
        ArrayList<String> permsArray = new ArrayList<>();
        permsArray.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permsArray.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        return PermissionUtil.hasPermissions(this, permsArray, deniedPerms);
    }

    private void requestPermissions(List<String> perms) {
        PermissionUtil.requestPermissions(this, perms, REQUEST_CODE_PERMISSION);
    }

    private void initUI() {
        mResultTxt = (TextView) findViewById(R.id.txt_result);
        mCreateProxyBtn = (Button) findViewById(R.id.btn_createProxy);
        mDestroyProxyBtn = (Button) findViewById(R.id.btn_destroyProxy);
        mConnectProxyBtn = (Button) findViewById(R.id.btn_connectProxy);
        mDisconnProxyBtn = (Button) findViewById(R.id.btn_disconnProxy);
        mCreateTcpBtn = (Button) findViewById(R.id.btn_createTcp);
        mCreateUdpBtn = (Button) findViewById(R.id.btn_createUdp);
        mConnectTcpBtn = (Button) findViewById(R.id.btn_connectTcp);
        mSendTcpBtn = (Button) findViewById(R.id.btn_sendTcp);
        mSendUdpBtn = (Button) findViewById(R.id.btn_sendUdp);
        mCloseTcpBtn = (Button) findViewById(R.id.btn_closeTcp);
        mCloseUdpBtn = (Button) findViewById(R.id.btn_closeUdp);
        mLocalPortEdt = (EditText) findViewById(R.id.edt_localPort);
        mServerSocketTcpEdt = (EditText) findViewById(R.id.edt_server_socketTcp);
        mSendContentEdt = (EditText) findViewById(R.id.edt_sendContent);

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        mResultTxt.setMovementMethod(ScrollingMovementMethod.getInstance());

        mCreateProxyBtn.setOnClickListener(this);
        mDestroyProxyBtn.setOnClickListener(this);
        mConnectProxyBtn.setOnClickListener(this);
        mDisconnProxyBtn.setOnClickListener(this);
        mCreateTcpBtn.setOnClickListener(this);
        mCreateUdpBtn.setOnClickListener(this);
        mConnectTcpBtn.setOnClickListener(this);
        mSendTcpBtn.setOnClickListener(this);
        mSendUdpBtn.setOnClickListener(this);
        mCloseTcpBtn.setOnClickListener(this);
        mCloseUdpBtn.setOnClickListener(this);
    }

    private String accToString(UsbAccessory acc) {
        StringBuilder sb = new StringBuilder();
        sb.append("\tManu=").append(acc.getManufacturer()).append("\n")
            .append("\tDes=").append(acc.getDescription()).append("\n")
            .append("\tModel=").append(acc.getModel()).append("\n")
            .append("\tUri=").append(acc.getUri()).append("\n");

        return sb.toString();
    }

    private void printResult(String op, String result, boolean indent) {
        if (mResultTxt.getLineCount() > 100) {
            clear();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(op + ":\n").append((indent ? "\t" : "") + result).append("\n");

        mResultTxt.append(sb.toString());
        int scrollAmount = mResultTxt.getLayout().getLineTop(mResultTxt.getLineCount())
                - mResultTxt.getHeight();
        if (scrollAmount > 0)
            mResultTxt.scrollTo(0, scrollAmount);
        else
            mResultTxt.scrollTo(0, 0);
    }

    private void clear() {
        mResultTxt.setText("");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_createProxy: {
                createProxy();
            } break;

            case R.id.btn_destroyProxy: {
                destroyProxy();
            } break;

            case R.id.btn_connectProxy: {
                connectProxy();
            } break;

            case R.id.btn_disconnProxy: {
                disconnectProxy();
            } break;

            case R.id.btn_createTcp: {
                createTcp();
            } break;

            case R.id.btn_createUdp: {
                createUdp();
            } break;

            case R.id.btn_connectTcp: {
                connectTcp();
            } break;

            case R.id.btn_sendTcp: {
                sendTcp();
            } break;

            case R.id.btn_sendUdp: {
                sendUdp();
            } break;

            case R.id.btn_closeTcp: {
                closeTcp();
            } break;

            case R.id.btn_closeUdp: {
                closeUdp();
            } break;
        }
    }

    private void createProxy() {
        if (mUsbDataProxy != null) {
            showTip("不要重复创建");
            return;
        }

        mUsbDataProxy = UsbDataProxy.createInstance(this, mUsbListener);
        if (mUsbDataProxy == null) {
            showTip("创建出错");
        }

        showTip("创建成功");
    }

    private void destroyProxy() {
        if (mUsbDataProxy == null) {
            showTip("Proxy为空");
            return;
        }

        mUsbDataProxy.destroy();
        mUsbDataProxy = null;

        showTip("销毁成功");
    }

    private void connectProxy() {
        if (mUsbDataProxy == null) {
            showTip("Proxy为空");
            return;
        }

        int ret = mUsbDataProxy.connect();
        if (ErrorCode.SUCCESS != ret) {
            showTip("error=" + ret);
        }
    }

    private void disconnectProxy() {
        if (mUsbDataProxy == null) {
            showTip("Proxy为空");
            return;
        }

        mUsbDataProxy.disconnect();
    }

    private int getLocalPort() {
        String port = mLocalPortEdt.getText().toString();

        return Integer.parseInt(port);
    }

    private void createTcp() {
        if (mUsbDataProxy == null) {
            showTip("Proxy为空");
            return;
        }


        if (mTcpConn != null) {
            showTip("不要重复创建");
            return;
        }

        int localPort = getLocalPort();
        mTcpConn = (TcpConnection) mUsbDataProxy.createNetConn(UsbDataProxy.CONN_TYPE_TCP, localPort);

        if (mTcpConn == null) {
            showTip("创建失败");
            return;
        }

        mTcpConn.setListener(mTcpListener);

        showTip("创建成功");
    }

    private void createUdp() {
        if (mUsbDataProxy == null) {
            showTip("Proxy为空");
            return;
        }


        if (mUdpConn != null) {
            showTip("不要重复创建");
            return;
        }

        int localPort = getLocalPort();
        mUdpConn = (UdpConnection) mUsbDataProxy.createNetConn(UsbDataProxy.CONN_TYPE_UDP, localPort);

        if (mUdpConn == null) {
            showTip("创建失败");
            return;
        }

        mUdpConn.setListener(mUdpListener);

        showTip("创建成功");
    }

    private void connectTcp() {
        if (mUsbDataProxy == null) {
            showTip("Proxy为空");
            return;
        }

        if (mTcpConn == null) {
            showTip("TcpConn为空");
            return;
        }

        String socketStr = mServerSocketTcpEdt.getText().toString();
        String[] parts = socketStr.split(":");

        if (parts.length != 2) {
            showTip("socket无效");
            return;
        }

        String serverIP = parts[0];
        int serverPort = Integer.parseInt(parts[1]);

        CatLogger.d(TAG, "try to connect %s:%d", serverIP, serverPort);

        int ret = mTcpConn.connect(serverIP, serverPort);
        if (ErrorCode.SUCCESS != ret) {
            showTip("error=" + ret);
            return;
        }

        showTip("Tcp连接成功");
    }

    private void sendTcp() {
        if (mTcpConn == null) {
            showTip("TcpConn为空");
            return;
        }

        String content = mSendContentEdt.getText().toString();

        int ret = mTcpConn.send(content.getBytes());
        if (ErrorCode.SUCCESS != ret) {
            showTip("error=" + ret);
        }
    }

    private void sendUdp() {
        if (mUdpConn == null) {
            showTip("TcpConn为空");
            return;
        }

        String content = mSendContentEdt.getText().toString();
        String socketStr = mServerSocketTcpEdt.getText().toString();
        String[] parts = socketStr.split(":");

        if (parts.length != 2) {
            showTip("socket无效");
            return;
        }

        String serverIP = parts[0];
        int serverPort = Integer.parseInt(parts[1]);

        int ret = mUdpConn.sendTo(content.getBytes(), serverIP, serverPort);
        if (ErrorCode.SUCCESS != ret) {
            showTip("error=" + ret);
        }
    }

    private void closeTcp() {
        if (mTcpConn == null) {
            showTip("TcpConn为空");
            return;
        }

        mTcpConn.close();
        mTcpConn = null;
    }

    private void closeUdp() {
        if (mUdpConn == null) {
            showTip("TcpConn为空");
            return;
        }

        mUdpConn.close();
        mUdpConn = null;
    }

    private UsbDataProxy mUsbDataProxy;

    private UsbDataProxy.UsbListener mUsbListener = new UsbDataProxy.UsbListener() {
        @Override
        public void onAccessoryAttached(UsbAccessory accessory) {
            printResult("attached", accToString(accessory), false);
        }

        @Override
        public void onAccessoryDetached(UsbAccessory accessory) {
            printResult("detached", accToString(accessory), false);
        }

        @Override
        public void onPermissionResult(UsbAccessory accessory, boolean isGranted) {
            printResult("permission", "isGranted=" + isGranted, true);
        }

        @Override
        public void onConnected(UsbAccessory accessory) {
            printResult("connected", accToString(accessory), false);
        }

        @Override
        public void onDisconnected() {
            printResult("disconnected", "", true);
        }
    };

    private FileOutputStream mTcpFos;

    private TcpConnection mTcpConn;

    private NetConnection.ConnectionListener mTcpListener = new NetConnection.ConnectionListener() {
        @Override
        public void onRecv(byte[] data, String ip, int port) {
//            final String s = "fromIP=" + ip + ", fromPort=" + port + ", dataLen=" + data.length;
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    printResult("recv", s, true);
//                }
//            });

            if (DataUtil.byteCompare("startofmusic".getBytes(), data, data.length)) {
                CatLogger.d(TAG, "startofmusic");

                final String s = "startofmusic fromIP=" + ip + ", fromPort=" + port + ", dataLen=" + data.length;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        printResult("Tcp, recv", s, true);
                    }
                });

                try {
                    mTcpFos = new FileOutputStream("/sdcard/usb_tcp_data.mp3");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else if (DataUtil.byteCompare("endofmusic".getBytes(), data, data.length)) {
                CatLogger.d(TAG, "endofmusic");

                final String s = "endofmusic fromIP=" + ip + ", fromPort=" + port + ", dataLen=" + data.length;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        printResult("Tcp, recv", s, true);
                    }
                });

                if (mTcpFos != null) {
                    try {
                        mTcpFos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (mTcpFos != null) {
                    try {
                        mTcpFos.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void onError(int error, String des) {
            final String s = "error=" + error + ", des=" + des;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printResult("Tcp, error", s, true);
                }
            });
        }
    };

    private FileOutputStream mUdpFos;

    private UdpConnection mUdpConn;

    private NetConnection.ConnectionListener mUdpListener = new NetConnection.ConnectionListener() {
        @Override
        public void onRecv(byte[] data, String ip, int port) {
            if (DataUtil.byteCompare("startofmusic".getBytes(), data, data.length)) {
                CatLogger.d(TAG, "startofmusic");

                final String s = "startofmusic fromIP=" + ip + ", fromPort=" + port + ", dataLen=" + data.length;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        printResult("Udp, recv", s, true);
                    }
                });

                try {
                    mUdpFos = new FileOutputStream("/sdcard/usb_udp_data.mp3");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else if (DataUtil.byteCompare("endofmusic".getBytes(), data, data.length)) {
                CatLogger.d(TAG, "endofmusic");

                final String s = "endofmusic fromIP=" + ip + ", fromPort=" + port + ", dataLen=" + data.length;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        printResult("Udp, recv", s, true);
                    }
                });

                if (mUdpFos != null) {
                    try {
                        mUdpFos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (mUdpFos != null) {
                    try {
                        mUdpFos.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void onError(int error, String des) {
            final String s = "error=" + error + ", des=" + des;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printResult("Udp, error", s, true);
                }
            });
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mUsbDataProxy != null) {
            mUsbDataProxy.destroy();
        }
    }

    private void showTip(final String tip) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(tip);
                mToast.show();
            }
        });
    }

}
