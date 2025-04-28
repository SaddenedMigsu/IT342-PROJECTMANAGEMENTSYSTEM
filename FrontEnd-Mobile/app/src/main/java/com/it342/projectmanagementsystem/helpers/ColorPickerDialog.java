package com.it342.projectmanagementsystem.helpers;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.it342.projectmanagementsystem.R;

public class ColorPickerDialog {
    private Dialog dialog;
    private OnColorSelectedListener listener;
    private int currentColor = Color.RED;
    private SeekBar seekBarRed, seekBarGreen, seekBarBlue;
    private View colorPreview;
    private TextView tvHex;

    public ColorPickerDialog(Context context, OnColorSelectedListener listener) {
        this.listener = listener;
        dialog = new Dialog(context);
        
        // Create view
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null);
        dialog.setContentView(view);
        dialog.setTitle("Choose Color");
        
        // Find views
        seekBarRed = view.findViewById(R.id.seekBarRed);
        seekBarGreen = view.findViewById(R.id.seekBarGreen);
        seekBarBlue = view.findViewById(R.id.seekBarBlue);
        colorPreview = view.findViewById(R.id.colorPreview);
        tvHex = view.findViewById(R.id.tvHex);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnOk = view.findViewById(R.id.btnOk);
        
        // Initialize UI
        updateColorDisplay();
        
        // Set up listeners
        SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateCurrentColor();
                updateColorDisplay();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        
        seekBarRed.setOnSeekBarChangeListener(seekBarChangeListener);
        seekBarGreen.setOnSeekBarChangeListener(seekBarChangeListener);
        seekBarBlue.setOnSeekBarChangeListener(seekBarChangeListener);
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnOk.setOnClickListener(v -> {
            if (listener != null) {
                listener.onColorSelected(getHexColor());
            }
            dialog.dismiss();
        });
    }
    
    public void show() {
        dialog.show();
    }
    
    public void setInitialColor(String hexColor) {
        if (hexColor != null && hexColor.startsWith("#")) {
            try {
                int color = Color.parseColor(hexColor);
                currentColor = color;
                int red = Color.red(color);
                int green = Color.green(color);
                int blue = Color.blue(color);
                
                seekBarRed.setProgress(red);
                seekBarGreen.setProgress(green);
                seekBarBlue.setProgress(blue);
                
                updateColorDisplay();
            } catch (IllegalArgumentException e) {
                // Invalid hex color format, keep default
            }
        }
    }
    
    private void updateCurrentColor() {
        int r = seekBarRed.getProgress();
        int g = seekBarGreen.getProgress();
        int b = seekBarBlue.getProgress();
        currentColor = Color.rgb(r, g, b);
    }
    
    private void updateColorDisplay() {
        colorPreview.setBackgroundColor(currentColor);
        tvHex.setText(getHexColor());
    }
    
    private String getHexColor() {
        return String.format("#%02X%02X%02X", 
                seekBarRed.getProgress(), 
                seekBarGreen.getProgress(), 
                seekBarBlue.getProgress());
    }
    
    public interface OnColorSelectedListener {
        void onColorSelected(String hexColor);
    }
} 