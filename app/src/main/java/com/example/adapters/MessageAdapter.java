package com.example.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.models.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private List<Message> messageList;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = (messageList != null) ? messageList : new ArrayList<>();
    }

    public void updateMessages(List<Message> newMessages) {
        if (newMessages != null) {
            this.messageList = new ArrayList<>(newMessages);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = messageList.get(position);
        if (msg != null && "me".equalsIgnoreCase(msg.getSender())) {
            return TYPE_SENT;
        }
        return TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messageList.get(position);
        if (msg == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String timeStr = sdf.format(new Date(msg.getTimestamp()));

        if (holder instanceof SentViewHolder) {
            SentViewHolder vh = (SentViewHolder) holder;
            vh.tvBody.setText(msg.getMessage());
            vh.tvTime.setText(timeStr);
            if (vh.tvStatus != null) {
                String status = msg.getStatus();
                if ("PENDING".equalsIgnoreCase(status)) {
                    vh.tvStatus.setText("Sending...");
                } else if ("SENT".equalsIgnoreCase(status)) {
                    vh.tvStatus.setText("✓");
                } else if ("DELIVERED".equalsIgnoreCase(status)) {
                    vh.tvStatus.setText("✓✓");
                } else if ("FAILED".equalsIgnoreCase(status)) {
                    vh.tvStatus.setText("Failed");
                } else {
                    vh.tvStatus.setText(status != null ? status : "");
                }
            }
        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder vh = (ReceivedViewHolder) holder;
            vh.tvBody.setText(msg.getMessage());
            vh.tvTime.setText(timeStr);
        }
    }

    @Override
    public int getItemCount() {
        return (messageList != null) ? messageList.size() : 0;
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvBody, tvTime, tvStatus;

        SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBody = itemView.findViewById(R.id.tvMessageBody);
            tvTime = itemView.findViewById(R.id.tvMessageTime);
            tvStatus = itemView.findViewById(R.id.tvMessageStatus);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvBody, tvTime;

        ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBody = itemView.findViewById(R.id.tvMessageBody);
            tvTime = itemView.findViewById(R.id.tvMessageTime);
        }
    }
}