package com.example.applicationdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.KeyListener;
import android.text.method.NumberKeyListener;
import android.text.method.TextKeyListener;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.bjw.bean.AssistBean;
import com.bjw.bean.ComBean;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android_serialport_api.SerialPortFinder;

public class MainActivity extends AppCompatActivity {
    EditText editTextRecDisp, editTextLines, editTextCOMA; //接受数据和发送数据的显示编辑文本框。
    EditText editTextTimeCOMA;      //循环发送时间编辑框。
    CheckBox checkBoxAutoClear, checkBoxAutoCOMA; //  清除文本，自动发送勾选框。
    Button ButtonClear, ButtonSendCOMA;   //  发送按钮
    ToggleButton toggleButtonCOMA;   //串口打开关闭选项
    Spinner SpinnerCOMA;     //   串口号设置下拉选项
    Spinner SpinnerBaudRateCOMA;    //波特率设置下拉选项
    RadioButton radioButtonTxt, radioButtonHex;   //选择数据接收格式的为hex或者txt，两个圆点按钮。
    SerialControl ComA;   //串口对象，包括了串口的一系列属性配置。
    DispQueueThread DispQueue;//
    SerialPortFinder mSerialPortFinder;//获取设备的串口信息。
    AssistBean AssistData;//  串口数据发送对象。
    int iRecLines = 0;//

    /**
     * Activity 启动后的主循环功能函数。
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ComA = new SerialControl();
        DispQueue = new DispQueueThread();   //新建线程对串口接受数据进行处理：
        DispQueue.start(); //启用线程：
        AssistData = getAssistData();
        setControls(); //这个

        setControls();
    }

    /**
     * 1.进行界面组件绑定；
     * 2.界面上相关文字数据显示；
     */
    private void setControls()//
    {
        String appName = getString(R.string.app_name);
        try {
            PackageInfo pinfo = getPackageManager().getPackageInfo("com.bjw.ComAssistant", PackageManager.GET_CONFIGURATIONS);
            String versionName = pinfo.versionName;
//			String versionCode = String.valueOf(pinfo.versionCode);
            setTitle(appName + " V" + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        editTextRecDisp = (EditText) findViewById(R.id.editTextRecDisp);
        editTextLines = (EditText) findViewById(R.id.editTextLines);
        editTextCOMA = (EditText) findViewById(R.id.editTextCOMA);

        editTextTimeCOMA = (EditText) findViewById(R.id.editTextTimeCOMA);


        checkBoxAutoClear = (CheckBox) findViewById(R.id.checkBoxAutoClear);
        checkBoxAutoCOMA = (CheckBox) findViewById(R.id.checkBoxAutoCOMA);

        ButtonClear = (Button) findViewById(R.id.ButtonClear);
        ButtonSendCOMA = (Button) findViewById(R.id.ButtonSendCOMA);

        toggleButtonCOMA = (ToggleButton) findViewById(R.id.toggleButtonCOMA);

        SpinnerCOMA = (Spinner) findViewById(R.id.SpinnerCOMA);

        SpinnerBaudRateCOMA = (Spinner) findViewById(R.id.SpinnerBaudRateCOMA);

        radioButtonTxt = (RadioButton) findViewById(R.id.radioButtonTxt);
        radioButtonHex = (RadioButton) findViewById(R.id.radioButtonHex);

        editTextCOMA.setOnEditorActionListener(new EditorActionEvent());

        editTextTimeCOMA.setOnEditorActionListener(new EditorActionEvent());

        editTextCOMA.setOnFocusChangeListener(new FocusChangeEvent());

        editTextTimeCOMA.setOnFocusChangeListener(new FocusChangeEvent());


        radioButtonTxt.setOnClickListener(new radioButtonClickEvent()); //绑定接受数据的转换类型函数。
        radioButtonHex.setOnClickListener(new radioButtonClickEvent());
        ButtonClear.setOnClickListener(new ButtonClickEvent());
        ButtonSendCOMA.setOnClickListener(new ButtonClickEvent());

        toggleButtonCOMA.setOnCheckedChangeListener(new ToggleButtonCheckedChangeEvent());

        checkBoxAutoCOMA.setOnCheckedChangeListener(new CheckBoxChangeEvent());


        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.baudrates_value, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        SpinnerBaudRateCOMA.setAdapter(adapter);

        SpinnerBaudRateCOMA.setSelection(12);


        //???SerialPortFinder  ???????????
        mSerialPortFinder = new SerialPortFinder();
        String[] entryValues = mSerialPortFinder.getAllDevicesPath();  //?entryValue?????????
        List<String> allDevices = new ArrayList<String>();
        for (int i = 0; i < entryValues.length; i++) {
            allDevices.add(entryValues[i]);
        }
        ArrayAdapter<String> aspnDevices = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, allDevices);
        aspnDevices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        SpinnerCOMA.setAdapter(aspnDevices);

        if (allDevices.size() > 0) {
            SpinnerCOMA.setSelection(6);
        }

        SpinnerCOMA.setOnItemSelectedListener(new ItemSelectedEvent());

        SpinnerBaudRateCOMA.setOnItemSelectedListener(new ItemSelectedEvent());

        DispAssistData(AssistData);
    }


    class EditorActionEvent implements EditText.OnEditorActionListener {
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (v == editTextCOMA) {
                setSendData(editTextCOMA);
            } else if (v == editTextTimeCOMA) {
                setDelayTime(editTextTimeCOMA);
            }
            return false;
        }
    }

    //----------------------------------------------------???????????
    class FocusChangeEvent implements EditText.OnFocusChangeListener {
        public void onFocusChange(View v, boolean hasFocus) {
            if (v == editTextCOMA) {
                setSendData(editTextCOMA);
            } else if (v == editTextTimeCOMA) {
                setDelayTime(editTextTimeCOMA);
            }
        }
    }


    /**
     * @return
     */
    private AssistBean getAssistData() {
        SharedPreferences msharedPreferences = getSharedPreferences("ComAssistant", Context.MODE_PRIVATE);
        AssistBean AssistData = new AssistBean();
        try {
            String personBase64 = msharedPreferences.getString("AssistData", "");
            byte[] base64Bytes = Base64.decode(personBase64.getBytes(), 0);
            ByteArrayInputStream bais = new ByteArrayInputStream(base64Bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            AssistData = (AssistBean) ois.readObject();
            return AssistData;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return AssistData;
    }

    /**
     *
     * @param AssistData
     */
    private void DispAssistData(AssistBean AssistData)
    {
        editTextCOMA.setText(AssistData.getSendA());

        setSendData(editTextCOMA);

        if (AssistData.isTxt())
        {
            radioButtonTxt.setChecked(true);
        } else
        {
            radioButtonHex.setChecked(true);
        }
        editTextTimeCOMA.setText(AssistData.sTimeA);

        setDelayTime(editTextTimeCOMA);
    }


    //串口数据接受处理线程类：
    private class DispQueueThread extends Thread {
        private Queue<ComBean> QueueList = new LinkedList<ComBean>();

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                final ComBean ComData;
                while ((ComData = QueueList.poll()) != null) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            DispRecData(ComData);
//							Log.d("msg",DispRecData(ComData));
                        }
                    });

                    try {
                        Thread.sleep(100);//???????????????????????????
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public synchronized void AddQueue(ComBean ComData) {
            QueueList.add(ComData);
        }
    }

    /**
     * ??这个应该是开始设置串口的相关属性
     * SerialHelper用来简化代码，其中包括打开监听线程 open();关闭线程释放函数 close();
     * 两个线程：1.发送线程SendThread()    2.接受线程ReadThread（）；
     */
    private class SerialControl extends SerialHelper {

        //		public SerialControl(String sPort, String sBaudRate){
//			super(sPort, sBaudRate);
//		}
        public SerialControl() {
        }

        @Override
        protected void onDataReceived(final ComBean ComRecData) {
            DispQueue.AddQueue(ComRecData);//

//			Log.d("abc",);
			/*
			runOnUiThread(new Runnable()//
			{
				public void run()
				{
					DispRecData(ComRecData);
				}
			});*/
        }
    }

    /**
     * 对接受到的数据，进行解析转换成String字符串。
     *
     * @param ComRecData
     */
    private void DispRecData(ComBean ComRecData) {
        StringBuilder sMsg = new StringBuilder();
        sMsg.append(ComRecData.sRecTime);
        sMsg.append("[");
        sMsg.append(ComRecData.sComPort);
        sMsg.append("]");
        if (radioButtonTxt.isChecked()) {
            sMsg.append("[Txt] ");
//			sMsg.append(new String((ComRecData.bRec)));
            try {
                String A = new String(ComRecData.bRec, "GBK");
                sMsg.append(A);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
//			sMsg.append(new String((ComRecData.bRec)));

            Log.d("msg", new String(ComRecData.bRec));

        } else if (radioButtonHex.isChecked()) {
            sMsg.append("[Hex] ");
            sMsg.append(MyFunc.ByteArrToHex(ComRecData.bRec));
        }
        sMsg.append("\r\n");
        editTextRecDisp.append(sMsg);
        iRecLines++;
        editTextLines.setText(String.valueOf(iRecLines));
        if ((iRecLines > 500) && (checkBoxAutoClear.isChecked()))//??500????????
        {
            editTextRecDisp.setText("");
            editTextLines.setText("0");
            iRecLines = 0;
        }
    }


    private void setSendData(TextView v) {
        if (v == editTextCOMA) {
            AssistData.setSendA(v.getText().toString());
            SetLoopData(ComA, v.getText().toString());

        }

    }

    /**
     * @param ComPort   串口对象：
     * @param sLoopData 循环发送数据对象：
     */
    private void SetLoopData(SerialHelper ComPort, String sLoopData) {
        if (radioButtonTxt.isChecked()) {
            ComPort.setTxtLoopData(sLoopData);
        } else if (radioButtonHex.isChecked()) {
            ComPort.setHexLoopData(sLoopData);
        }
    }

    /**
     * @param v
     */
    private void setDelayTime(TextView v) {
        if (v == editTextTimeCOMA) {
            AssistData.sTimeA = v.getText().toString();
            SetiDelayTime(ComA, v.getText().toString());
        }
    }

    private void SetiDelayTime(SerialHelper ComPort, String sTime) {
        ComPort.setiDelay(Integer.parseInt(sTime));
    }

    /**
     * 1.绑定按键的触发函数。
     */
    class radioButtonClickEvent implements RadioButton.OnClickListener {
        public void onClick(View v) {
            if (v == radioButtonTxt) {
                KeyListener TxtkeyListener = new TextKeyListener(TextKeyListener.Capitalize.NONE, false);
                editTextCOMA.setKeyListener(TxtkeyListener);

                AssistData.setTxtMode(true);
            } else if (v == radioButtonHex) {
                KeyListener HexkeyListener = new NumberKeyListener() {
                    public int getInputType() {
                        return InputType.TYPE_CLASS_TEXT;
                    }

                    @Override
                    protected char[] getAcceptedChars() {
                        return new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                                'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D', 'E', 'F'};
                    }
                };
                editTextCOMA.setKeyListener(HexkeyListener);

                AssistData.setTxtMode(false);
            }
            editTextCOMA.setText(AssistData.getSendA());

            setSendData(editTextCOMA);

        }
    }

    /**
     * 判断进行数据清理还是数据发送操作。
     * 1.清理接收到的数据。
     * 2.发送串口发送文本框中的数据。
     */
    class ButtonClickEvent implements View.OnClickListener {
        public void onClick(View v) {
            if (v == ButtonClear) {
                editTextRecDisp.setText("");
            } else if (v == ButtonSendCOMA) {
                sendPortData(ComA, editTextCOMA.getText().toString());
//				Log.d("BUttonCLick:",editTextCOMA.getText().toString());
            }
        }
    }

    /**
     *   1.数据发送函数
     * @param ComPort
     * @param sOut
     */
    private void sendPortData(SerialHelper ComPort,String sOut){
        if (ComPort!=null && ComPort.isOpen())
        {     String A;
            if (radioButtonTxt.isChecked())
            {
                ComPort.sendTxt(sOut);
//				Log.d("SendPortData",sOut);

            }else if (radioButtonHex.isChecked()) {
                ComPort.sendHex(sOut);
            }
        }
    }
    /**
     *
     */
    class ToggleButtonCheckedChangeEvent implements ToggleButton.OnCheckedChangeListener {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (buttonView == toggleButtonCOMA) {
                if (isChecked) {
//						ComA=new SerialControl("/dev/s3c2410_serial0", "9600");
                        ComA.setPort(SpinnerCOMA.getSelectedItem().toString());
                        Log.d("comA", "SpinnerCOMA.getSelectedItem().toString()");
                        ComA.setBaudRate(SpinnerBaudRateCOMA.getSelectedItem().toString());
                        OpenComPort(ComA);
                    }
                } else {
                    CloseComPort(ComA);
                    checkBoxAutoCOMA.setChecked(false);
                }
            }
        }

    /**
     *  串口开启方法：
     * @param ComPort  串口对象
     */
    private void OpenComPort(SerialHelper ComPort){
        try
        {
            ComPort.open();
        } catch (SecurityException e) {
            ShowMessage("SecurityException");
        } catch (IOException e) {
            ShowMessage("IOException!");
        } catch (InvalidParameterException e) {
            ShowMessage("InvalidParameterException");
        }
    }
    private void ShowMessage(String sMsg)
    {
        Toast.makeText(this, sMsg, Toast.LENGTH_SHORT).show();
    }

    /**
     *   串口关闭方法：
     * @param ComPort  窜口对象
     */
    private void CloseComPort(SerialHelper ComPort){
        if (ComPort!=null){
            ComPort.stopSend();
            ComPort.close();
        }
    }

    class CheckBoxChangeEvent implements CheckBox.OnCheckedChangeListener {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (buttonView == checkBoxAutoCOMA) {
                if (!toggleButtonCOMA.isChecked() && isChecked) {
                    buttonView.setChecked(false);
                    return;
                }
                SetLoopData(ComA, editTextCOMA.getText().toString());
                SetAutoSend(ComA, isChecked);
            }

        }
    }

    /**
     * 1.设置文本框数据自动发送。
     * @param ComPort     串口对象
     * @param isAutoSend   自动发送指令（true;false）
     */
    private void SetAutoSend(SerialHelper ComPort,boolean isAutoSend){
        if (isAutoSend)
        {
            ComPort.startSend();
        } else
        {
            ComPort.stopSend();
        }
    }


    class ItemSelectedEvent implements Spinner.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            if ((arg0 == SpinnerCOMA) || (arg0 == SpinnerBaudRateCOMA)) {
                CloseComPort(ComA);
                checkBoxAutoCOMA.setChecked(false);
                toggleButtonCOMA.setChecked(false);
            }
//            public void onNothingSelected (AdapterView < ? > arg0){        //这段代码是干嘛的？没方法内容耶。
//            }

        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }
}