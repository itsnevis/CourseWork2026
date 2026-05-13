package com.mirea.kt.ribo.kazintsev;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TaskActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        TextView tvV = findViewById(R.id.tvVariant);
        TextView tvT = findViewById(R.id.tvTaskDescription);
        Button btn = findViewById(R.id.btnGoToMain);

        String variant = getIntent().getStringExtra("variant");
        String task = getIntent().getStringExtra("task");

        tvV.setText("Вариант: " + variant);
        tvT.setText(task);

        btn.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
    }
}