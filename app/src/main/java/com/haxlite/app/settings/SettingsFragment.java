package com.goalrift.football.app.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.goalrift.football.app.R;

public class SettingsFragment extends Fragment {

    private SharedPreferences prefs;

    private static final String K_SENS = "joystick_sensitivity"; // int 50..200 (100=1.0x)
    private static final String K_LEFT = "left_handed";
    private static final String K_VIB  = "vibration_enabled";
    private static final String K_GFX  = "graphics_quality";

    private static final int SENS_MIN = 50;
    private static final int SENS_MAX = 200;
    private static final int SENS_STEP = 5;

    private Slider sliderSens;
    private TextView txtSensValue;
    private MaterialSwitch swLeft, swVib;
    private AutoCompleteTextView ddGfx;
    private MaterialButton btnReset;

    @Override public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Nullable
    @Override
    public android.view.View onCreateView(@NonNull android.view.LayoutInflater inflater,
                                          @Nullable android.view.ViewGroup container,
                                          @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        sliderSens   = v.findViewById(R.id.sliderSens);
        txtSensValue = v.findViewById(R.id.txtSensValue);
        swLeft       = v.findViewById(R.id.swLeft);
        swVib        = v.findViewById(R.id.swVib);
        ddGfx        = v.findViewById(R.id.ddGraphics);
        btnReset     = v.findViewById(R.id.btnReset);

        // Slider’ı programatik kur
        sliderSens.setValueFrom(SENS_MIN);
        sliderSens.setValueTo(SENS_MAX);
        sliderSens.setStepSize(SENS_STEP);

        // Başlangıç değerini oku → 5’e "snap" et → hem slider’a ver hem de preferencelere geri yaz
        int sens = prefs.getInt(K_SENS, 100);
        int snapped = snapToStep(sens, SENS_MIN, SENS_MAX, SENS_STEP);
        if (snapped != sens) {
            prefs.edit().putInt(K_SENS, snapped).apply();
        }
        sliderSens.setValue(snapped);
        txtSensValue.setText("Hassasiyet: " + (snapped / 100f) + "x");

        // Switch’ler
        swLeft.setChecked(prefs.getBoolean(K_LEFT, false));
        swVib.setChecked(prefs.getBoolean(K_VIB, true));

        // Grafik kalitesi dropdown
        String gfx = prefs.getString(K_GFX, "medium");
        String[] entries = getResources().getStringArray(R.array.graphics_quality_entries);
        String[] values  = getResources().getStringArray(R.array.graphics_quality_values);
        int idx = indexOf(values, gfx);
        if (idx < 0) idx = 1; // medium
        ddGfx.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, entries));
        ddGfx.setText(entries[idx], false);

        // Dinleyiciler
        sliderSens.addOnChangeListener((slider, value, fromUser) -> {
            int iv = Math.round(value);                 // float → int
            iv = snapToStep(iv, SENS_MIN, SENS_MAX, SENS_STEP); // güvenli
            txtSensValue.setText("Hassasiyet: " + (iv / 100f) + "x");
            // Slider zaten step’e kilitli olduğu için setValue tekrar çağırmaya gerek yok
            prefs.edit().putInt(K_SENS, iv).apply();
        });

        swLeft.setOnCheckedChangeListener((btn, checked) -> prefs.edit().putBoolean(K_LEFT, checked).apply());
        swVib.setOnCheckedChangeListener((btn, checked) -> prefs.edit().putBoolean(K_VIB, checked).apply());

        ddGfx.setOnItemClickListener((parent, view, position, id1) -> {
            String selectedValue = values[position];
            prefs.edit().putString(K_GFX, selectedValue).apply();
        });

        btnReset.setOnClickListener(v1 -> {
            prefs.edit()
                    .putInt(K_SENS, 100)
                    .putBoolean(K_LEFT, false)
                    .putBoolean(K_VIB, true)
                    .putString(K_GFX, "medium")
                    .apply();

            sliderSens.setValue(100);
            txtSensValue.setText("Hassasiyet: 1.0x");
            swLeft.setChecked(false);
            swVib.setChecked(true);
            ddGfx.setText(entries[1], false); // medium
        });
    }

    private static int snapToStep(int val, int min, int max, int step){
        // Aralığa kırp
        int clamped = Math.max(min, Math.min(max, val));
        // min’den itibaren en yakın step’e yuvarla
        int offset = clamped - min;
        int snapped = Math.round(offset / (float) step) * step + min;
        // olası yuvarlamayı tekrar aralıkta tut
        return Math.max(min, Math.min(max, snapped));
    }

    private int indexOf(String[] arr, String needle){
        for (int i=0;i<arr.length;i++) if (arr[i].equals(needle)) return i;
        return -1;
    }
}
