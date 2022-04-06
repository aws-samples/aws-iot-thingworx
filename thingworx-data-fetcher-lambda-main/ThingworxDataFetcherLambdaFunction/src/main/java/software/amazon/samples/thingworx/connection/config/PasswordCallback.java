package software.amazon.samples.thingworx.connection.config;

import com.thingworx.communications.client.IPasswordCallback;

/**
 * Is used to set password for Thingworx client connector
 *
 * @author Krzysztof Lisowski
 * @version 1.0 20 Oct 2021
 */
public class PasswordCallback implements IPasswordCallback {
    private final String appKey;

    /**
     * Constructor for class PasswordCallback
     *
     * @param appKey app key from Thingworx
     */
    public PasswordCallback(final String appKey) {
        this.appKey = appKey;
    }

    /**
     * Returns Thingworx app key
     *
     * @return app key
     */
    @Override
    public char[] getSecret() {
        return appKey.toCharArray();
    }
}
