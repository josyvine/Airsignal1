package com.example.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.activities.DialerActivity;
import com.example.adapters.CallsAdapter;
import com.example.call.CallManager;
import com.example.database.DatabaseHelper;
import com.example.models.CallLogItem;
import com.example.services.AudioTransferService;

import java.util.List;

public class CallsFragment extends Fragment {

    private RecyclerView rvCalls;
    private CallsAdapter adapter;
    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calls, container, false);

        rvCalls = view.findViewById(R.id.rvCalls);
        dbHelper = DatabaseHelper.getInstance(requireContext());

        rvCalls.setLayoutManager(new LinearLayoutManager(requireContext()));

        view.findViewById(R.id.btnOpenDialer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(requireContext(), DialerActivity.class);
                startActivity(intent);
            }
        });

        view.findViewById(R.id.btnStartAudioDataCall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serviceIntent = new Intent(requireContext(), AudioTransferService.class);
                requireContext().startService(serviceIntent);
            }
        });

        loadCalls();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCalls();
    }

    private void loadCalls() {
        List<CallLogItem> calls = dbHelper.getAllCalls();
        adapter = new CallsAdapter(calls, item -> {
            if (item != null && item.getNumber() != null) {
                CallManager.placeCall(requireContext(), item.getNumber());
            }
        });
        rvCalls.setAdapter(adapter);
    }
}
