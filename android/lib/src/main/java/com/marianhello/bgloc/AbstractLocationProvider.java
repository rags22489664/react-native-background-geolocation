/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.marianhello.bgloc;

import android.annotation.TargetApi;
import android.content.Context;
import android.location.Location;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.BatteryManager;
import android.telephony.CellInfoGsm;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.TelephonyManager;

import com.marianhello.bgloc.data.BackgroundLocation;
import com.marianhello.cordova.JSONErrorFactory;

import org.json.JSONObject;

/**
 * AbstractLocationProvider
 */
public abstract class AbstractLocationProvider implements LocationProvider {
    private static final int PERMISSION_DENIED_ERROR_CODE = 2;

    protected static enum Tone {
        BEEP,
        BEEP_BEEP_BEEP,
        LONG_BEEP,
        DOODLY_DOO,
        CHIRP_CHIRP_CHIRP,
        DIALTONE
    };

    protected Integer PROVIDER_ID;
    protected LocationService locationService;
    protected Location lastLocation;
    protected Config config;

    protected ToneGenerator toneGenerator;

    protected AbstractLocationProvider(LocationService locationService) {
        this.locationService = locationService;
        this.config = locationService.getConfig();
    }

    public void onCreate() {
        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
    }

    public void onDestroy() {
        toneGenerator.release();
        toneGenerator = null;
    }

    /**
     * Register broadcast reciever
     * @param receiver
     */
    public Intent registerReceiver (BroadcastReceiver receiver, IntentFilter filter) {
        return locationService.registerReceiver(receiver, filter);
    }

    /**
     * Unregister broadcast reciever
     * @param receiver
     */
    public void unregisterReceiver (BroadcastReceiver receiver) {
        locationService.unregisterReceiver(receiver);
    }

    /**
     * Handle location as recorder by provider
     * @param location
     */
    public void handleLocation (Location location) {
        locationService.handleLocation(updateBackgroundLocation(new BackgroundLocation(PROVIDER_ID, location)));
    }

    /**
     * Handle stationary location with radius
     *
     * @param location
     * @param radius radius of stationary region
     */
    public void handleStationary (Location location, float radius) {
        locationService.handleStationary(updateBackgroundLocation(new BackgroundLocation(PROVIDER_ID, location, radius)));
    }

    /**
     * Handle stationary location without radius
     *
     * @param location
     */
    public void handleStationary (Location location) {
        locationService.handleStationary(updateBackgroundLocation(new BackgroundLocation(PROVIDER_ID, location)));
    }

    /**
     * Handle security exception
     * @param exception
     */
    public void handleSecurityException (SecurityException exception) {
        JSONObject error = JSONErrorFactory.getJSONError(PERMISSION_DENIED_ERROR_CODE, exception.getMessage());
        locationService.handleError(error);
    }

    /**
     * Plays debug sound
     * @param name tone
     */
    protected void startTone(Tone name) {
        if (toneGenerator == null) return;

        int tone = 0;
        int duration = 1000;

        switch (name) {
            case BEEP:
                tone = ToneGenerator.TONE_PROP_BEEP;
                break;
            case BEEP_BEEP_BEEP:
                tone = ToneGenerator.TONE_CDMA_CONFIRM;
                break;
            case LONG_BEEP:
                tone = ToneGenerator.TONE_CDMA_ABBR_ALERT;
                break;
            case DOODLY_DOO:
                tone = ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE;
                break;
            case CHIRP_CHIRP_CHIRP:
                tone = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD;
                break;
            case DIALTONE:
                tone = ToneGenerator.TONE_SUP_RINGTONE;
                break;
        }

        toneGenerator.startTone(tone, duration);
    }

    private void updateBatteryLevel(BackgroundLocation location) {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if(level != -1 && scale != -1) {
            location.setBatteryLevel(new Float(((float)level / (float)scale) * 100.0f).intValue());
        }
    }

    @TargetApi(17)
    private void updateSignalStrength(BackgroundLocation location) {
        TelephonyManager telephonyManager = (TelephonyManager)locationService.getSystemService(Context.TELEPHONY_SERVICE);
        CellInfoGsm cellinfogsm = (CellInfoGsm)telephonyManager.getAllCellInfo().get(0);
        CellSignalStrengthGsm cellSignalStrengthGsm = cellinfogsm.getCellSignalStrength();
        location.setSignalStrength(cellSignalStrengthGsm.getDbm());
        location.setDeviceId(telephonyManager.getDeviceId());
    }

    private void updateDeviceInfo(BackgroundLocation location) {
        location.setDeviceManufacturer(android.os.Build.MANUFACTURER);
        location.setDeviceModel(android.os.Build.MODEL);
    }

    private BackgroundLocation updateBackgroundLocation(BackgroundLocation location) {
        updateBatteryLevel(location);
        updateSignalStrength(location);
        updateDeviceInfo(location);
        return location;
    }
}
