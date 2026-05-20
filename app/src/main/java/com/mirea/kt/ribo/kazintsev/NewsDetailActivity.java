package com.mirea.kt.ribo.kazintsev;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class NewsDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        String title   = getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content"); // краткое описание из RSS
        String link    = getIntent().getStringExtra("link");
        String date    = getIntent().getStringExtra("date");

        TextView tvTitle   = findViewById(R.id.tvDetailTitle);
        TextView tvDate    = findViewById(R.id.tvDetailDate);
        TextView tvContent = findViewById(R.id.tvDetailContent);
        ProgressBar loader = findViewById(R.id.detailLoader);
        ScrollView scroll  = findViewById(R.id.detailScrollView);
        Button btnShare    = findViewById(R.id.btnShare);

        tvTitle.setText(title != null ? title : "");
        tvDate.setText(date != null ? date : "");


        if (content != null && !content.isEmpty()) {
            tvContent.setText(content);
        }


        if (link != null && !link.isEmpty()) {
            loader.setVisibility(View.VISIBLE);

            new Thread(() -> {
                String fullText = NetworkUtils.fetchArticleContent(link);

                runOnUiThread(() -> {
                    loader.setVisibility(View.GONE);
                    if (fullText != null && fullText.length() > 50) {
                        tvContent.setText(fullText);
                    }

                });
            }).start();
        }


        btnShare.setOnClickListener(v -> {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    title + "\n\nПодробнее: " + link);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "Поделиться через:"));
        });
    }
}