package software.amazon.samples.thingworx;

import com.thingworx.communications.client.ClientConfigurator;
import com.thingworx.communications.client.IPasswordCallback;
import lombok.extern.slf4j.Slf4j;


/**
 * Thingworx custom client configuration
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */

@Slf4j
public class ThingworxClientConfigurator extends ClientConfigurator {

    public ThingworxClientConfigurator(String uri, int reconnectInterval, String appKey, boolean ignoreSSLErrors) {
        this.setUri(uri);
        log.debug("Setting Thingworx Client endpoint: [{}]", uri);
        this.setReconnectInterval(reconnectInterval);
        log.debug("Setting Thingworx Client reconnect interval: [{}]", reconnectInterval);
        this.setSecurityClaims(new SamplePasswordCallback(appKey));
        log.debug("Setting Thingworx Client app key: [{}]", appKey);
        this.ignoreSSLErrors(ignoreSSLErrors);
        log.debug("Setting Thingworx Client ignore SSL flag: [{}]", ignoreSSLErrors);
    }

    public static class SamplePasswordCallback implements IPasswordCallback {
        private final String appKey;

        public SamplePasswordCallback(String appKey) {
            this.appKey = appKey;
        }

        @Override
        public char[] getSecret() {
            return appKey.toCharArray();
        }
    }
}
