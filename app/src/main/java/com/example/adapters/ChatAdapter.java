package com.example.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.models.User;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private List<User> userList;
    private OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(User user);
    }

    public ChatAdapter(List<User> userList, OnChatClickListener listener) {
        this.userList = (userList != null) ? userList : new ArrayList<>();
        this.listener = listener;
    }

    public void updateUserList(List<User> newUsers) {
        if (newUsers != null) {
            this.userList = new ArrayList<>(newUsers);
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);

        if (user != null) {
            String name = (user.getName() != null && !user.getName().trim().isEmpty()) ? user.getName() : user.getPhone();
            holder.tvName.setText(name);

            String phoneLabel = (user.getPhone() != null && !user.getPhone().trim().isEmpty()) ? user.getPhone() : "";
            holder.tvLastMsg.setText("Tap to open SMS / Data conversation (" + phoneLabel + ")");
            holder.tvTime.setText("Now");

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onChatClick(user);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return (userList != null) ? userList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLastMsg, tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvChatName);
            tvLastMsg = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvChatTimestamp);
        }
    }
}