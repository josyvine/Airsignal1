package com.example.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.activities.ConversationActivity;
import com.example.adapters.ChatAdapter;
import com.example.database.DatabaseHelper;
import com.example.models.Message;
import com.example.models.User;
import com.example.services.SmsService;
import com.example.utils.AirLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

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
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Map<String, User> threadMap = new LinkedHashMap<>();

                // 1. Fetch messages from DatabaseHelper
                List<Message> allMessages = dbHelper.getAllMessages();
                if (allMessages != null) {
                    // Sort descending by timestamp so newest messages create thread headers
                    Collections.sort(allMessages, (m1, m2) -> Long.compare(m2.getTimestamp(), m1.getTimestamp()));

                    for (Message msg : allMessages) {
                        if (msg == null) continue;
                        String remoteNumber = "me".equalsIgnoreCase(msg.getSender()) ? msg.getReceiver() : msg.getSender();
                        if (remoteNumber == null || remoteNumber.trim().isEmpty() || "me".equalsIgnoreCase(remoteNumber)) continue;

                        String cleanKey = cleanNumber(remoteNumber);
                        if (!threadMap.containsKey(cleanKey)) {
                            String resolvedName = resolveContactName(remoteNumber);
                            threadMap.put(cleanKey, new User(0, resolvedName, remoteNumber, ""));
                        }
                    }
                }

                List<User> conversationThreads = new ArrayList<>(threadMap.values());

                // Fallback to database users if no conversation messages exist yet
                if (conversationThreads.isEmpty()) {
                    conversationThreads = dbHelper.getAllUsers();
                }

                final List<User> finalThreads = conversationThreads;

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;

                        if (finalThreads.isEmpty()) {
                            layoutEmpty.setVisibility(View.VISIBLE);
                            rvChats.setVisibility(View.GONE);
                        } else {
                            layoutEmpty.setVisibility(View.GONE);
                            rvChats.setVisibility(View.VISIBLE);
                            adapter = new ChatAdapter(finalThreads, new ChatAdapter.OnChatClickListener() {
                                @Override
                                public void onChatClick(User user) {
                                    if (user != null && user.getPhone() != null) {
                                        Intent intent = new Intent(requireContext(), ConversationActivity.class);
                                        intent.putExtra("target_recipient", user.getPhone());
                                        startActivity(intent);
                                    }
                                }
                            });
                            rvChats.setAdapter(adapter);
                        }
                    });
                }
            } catch (Exception e) {
                AirLogger.e("ChatFragment", "Error loading conversation threads", e);
            }
        });
    }

    private String resolveContactName(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) return "Unknown";

        // Check DB users
        List<User> dbUsers = dbHelper.getAllUsers();
        if (dbUsers != null) {
            for (User u : dbUsers) {
                if (u != null && u.getPhone() != null && cleanNumber(u.getPhone()).equals(cleanNumber(phoneNumber))) {
                    return u.getName();
                }
            }
        }

        // Check System Contacts
        if (getContext() != null && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            try {
                ContentResolver resolver = requireContext().getContentResolver();
                Cursor cursor = resolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                        ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ?",
                        new String[]{"%" + cleanNumber(phoneNumber) + "%"},
                        null
                );
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    if (nameIdx != -1) {
                        String name = cursor.getString(nameIdx);
                        cursor.close();
                        if (name != null && !name.trim().isEmpty()) {
                            return name;
                        }
                    }
                    cursor.close();
                }
            } catch (Exception ignored) {
            }
        }

        return phoneNumber;
    }

    private String cleanNumber(String raw) {
        if (raw == null) return "";
        String cleaned = raw.replaceAll("[^0-9]", "");
        if (cleaned.length() > 10) {
            cleaned = cleaned.substring(cleaned.length() - 10);
        }
        return cleaned;
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

                    AirLogger.i("ChatFragment", "User triggered SMS send to " + phone + ", msgId=" + msgId);
                    SmsService.sendSms(requireContext(), phone, text, -1, msgId);

                    Toast.makeText(requireContext(), "Sending SMS to " + phone + "...", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();

                    // Immediately open conversation thread for that recipient
                    Intent intent = new Intent(requireContext(), ConversationActivity.class);
                    intent.putExtra("target_recipient", phone);
                    startActivity(intent);
                } else {
                    Toast.makeText(requireContext(), "Please enter phone and message", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private void showContactPickerDialog(final EditText targetEditText) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<User> contacts = new ArrayList<>();

            if (getContext() != null && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                try {
                    ContentResolver resolver = requireContext().getContentResolver();
                    Cursor cursor = resolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            new String[]{
                                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                            },
                            null,
                            null,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                    );

                    if (cursor != null) {
                        int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                        int phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        int idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);

                        while (cursor.moveToNext()) {
                            String name = nameIdx != -1 ? cursor.getString(nameIdx) : "Unknown";
                            String phone = phoneIdx != -1 ? cursor.getString(phoneIdx) : "";
                            long id = 0;
                            try {
                                if (idIdx != -1) id = Long.parseLong(cursor.getString(idIdx));
                            } catch (Exception ignored) {
                            }

                            if (phone != null && !phone.trim().isEmpty()) {
                                contacts.add(new User(id, name, phone, ""));
                            }
                        }
                        cursor.close();
                    }
                } catch (Exception e) {
                    AirLogger.e("ChatFragment", "Failed reading system contacts for picker", e);
                }
            }

            if (contacts.isEmpty()) {
                contacts.addAll(dbHelper.getAllUsers());
            }

            final List<User> finalContacts = contacts;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;

                    if (finalContacts.isEmpty()) {
                        Toast.makeText(requireContext(), "No contacts found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Build Searchable Contact Picker Dialog Layout Programmatically
                    LinearLayout containerLayout = new LinearLayout(requireContext());
                    containerLayout.setOrientation(LinearLayout.VERTICAL);
                    containerLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
                    containerLayout.setBackgroundColor(Color.parseColor("#1E293B"));

                    // Title Header
                    TextView tvTitle = new TextView(requireContext());
                    tvTitle.setText("Select Contact");
                    tvTitle.setTextColor(Color.WHITE);
                    tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                    tvTitle.setTypeface(null, Typeface.BOLD);
                    tvTitle.setPadding(0, 0, 0, dpToPx(12));

                    // Search EditText with person_search_24px icon
                    EditText etSearch = new EditText(requireContext());
                    etSearch.setHint("Search contact name or number...");
                    etSearch.setHintTextColor(Color.parseColor("#94A3B8"));
                    etSearch.setTextColor(Color.WHITE);
                    etSearch.setBackgroundColor(Color.parseColor("#334155"));
                    etSearch.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
                    etSearch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

                    try {
                        int searchDrawableId = getResources().getIdentifier("person_search_24px", "drawable", requireContext().getPackageName());
                        if (searchDrawableId != 0) {
                            etSearch.setCompoundDrawablesWithIntrinsicBounds(searchDrawableId, 0, 0, 0);
                            etSearch.setCompoundDrawablePadding(dpToPx(8));
                        }
                    } catch (Exception ignored) {
                    }

                    // Contacts List Adapter & View
                    List<User> displayList = new ArrayList<>(finalContacts);
                    List<String> formattedNames = new ArrayList<>();
                    for (User u : displayList) {
                        formattedNames.add(u.getName() + " (" + u.getPhone() + ")");
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            formattedNames
                    ) {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                            TextView view = (TextView) super.getView(position, convertView, parent);
                            view.setTextColor(Color.WHITE);
                            view.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12));
                            return view;
                        }
                    };

                    ListView listView = new ListView(requireContext());
                    listView.setAdapter(adapter);
                    LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dpToPx(320)
                    );
                    listParams.setMargins(0, dpToPx(12), 0, 0);

                    containerLayout.addView(tvTitle);
                    containerLayout.addView(etSearch);
                    containerLayout.addView(listView, listParams);

                    AlertDialog dialog = new AlertDialog.Builder(requireContext())
                            .setView(containerLayout)
                            .setNegativeButton("Cancel", null)
                            .create();

                    // Search Filtering Listener
                    etSearch.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            String query = s.toString().toLowerCase().trim();
                            displayList.clear();
                            formattedNames.clear();

                            for (User u : finalContacts) {
                                if (query.isEmpty() ||
                                        (u.getName() != null && u.getName().toLowerCase().contains(query)) ||
                                        (u.getPhone() != null && u.getPhone().contains(query))) {
                                    displayList.add(u);
                                    formattedNames.add(u.getName() + " (" + u.getPhone() + ")");
                                }
                            }
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void afterTextChanged(Editable s) {}
                    });

                    listView.setOnItemClickListener((parent, view, position, id) -> {
                        if (position >= 0 && position < displayList.size()) {
                            User selected = displayList.get(position);
                            targetEditText.setText(selected.getPhone());
                        }
                        dialog.dismiss();
                    });

                    dialog.show();
                });
            }
        });
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return dp;
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }
}