package com.example.mobilecw;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobilecw.activities.HomeActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Launch HomeActivity directly
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}