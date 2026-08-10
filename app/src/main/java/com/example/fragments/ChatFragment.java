package com.example.fragments;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.adapters.ChatAdapter;
import com.example.database.DatabaseHelper;
import com.example.models.Message;
import com.example.models.User;
import com.example.utils.AirLogger;

import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView rvChats;
    private View layoutEmpty;
    private ChatAdapter adapter;
    private DatabaseHelper dbHelper;

    private final BroadcastReceiver smsUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AirLogger.i("ChatFragment", "SMS update broadcast received (" + (intent != null ? intent.getAction() : "null") + "), refreshing UI");
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> loadChats());
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        loadChats();
        if (getArguments() != null && getArguments().containsKey("target_recipient")) {
            String target = getArguments().getString("target_recipient");
            getArguments().remove("target_recipient");
            if (target != null && !target.isEmpty()) {
                showComposeDialog(target);
            }
        }
        if (getContext() != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.example.ACTION_SMS_RECEIVED");
            filter.addAction("com.example.ACTION_SMS_SENT");
            filter.addAction("com.example.ACTION_SMS_DELIVERED");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getContext().registerReceiver(smsUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                getContext().registerReceiver(smsUpdateReceiver, filter);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getContext() != null) {
            try {
                getContext().unregisterReceiver(smsUpdateReceiver);
            } catch (Exception ignored) {
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        rvChats = view.findViewById(R.id.rvChats);
        layoutEmpty = view.findViewById(R.id.layoutEmptyChats);
        dbHelper = DatabaseHelper.getInstance(requireContext());

        rvChats.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadChats();

        view.findViewById(R.id.fabNewChat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showComposeDialog();
            }
        });

        return view;
    }

    private void loadChats() {
        List<User> users = dbHelper.getAllUsers();
        if (users.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvChats.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvChats.setVisibility(View.VISIBLE);
            adapter = new ChatAdapter(users, new ChatAdapter.OnChatClickListener() {
                @Override
                public void onChatClick(User user) {
                    Toast.makeText(requireContext(), "Opening conversation with " + user.getName(), Toast.LENGTH_SHORT).show();
                }
            });
            rvChats.setAdapter(adapter);
        }
    }

    private void showComposeDialog() {
        showComposeDialog("");
    }

    private void showComposeDialog(String prefillPhone) {
        final View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_compose_sms, null);
        final EditText etPhone = dialogView.findViewById(R.id.etRecipientPhone);
        final EditText etMessage = dialogView.findViewById(R.id.etComposeText);

        if (prefillPhone != null && !prefillPhone.isEmpty()) {
            etPhone.setText(prefillPhone);
        }

        final AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        View btnDismiss = dialogView.findViewById(R.id.btnDismissComposeDialog);
        if (btnDismiss != null) {
            btnDismiss.setOnClickListener(v -> dialog.dismiss());
        }

        View btnPickContact = dialogView.findViewById(R.id.btnPickContact);
        if (btnPickContact != null) {
            btnPickContact.setOnClickListener(v -> showContactPickerDialog(etPhone));
        }

        dialogView.findViewById(R.id.btnCancelCompose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialogView.findViewById(R.id.btnSendCompose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = etPhone.getText().toString().trim();
                String text = etMessage.getText().toString().trim();
                if (!phone.isEmpty() && !text.isEmpty()) {
                    Message msg = new Message(0, "me", phone, text, System.currentTimeMillis(), "SMS", "SENDING");
                    long msgId = dbHelper.insertMessage(msg);

                    com.example.utils.AirLogger.i("ChatFragment", "User triggered SMS send to " + phone + ", msgId=" + msgId);
                    com.example.sms.SmsSenderManager.sendSms(requireContext(), phone, text, msgId);

                    Toast.makeText(requireContext(), "Sending SMS to " + phone + "...", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadChats();
                } else {
                    Toast.makeText(requireContext(), "Please enter phone and message", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private void showContactPickerDialog(final EditText targetEditText) {
        List<User> users = dbHelper.getAllUsers();
        if (users.isEmpty()) {
            Toast.makeText(requireContext(), "No contacts found in database", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[users.size()];
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            names[i] = u.getName() + " (" + u.getPhone() + ")";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Contact")
                .setItems(names, (dialog, which) -> {
                    User selected = users.get(which);
                    targetEditText.setText(selected.getPhone());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
