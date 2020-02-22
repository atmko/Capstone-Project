package com.atmko.onmywatch;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BillingActivity extends AppCompatActivity implements
        SkuDetailsAdapter.OnListItemClickListener,
        PurchasesUpdatedListener, BillingClientStateListener {
    private static final String TAG = BillingActivity.class.getSimpleName();

    BillingClient mBillingClient;
    SkuDetailsAdapter mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing);

        defineViews();
        setValues(savedInstanceState);
        mBillingClient.startConnection(this);
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

    private void setValues(Bundle savedInstanceState) {
        //if first init setup values, else restore values
        if (savedInstanceState == null) {
            mBillingClient = BillingClient.newBuilder(this)
                    .enablePendingPurchases()
                    .setListener(this).build();
        }
    }

    public SkuDetailsParams getSkuDetailsParams() {
        List<String> skuList = Arrays.asList(getResources().getStringArray(R.array.skus));
        SkuDetailsParams.Builder skuDetailsParamsBuilder = SkuDetailsParams.newBuilder();
        skuDetailsParamsBuilder.setSkusList(skuList);
        return skuDetailsParamsBuilder.build();
    }

    @Override
    public void onItemClick(int position) {
        SkuDetails skuDetails = mAdapter.getAdapterData().get(position);
        if (skuDetails.getType().equals(BillingClient.SkuType.SUBS)) {
            int supportResponseCode = mBillingClient.isFeatureSupported(FeatureType.SUBSCRIPTIONS).getResponseCode();
            boolean isSupported = supportResponseCode == BillingClient.BillingResponseCode.OK;
            if (!isSupported) {
                Snackbar.make(findViewById(R.id.top_layout), "Subscriptions not supported. Update Google Play Store",
                        Snackbar.LENGTH_LONG);
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
    public void onBillingSetupFinished(BillingResult billingResult) {
        //if connection successful
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            // The BillingClient is ready. You can query purchases here.
            mBillingClient.querySkuDetailsAsync(getSkuDetailsParams(), new SkuDetailsResponseListener() {
                @Override
                public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetails) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                            && skuDetails != null) {
                        mAdapter.addAdapterData(skuDetails);

                    } else {
                        Snackbar.make(findViewById(R.id.top_layout), billingResult.getDebugMessage(),
                                Snackbar.LENGTH_LONG).show();
                    }

                    ArrayList<SkuDetails> testSkuDetailsList = new ArrayList<>();
                    String[] testPurchaseStrings = getResources().getStringArray(R.array.test_purchases);
                    for (String jsonString: testPurchaseStrings) {
                        try {
                            SkuDetails testSkuDetail = new SkuDetails(jsonString);
                            testSkuDetailsList.add(testSkuDetail);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    mAdapter.addAdapterData(testSkuDetailsList);
                }
            });

            //get in app purchases to check for unverified, unacknowledged and pending transactions
            Purchase.PurchasesResult inAppResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
            List<Purchase> inAppPurchases = inAppResult.getPurchasesList();

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && inAppPurchases != null) {
                for (Purchase purchase : inAppPurchases) {
                    handlePurchase(purchase);
                }

            } else {
                Snackbar.make(findViewById(R.id.top_layout), billingResult.getDebugMessage(),
                        Snackbar.LENGTH_LONG);
            }

            //get subs purchases to check for unverified, unacknowledged and pending transactions
            Purchase.PurchasesResult subsResult = mBillingClient.queryPurchases(BillingClient.SkuType.SUBS);
            List<Purchase> subsPurchases = subsResult.getPurchasesList();

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && subsPurchases != null) {
                for (Purchase purchase : subsPurchases) {
                    handlePurchase(purchase);
                }

            } else {
                Snackbar.make(findViewById(R.id.top_layout), billingResult.getDebugMessage(),
                        Snackbar.LENGTH_LONG);
            }
        }
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }

        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            // Handle any other error codes.
        }
    }

    void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            // Grant entitlement to the user.
            verifyPurchase(purchase.getPurchaseToken());

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
                            }
                        });
            }
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            //inform user of steps to complete purchase
        }
    }

    private void verifyPurchase(String purchaseToken) {
        String[] credentials = new String[2];
        credentials[0] = purchaseToken;
        credentials[1] = MasterActivity.getCurrentUser().getUid();
        FirebaseFunctions.getInstance().getHttpsCallable("verifyPurchase").call(credentials)
                .addOnSuccessListener(new OnSuccessListener<HttpsCallableResult>() {
                    @Override
                    public void onSuccess(HttpsCallableResult httpsCallableResult) {
                        Log.d(TAG, "Purchase Verified");
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Purchase Verification Failed");
            }
        });
    }

    @Override
    public void onBillingServiceDisconnected() {
        mBillingClient.startConnection(this);
    }
}
