package software.amazon.samples.thingworx.connection;

import com.thingworx.communications.client.ClientConfigurator;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.samples.thingworx.connection.config.PasswordCallback;
import software.amazon.samples.thingworx.connection.config.ThingworxClient;

/**
 * Singleton class Thingworx connection object
 *
 * @author Krzysztof Lisowski
 * @version 1.0 20 Oct 2021
 */
public class ThingworxConnection {
    private static final Logger logger = LoggerFactory.getLogger(ThingworxConnection.class + "::LAMBDA_BODY");

    private static final ThingworxConnection instance = new ThingworxConnection();
    private static ThingworxClient connection = null;

    private ThingworxConnection() {
    }

    /**
     * Thingworx connection object
     *
     * @param thingworxServerUrl         Thingworx server url
     * @param thingworxApiKey            Thingworx app Key
     * @param thingworxIgnoreSSLError    Thingworx ignore ssl error state
     * @param thingworxReconnectInterval Thingworx reconnect interval time
     */
    @SneakyThrows
    public void connect(final String thingworxServerUrl, final String thingworxApiKey,
                        final boolean thingworxIgnoreSSLError, final int thingworxReconnectInterval) {
        logger.debug("Properties for class: {}", Object.class.getName(), logger.isDebugEnabled());
        logger.debug("Uri: {}", thingworxServerUrl, logger.isDebugEnabled());
        logger.debug("Reconnect interval: {}", thingworxReconnectInterval, logger.isDebugEnabled());
        logger.debug("Security claims: {}", thingworxApiKey, logger.isDebugEnabled());
        logger.debug("Ignore SSL errors: {}", thingworxIgnoreSSLError, logger.isDebugEnabled());

        ClientConfigurator config = new ClientConfigurator();
        config.setUri(thingworxServerUrl);
        config.setReconnectInterval(thingworxReconnectInterval);
        config.setSecurityClaims(new PasswordCallback(thingworxApiKey));
        config.ignoreSSLErrors(thingworxIgnoreSSLError);

        try {
            connection = new ThingworxClient(config);
            connection.start();

            logger.debug("Connection to thingworx initialized", logger.isDebugEnabled());
        } catch (Exception eStart) {
            logger.error("Couldn't create connection to thingworx: {}", eStart, logger.isErrorEnabled());
            throw eStart;
        }
    }

    /**
     * Returns client of Thingworx
     *
     * @return Thingworx client
     */
    public ThingworxClient getConnection() {
        return connection;
    }

    /**
     * Returns instance of ThingworxConnection class
     *
     * @return ThingworxConnection
     */
    public static ThingworxConnection getInstance() {
        return instance;
    }
}
