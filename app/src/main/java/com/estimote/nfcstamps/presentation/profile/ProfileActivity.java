package com.estimote.nfcstamps.presentation.profile;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.estimote.coresdk.common.requirements.SystemRequirementsChecker;
import com.estimote.nfcstamps.NetworkService;
import com.estimote.nfcstamps.R;
import com.estimote.nfcstamps.SharedPrefHelper;

import butterknife.ButterKnife;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = ProfileActivity.class.getName();

    /*
    @Bind(R.id.activity_profile_recycler_stamp)
    RecyclerView stampCollectionView;
    @Bind(R.id.activity_profile_stamps_left_info)
    TextView stampsLeftInfo;
    */

    private StampCollectionAdapter stampCollectionAdapter;
    private SharedPrefHelper sharedPrefHelper;
    private NetworkService networkService;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
        ButterKnife.bind(this);
        //sharedPrefHelper = new SharedPrefHelper(this);
        //setupStampCollectionView();
        //networkService = new NetworkService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //updateStampsInfo();
        //fetchToken();
    }

    public void fetchToken() {
        Log.d(TAG, "fetchToken()");
        new Thread(new Runnable() {
            @Override
            public void run() {
                token = networkService.getToken();
                Log.d(TAG, "token=" + token);
            }
        }).start();
    }

    private void setupStampCollectionView() {
        //stampCollectionView.setLayoutManager(new GridLayoutManager(this, Utils.GRID_SPAN_COUNT, GridLayoutManager.VERTICAL, false));
        stampCollectionAdapter = new StampCollectionAdapter(this, 0);
        //stampCollectionView.setAdapter(stampCollectionAdapter);
    }

    private void updateStampsInfo() {
        int stampsLeft = sharedPrefHelper.getStampAmountValue();
        stampCollectionAdapter.update(stampsLeft);
        //setupStampLeftInfo(Utils.STAMPS_AMOUNT - stampsLeft);

    }

    /*
    private void setupStampLeftInfo(int stampsLeft) {
        if (stampsLeft <= 0) {
            stampsLeftInfo.setText(getString(R.string.activity_points_stamps_collected));
        } else {
            stampsLeftInfo.setText(getString(R.string.activity_points_stamps_left, stampsLeft));
        }
    }

    @OnClick(R.id.activity_profile_button)
    void onButtonClick() {
        if (sharedPrefHelper.getStampAmountValue() < Utils.STAMPS_AMOUNT) {
            Toast.makeText(this, getString(R.string.activity_profile_not_collected), Toast.LENGTH_LONG).show();
        } else {
            sharedPrefHelper.saveStampAmountValue(0);
            stampCollectionAdapter.update(sharedPrefHelper.getStampAmountValue());
        }
    }
    */
}
