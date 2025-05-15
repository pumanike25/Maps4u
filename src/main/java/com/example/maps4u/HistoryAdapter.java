package com.example.maps4u;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    private Cursor cursor;

    public HistoryAdapter(Cursor cursor) {
        this.cursor = cursor;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        if (cursor.moveToPosition(position)) {
            holder.bindData(cursor);
        }
    }

    @Override
    public int getItemCount() {
        return cursor != null ? cursor.getCount() : 0;
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView originText;
        private final TextView destinationText;
        private final TextView timestampText;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            originText = itemView.findViewById(R.id.history_origin);
            destinationText = itemView.findViewById(R.id.history_destination);
            timestampText = itemView.findViewById(R.id.history_timestamp);
        }

        public void bindData(Cursor cursor) {
            originText.setText("From: " + cursor.getString(cursor.getColumnIndexOrThrow("origin")));
            destinationText.setText("To: " + cursor.getString(cursor.getColumnIndexOrThrow("destination")));

            // Get transport mode and format it
            String transportMode = cursor.getString(cursor.getColumnIndexOrThrow("transport_mode"));
            String modeText = "";
            switch (transportMode) {
                case "car":
                    modeText = "🚗 By car";
                    break;
                case "walking":
                    modeText = "🚶‍♂️ Walking";
                    break;
                case "bicycle":
                    modeText = "🚴‍♂️ By bicycle";
                    break;
                default:
                    modeText = "Transport: " + transportMode;
            }

            timestampText.setText(formatDate(cursor.getString(cursor.getColumnIndexOrThrow("timestamp"))) +
                    " • " + modeText);
        }

        private String formatDate(String timestamp) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                Date date = inputFormat.parse(timestamp);
                return outputFormat.format(date);
            } catch (ParseException e) {
                return timestamp;
            }
        }
    }
}