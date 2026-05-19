package com.mirea.kt.ribo.kazintsev;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ViewHolder> {

    public interface OnNewsClickListener {
        void onNewsClick(NewsItem item);
    }

    private List<NewsItem> newsList;
    private final OnNewsClickListener clickListener;

    public NewsAdapter(List<NewsItem> newsList, OnNewsClickListener clickListener) {
        this.newsList = new ArrayList<>(newsList);
        this.clickListener = clickListener;
    }

    public void updateItems(List<NewsItem> items) {
        this.newsList = new ArrayList<>(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NewsItem item = newsList.get(position);
        holder.title.setText(item.getTitle());
        holder.date.setText(item.getPubDate());
        holder.itemView.setOnClickListener(v -> clickListener.onNewsClick(item));
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView date;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvNewsTitle);
            date = itemView.findViewById(R.id.tvNewsDate);
        }
    }
}