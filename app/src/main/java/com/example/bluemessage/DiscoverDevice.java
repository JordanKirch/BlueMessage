package com.example.bluemessage;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class DiscoverDevice extends AppCompatActivity {

    private TextView textView;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover_device);
        textView = findViewById(R.id.name);

        userName = getIntent().getExtras().getString("userName");
        textView.setText(userName);
    }
}