package test.com.nihility.service.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.nihility.service.PushServiceTimeline;
import com.nihility.service.ServiceTimelineAbility;
import com.nihility.service.XMPushServiceListener.ConnectionStatus;

import org.junit.Test;

/**
 * {@link PushServiceTimeline} holds process-wide state with no reset hook, so each case sets the
 * status it depends on first instead of assuming where the previous one left things.
 */
public class ServiceTimelineAbilityTest {

    private final ServiceTimelineAbility ability = new ServiceTimelineAbility();

    @Test
    public void recordsWhenConnectionIsEstablished() {
        ability.connectionStatusChanged(ConnectionStatus.disconnected);
        ability.connectionStatusChanged(ConnectionStatus.connecting);

        assertNull(PushServiceTimeline.INSTANCE.getConnectedSince());

        ability.connectionStatusChanged(ConnectionStatus.connected);

        assertNotNull(PushServiceTimeline.INSTANCE.getConnectedSince());
    }

    @Test
    public void keepsTheFirstTimestampWhileStillConnected() {
        ability.connectionStatusChanged(ConnectionStatus.disconnected);
        ability.connectionStatusChanged(ConnectionStatus.connected);
        Long established = PushServiceTimeline.INSTANCE.getConnectedSince();

        // The SDK reports the same status repeatedly; the count must not restart on each one.
        ability.connectionStatusChanged(ConnectionStatus.connected);
        ability.connectionStatusChanged(ConnectionStatus.connected);

        assertEquals(established, PushServiceTimeline.INSTANCE.getConnectedSince());
    }

    @Test
    public void recordsServiceStart() {
        ability.created();

        assertNotNull(PushServiceTimeline.INSTANCE.getServiceStartedAt());
    }
}
