package software.amazon.samples.deviceShadow;

import com.thingworx.communications.client.things.VirtualThingPropertyChangeEvent;

/**
 * Callback interface for thingworx property changes handler
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */

public interface ThingworxPropertyChangeCallback {

    void call(VirtualThingPropertyChangeEvent event);
}
