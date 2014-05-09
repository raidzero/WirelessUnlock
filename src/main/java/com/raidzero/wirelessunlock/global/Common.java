package com.raidzero.wirelessunlock.global;

import android.util.Log;
import com.raidzero.wirelessunlock.activities.MainActivity;

/**
 * Created by raidzero on 5/8/14 9:42 AM
 */
public class Common {
    public static final String tag = "WirelessUnlock/Common";
    public static final String messageIntent = "com.raidzero.wirelessunlock.SERVICE_MESSAGE";

    // activity request codes
    public static final int addWifiRequestCode = 1001;
    public static final int addBluetoothRequestCode = 1002;

    public static AppHelper getAppHelper() {
        if (MainActivity.appHelper != null) {
            return MainActivity.appHelper;
        } else {
            // THIS DOES NOT WORK
            Log.d(tag, "Activity not started, returning new appHelper");
            return new AppHelper();
        }
    }
}