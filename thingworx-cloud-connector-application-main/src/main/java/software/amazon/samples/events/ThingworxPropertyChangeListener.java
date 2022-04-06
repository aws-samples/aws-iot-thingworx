package software.amazon.samples.events;

import com.thingworx.communications.client.things.VirtualThingPropertyChangeEvent;
import com.thingworx.communications.client.things.VirtualThingPropertyChangeListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.samples.deviceShadow.ThingworxPropertyChangeCallback;

import java.util.Optional;

/**
 * Thingworx property change listener implementation
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */

@RequiredArgsConstructor
@Slf4j
public class ThingworxPropertyChangeListener implements VirtualThingPropertyChangeListener {

    private final ThingworxPropertyChangeCallback callback;

    @Override
    public void propertyChangeEventReceived(VirtualThingPropertyChangeEvent event) {
        Optional.ofNullable(callback).ifPresent(callback1 -> callback1.call(event));
    }
}
