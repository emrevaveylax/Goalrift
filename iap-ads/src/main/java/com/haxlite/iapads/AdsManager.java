package com.haxlite.iapads;

import android.app.Activity;
import android.util.Log;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class AdsManager {
    private RewardedAd mRewardedAd;
    private final Activity activity;
    private final String TAG = "AdsManager";

    // TEST ID'sidir. Canlıya geçerken kendi "Ödüllü Reklam" ID'n ile değiştir:
    private final String REWARDED_AD_ID = "ca-app-pub-8027726734791639/7903757358";

    public interface Listener {
        void onReward();
    }

    public AdsManager(Activity activity) {
        this.activity = activity;
    }

    // Reklam sistemini başlat ve ilk reklamı yükle
    public void init() {
        loadRewardedAd();
    }

    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(activity, REWARDED_AD_ID,
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Reklam yüklenemedi
                        Log.d(TAG, loadAdError.toString());
                        mRewardedAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        // Reklam yüklendi
                        mRewardedAd = rewardedAd;
                        Log.d(TAG, "Ad was loaded.");
                    }
                });
    }

    public void showRewarded(Listener listener) {
        if (mRewardedAd != null) {
            mRewardedAd.show(activity, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    // Kullanıcı reklamı sonuna kadar izledi, ödülü ver
                    if (listener != null) {
                        listener.onReward();
                    }
                    // Bir sonraki izleme için yeni reklam yükle
                    loadRewardedAd();
                }
            });
            // Gösterilen reklam nesnesini temizle (tek kullanımlıktır)
            mRewardedAd = null;
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.");
            // Reklam hazır değilse, tekrar yüklemeyi dene
            loadRewardedAd();
        }
    }
}