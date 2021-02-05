package com.example.hc_06bluetooth;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private int STORAGE_PERMISSION_CODE = 1;
    private TextView mOutput;
    private Button mListPairedDevicesBtn;
    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;
    private Button sendDevice;
    //private SeekBar mStartsb,mEndsb,mTimesb;
    private DrawView pic;
    private EditText endtext,starttext,timetext;
    //private EditText startvalue,endvalue,timevalue;
    private Button mStopbt;
    private Handler mHandler;
    private ConnectedThread mConnectedThread;
    private BluetoothSocket mBTSocket = null;
    private ImageButton msavebt,mclearbt;

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1;
    // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2;
    // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3;
    // used in bluetooth handler to identify message status
    private  String _recieveData = "";
    private OutputStream os;
    private int thetime=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        mOutput = (TextView)findViewById(R.id.output);
        mOutput.setMovementMethod(ScrollingMovementMethod.getInstance());
        mListPairedDevicesBtn = (Button)findViewById(R.id.PairedBtn);
        sendDevice = (Button)findViewById(R.id.send);
        pic=(DrawView)findViewById(R.id.pic);
        endtext=(EditText)findViewById(R.id.endtext);
        starttext=(EditText)findViewById(R.id.starttext);
        timetext=(EditText)findViewById(R.id.time);


        mStopbt=(Button)findViewById(R.id.stopbt);
        msavebt=(ImageButton)findViewById(R.id.imageButton);
        mclearbt=(ImageButton)findViewById(R.id.imageButton2);

        mBTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        pic.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                starttext.setText(""+(int)pic.startangle);
                endtext.setText(""+(180-(int)pic.endangle));
                return false;
            }
        });

        starttext.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int i, KeyEvent keyEvent) {
                String a = starttext.getText().toString();
                int b = Integer.parseInt(a);
                if(littlecheck(b,pic.endangle)==true) {
                    pic.startangle = b;
                    pic.caltheangle(0);
                    pic.invalidate();
                }
                else {
                    Toast.makeText(getApplicationContext(),"角度為0-180，且end必須大於start!",Toast.LENGTH_LONG).show();
                }
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                starttext.clearFocus();
                return false;
            }
        });

        endtext.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int i, KeyEvent keyEvent) {
                String a = endtext.getText().toString();
                int b = 180-Integer.parseInt(a);
                if(littlecheck(pic.startangle,b)==true) {
                    pic.endangle =b;
                    pic.caltheangle(1);
                    pic.invalidate();
                }
                else {
                    Toast.makeText(getApplicationContext(),"角度為0-180，且end必須大於start!",Toast.LENGTH_LONG).show();
                }
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                endtext.clearFocus();
                return true;
            }
        });


        timetext.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int i, KeyEvent keyEvent) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                timetext.clearFocus();
                return false;
            }
        });

        mStopbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                write("stop","","");
            }
        });

        msavebt.setOnClickListener(saveClickListener);

        mclearbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOutput.setText("");
            }
        });


        if (mBTArrayAdapter == null) {
            mOutput.append("Status: Bluetooth not found"+"\n");
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        else {
            sendDevice.setOnClickListener(new View.OnClickListener(){//當按下send開始傳輸資料
                @Override
                public void onClick(View v){
                    _recieveData = ""; //清除上次收到的資料
                    thesend(v);
                }
            });
            mListPairedDevicesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
                    listPairedDevices(v);
                }
            });
        }
        mHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                Calendar mCal = Calendar.getInstance();
                CharSequence s = DateFormat.format("yyyy-MM-dd kk:mm:ss.sss", mCal.getTime());
                String g = (String) s;
                if(msg.what == MESSAGE_READ){ //收到MESSAGE_READ 開始接收資料
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");

                        String[] a=readMessage.split("\\n");
                        String b="";
                        for(int i=0; i<=(a.length-2);i++){
                            b+=g+": ";
                            b+=a[i];
                            b+="\n";
                        }
                        readMessage=b;
                        //readMessage =  readMessage.substring(0,1);
                        //取得傳過來字串的第一個字元，其餘為雜訊
                        _recieveData += readMessage; //拼湊每次收到的字元成字串
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    mOutput.append(readMessage); //將收到的字串呈現在畫面上

                }

                if(msg.what == CONNECTING_STATUS){
                    //收到CONNECTING_STATUS 顯示以下訊息
                    if(msg.arg1 == 1)
                        mOutput.append(g+": Connected to Device: " + (String)(msg.obj)+"\n");
                    else
                        mOutput.append(g+": Connection Failed"+"\n");
                }
            }
        };
    }

    private void thesend(View v)
    {
        String startv,endv,timev;
        startv=starttext.getText().toString();
        endv=endtext.getText().toString();
        float a=10*Float.valueOf(timetext.getText().toString());
        timev=Integer.toString((int)a);
        if(check(startv,endv,timev)==true)
        {
            startv = "!" + change(startv);
            endv = change(endv);
            timev = change(timev);
            write(startv, endv, timev);
        }
        else
        {
            Toast.makeText(this, "時間不可為0，且需小於100", Toast.LENGTH_LONG).show();
        }
    }

    private boolean check(String q,String w,String e)
    {
        int a= Integer.valueOf(q).intValue();
        int b= Integer.valueOf(w).intValue();
        int c= Integer.valueOf(e).intValue();
        if(a>b||b==0||c==0)
            return false;
        else if (c>999)
            return false;
        else
            return true;
    }

    private String change(String s)
    {
        int a= Integer.valueOf(s).intValue();
        if(a<100)
        {
            if(a<10)
                s="00"+s;
            else if(a==0)
                s="000";
            else
                s="0"+s;
        }
        return s;
    }

    private void write(String a,String b,String c)
    {
        String info=a+b+c+"\n\r";

        byte[] bytes = new byte[0];
        try {
            bytes = info.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if(mConnectedThread != null) //First check to make sure thread created
        {
            try {
                mBTSocket.getOutputStream().write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    //定義當按下跳出是否開啟藍芽視窗後要做的內容
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        super.onActivityResult(requestCode, resultCode, Data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                mOutput.setText("Enabled");
            } else
                mOutput.setText("Disabled");
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private void listPairedDevices(View view){
        mPairedDevices = mBTAdapter.getBondedDevices();
        Context a = null;
        if(mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose any item");

        builder.setAdapter(mBTArrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if(!mBTAdapter.isEnabled()) {
                    Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                    return;
                }
                Calendar mCal = Calendar.getInstance();
                CharSequence s = DateFormat.format("yyyy-MM-dd kk:mm:ss.sss", mCal.getTime());
                String g = (String) s;
                mOutput.append(g+": Connecting..."+"\n");
                // Get the device MAC address, which is the last 17 chars in the View
                String info = mBTArrayAdapter.getItem(which);
                final String address = info.substring(info.length() - 17);
                final String name = info.substring(0,info.length() - 17);

                // Spawn a new thread to avoid blocking the GUI one
                new Thread()
                {
                    public void run() {
                        boolean fail = false;
                        //取得裝置MAC找到連接的藍芽裝置
                        BluetoothDevice device = mBTAdapter.getRemoteDevice(address);
                        try {
                            mBTSocket = createBluetoothSocket(device);
                            //建立藍芽socket
                        } catch (IOException e) {
                            fail = true;
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                        // Establish the Bluetooth socket connection.
                        try {
                            mBTSocket.connect(); //建立藍芽連線
                        } catch (IOException e) {
                            try {
                                fail = true;
                                mBTSocket.close(); //關閉socket
                                //開啟執行緒 顯示訊息
                                mHandler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                            } catch (IOException e2) {
                                //insert code to deal with this
                                Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                        if(fail == false) {
                            //開啟執行緒用於傳輸及接收資料
                            mConnectedThread = new ConnectedThread(mBTSocket);
                            mConnectedThread.start();
                            //開啟新執行緒顯示連接裝置名稱
                            mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name).sendToTarget();
                        }
                    }
                }.start();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }

    private class ConnectedThread extends Thread {
        private BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            try {
                mmInStream = socket.getInputStream();
                mmOutStream = socket.getOutputStream();
            } catch (IOException e) { }

        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if(bytes != 0) {
                        SystemClock.sleep(100);
                        //pause and wait for rest of data
                        bytes = mmInStream.available();
                        // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes);
                        // record how many bytes we actually read
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }

        public void write(byte[] data) throws IOException {
            mBTSocket.getOutputStream().write(data);
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


    private ImageButton.OnClickListener saveClickListener = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View arg0) {

            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermission();
            }

            Calendar mCal = Calendar.getInstance();
            CharSequence s = DateFormat.format("yyyy-MM-dd kk:mm:ss.sss", mCal.getTime());
            String a = (String) s;
            String filename = a+".txt";
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, filename);
            try
            {
                FileOutputStream Output = new FileOutputStream(file, true);
                String string =  mOutput.getText().toString();
                byte[] bytes = new byte[0];
                bytes = string.getBytes();
                Output.write(bytes);
                Output.close();
                if (string=="")
                    Toast.makeText(getApplicationContext(),"Output是空的",Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(getApplicationContext(),"以儲存到Download資料夾!",Toast.LENGTH_LONG).show();
            }
            catch (IOException e) { e.printStackTrace(); }
        }
    };

    public void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE))
        {
            new AlertDialog.Builder(this).setTitle("權限需求").setMessage("需要打開存儲權限!")
                    .setPositiveButton("好", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE)  {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),"權限已允許，請再次點擊save鍵",Toast.LENGTH_SHORT).show();
            }
        }
    }

    public boolean littlecheck(int start,int end)
    {
        if(start<0||end<0||180-end-start<5)
            return false;
        else
            return true;
    }
}

//圖案拖拉(ondrage)移動、放大縮小
//試著改動textview的數字
//試著做扇形加手把
//(開新專題)