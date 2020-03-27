package com.atmko.onmywatch;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.FeatureType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.atmko.onmywatch.adapters.SkuDetailsAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BillingActivity extends AppCompatActivity implements
        SkuDetailsAdapter.OnListItemClickListener,
        SkuDetailsAdapter.OnCheckPurchaseStateListener,
        PurchasesUpdatedListener, BillingClientStateListener {
    private static final String TAG = BillingActivity.class.getSimpleName();

    BillingClient mBillingClient;
    SkuDetailsAdapter mAdapter;
    Button inAppButton;
    List<String> purchasedSkus;

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
        setValues(savedInstanceState);
        mBillingClient.startConnection(this);
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

    @Override
    protected void onResume() {
        super.onResume();
        handleUncreditedPurchases();
    }

    private void defineViews() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(configureLayoutManager());
        mAdapter = new SkuDetailsAdapter(this, this);
        recyclerView.setAdapter(mAdapter);
        inAppButton = findViewById(R.id.inappButton);
    }

    private LinearLayoutManager configureLayoutManager() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        layoutManager.setOrientation(RecyclerView.VERTICAL);
        return layoutManager;
    }

    private void setValues(Bundle savedInstanceState) {
        //if first init setup values, else restore values
        if (savedInstanceState == null) {
            mBillingClient = BillingClient.newBuilder(this)
                    .enablePendingPurchases()
                    .setListener(this).build();
        }
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
        inAppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get in app purchases to check for unverified, unacknowledged and pending transactions
                Purchase.PurchasesResult inAppResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
                List<Purchase> inAppPurchases = inAppResult.getPurchasesList();

                if (inAppResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                        && inAppPurchases != null) {
                    if (inAppPurchases.size() == 0) showSnackBarMessage("no purchases");

                    for (Purchase purchase : inAppPurchases) {
                        ConsumeParams.Builder builder = ConsumeParams.newBuilder();
                        builder.setPurchaseToken(purchase.getPurchaseToken());
                        mBillingClient.consumeAsync(builder.build(), new ConsumeResponseListener() {
                            @Override
                            public void onConsumeResponse(BillingResult billingResult, String s) {
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    showSnackBarMessage("inapp cleared");

                                } else {
                                    showSnackBarMessage(billingResult.getDebugMessage());
                                }
                            }
                        });
                    }

                } else {
                    showSnackBarMessage(inAppResult.getBillingResult().getDebugMessage());
                }
            }
        });

        //if connection successful
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            handleUncreditedPurchases();

            // The BillingClient is ready. You can query purchases here.
            mBillingClient.querySkuDetailsAsync(getInappSkuDetailsParams(), new SkuDetailsResponseListener() {
                @Override
                public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetails) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                            && skuDetails != null) {
                        mAdapter.addAdapterData(skuDetails);

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

                    } else {
                        showSnackBarMessage(billingResult.getDebugMessage());
                    }
                }
            });
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
                handlePurchase(purchase);
            }

        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            showSnackBarMessage(billingResult.getDebugMessage());
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            showSnackBarMessage(billingResult.getDebugMessage());
            // Handle any other error codes.
        }
    }

    private void handleUncreditedPurchases() {
        Log.d(TAG, "handling uncredited purchases");
        //get in app purchases to check for unverified, unacknowledged and pending transactions
        Purchase.PurchasesResult inAppResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
        List<Purchase> inAppPurchases = inAppResult.getPurchasesList();

        if (inAppResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && inAppPurchases != null) {
            purchasedSkus = new ArrayList<>();

            for (Purchase purchase : inAppPurchases) {
                purchasedSkus.add(purchase.getSku());

                if (purchase.getSku().equals("pro_mode") && !MasterActivity.sIsProMode) {
                    Log.d(TAG, "handling pro_mode");
                    handlePurchase(purchase);

                } else if (purchase.getSku().equals("android.test.purchased")
                        && (!MasterActivity.sIsProMode || !MasterActivity.sAllowCloudBackup)) {
                    Log.d(TAG, "handling android.test.purchased");
                    handlePurchase(purchase);
                }
            }
        } else {
            showSnackBarMessage(inAppResult.getBillingResult().getDebugMessage());
        }
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

    void handlePurchase(final Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            // Acknowledge the purchase if it hasn't already been acknowledged.
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                mBillingClient.acknowledgePurchase(acknowledgePurchaseParams,
                        new AcknowledgePurchaseResponseListener() {
                            @Override
                            public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                                Log.d(TAG, "Purchase Acknowledged");
                                // Grant entitlement to the user.
                                verifyPurchase(purchase);
                            }
                        });
            } else {
                // Grant entitlement to the user.
                verifyPurchase(purchase);
            }

        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            //inform user of steps to complete purchase
        }
    }

    private void verifyPurchase(final Purchase purchase) {
        List<String> credentials = new ArrayList<>();
        credentials.add(purchase.getSku());
        credentials.add(purchase.getPurchaseToken());
        FirebaseFunctions.getInstance().getHttpsCallable("verifyPurchase").call(credentials)
                .addOnSuccessListener(new OnSuccessListener<HttpsCallableResult>() {
                    @Override
                    public void onSuccess(HttpsCallableResult httpsCallableResult) {
                        Map<String, String> results = ((Map<String, String>) httpsCallableResult.getData());
                        if (results.get("error") != null) {
                            showSnackBarMessage(results.get("error"));

                        } else if (results.get("status") != null) {
                            showSnackBarMessage("Purchase Verified");
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, e.getMessage());
                Log.d(TAG, "Purchase Verification Failed");
                showSnackBarMessage("sever down, purchase will complete when server available");
            }
        });
    }

    private void showSnackBarMessage(String string) {
        if (string == null || string.equals("")) return;
        Snackbar.make(findViewById(R.id.top_layout), string, Snackbar.LENGTH_LONG).show();
    }
}
