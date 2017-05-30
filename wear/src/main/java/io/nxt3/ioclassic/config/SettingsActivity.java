package io.nxt3.ioclassic.config;


import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;

import io.nxt3.ioclassic.R;

public class SettingsActivity  extends PreferenceActivity implements BillingProcessor.IBillingHandler {
    private static BillingProcessor sBillingProcessor;

    public static void donate(Activity activity, String productId) {
        sBillingProcessor.purchase(activity, productId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsFragment settingsPreferenceFragment = new SettingsFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content, settingsPreferenceFragment).commit();

        sBillingProcessor = new BillingProcessor(this, null, this);
        sBillingProcessor.loadOwnedPurchasesFromGoogle();
    }

    @Override
    public void onBillingInitialized() {
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        sBillingProcessor.consumePurchase(productId);
        donate(this, productId);
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        Toast.makeText(this, getResources().getString(R.string.settings_donation_error), Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onPurchaseHistoryRestored() {
    }
}
