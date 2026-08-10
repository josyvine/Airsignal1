package com.example.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.models.CallLogItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CallsAdapter extends RecyclerView.Adapter<CallsAdapter.ViewHolder> {

    public interface OnCallItemClickListener {
        void onCallClick(CallLogItem item);
    }

    private List<CallLogItem> callList;
    private OnCallItemClickListener listener;

    public CallsAdapter(List<CallLogItem> callList) {
        this.callList = callList;
    }

    public CallsAdapter(List<CallLogItem> callList, OnCallItemClickListener listener) {
        this.callList = callList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_call, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CallLogItem item = callList.get(position);

        String typeStr = item.getType();
        String displayType = typeStr;
        int colorRes = 0xFF4CAF50;

        if ("OUTGOING".equalsIgnoreCase(typeStr) || "DIALLED".equalsIgnoreCase(typeStr)) {
            displayType = "Dialled";
            colorRes = 0xFF4CAF50;
        } else if ("INCOMING".equalsIgnoreCase(typeStr) || "RECEIVED".equalsIgnoreCase(typeStr)) {
            displayType = "Received";
            colorRes = 0xFF2196F3;
        } else if ("MISSED".equalsIgnoreCase(typeStr)) {
            displayType = "Missed";
            colorRes = 0xFFFF5252;
        } else if ("AUDIO_DATA".equalsIgnoreCase(typeStr)) {
            displayType = "Audio Data";
            colorRes = 0xFF00E5FF;
        }

        holder.tvNumber.setText(item.getNumber() + " (" + displayType + ")");
        if (holder.imgCallType != null) {
            holder.imgCallType.setColorFilter(colorRes);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
        holder.tvTime.setText(sdf.format(new Date(item.getTimestamp())));
        holder.tvDuration.setText(item.getDuration() > 0 ? item.getDuration() + "s" : displayType);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCallClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return callList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumber, tvTime, tvDuration;
        ImageView imgCallType;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tvCallNumber);
            tvTime = itemView.findViewById(R.id.tvCallTime);
            tvDuration = itemView.findViewById(R.id.tvCallDuration);
            imgCallType = itemView.findViewById(R.id.imgCallType);
        }
    }
}
