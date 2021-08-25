package com.example.applicationdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class StartUpActivity extends AppCompatActivity {
    Button button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startup_activity);
         button=findViewById(R.id.button);

         button.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {

              Intent intent = new Intent(StartUpActivity.this,MainActivity.class);
              startActivity(intent);
             }
         });





}}