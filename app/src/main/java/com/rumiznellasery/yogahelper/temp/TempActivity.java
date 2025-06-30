package com.rumiznellasery.yogahelper.temp;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.rumiznellasery.yogahelper.R;

public class TempActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp);

        Button backButton = findViewById(R.id.button_back);
        backButton.setOnClickListener(v -> finish());
    }
}

