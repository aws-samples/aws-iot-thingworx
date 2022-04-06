package software.amazon.samples.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static software.amazon.samples.ThingworxDataFetcherLambda.ENVIRONMENT;

/**
 * Singleton class reads and puts data to redis
 *
 * @author Krzysztof Lisowski
 * @version 1.0 20 Oct 2021
 */
public class RedisModule {
    private static final Logger logger = LoggerFactory.getLogger(RedisModule.class + "::LAMBDA_BODY");

    private static final RedisModule instance = new RedisModule();

    private static RedissonClient redisson = null;
    private static final String environment = System.getenv(ENVIRONMENT);

    private RedisModule() {
    }

    /**
     * Connects to redis. If environment variable "ENVIRONMENT" is set to "DEV" then lambda connects to local redis
     * If it is set to "PROD" then lambda connects to Aws redis
     *
     * @param endpoint redis endpoint
     * @throws Exception
     */
    public void connect(final String endpoint) {
        try {
            logger.debug("Creating connection to redis", logger.isDebugEnabled());
            Config config = new Config();
            config.setCodec(new JsonJacksonCodec());
            String connection;
            if (environment != null && environment.equals("DEV")) {
                connection = "redis://172.17.0.2:6379";
                config.useSingleServer().setAddress(connection);
                logger.debug("Using local redis instance at: {}", connection, logger.isDebugEnabled());
            } else if (environment != null && environment.equals("PROD")) {
                connection = "redis://" + endpoint;
                config.useClusterServers().addNodeAddress(connection);
                logger.debug("Using redis cluster instance at: {}", connection, logger.isDebugEnabled());
            } else {
                logger.error("Not defined ENVIRONMENT in system variables. You need to add ENVIRONMENT=DEV or ENVIRONMENT=PROD", logger.isErrorEnabled());
            }

            redisson = Redisson.create(config);
            logger.debug("Connected to redis", logger.isDebugEnabled());
        } catch (Exception e) {
            logger.error("Couldn't connect to redis at: {}", e, logger.isErrorEnabled());
            throw e;
        }
    }

    /**
     * Ends connection to redis
     *
     * @throws Exception
     */
    public void endConnection() {
        try {
            if (!redisson.isShutdown()) {
                redisson.shutdown();
                logger.debug("Disconnected from redis", logger.isDebugEnabled());
            }
        } catch (Exception e) {
            logger.error("Couldn't end connection to redis: {}", e, logger.isErrorEnabled());
            throw e;
        }
    }

    /**
     * Reads element from redis
     *
     * @param key name of key element
     * @return String value of that element
     */
    public String getString(final String key) {
        RBucket<String> bucket = redisson.getBucket(key, StringCodec.INSTANCE);
        return bucket.get();
    }

    /**
     * Reads list from redis and map read data to ThingworxClientApplication object
     *
     * @param appInstanceListNameInRedis name of app instance redis list that contains Thingworx client application instances
     * @return list of Thingworx client application instances
     * @throws JsonProcessingException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @SneakyThrows
    public List<ThingworxClientApplication> getThingworxClientApplication(final String appInstanceListNameInRedis) {
        logger.debug("Reading Thingworx client applications from list {}", appInstanceListNameInRedis, logger.isDebugEnabled());
        List<ThingworxClientApplication> thingworxClientApplications = new ArrayList<>();

        List<String> instances = redisson.getList(appInstanceListNameInRedis);

        int i = 0;
        for (String key : instances) {
            try {
                ThingworxClientApplication thingworxClientApplication;
                try {
                    thingworxClientApplication = new ObjectMapper().readValue(key, ThingworxClientApplication.class);
                    thingworxClientApplication.setIndexInRedisList(i);
                    logger.debug("Thingworx client application instance read from redis:", logger.isDebugEnabled());
                    logger.debug("Instance name: {}", thingworxClientApplication.getInstanceName(), logger.isDebugEnabled());
                    logger.debug("Instance list index: {}", thingworxClientApplication.getIndexInRedisList(), logger.isDebugEnabled());
                    logger.debug("Instance Things client ids: {}", thingworxClientApplication.getThingNames(), logger.isDebugEnabled());
                    logger.debug("Instance instanceIsDead: {}", thingworxClientApplication.getInstanceIsDead(), logger.isDebugEnabled());
                } catch (JsonProcessingException e) {
                    logger.error("Couldn't parse json from redis list into object: {}", e, logger.isErrorEnabled());
                    throw e;
                }
                long listeners = redisson.getTopic(thingworxClientApplication.getInstanceName()).countSubscribersAsync().await().get();
                logger.debug("Number of subscribers for topic {}: {}", thingworxClientApplication.getInstanceName(), listeners, logger.isDebugEnabled());
                //Checking the number of subscribers for appInstanceListNameInRedis.
                //If there is only one subscriber listening on the topic then thingworx client application appInstanceListNameInRedis is not dead
                if (listeners == 1) {
                    thingworxClientApplication.setInstanceDead(false);
                }
                //If there are no subscribers on the topic with the name of the appInstanceListNameInRedis, then mark appInstanceListNameInRedis as dead
                else if (listeners == 0) {
                    logger.warn("No alive appInstanceListNameInRedis for topic: {}", thingworxClientApplication.getInstanceName(), logger.isWarnEnabled());
                    thingworxClientApplication.setInstanceDead(true);
                }
                //If there are more then one subscribers for an given appInstanceListNameInRedis topic, then we log the error
                else if (listeners > 1) {
                    logger.warn("More than one appInstanceListNameInRedis is subscribed to topic: {}", thingworxClientApplication.getInstanceName(), logger.isWarnEnabled());
                }
                thingworxClientApplications.add(thingworxClientApplication);
                i++;
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Exception thrown while waiting to read appInstanceListNameInRedis redis list: {}", e, logger.isErrorEnabled());
                throw e;
            }
        }

        return thingworxClientApplications;
    }

    /**
     * Adds new instance to Thingworx client application instance list
     *
     * @param thingworxClientApplicationList name of Thingworx client application instance list
     * @param jsonValue                      json value to put to list
     */
    public void addInstanceToRedisList(final String thingworxClientApplicationList, final String jsonValue) {
        redisson.getList(thingworxClientApplicationList).add(jsonValue);
    }

    /**
     * Updating index of Thingworx client application list
     *
     * @param thingworxClientApplicationList name of Thingworx client application instance list
     * @param index                          number of index to update
     * @param jsonValue                      json value to put at index
     */
    public void updateThingworxClientApplicationInstanceElementInRedisList(final String thingworxClientApplicationList, final int index, final String jsonValue) {
        redisson.getList(thingworxClientApplicationList).fastSet(index, jsonValue);
    }

    /**
     * Sets value to redis at key
     *
     * @param key   name of key element
     * @param value String value to put at index
     */
    public void setString(final String key, final String value) {
        RBucket<String> bucket = redisson.getBucket(key, StringCodec.INSTANCE);
        bucket.set(value);
    }

    /**
     * Returns instance of RedisModule class
     *
     * @return RedisModule
     */
    public static RedisModule getInstance() {
        return instance;
    }
}
