package com.prsioner.jnilearnproject;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    TextView tv;
    Button sendDataToJniBtn;
    Button getDataFromJniBtn;
    Button callBackBtn;
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        tv = findViewById(R.id.tv_showText);
        sendDataToJniBtn = findViewById(R.id.btn_send_text_to_jni);
        getDataFromJniBtn = findViewById(R.id.btn_get_data_from_jni);
        callBackBtn = findViewById(R.id.btn_callback);


        tv.setText(stringFromJNI());

        sendDataToJniBtn.setOnClickListener(v -> {
                tv.setText(sendDataToJni(MainActivity.class.getSimpleName()));
        });

        getDataFromJniBtn.setOnClickListener(v -> tv.setText(getDataFromJniBtn()));

        callBackBtn.setOnClickListener(v -> {
            sendCallBackCmd();
        });

    }

    /**
     * 给JNI 回调的方法
     */
    public void jniCallBack(int code){
        Toast.makeText(MainActivity.this,"call back by jni code:"+code,Toast.LENGTH_SHORT).show();
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();


    /**
     * send data to jni
     */
    public native String sendDataToJni(String className);


    /**
     * get data from jni
     */
    public native String getDataFromJniBtn();

    /**
     * send command and call back
     */
    public native void sendCallBackCmd();
}