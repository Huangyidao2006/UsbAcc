package com.dmai.usbacc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";

    private TextView mResultTxt;
    private Button mScanBtn;
    private Button mClearBtn;
    private Button mConnectBtn;
    private Button mDisconnectBtn;
    private EditText mContentEdt;
    private Button mSendBtn;

    private Toast mToast;

    private UsbManager mUsbManager;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    showTip("授权成功");
                } else {
                    showTip("未授权");
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
                UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                printResult("acc_attached", accToString(accessory), false);
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                printResult("acc_detached", accToString(accessory), false);

                disconnect();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsbManager = (UsbManager) getSystemService(USB_SERVICE);

        initUI();

        registerReceivers();
    }

    private void initUI() {
        mResultTxt = (TextView) findViewById(R.id.txt_result);
        mScanBtn = (Button) findViewById(R.id.btn_scan);
        mClearBtn = (Button) findViewById(R.id.btn_clear);
        mConnectBtn = (Button) findViewById(R.id.btn_connect);
        mDisconnectBtn = (Button) findViewById(R.id.btn_disconnect);
        mContentEdt = (EditText) findViewById(R.id.edt_content);
        mSendBtn = (Button) findViewById(R.id.btn_send);

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        mResultTxt.setMovementMethod(ScrollingMovementMethod.getInstance());

        mScanBtn.setOnClickListener(this);
        mClearBtn.setOnClickListener(this);
        mConnectBtn.setOnClickListener(this);
        mDisconnectBtn.setOnClickListener(this);
        mSendBtn.setOnClickListener(this);
    }

    private String accToString(UsbAccessory acc) {
        StringBuilder sb = new StringBuilder();
        sb.append("\tManu=").append(acc.getManufacturer()).append("\n")
            .append("\tDes=").append(acc.getDescription()).append("\n")
            .append("\tModel=").append(acc.getModel()).append("\n")
            .append("\tSerial=").append(acc.getSerial()).append("\n")
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

    private PendingIntent mPermissionIntent;

    private void registerReceivers() {
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mReceiver, filter);
    }

    private void unregisterReceivers() {
        unregisterReceiver(mReceiver);
    }

    private void requestPermission(UsbAccessory acc) {
        mUsbManager.requestPermission(acc, mPermissionIntent);
    }

    private void scan() {
        UsbAccessory[] accArray = mUsbManager.getAccessoryList();
        if (accArray == null) {
            showTip("未检测到设备");

            printResult("scan", "none", true);
        } else {
            StringBuilder sb = new StringBuilder();
            for (UsbAccessory acc : accArray) {
                if (mUsbManager.hasPermission(acc)) {
                    sb.append(accToString(acc)).append("\n");
                } else {
                    requestPermission(acc);

                    return;
                }
            }

            printResult("scan", sb.toString(), false);
        }
    }

    private void clear() {
        mResultTxt.setText("");
    }

    private ParcelFileDescriptor mFileDescriptor;
    private HandlerThread mSendThread;
    private SendHandler mSendHandler;
    private RecvThread mRecvThread;

    private boolean mIsConnected = false;

    private void connect() {
        if (mIsConnected) {
            showTip("不要重复连接");
            return;
        }

        UsbAccessory[] accArray = mUsbManager.getAccessoryList();
        if (accArray == null) {
            showTip("未检测到设备");

            printResult("scan", "none", true);
        } else {
            mFileDescriptor = mUsbManager.openAccessory(accArray[0]);

            mSendThread = new HandlerThread("sender");
            mSendThread.start();
            mSendHandler = new SendHandler(mSendThread.getLooper());

            mRecvThread = new RecvThread();
            mRecvThread.start();

            showTip("连接成功");

            printResult("connect", "success", true);

            mIsConnected = true;
        }
    }

    private void disconnect() {
        if (!mIsConnected) {
            return;
        }

        mSendHandler.removeMessages(MSG_SEND);
        mSendHandler.sendEmptyMessage(MSG_CLOSE);
        mSendThread.quitSafely();
        mRecvThread.stopRun();

        if (mFileDescriptor != null) {
            try {
                mFileDescriptor.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mIsConnected = false;

        showTip("断开连接");

        printResult("disconnect", "success", true);
    }

    private void send() {
        if (!mIsConnected) {
            showTip("未连接");
            return;
        }

        String content = mContentEdt.getText().toString();
        if (TextUtils.isEmpty(content)) {
            content = "empty";
        }

        Message.obtain(mSendHandler, MSG_SEND, content).sendToTarget();

        printResult("send", content, true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan: {
                scan();
            } break;

            case R.id.btn_clear: {
                clear();
            } break;

            case R.id.btn_connect: {
                connect();
            } break;

            case R.id.btn_disconnect: {
                disconnect();
            } break;

            case R.id.btn_send: {
                send();
            } break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceivers();
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

    private static final int MSG_SEND = 1;
    private static final int MSG_CLOSE = 2;

    private class SendHandler extends Handler {
        private OutputStream mOs;

        public SendHandler(Looper looper) {
            super(looper);

            try {
                mOs = new FileOutputStream(mFileDescriptor.getFileDescriptor());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_SEND: {
                    String content = (String) msg.obj;
                    byte[] data = content.getBytes();

                    try {
                        mOs.write(data, 0, data.length);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } break;

                case MSG_CLOSE: {
                    if (mOs != null) {
                        try {
                            mOs.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } break;
            }
        }
    }

    private class RecvThread extends Thread {
        private boolean mStop = false;

        public void stopRun() {
            mStop = true;
        }

        @Override
        public void run() {
            try {
                InputStream ins = new FileInputStream(mFileDescriptor.getFileDescriptor());
                byte[] buffer = new byte[16384];

                while (!mStop) {
                    int len = ins.read(buffer);
                    Log.d("usbacc", "recv, len=" + len);

                    if (len > 0) {
                        final String content = new String(buffer, 0, len);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                printResult("recv", content, true);
                            }
                        });
                    }
                }

                ins.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}