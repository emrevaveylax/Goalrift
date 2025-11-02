package com.goalrift.football.app;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * Ayarlar ekranı:
 * - Toolbar geri ok görünür ve tıklanınca finish() çalışır.
 * - İçeriğe SettingsFragment yerleştirilir.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        // Programatik navIcon + beyaz tint (XML’deki çalışmasa bile garanti)
        if (toolbar != null) {
            Drawable nav = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_24);
            if (nav != null) {
                nav = DrawableCompat.wrap(nav.mutate());
                DrawableCompat.setTint(nav, ContextCompat.getColor(this, android.R.color.white));
                toolbar.setNavigationIcon(nav);
                toolbar.setNavigationContentDescription(R.string.back);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new com.goalrift.football.app.settings.SettingsFragment())
                    .commit();
        }
    }
}
