package software.amazon.samples.module;

import lombok.Getter;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper with singleton for Redis access.
 *
 * @version 1.0 20 Oct 2021
 * @author Rafal Wysocki
 */

public class RedisModule {

    private static final Logger logger = LoggerFactory.getLogger(
            RedisModule.class + "::LAMBDA_BODY");

    @Getter
    private static final RedisModule instance = new RedisModule();
    private static final String environment = System.getenv("ENVIRONMENT");
    private static final String REDIS_LOCAL = "redis://172.17.0.2:6379";
    RedissonClient redisson = null;

    private RedisModule() {
    }

    public boolean connect(final String endpoint) {
        try {
            Config config = new Config();
            config.setCodec(new JsonJacksonCodec());
            String connection;
            if (environment != null && environment.equals("DEV")) {
                connection = REDIS_LOCAL;
                config.useSingleServer().setAddress(connection);
                logger.debug("Using local redis instance at: " + connection,
                             logger.isDebugEnabled());
            } else if (environment != null && environment.equals("PROD")) {
                connection = "redis://" + endpoint;
                config.useClusterServers().addNodeAddress(connection);
                logger.debug("Using redis cluster instance at: " + connection,
                             logger.isDebugEnabled());
            } else {
                logger.error("Not defined ENVIRONMENT in system variables. " +
                                     "You need to add ENVIRONMENT=DEV or ENVIRONMENT=PROD",
                             logger.isErrorEnabled());
                return false;
            }
            redisson = Redisson.create(config);
            logger.debug("Connected to redis", logger.isDebugEnabled());
            return true;
        } catch (Exception e) {
            logger.error("Couldn't connect to redis at: " + e, logger.isErrorEnabled());
            return false;
        }

    }

    public void disconnect() {
        try {
            if (!redisson.isShutdown()) {
                redisson.shutdown();
                logger.debug("Disconnected from redis", logger.isDebugEnabled());
            }
        } catch (Exception e) {
            logger.error("Couldn't end connection to redis: " + e, logger.isErrorEnabled());
        }
    }

    public String getString(final String key) {
        RBucket<String> bucket = redisson.getBucket(key, StringCodec.INSTANCE);
        String value = bucket.get();
        logger.debug("Redisson bucket.get[{}]: [{}]", key, value, logger.isDebugEnabled());
        return value;
    }

    public void setString(final String key, final String value) {
        RBucket<String> bucket = redisson.getBucket(key, StringCodec.INSTANCE);
        bucket.set(value);
        logger.debug("Redisson bucket.set[{}] to [{}]", key, value, logger.isDebugEnabled());
    }

    public long sendStringToTopic(final String topic, final String message) {
        RTopic publishTopic = redisson.getTopic(topic);
        long publishResult = publishTopic.publish(message);
        logger.debug("Redisson publish topic[{}], payload[{}]", topic, message,
                     logger.isDebugEnabled());
        return publishResult;
    }
}
