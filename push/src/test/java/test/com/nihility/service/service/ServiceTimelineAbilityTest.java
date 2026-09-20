package test.com.nihility.service.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.nihility.service.PushServiceTimeline;
import com.nihility.service.ServiceTimelineAbility;

import org.junit.Test;

/**
 * {@link PushServiceTimeline} holds process-wide state with no reset hook, so a case that needs a
 * particular starting point has to establish it rather than assume what the previous one left.
 */
public class ServiceTimelineAbilityTest {

    private final ServiceTimelineAbility ability = new ServiceTimelineAbility();

    @Test
    public void recordsServiceStart() {
        ability.created();

        assertNotNull(PushServiceTimeline.INSTANCE.getServiceStartedAt());
    }

    /**
     * The dashboard shows how long the *current* service has been up, so a service created later
     * in the same process must move the anchor forward rather than keep the stale one.
     */
    @Test
    public void aLaterServiceCreationMovesTheAnchor() throws InterruptedException {
        ability.created();
        Long first = PushServiceTimeline.INSTANCE.getServiceStartedAt();

        Thread.sleep(5);
        ability.created();
        Long second = PushServiceTimeline.INSTANCE.getServiceStartedAt();

        assertTrue("second creation should be at or after the first",
                second >= first);
    }
}
