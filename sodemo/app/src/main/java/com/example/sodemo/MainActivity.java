package com.example.sodemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    static {
        System.loadLibrary("translib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onCalClick(View view) {
        TextView editText = findViewById(R.id.editText);
        String msg = editText.getText().toString().trim();
        TextView showText = findViewById(R.id.textView);
        showText.setText("cal: " + transMsg(msg));
    }

    public native String transMsg(String msg);
}
