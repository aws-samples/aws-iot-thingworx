package software.amazon.samples.thingworx;

import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.client.things.VirtualThing;
import lombok.extern.slf4j.Slf4j;
import software.amazon.samples.configuration.awsAppconfig.AppConfigModule;

/**
 * Creates a singleton instance of thingworx client connector
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */

@Slf4j
public enum ThingworxClientSingleton {
    INSTANCE;
    private ConnectedThingClient connectedThingClient = null;
    private AppConfigModule appConfigModule;

    private void generateThingsClient(ThingworxClientConfigurator configurator) {
        try {
            connectedThingClient = new ConnectedThingClient(configurator);
        } catch (Exception e) {
            log.error("Error creating new thingworx client instance.", e);
        }
    }

    public void generateThingworxClientInstance(AppConfigModule appConfigModule) {
        this.appConfigModule = appConfigModule;
        ThingworxClientConfigurator configurator = new ThingworxClientConfigurator(appConfigModule.getThingworxURL(), appConfigModule.getReconnectInterval(), appConfigModule.getThingworxAppKey(), appConfigModule.getIgnoreSSLError());
        if (connectedThingClient == null) {
            generateThingsClient(configurator);
        }

        if (!connectedThingClient.isConnected()) {
            startConnectionToThingworx();
        }

    }

    public ConnectedThingClient getConnectedThingClient() {
        checkClientInstanceHasConnection();
        return connectedThingClient;
    }

    private void checkClientInstanceHasConnection() {
        if (connectedThingClient != null) {
            if (!connectedThingClient.isConnected()) {
                startConnectionToThingworx();
            }
        }
    }

    private void startConnectionToThingworx() {
        long start = System.currentTimeMillis();
        try {
            connectedThingClient.start();
        } catch (Exception eStart) {
            log.error("Error staring thingworx client, shutting down instance.  " + eStart);
        }
        if (!connectedThingClient.waitForConnection(appConfigModule.getWaitForConnectionTimeoutInMillis())) {
            log.error("Couldn't establish connection to thingworx with timeout [{}]] milliseconds.going to retry connection.", appConfigModule.getWaitForConnectionTimeoutInMillis());
            try {
                Thread.sleep(5000); // wait for 5 seconds between connection retry
            } catch (InterruptedException e) {
                log.error("error putting thread into sleep {}", e.getMessage());
            }
            startConnectionToThingworx();
        } else {
            long finish = System.currentTimeMillis();
            long timeElapsed = finish - start;
            log.info("Connection to thingworx established in [{}] milliseconds.", timeElapsed);
        }
    }

    public void bindThing(VirtualThing thing) {
        checkClientInstanceHasConnection();
        log.debug("going to bind thing {}", thing.getName());
        try {
            this.connectedThingClient.bindThing(thing);
            log.info("Number of bound things to the client: [{}]", connectedThingClient.getThings().size());
        } catch (Exception e) {
            log.error("Error binding thing {} [{}] ", thing.getName(), e);
        }
    }

    public void unBindThing(VirtualThing thing) {
        checkClientInstanceHasConnection();
        log.debug("going to unbind thing {}", thing.getName());
        try {
            this.connectedThingClient.unbindThing(thing);
            log.info("Number of bound things to the client: [{}]", connectedThingClient.getThings().size());
        } catch (Exception e) {
            log.error("Error unbinding thing {} [{}] ", thing.getName(), e);
        }
    }
}
