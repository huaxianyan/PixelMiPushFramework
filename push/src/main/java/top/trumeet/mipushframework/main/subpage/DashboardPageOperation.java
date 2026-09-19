package top.trumeet.mipushframework.main.subpage;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xiaomi.mipush.sdk.AppInfoHolder;
import com.xiaomi.xmsf.SettingUtils;

import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.db.EventDb;
import top.trumeet.mipush.provider.entities.Event;

/**
 * Collects everything the dashboard shows. Meant to run off the main thread.
 */
public class DashboardPageOperation {

    private static final long ONE_DAY_MS = 24L * 60L * 60L * 1000L;

    /**
     * Keys inside the preferences returned by {@link AppInfoHolder#getSharedPreferences(Context)}.
     * The SDK declares its own {@code PREF_KEY_*} constants private, these are their values.
     */
    private static final String PREF_KEY_DEVICE_ID = "devId";
    private static final String PREF_KEY_REG_ID = "regId";

    public static class DashboardInfo {
        public int registeredAppCount;
        /** 0 when no push has ever been received. */
        public long lastReceiveTime;
        public long recentPushCount;
        public @Nullable String storedXmppServer;
        public @Nullable String deviceId;
        public @Nullable String registrationId;
    }

    public static @NonNull DashboardInfo load(Context context) {
        DashboardInfo info = new DashboardInfo();
        info.registeredAppCount = EventDb.queryRegistered().registered.size();

        Event latest = EventDb.queryLatestReceivePush();
        info.lastReceiveTime = latest == null ? 0L : latest.getDate();

        info.recentPushCount = EventDb.countReceivePushSince(Utils.getUTC().getTime() - ONE_DAY_MS);

        info.storedXmppServer = SettingUtils.getXMPPServer(context);

        SharedPreferences preferences = AppInfoHolder.getSharedPreferences(context);
        info.deviceId = preferences.getString(PREF_KEY_DEVICE_ID, null);
        info.registrationId = preferences.getString(PREF_KEY_REG_ID, null);
        return info;
    }
}
