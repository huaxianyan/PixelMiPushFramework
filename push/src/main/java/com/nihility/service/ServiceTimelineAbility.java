package com.nihility.service;

/**
 * Feeds {@link PushServiceTimeline} from the service lifecycle, so the dashboard can show how long
 * the service has been up.
 *
 * <p>The callback already existed and is already dispatched — {@code XMPushServiceAspect} calls
 * {@code created()} after {@code onCreate} — this only records the moment.
 */
public class ServiceTimelineAbility implements XMPushServiceListener {

    @Override
    public void created() {
        PushServiceTimeline.onServiceCreated();
    }
}
