package top.trumeet.mipushframework;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.Nullable;

import com.nihility.Global;
import com.nihility.InternalMessenger;
import com.nihility.service.XMPushServiceListener;
import com.xiaomi.channel.commonutils.android.DeviceInfo;
import com.xiaomi.channel.commonutils.android.MIUIUtils;
import com.xiaomi.push.service.XMPushServiceMessenger;
import com.xiaomi.smack.ConnectionConfiguration;

public class MainPageUtils {
    private static final String TAG = MainPageUtils.class.getSimpleName();
    InternalMessenger messenger;

    public interface ConnectionStatusChanged {
        /**
         * @param status the connection status, null when the service reported a value we do not know.
         * @param host   the host of the live connection, null when there is no connection.
         */
        void onChange(@Nullable XMPushServiceListener.ConnectionStatus status, @Nullable String host);
    }

    public MainPageUtils() {
    }

    public void initOnCreate(Context context, ConnectionStatusChanged connectionStatusChanged) {
        context = context.getApplicationContext();
        messenger = new InternalMessenger(context) {{
            register(new IntentFilter(XMPushServiceMessenger.IntentSetConnectionStatus));
            addListener(intent -> {
                connectionStatusChanged.onChange(parseStatus(intent.getStringExtra("status")),
                        intent.getStringExtra("host"));
            });
        }};

        printHookResultForCheck();

        Global.ConfigCenter().loadConfigurations(context);

        messenger.send(new Intent(XMPushServiceMessenger.IntentGetConnectionStatus));
    }

    /**
     * {@link XMPushServiceMessenger#getDesc(int)} also reports "unknown", which is not an enum constant.
     */
    private static @Nullable XMPushServiceListener.ConnectionStatus parseStatus(@Nullable String status) {
        if (status == null) {
            return null;
        }
        try {
            return XMPushServiceListener.ConnectionStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Unrecognized connection status: " + status);
            return null;
        }
    }

    void printHookResultForCheck() {
        Log.i(TAG, String.format("[hook_res] MIUIUtils.getIsMIUI() -> [%s]", MIUIUtils.getIsMIUI()));
        Log.i(TAG, String.format("[hook_res] DeviceInfo.quicklyGetIMEI() -> [%s]", DeviceInfo.quicklyGetIMEI(null)));
        Log.i(TAG, String.format("[hook_res] DeviceInfo.getMacAddress() -> [%s]", DeviceInfo.getMacAddress(null)));
        Log.i(TAG, String.format("[hook_res] ConnectionConfiguration.getXmppServerHost() -> [%s]", ConnectionConfiguration.getXmppServerHost()));
    }
}
