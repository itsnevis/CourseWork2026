package com.mirea.kt.ribo.kazintsev;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class NewsDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        String title = getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content");
        String link = getIntent().getStringExtra("link");

        ((TextView) findViewById(R.id.tvDetailTitle)).setText(title);
        ((TextView) findViewById(R.id.tvDetailContent)).setText(content);

        Button btnShare = findViewById(R.id.btnShare);
        btnShare.setOnClickListener(v -> {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            // Формируем текст сообщения
            sendIntent.putExtra(Intent.EXTRA_TEXT, title + "\n\nПодробнее: " + link);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "Поделиться через:"));
        });
    }
}