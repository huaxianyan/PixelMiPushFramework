package com.nihility.service;

/**
 * Feeds {@link PushServiceTimeline} from the service lifecycle, so the dashboard can show how long
 * the service has been up and how long the current connection has held.
 *
 * <p>Both callbacks already existed and are already dispatched — {@code XMPushServiceAspect} calls
 * {@code created()} after {@code onCreate} and {@code connectionStatusChanged()} from
 * {@code Connection.setConnectionStatus} — this only records the moments.
 */
public class ServiceTimelineAbility implements XMPushServiceListener {

    @Override
    public void created() {
        PushServiceTimeline.onServiceCreated();
    }

    @Override
    public void connectionStatusChanged(ConnectionStatus connectionStatus) {
        PushServiceTimeline.onConnectionStatusChanged(connectionStatus);
    }
}
