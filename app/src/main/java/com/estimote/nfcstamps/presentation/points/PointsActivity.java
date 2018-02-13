package com.estimote.nfcstamps.presentation.points;

import android.content.Context;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.coresdk.recognition.utils.DeviceId;
import com.estimote.nfcstamps.NetworkService;
import com.estimote.nfcstamps.R;
import com.estimote.nfcstamps.SharedPrefHelper;
import com.estimote.nfcstamps.Utils;

import java.io.IOException;
import java.nio.charset.Charset;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PointsActivity extends AppCompatActivity {

    private Context mContext;

    //@Bind(R.id.activity_points_stamps_left)
    //TextView stampsLeftInfo;
    private SharedPrefHelper sharedPrefHelper;

    private NetworkService networkService;
    private String token;
    private String friendlyName;
    private String identity;

    @Bind(R.id.current_name)
    TextView mCurrentName;

    @Bind(R.id.current_mac)
    TextView mCurrentMac;

    @Bind(R.id.input_name)
    EditText mInputName;

    @Bind(R.id.modify_name)
    Button mModifyName;

    private static final String TAG = PointsActivity.class.getName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_points);
        sharedPrefHelper = new SharedPrefHelper(this);
        sharedPrefHelper.increaseStampAmount();
        ButterKnife.bind(this);
        setStampsLeftInfo();
        networkService = new NetworkService();
    }

    private void setStampsLeftInfo() {
        int stampsLeft = Utils.STAMPS_AMOUNT - sharedPrefHelper.getStampAmountValue();
        //stampsLeftInfo.setText(stampsLeft <= 0 ? getString(R.string.activity_points_stamps_collected) : getString(R.string.activity_points_stamps_left, stampsLeft));
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

    public void refreshToken(final String oldToken) {
        Log.d(TAG, "refreshToken()");
        new Thread(new Runnable() {
            @Override
            public void run() {
                token = networkService.refreshToken(oldToken);
                Log.d(TAG, "token=" + token);
            }
        }).start();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        fetchToken();

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                for (int i = 0; i < rawMsgs.length; i++) {
                    NdefMessage msg = (NdefMessage) rawMsgs[i];
                    Log.d(TAG, "msg[" + i + "]=" + msg.toString());
                    DeviceId beaconId = findBeaconId(msg);
                    if (beaconId != null) {
                        Log.d(TAG, "beaconId=" + beaconId.toString());
                    }
                }
            }
        }
    }

    private DeviceId findBeaconId(NdefMessage msg) {
        NdefRecord[] records = msg.getRecords();
        for (NdefRecord record : records) {
            Log.d(TAG, "    record=" + record.toString());
            if (record.getTnf() == NdefRecord.TNF_EXTERNAL_TYPE) {
                String type = new String(record.getType(), Charset.forName("ascii"));
                if ("estimote.com:id".equals(type)) {
                    DeviceId deviceId = DeviceId.fromBytes(record.getPayload());
                    identity = deviceId.toHexString();
                    mCurrentName.setText("ID: " + identity);
                    Log.d(TAG, "ID=" + deviceId.toHexString());
                }
                if ("estimote.com:mac".equals(type)) {
                    byte[] mac = record.getPayload();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                    }
                    mCurrentMac.setText("MAC: " + sb.toString());
                    Log.d(TAG, "MAC=" + sb.toString());
                }
            }
        }
        return null;
    }

    private void changeNameAction(final Request request) {
        System.out.println("Request: " + request.toString());
        OkHttpClient client = networkService.buildClient();
        System.out.println("Request execute ...");

        //Response response = client.newCall(request).execute();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "onFailure: " + e.getMessage());
                promptMessage("Failed to change the friendly name!");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    promptMessage("onResponse: Succeed to change the friendly name!");
                    updateFriendlyName();
                } else {
                    promptMessage("onResponse: code=" + response.code() + ", refresh token ...");
                    refreshToken(token);
                }
            }
        });
    }

    private void updateFriendlyName() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String result = "ID: " + identity + "\nName: " + friendlyName;
                mCurrentName.setText(result);
                mInputName.setHint("Input Friendly Name");
            }
        });
    }

    @OnTextChanged(R.id.input_name)
    protected void onTextChanged(CharSequence text) {
        friendlyName = text.toString();
        Log.d(TAG, "onTextChanged(): name=" + friendlyName);
    }

    @OnClick(R.id.modify_name)
    protected void onClick() {
        if (token != null && friendlyName != null && !friendlyName.equals("")) {
            Request request = networkService.changeNameRequest(friendlyName.trim(), identity, token);
            changeNameAction(request);
        } else if (token == null) {
            fetchToken();
        }
    }

    private void promptMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
