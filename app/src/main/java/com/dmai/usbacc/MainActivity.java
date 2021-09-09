package com.dmai.usbacc;

import androidx.appcompat.app.AppCompatActivity;

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
import com.hj.data_proxy.UsbDataProxy;
import com.hj.data_proxy.log.CatLogger;
import com.hj.data_proxy.log.LogLevel;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "ProxyDemo";
    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";

    private TextView mResultTxt;
    private Button mCreateProxyBtn;
    private Button mDestroyProxyBtn;
    private Button mConnectProxyBtn;
    private Button mDisconnProxyBtn;
    private Button mCreateTcpBtn;
    private Button mConnectTcpBtn;
    private Button mSendTcpBtn;
    private Button mCloseTcpBtn;
    private EditText mLocalPortTcpEdt;
    private EditText mServerSocketTcpEdt;
    private EditText mContentTcpEdt;

    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();

        CatLogger.getInstance().setPrintLogLevel(LogLevel.DEBUG);
    }

    private void initUI() {
        mResultTxt = (TextView) findViewById(R.id.txt_result);
        mCreateProxyBtn = (Button) findViewById(R.id.btn_createProxy);
        mDestroyProxyBtn = (Button) findViewById(R.id.btn_destroyProxy);
        mConnectProxyBtn = (Button) findViewById(R.id.btn_connectProxy);
        mDisconnProxyBtn = (Button) findViewById(R.id.btn_disconnProxy);
        mCreateTcpBtn = (Button) findViewById(R.id.btn_createTcp);
        mConnectTcpBtn = (Button) findViewById(R.id.btn_connectTcp);
        mSendTcpBtn = (Button) findViewById(R.id.btn_sendTcp);
        mCloseTcpBtn = (Button) findViewById(R.id.btn_closeTcp);
        mLocalPortTcpEdt = (EditText) findViewById(R.id.edt_portTcp);
        mServerSocketTcpEdt = (EditText) findViewById(R.id.edt_server_socketTcp);
        mContentTcpEdt = (EditText) findViewById(R.id.edt_contentTcp);

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        mResultTxt.setMovementMethod(ScrollingMovementMethod.getInstance());

        mCreateProxyBtn.setOnClickListener(this);
        mDestroyProxyBtn.setOnClickListener(this);
        mConnectProxyBtn.setOnClickListener(this);
        mDisconnProxyBtn.setOnClickListener(this);
        mCreateTcpBtn.setOnClickListener(this);
        mConnectTcpBtn.setOnClickListener(this);
        mSendTcpBtn.setOnClickListener(this);
        mCloseTcpBtn.setOnClickListener(this);
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

            case R.id.btn_connectTcp: {
                connectTcp();
            } break;

            case R.id.btn_sendTcp: {
                sendTcp();
            } break;

            case R.id.btn_closeTcp: {
                closeTcp();
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

    private int getLocalTcpPort() {
        String port = mLocalPortTcpEdt.getText().toString();

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

        int localPort = getLocalTcpPort();
        mTcpConn = (TcpConnection) mUsbDataProxy.createNetConn(UsbDataProxy.CONN_TYPE_TCP, localPort);

        if (mTcpConn == null) {
            showTip("创建失败");
            return;
        }

        mTcpConn.setListener(mTcpListener);

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

        String content = mContentTcpEdt.getText().toString();

        int ret = mTcpConn.send(content.getBytes());
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

    private TcpConnection mTcpConn;

    private NetConnection.ConnectionListener mTcpListener = new NetConnection.ConnectionListener() {
        @Override
        public void onRecv(byte[] data, String ip, int port) {
            final String s = "fromIP=" + ip + ", fromPort=" + port + ", dataLen=" + data.length;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printResult("recv", s, true);
                }
            });
        }

        @Override
        public void onError(int error, String des) {
            final String s = "error=" + error + ", des=" + des;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printResult("error", s, true);
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