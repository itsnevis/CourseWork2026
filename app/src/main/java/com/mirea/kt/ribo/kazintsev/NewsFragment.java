package com.mirea.kt.ribo.kazintsev;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.List;

public class NewsFragment extends Fragment implements NewsAdapter.OnNewsClickListener {
    private RecyclerView rv;
    private ProgressBar loader;
    private String category;

    // Метод для создания фрагмента с параметром (исправляет ошибку в MainActivity)
    public static NewsFragment newInstance(String category) {
        NewsFragment fragment = new NewsFragment();
        Bundle args = new Bundle();
        args.putString("category", category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            category = getArguments().getString("category");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news, container, false);
        rv = view.findViewById(R.id.rvNews);
        loader = view.findViewById(R.id.loader);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        loadNews();
        return view;
    }

    private void loadNews() {
        if (loader != null) loader.setVisibility(View.VISIBLE);
        new Thread(() -> {
            // Передаем категорию в парсер
            List<NewsItem> items = NetworkUtils.fetchNews(category != null ? category : "all");

            // Сортировка для Варианта 69
            if (items != null && items.size() > 1) {
                Collections.sort(items, (n1, n2) -> n2.getPubDate().compareTo(n1.getPubDate()));
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (loader != null) loader.setVisibility(View.GONE);
                    if (items != null && !items.isEmpty()) {
                        rv.setAdapter(new NewsAdapter(items, this));
                    }
                });
            }
        }).start();
    }

    @Override
    public void onNewsClick(NewsItem item) {
        Intent intent = new Intent(getContext(), NewsDetailActivity.class);
        intent.putExtra("title", item.getTitle());
        intent.putExtra("content", item.getDescription());
        intent.putExtra("link", item.getLink());
        startActivity(intent);
    }
}