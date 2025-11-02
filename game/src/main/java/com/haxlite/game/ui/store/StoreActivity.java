package com.goalrift.football.game.ui.store;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.goalrift.football.app.utils.ActiveItemManager;
import com.goalrift.football.app.utils.JetonManager;
import com.goalrift.football.app.utils.OwnedManager;
import com.goalrift.football.game.R;

public class StoreActivity extends AppCompatActivity {

    // Sistem yöneticileri
    private JetonManager jetonManager;
    private OwnedManager ownedManager;
    private ActiveItemManager activeItemManager;

    // UI öğeleri
    private TextView jetonText;

    // --- Formlar ---
    private Button buyForma, activeForma;
    private Button buyFormaGold, activeFormaGold;

    // --- Toplar ---
    private Button buyTop, activeTop;
    private Button buyTopGold, activeTopGold;

    // --- Sahalar ---
    private Button buyArkaPlan, activeArkaPlan;
    private Button buyFieldBlue, activeFieldBlue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        // Sağdan kayarak açılma animasyonu
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);

        // Yöneticileri başlat
        jetonManager = new JetonManager(this);
        ownedManager = new OwnedManager(this);
        activeItemManager = new ActiveItemManager(this);

        // Jeton metni
        jetonText = findViewById(R.id.jetonText);
        updateJetonText();

        // Geri butonu
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        // --- Bağlantılar ---
        // Formlar
        buyForma = findViewById(R.id.btnBuyForma);
        activeForma = findViewById(R.id.btnActiveForma);
        buyFormaGold = findViewById(R.id.btnBuyFormaGold);
        activeFormaGold = findViewById(R.id.btnActiveFormaGold);

        // Toplar
        buyTop = findViewById(R.id.btnBuyTop);
        activeTop = findViewById(R.id.btnActiveTop);
        buyTopGold = findViewById(R.id.btnBuyTopGold);
        activeTopGold = findViewById(R.id.btnActiveTopGold);

        // Sahalar
        buyArkaPlan = findViewById(R.id.btnBuyArkaPlan);
        activeArkaPlan = findViewById(R.id.btnActiveArkaPlan);
        buyFieldBlue = findViewById(R.id.btnBuyFieldBlue);
        activeFieldBlue = findViewById(R.id.btnActiveFieldBlue);

        // --- Satın alma işlemleri ---
        buyForma.setOnClickListener(v -> buyItem("Mavi Forma", 0, "forma_blue"));
        buyFormaGold.setOnClickListener(v -> buyItem("Altın Forma", 500, "forma_gold"));
        buyTop.setOnClickListener(v -> buyItem("Beyaz Top", 0, "top_white"));
        buyTopGold.setOnClickListener(v -> buyItem("Altın Top", 400, "top_gold"));
        buyArkaPlan.setOnClickListener(v -> buyItem("Yeşil Saha", 0, "field_green"));
        buyFieldBlue.setOnClickListener(v -> buyItem("Mavi Saha", 300, "field_blue"));

        // --- Aktif etme işlemleri ---
        activeForma.setOnClickListener(v -> activateItem("forma_blue", "forma"));
        activeFormaGold.setOnClickListener(v -> activateItem("forma_gold", "forma"));
        activeTop.setOnClickListener(v -> activateItem("top_white", "top"));
        activeTopGold.setOnClickListener(v -> activateItem("top_gold", "top"));
        activeArkaPlan.setOnClickListener(v -> activateItem("field_green", "arka_plan"));
        activeFieldBlue.setOnClickListener(v -> activateItem("field_blue", "arka_plan"));

        // Başlangıçta buton durumlarını güncelle
        updateButtons();
    }

    // ------------------ Satın alma işlemi ------------------
    private void buyItem(String itemName, int price, String key) {
        if (ownedManager.isOwned(key)) {
            Toast.makeText(this, itemName + " zaten sende!", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentJeton = jetonManager.getJeton();
        if (currentJeton >= price) {
            jetonManager.addJeton(-price);
            ownedManager.setOwned(key, true);
            Toast.makeText(this, itemName + " satın alındı!", Toast.LENGTH_SHORT).show();
            updateJetonText();
            updateButtons();
        } else {
            Toast.makeText(this, "Yetersiz jeton!", Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------ Aktif etme işlemi ------------------
    private void activateItem(String key, String category) {
        if (!ownedManager.isOwned(key)) {
            Toast.makeText(this, "Bu ürüne sahip değilsin!", Toast.LENGTH_SHORT).show();
            return;
        }

        activeItemManager.setActive(category, key);
        Toast.makeText(this, "Aktif ürün değiştirildi!", Toast.LENGTH_SHORT).show();
        updateButtons();
    }

    // ------------------ Jeton metni ------------------
    private void updateJetonText() {
        jetonText.setText("Jeton: " + jetonManager.getJeton());
    }

    // ------------------ Buton durumlarını güncelle ------------------
    private void updateButtons() {
        // Satın alınmış kontrolü
        checkOwned(buyForma, "forma_blue");
        checkOwned(buyFormaGold, "forma_gold");
        checkOwned(buyTop, "top_white");
        checkOwned(buyTopGold, "top_gold");
        checkOwned(buyArkaPlan, "field_green");
        checkOwned(buyFieldBlue, "field_blue");

        // Aktif ürün kontrolü
        checkActive(activeForma, "forma", "forma_blue");
        checkActive(activeFormaGold, "forma", "forma_gold");
        checkActive(activeTop, "top", "top_white");
        checkActive(activeTopGold, "top", "top_gold");
        checkActive(activeArkaPlan, "arka_plan", "field_green");
        checkActive(activeFieldBlue, "arka_plan", "field_blue");
    }

    private void checkOwned(Button btn, String key) {
        if (ownedManager.isOwned(key)) {
            btn.setText("Sahip");
            btn.setEnabled(false);
        } else {
            btn.setText("Satın Al");
            btn.setEnabled(true);
        }
    }

    private void checkActive(Button btn, String category, String key) {
        String active = activeItemManager.getActive(category);
        if (key.equals(active)) {
            btn.setText("Aktif ✓");
            btn.setEnabled(false);
        } else {
            btn.setText("Aktif Et");
            btn.setEnabled(true);
        }
    }
}
