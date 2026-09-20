package top.trumeet.mipushframework.main.subpage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xiaomi.xmsf.SettingUtils;

import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.provider.db.EventDb;
import top.trumeet.mipush.provider.entities.Event;

/**
 * Collects everything the dashboard shows. Meant to run off the main thread.
 *
 * <p>The device and registration identifiers used to be read here and shown on the page. They are
 * gone: they identify the device rather than tell the user anything actionable, the dashboard is
 * the first screen anyone opens, and a screenshot would carry them off as-is. What replaced them
 * are the two durations, which say the same thing about the link's health without naming anything.
 */
public class DashboardPageOperation {

    private static final long ONE_DAY_MS = 24L * 60L * 60L * 1000L;

    public static class DashboardInfo {
        public int registeredAppCount;
        /** 0 when no push has ever been received. */
        public long lastReceiveTime;
        public long recentPushCount;
        public @Nullable String storedXmppServer;
    }

    public static @NonNull DashboardInfo load(Context context) {
        DashboardInfo info = new DashboardInfo();
        info.registeredAppCount = EventDb.queryRegistered().registered.size();

        Event latest = EventDb.queryLatestReceivePush();
        info.lastReceiveTime = latest == null ? 0L : latest.getDate();

        info.recentPushCount = EventDb.countReceivePushSince(Utils.getUTC().getTime() - ONE_DAY_MS);

        info.storedXmppServer = SettingUtils.getXMPPServer(context);
        return info;
    }
}
