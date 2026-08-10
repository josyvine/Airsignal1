package com.example.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.R;
import com.example.utils.SmsRoleManager;
import com.google.android.material.slider.Slider;

public class SettingsFragment extends Fragment {

    private TextView tvDefaultSmsStatus;
    private TextView tvDefaultDialerStatus;
    private TextView tvBaudRateVal;
    private Slider sliderBaudRate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        tvDefaultSmsStatus = view.findViewById(R.id.tvDefaultSmsStatus);
        tvDefaultDialerStatus = view.findViewById(R.id.tvDefaultDialerStatus);
        tvBaudRateVal = view.findViewById(R.id.tvBaudRateVal);
        sliderBaudRate = view.findViewById(R.id.sliderBaudRate);

        updateRoleStatuses();

        view.findViewById(R.id.btnSetDefaultSms).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SmsRoleManager.requestDefaultSmsRole(requireActivity());
            }
        });

        view.findViewById(R.id.btnSetDefaultDialer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SmsRoleManager.requestDefaultDialerRole(requireActivity());
            }
        });

        view.findViewById(R.id.btnOpenSystemDefaultApps).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SmsRoleManager.openSystemDefaultAppsSettings(requireContext());
            }
        });

        sliderBaudRate.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                int baud = (int) value;
                tvBaudRateVal.setText(baud + " Baud (FSK Tones)");
            }
        });

        view.findViewById(R.id.btnViewAirLogs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogViewerDialog();
            }
        });

        view.findViewById(R.id.btnClearAirLogs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                com.example.utils.AirLogger.clearLogs();
                Toast.makeText(requireContext(), "AirLog file cleared", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void showLogViewerDialog() {
        String logContent = com.example.utils.AirLogger.readLogContent();
        if (logContent.isEmpty()) {
            logContent = "No log entries found yet in Download/airlog/air_actions.log";
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("AirSignal Action Log")
                .setMessage(logContent)
                .setPositiveButton("OK", null)
                .setNeutralButton("Clear", (dialog, which) -> {
                    com.example.utils.AirLogger.clearLogs();
                    Toast.makeText(requireContext(), "Logs cleared", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void updateRoleStatuses() {
        boolean smsDefault = SmsRoleManager.isDefaultSmsApp(requireContext());
        boolean dialerDefault = SmsRoleManager.isDefaultDialerApp(requireContext());

        tvDefaultSmsStatus.setText(smsDefault ? "Status: Default SMS Handler Active" : "Status: Not Default SMS App");
        tvDefaultDialerStatus.setText(dialerDefault ? "Status: Default Phone Handler Active" : "Status: Not Default Phone App");
    }

    @Override
    public void onResume() {
        super.onResume();
        updateRoleStatuses();
    }
}
