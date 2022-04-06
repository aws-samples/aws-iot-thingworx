package software.amazon.samples.thingworx.connection.config;

import com.thingworx.communications.client.ClientConfigurator;
import com.thingworx.communications.client.ConnectedThingClient;

/**
 * Represents Thingworx client
 *
 * @author Krzysztof Lisowski
 * @version 1.0 20 Oct 2021
 */
public class ThingworxClient extends ConnectedThingClient {
    /**
     * Constructor for class ThingworxClient
     *
     * @param config configuration of Thingworx client
     * @throws Exception
     */
    public ThingworxClient(final ClientConfigurator config) throws Exception {
        super(config);
    }
}
