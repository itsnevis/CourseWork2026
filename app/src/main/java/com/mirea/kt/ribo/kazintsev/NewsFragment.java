package com.mirea.kt.ribo.kazintsev;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NewsFragment extends Fragment implements NewsAdapter.OnNewsClickListener {

    private static final String TAG = "NewsFragment";
    private static final String ARG_CATEGORY = "category";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
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
        SearchView searchView = view.findViewById(R.id.searchView);

        adapter = new NewsAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    filterItems(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filterItems(newText);
                    return true;
                }
            });

            // Сброс при очистке поля поиска
            searchView.setOnCloseListener(() -> {
                filterItems("");
                return false;
            });
        }

        loadNews();
    }

    private void filterItems(String query) {
        if (allLoadedItems.isEmpty()) return;

        if (query == null || query.trim().isEmpty()) {
            adapter.updateItems(allLoadedItems);
            showEmpty(false);
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

        adapter.updateItems(filtered);
        // Показываем "не найдено" только если список пуст,
        // но RecyclerView всегда остаётся видимым
        if (tvEmpty != null) {
            tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void loadNews() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (tvEmpty != null)     tvEmpty.setVisibility(View.GONE);
        Log.d(TAG, "Загружаем новости. Категория: " + category);

        new Thread(() -> {
            List<NewsItem> items = NetworkUtils.fetchNews(category);

            // Сортировка по дате (новые сначала)
            if (items != null && items.size() > 1) {
                Collections.sort(items,
                        (n1, n2) -> n2.getPubDate().compareTo(n1.getPubDate()));
            }

            if (getActivity() == null || !isAdded()) return;

            getActivity().runOnUiThread(() -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                if (items == null || items.isEmpty()) {
                    Log.w(TAG, "Новости не загружены");
                    showEmpty(true);
                } else {
                    Log.d(TAG, "Загружено новостей: " + items.size());
                    allLoadedItems = items;
                    adapter.updateItems(items);
                    showEmpty(false);
                }
            });
        }).start();
    }

    /** Показывает/скрывает сообщение "Новости не найдены".
     *  RecyclerView намеренно НЕ скрываем — это ломает поиск. */
    private void showEmpty(boolean isEmpty) {
        if (tvEmpty != null) {
            tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
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