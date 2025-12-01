package com.goalrift.football.app;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.goalrift.football.game.ui.MenuView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // AdMob'u başlat
        MobileAds.initialize(this, initializationStatus -> {});

        // Ana düzen oluştur (Dikey yerleşim)
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Oyun Görünümü (Ekranın geri kalanını kaplayacak)
        MenuView gameView = new MenuView(this);
        LinearLayout.LayoutParams gameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        gameView.setLayoutParams(gameParams);

        // Banner Reklamı
        AdView adView = new AdView(this);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId("ca-app-pub-8027726734791639/7641742366");

        // Reklamı yükle
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        // Görünümleri düzen içine ekle (Oyun üstte, reklam altta)
        mainLayout.addView(gameView);
        mainLayout.addView(adView);

        setContentView(mainLayout);

        // Immersive modu reklamla uyumlu olması için sadece navigation gizleyecek şekilde güncelleyebilirsiniz,
        // ancak tam ekran deneyimi için mevcut kodunuzu koruyarak enterImmersive()'i çağırıyoruz.
        enterImmersive();
    }

    private void enterImmersive() {
        final View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersive();
    }
}