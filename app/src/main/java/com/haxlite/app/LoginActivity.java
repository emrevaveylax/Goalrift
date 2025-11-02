package com.goalrift.football.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText nicknameInput;
    private Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Eğer daha önce nick kaydedilmişse direkt oyuna geç
        SharedPreferences prefs = getSharedPreferences("HaxPrefs", Context.MODE_PRIVATE);
        String savedNick = prefs.getString("nickname", null);
        if (savedNick != null) {
            goToGame();
            return;
        }

        setContentView(R.layout.activity_login);

        nicknameInput = findViewById(R.id.nicknameInput);
        startButton = findViewById(R.id.startButton);

        startButton.setOnClickListener(v -> {
            String nick = nicknameInput.getText().toString().trim();
            if (!nick.isEmpty()) {
                prefs.edit().putString("nickname", nick).apply();
                goToGame();
            } else {
                nicknameInput.setError("Lütfen bir isim gir");
            }
        });
    }

    private void goToGame() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
