package com.example.mborper.breathbetter;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class BeaconListeningService extends IntentService {
    private static final String LOG_TAG = ">>>>";

    private long waitTime = 10000;

    private boolean keepRunning = true;

    // Constructor must call the super with a name for the worker thread
    public BeaconListeningService() {
        super("BeaconListeningServiceWorkerThread");

        Log.d(LOG_TAG, "BeaconListeningService: constructor ends");
    }

    public void stopServiceManually() {

        Log.d(LOG_TAG, "BeaconListeningService.stopServiceManually()");

        if (!this.keepRunning) {
            return;
        }

        this.keepRunning = false;
        this.stopSelf();

        Log.d(LOG_TAG, "BeaconListeningService.stopServiceManually(): ends");
    }

    // cleanup resources and stop service
    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "BeaconListeningService.onDestroy()");

        this.stopServiceManually(); // Might not be needed, as stopService() should stop the service and its worker thread.
        super.onDestroy(); // to ensure proper cleanup
    }

    /**
     * Handles the intent in the background worker thread.
     *
     * @param intent The intent that started the service.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        // ERROR MAY PRODUCE NULL POINTER EXCEPTION
        this.waitTime = intent.getLongExtra("waitTime", /* default */ 50000);
        this.keepRunning = true;

        // This runs in a WORKER THREAD!
        long counter = 1;

        Log.d(LOG_TAG, "BeaconListeningService.onHandleIntent: starts: thread=" + Thread.currentThread().getId());

        try {
            while (this.keepRunning) {
                // WARNING CALL TO SLEEP IN A LOOP, PROBABLY BUSY WAITING
                Thread.sleep(waitTime);
                Log.d(LOG_TAG, "BeaconListeningService.onHandleIntent: after waiting: " + counter);
                counter++;
            }

            Log.d(LOG_TAG, "BeaconListeningService.onHandleIntent: task completed (after while loop)");

        } catch (InterruptedException e) {
            // Restore interrupt status.
            Log.d(LOG_TAG, "BeaconListeningService.onHandleIntent: issue with thread");
            Thread.currentThread().interrupt();
        }

        Log.d(LOG_TAG, "BeaconListeningService.onHandleIntent: ends");
    }
} // class
