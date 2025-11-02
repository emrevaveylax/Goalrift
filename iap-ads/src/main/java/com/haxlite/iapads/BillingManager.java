package com.goalrift.football.iapads;

public class BillingManager {
    public interface Listener {
        void onPurchase(String sku, boolean success);
    }
    public void init() {}
    public void buy(String sku, Listener l) {
        // stub: immediately succeed
        if (l != null) l.onPurchase(sku, true);
    }
}
