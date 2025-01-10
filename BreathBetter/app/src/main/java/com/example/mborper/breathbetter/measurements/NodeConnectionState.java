package com.example.mborper.breathbetter.measurements;

import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.os.Handler;

/**
 * Manages the connection state of the node, including detecting connection loss,
 * attempting reconnections, and notifying listeners about the connection status.
 *
 * @author Alejandro Rosado
 * @since  2024-12-11
 * last edited: 2024-12-12
 */
public class NodeConnectionState {
    private static final String LOG_TAG = "NodeConnectionState";
    private static NodeConnectionState instance;

    private boolean wasPreviouslyDisconnected = false;

    /**
     * Enum representing the connection status of the node.
     */
    public enum ConnectionStatus {
        CONNECTED,
        DISCONNECTED,
        RECONNECTING
    }

    private ConnectionStatus currentStatus = ConnectionStatus.DISCONNECTED;
    private long lastValidMeasurementTimestamp = 0;
    private static final long CONNECTION_TIMEOUT_MS = 40000; // 40 segundos
    private int consecutiveConnectionAttempts = 0;
    private static final int MAX_RECONNECTION_ATTEMPTS = 3;

    private HandlerThread handlerThread;
    private Handler timeoutHandler;

    private ConnectionStatusListener statusListener;

    /**
     * Constructor for NodeConnectionState. Initializes the connection state
     * and sets up a thread to periodically check the connection timeout.
     */
    private NodeConnectionState() {
        // Inicializar HandlerThread
        handlerThread = new HandlerThread("ConnectionTimeoutThread");
        handlerThread.start();
        timeoutHandler = new Handler(handlerThread.getLooper());

        // Determinar el estado inicial en función de mediciones válidas
        lastValidMeasurementTimestamp = System.currentTimeMillis(); // Simula que inicia conectado
        currentStatus = ConnectionStatus.CONNECTED;

        startConnectionTimeoutCheck();
    }

    /**
     * Starts a periodic check for connection timeout.
     * This method runs every 40 seconds to check if the connection is still valid.
     */
    private void startConnectionTimeoutCheck() {
        timeoutHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkConnectionTimeout();
                timeoutHandler.postDelayed(this, CONNECTION_TIMEOUT_MS);
            }
        }, CONNECTION_TIMEOUT_MS);
    }

    /**
     * Checks if the connection has timed out by comparing the last valid measurement timestamp
     * with the current time. If the connection has timed out, it handles the connection loss.
     */
    private synchronized void checkConnectionTimeout() {
        long currentTime = System.currentTimeMillis();

        if (lastValidMeasurementTimestamp == 0 ||
                currentTime - lastValidMeasurementTimestamp > CONNECTION_TIMEOUT_MS) {
            if (currentStatus != ConnectionStatus.DISCONNECTED) {
                handleConnectionLoss();
            }
        }
    }

    /**
     * Returns the singleton instance of NodeConnectionState.
     *
     * @return The single instance of NodeConnectionState
     */
    public static synchronized NodeConnectionState getInstance() {
        if (instance == null) {
            instance = new NodeConnectionState();
        }
        return instance;
    }

    /**
     * Listener interface to notify when the connection status changes.
     */
    public interface ConnectionStatusListener {
        void onConnectionLost();
        void onConnectionRestored();
    }

    /**
     * Sets the listener to handle connection status changes.
     *
     * @param listener The listener that will handle connection status changes
     */
    public void setConnectionStatusListener(ConnectionStatusListener listener) {
        this.statusListener = listener;
    }

    /**
     * Triggers a connection lost notification.
     * This method is called when the connection is lost.
     */
    private void triggerConnectionLostNotification() {
        if (statusListener != null) {
            // Usar un Handler para asegurar que la notificación se muestre en el hilo principal
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    statusListener.onConnectionLost();
                }
            });
        }
    }

    /**
     * Triggers a reconnection notification.
     * This method is called when the node successfully reconnects.
     */
    private void triggerReconnectionNotification() {
        Log.i(LOG_TAG, "Reconnection notification triggered");
        if (statusListener != null) {
            statusListener.onConnectionRestored();
        }
        wasPreviouslyDisconnected = false;
    }

    /**
     * Centralized method for updating the connection state based on the validity of the measurement.
     * <p>
     * If the measurement is valid, it updates the connection status accordingly, either
     * reconnecting or confirming the connection.
     *
     * @param isValidMeasurement Indicates whether the received measurement is valid.
     */
    public synchronized void updateConnectionState(boolean isValidMeasurement) {
        long currentTime = System.currentTimeMillis();

        if (isValidMeasurement) {
            // Medición válida
            lastValidMeasurementTimestamp = currentTime;

            // Si estaba desconectado, intentar reconectar
            if (currentStatus == ConnectionStatus.DISCONNECTED) {
                wasPreviouslyDisconnected = true;
                attemptReconnection();
            } else {
                currentStatus = ConnectionStatus.CONNECTED;
                consecutiveConnectionAttempts = 0;
                wasPreviouslyDisconnected = false;
            }
        }
    }

    /**
     * Handles connection loss by setting the status to disconnected and notifying the listener.
     */
    private void handleConnectionLoss() {
        currentStatus = ConnectionStatus.DISCONNECTED;
        consecutiveConnectionAttempts = 0;
        triggerConnectionLostNotification();
    }

    /**
     * Attempts to reconnect the node. If the maximum number of reconnection attempts
     * is reached, the connection status is set to disconnected.
     */
    private void attemptReconnection() {
        consecutiveConnectionAttempts++;

        if (consecutiveConnectionAttempts <= MAX_RECONNECTION_ATTEMPTS) {
            currentStatus = ConnectionStatus.RECONNECTING;
            Log.i(LOG_TAG, "Attempting to reconnect. Attempt: " + consecutiveConnectionAttempts);
            triggerReconnectionNotification();
        } else {
            handleConnectionLoss();
        }
    }

    /**
     * Cleans up resources used by the connection state handler, including quitting the handler thread.
     */
    public void cleanup() {
        if (handlerThread != null) {
            handlerThread.quitSafely();
        }
    }

    /**
     * Forcefully resets the connection state, setting it to connected and clearing any previous attempts.
     */
    public synchronized void forceResetConnectionState() {
        currentStatus = ConnectionStatus.CONNECTED;
        lastValidMeasurementTimestamp = System.currentTimeMillis();
        consecutiveConnectionAttempts = 0;
        Log.d(LOG_TAG, "Connection state forcefully reset");
    }

    /**
     * Retrieves the current connection status.
     *
     * @return The current connection status.
     */
    public synchronized ConnectionStatus getConnectionStatus() {
        return currentStatus;
    }

    /**
     * Checks if the node is currently connected.
     *
     * @return true if the node is connected, false otherwise.
     */
    public synchronized boolean isConnected() {
        return currentStatus == ConnectionStatus.CONNECTED;
    }
}