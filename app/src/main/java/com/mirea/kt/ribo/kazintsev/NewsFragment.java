package com.mirea.kt.ribo.kazintsev;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NewsFragment extends Fragment implements NewsAdapter.OnNewsClickListener {

    private static final String TAG = "NewsFragment";
    private static final String ARG_CATEGORY = "category";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private TextInputEditText etSearch;
    private NewsAdapter adapter;
    private String category;
    private List<NewsItem> allLoadedItems = new ArrayList<>();

    public static NewsFragment newInstance(String category) {
        NewsFragment fragment = new NewsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            category = getArguments().getString(ARG_CATEGORY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_news, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.rvNews);
        progressBar  = view.findViewById(R.id.loader);
        tvEmpty      = view.findViewById(R.id.tvEmpty);
        etSearch     = view.findViewById(R.id.etSearch);

        adapter = new NewsAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterItems(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadNews();
    }

    private void filterItems(String query) {
        if (allLoadedItems.isEmpty()) return;

        if (query == null || query.trim().isEmpty()) {
            adapter.updateItems(allLoadedItems);
            setEmptyVisible(false);
            return;
        }

        String lower = query.toLowerCase().trim();
        List<NewsItem> filtered = new ArrayList<>();
        for (NewsItem item : allLoadedItems) {
            if (item.getTitle().toLowerCase().contains(lower)
                    || item.getDescription().toLowerCase().contains(lower)) {
                filtered.add(item);
            }
        }

        Log.d(TAG, "Поиск '" + query + "': найдено " + filtered.size());
        adapter.updateItems(filtered);
        setEmptyVisible(filtered.isEmpty());
    }

    private void loadNews() {
        setLoading(true);
        Log.d(TAG, "Загружаем новости. Категория: " + category);

        new Thread(() -> {
            List<NewsItem> items = NetworkUtils.fetchNews(category);

            if (items != null && items.size() > 1) {
                Collections.sort(items,
                        (a, b) -> b.getPubDate().compareTo(a.getPubDate()));
            }

            if (getActivity() == null || !isAdded()) return;

            getActivity().runOnUiThread(() -> {
                setLoading(false);
                if (items == null || items.isEmpty()) {
                    Log.w(TAG, "Новости не загружены");
                    setEmptyVisible(true);
                } else {
                    Log.d(TAG, "Загружено: " + items.size());
                    allLoadedItems = items;
                    adapter.updateItems(items);
                    setEmptyVisible(false);
                }
            });
        }).start();
    }

    private void setLoading(boolean loading) {
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void setEmptyVisible(boolean visible) {
        if (tvEmpty != null)
            tvEmpty.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onNewsClick(NewsItem item) {
        Log.d(TAG, "Нажата новость: " + item.getTitle());
        Intent intent = new Intent(requireContext(), NewsDetailActivity.class);
        intent.putExtra("title",   item.getTitle());
        intent.putExtra("content", item.getDescription());
        intent.putExtra("link",    item.getLink());
        intent.putExtra("date",    item.getPubDate());
        startActivity(intent);
    }
}