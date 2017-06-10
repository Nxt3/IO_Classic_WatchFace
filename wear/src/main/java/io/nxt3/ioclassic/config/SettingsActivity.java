package io.nxt3.ioclassic.config;


import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;

import io.nxt3.ioclassic.R;

public class SettingsActivity  extends PreferenceActivity implements BillingProcessor.IBillingHandler {
    private BillingProcessor mBillingProcessor;

    public void donate(Activity activity, String productId) {
        mBillingProcessor.purchase(activity, productId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new SettingsFragment()).commit();

        mBillingProcessor = new BillingProcessor(this, null, this);
        mBillingProcessor.loadOwnedPurchasesFromGoogle();
    }

    @Override
    public void onBillingInitialized() {
        //nothing
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        mBillingProcessor.consumePurchase(productId);
        donate(this, productId);
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        Toast.makeText(this, getResources().getString(R.string.settings_donation_error), Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onPurchaseHistoryRestored() {
        //nothing
    }

    @Override
    public void onDestroy() {
        if (mBillingProcessor != null) {
            mBillingProcessor.release();
        }

        super.onDestroy();
    }
}
