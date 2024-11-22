# HowsAir Android Application

## Description

HowsAir is an organization focused on improving air quality awareness by providing real-time data on air pollution. The Android application, which includes the BeaconListeningService, is part of a larger system consisting of an Arduino beacon transmitter and a web platform. The BeaconListeningService scans for Bluetooth Low Energy (BLE) beacons transmitted by air quality sensors and processes the received data (ozone concentration, temperature) to provide real-time information on air quality.

This service is a crucial part of the Android app, responsible for continuously scanning and posting measurements to the database while the app runs in the background.

## Code Examples

Starting the BLE scan in the service:

```java
Copiar código
/**
     * Manages the intervals between scanning and non scanning periods
     */
    private final Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            if (!keepRunning) {
                return;
            }

            startScan();

            // Schedule scan stop after SCAN_PERIOD
            serviceHandler.postDelayed(() -> {
                stopScan();

                if (keepRunning) {
                    //Log.d(LOG_TAG, "Scheduling next scan cycle");
                    serviceHandler.postDelayed(scanRunnable, SCAN_INTERVAL - SCAN_PERIOD);
                }
            }, SCAN_PERIOD);
        }
    };
```

Processing a BLE scan result:

```java
Copiar código
/**
     * Processes the result of a BLE device scan. If a device with the matching UUID obtained in onStartCommand()
     * is found, a new Measurement is created.
     * <p>
     *    ScanResult:result ---> processScanResult()
     *
     * @param result The result of the BLE scan containing device information.
     */
    private void processScanResult(ScanResult result) {
        IBeaconFrame tib = new IBeaconFrame(result.getScanRecord().getBytes());
        if (Utilities.bytesToString(tib.getUUID()).equals(targetDeviceUUID)) {
            // Get ppm
            Measurement newMeasurement = new Measurement();
            newMeasurement.setO3Value(Utilities.bytesToInt(tib.getMajor()));

            // Fetch the current location
            setMeasurementLocation(newMeasurement);

            //the date is set on the server logic
            if (!newMeasurement.equals(lastMeasurement)) {
                lastMeasurement = newMeasurement;
                if (measurementCallback != null) {
                    measurementCallback.onMeasurementReceived(newMeasurement);
                }

                gasAlertManager.checkAndAlert(newMeasurement.getO3Value());
            }
        }
```

## FAQ

Q: What permissions are required for the service?

The service requires BLUETOOTH_SCAN, BLUETOOTH_CONNECT, and ACCESS_FINE_LOCATION to function correctly.

Q: How frequently does the service scan for beacons?

By default, the service scans for BLE devices every second, waits for 10 seconds, and repeats the process. This can be adjusted by modifying the SCAN_PERIOD and SCAN_INTERVAL constants.

Q: Can the service run in the background?

Yes, the service runs as a foreground service with a persistent notification, which prevents it from being killed by the system.

## Testing

To verify that the BeaconListeningService works as intended:

1. Enable Bluetooth and location services on your Android device.
2. Run the Android application with the BeaconListeningService.
3. Use a BLE beacon transmitter (such as the Breeze from HowsAir) to broadcast air quality data.
4. Monitor the logs to confirm that the service successfully detects and processes the beacons.
5. The air quality data (e.g., ozone concentration and temperature) should appear in the app's UI or logs.

You can also test the fake logic running the classes on the test packages!

## Class Diagram

In the doc folder, there is a class diagram showing the interaction between the main components:

Feel free to modify any part of this as needed, and let me know if there's anything else
