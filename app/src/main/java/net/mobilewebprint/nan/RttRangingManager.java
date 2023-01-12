package net.mobilewebprint.nan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.Executor;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

class RttRangingManager {

    String TAG="RttRangingManager";
    private final Executor mainExecutor;
    private final WifiRttManager rttManager;
    private Disposable rangingDisposable;
    Context mContext;
    @SuppressLint("WrongConstant")
    RttRangingManager(final Context context) {
        rttManager = (WifiRttManager) context.getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        mainExecutor = context.getMainExecutor();
        mContext = context;
    }


    public Single<List<RangingResult>> startRanging(
            @NonNull final ScanResult scanResult) {
        return Single.create(emitter -> {
            final RangingRequest request = new RangingRequest.Builder()
                    .addAccessPoint(scanResult)
                    .build();
            final RangingResultCallback callback = new RangingResultCallback() {
                @Override
                public void onRangingFailure(final int i) {
                    emitter.onError(new RuntimeException("The WiFi-Ranging failed with error code: " + i));
                }

                @Override
                public void onRangingResults(final List<RangingResult> list) {
                    emitter.onSuccess(list);
                }
            };
            rttManager.startRanging(request, mainExecutor, callback);
        });
    }


    public Single<List<RangingResult>> startRTTRanging(
            @NonNull final PeerHandle peerHandle) {
        return Single.create(emitter -> {
            final RangingRequest request = new RangingRequest.Builder()
                    .addWifiAwarePeer(peerHandle)
                    .build();
            final RangingResultCallback callback = new RangingResultCallback() {
                @Override
                public void onRangingFailure(final int i) {
                    emitter.onError(new RuntimeException("The WiFi-Ranging failed with error code: " + i));
                }

                @Override
                public void onRangingResults(final List<RangingResult> list) {
                    emitter.onSuccess(list);
                }
            };
            rttManager.startRanging(request, mainExecutor, callback);
        });
    }

    public void startGetDistance(@NonNull final PeerHandle peerHandle){
        rangingDisposable = startRTTRanging(peerHandle)
//                .repeat()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::writeOutput,
                        throwable -> {
                    Log.e(TAG, throwable.getMessage());
//                            Timber.e(throwable, "An unexpected error occurred while start ranging.");
//                            Snackbar.make(logView, throwable.getMessage(), Snackbar.LENGTH_LONG).show();
                        });
    }


    private void writeOutput(@NonNull final List<RangingResult> result) {
        if (result.isEmpty()) {
        Log.d(TAG,"EMPTY ranging result received.");
            return;
        }
        for (RangingResult res : result) {
            if(res.getStatus()== RangingResult.STATUS_FAIL){
                Log.e(TAG," getDistanceMm(): invoked on an invalid result: getStatus()=1");

                 ((FuseWifiAwareRTTActivity) mContext).displayToast("getDistanceMm(): invoked on an invalid result: getStatus()=1");
            }else {
                Log.d(TAG, res.getDistanceMm() + " is distance");
                ((TextView) ((MainActivity) mContext).findViewById(R.id.distance)).setText("  distance: " + res.getDistanceMm());
            }
//            Timber.d("Result: %d RSSI: %d Distance: %d mm", res.getRangingTimestampMillis(), res.getRssi(), res
//                    .getDistanceMm());
        }
    }

    private String buildLogString(final RangingResult result) {
        String resultString = mContext.getString(R.string.log, result.getRangingTimestampMillis(), result.getRssi(), result
                .getDistanceMm(), "");
        if (resultString.length() > 5000) {
            return resultString.substring(0, 5000);
        }
        return resultString;
    }

}
