package com.atmko.onmywatch;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.FeatureType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.atmko.onmywatch.adapters.SkuDetailsAdapter;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BillingActivity extends AppCompatActivity implements
        SkuDetailsAdapter.OnListItemClickListener,
        SkuDetailsAdapter.OnCheckPurchaseStateListener,
        PurchasesUpdatedListener, BillingClientStateListener {
    private static final String TAG = BillingActivity.class.getSimpleName();

    private BillingClient mBillingClient;
    private SkuDetailsAdapter mAdapter;
    private List<String> purchasedSkus;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        defineViews();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;

        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void startBillingClient() {
        if (mBillingClient != null) {
            if (mBillingClient.isReady()) {
                queryPurchases();

            } else {
                mBillingClient.startConnection(this);
            }
        } else {
            mBillingClient = BillingClient.newBuilder(this)
                    .enablePendingPurchases()
                    .setListener(this).build();
            mBillingClient.startConnection(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBillingClient();
    }

    private void defineViews() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(configureLayoutManager());
        mAdapter = new SkuDetailsAdapter(this, this);
        recyclerView.setAdapter(mAdapter);
    }

    private LinearLayoutManager configureLayoutManager() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    @Override
    public void onItemClick(int position) {
        SkuDetails skuDetails = mAdapter.getAdapterData().get(position);
        if (skuDetails.getType().equals(BillingClient.SkuType.SUBS)) {
            int supportResponseCode = mBillingClient.isFeatureSupported(FeatureType.SUBSCRIPTIONS).getResponseCode();
            boolean isSupported = supportResponseCode == BillingClient.BillingResponseCode.OK;
            if (!isSupported) {
                showSnackBarMessage("Not supported. Update Google Play Store");
                return;
            }
        }

        // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build();
        mBillingClient.launchBillingFlow(BillingActivity.this, flowParams);
    }

    @Override
    public void onPurchaseStateCheck(String sku, AppCompatCheckBox checkBox) {
        if (purchasedSkus.contains(sku)) {
            checkBox.setVisibility(View.VISIBLE);

        } else {
            checkBox.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBillingSetupFinished(final BillingResult billingResult) {
        //if connection successful
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            queryPurchases();
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        mBillingClient.startConnection(this);
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                MasterActivity.handlePurchase(purchase, this);
            }

        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            showSnackBarMessage(billingResult.getDebugMessage());
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            showSnackBarMessage(billingResult.getDebugMessage());
            // Handle any other error codes.
        }
    }

    //get purchases made by user
    //ensures app values match purchases
    public void queryPurchases() {
        Log.d(TAG, "fetch user's purchases");
        //get in app purchases to check for unverified, unacknowledged and pending transactions
        Purchase.PurchasesResult inAppResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
        List<Purchase> inAppPurchases = inAppResult.getPurchasesList();

        if (inAppResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && inAppPurchases != null) {
            //update purchased skus list for comparison
            purchasedSkus = new ArrayList<>();
            for (Purchase purchase : inAppPurchases) {
                purchasedSkus.add(purchase.getSku());
            }
        } else {
            showSnackBarMessage(inAppResult.getBillingResult().getDebugMessage());
        }

        populateAdapter();
    }

    private void populateAdapter() {
        mAdapter.getAdapterData().clear();
        // The BillingClient is ready. You can query purchases here.
        mBillingClient.querySkuDetailsAsync(getInappSkuDetailsParams(), new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetails) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                        && skuDetails != null) {
                    mAdapter.addAdapterData(skuDetails);
                    mAdapter.notifyDataSetChanged();

                } else {
                    showSnackBarMessage(billingResult.getDebugMessage());
                }
            }
        });

        mBillingClient.querySkuDetailsAsync(getTestSkuDetailsParams(), new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetails) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                        && skuDetails != null) {
                    mAdapter.addAdapterData(skuDetails);
                    mAdapter.notifyDataSetChanged();

                } else {
                    showSnackBarMessage(billingResult.getDebugMessage());
                }
            }
        });
    }

    public SkuDetailsParams getInappSkuDetailsParams() {
        List<String> skuList = Arrays.asList(getResources().getStringArray(R.array.inapp_skus));
        SkuDetailsParams.Builder skuDetailsParamsBuilder = SkuDetailsParams.newBuilder();
        skuDetailsParamsBuilder.setSkusList(skuList);
        skuDetailsParamsBuilder.setType(BillingClient.SkuType.INAPP);
        return skuDetailsParamsBuilder.build();
    }

    public SkuDetailsParams getTestSkuDetailsParams() {
        List<String> skuList = Arrays.asList(getResources().getStringArray(R.array.test_purchase_skus));
        SkuDetailsParams.Builder skuDetailsParamsBuilder = SkuDetailsParams.newBuilder();
        skuDetailsParamsBuilder.setSkusList(skuList);
        skuDetailsParamsBuilder.setType(BillingClient.SkuType.INAPP);
        return skuDetailsParamsBuilder.build();
    }

    private void showSnackBarMessage(String string) {
        if (string == null || string.equals("")) return;
        Snackbar.make(findViewById(R.id.top_layout), string, Snackbar.LENGTH_LONG).show();
    }
}
