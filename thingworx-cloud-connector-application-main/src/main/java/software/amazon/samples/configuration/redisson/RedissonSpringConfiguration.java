package software.amazon.samples.configuration.redisson;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.samples.configuration.awsAppconfig.AppConfigModule;
import software.amazon.samples.ThingworxConnectorApplication;
import software.amazon.samples.events.RedisTopicListener;

/**
 * Redisson configuration Bean
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */

@Configuration
@Slf4j
public class RedissonSpringConfiguration {

    @Bean
    RedissonClient getRedissonClient(AppConfigModule appConfigModule, Environment environment, ApplicationContext applicationContext) {
        String uniqueIdInstance = getInstanceNameFromEnvironmentalVariables(appConfigModule);
        Config configuration = new Config();
        configuration.setCodec(new JsonJacksonCodec());
        if ("dev".equalsIgnoreCase(System.getenv(ThingworxConnectorApplication.environmentNameIndicator))) {
            configuration.useSingleServer().setAddress("redis://127.0.0.1:6379");
            log.debug("Using local redis instance \"redis://127.0.0.1:6379\"");
        } else if ("prod".equalsIgnoreCase(System.getenv(ThingworxConnectorApplication.environmentNameIndicator))) {
            configuration.useClusterServers().addNodeAddress("redis://" + appConfigModule.getRedisConfigurationEndpoint());
            log.debug("Using redis cluster at: [{}]", appConfigModule.getRedisConfigurationEndpoint());
        }
        RedissonClient redissonClient = Redisson.create(configuration);
        RTopic topic = redissonClient.getTopic(uniqueIdInstance);
        log.debug("Subscribing to topic: [{}]", uniqueIdInstance);
        topic.addListener(String.class, new RedisTopicListener(applicationContext, redissonClient, appConfigModule));
        return redissonClient;
    }

    private String getInstanceNameFromEnvironmentalVariables(AppConfigModule appConfigModule) {
        String InstanceNameEnvironmentVariableIndicator = appConfigModule.getInstanceNameEnvironmentVariableIndicator();
        String uniqueIdInstance = System.getenv(InstanceNameEnvironmentVariableIndicator);
        if (uniqueIdInstance == null) {
            log.error("The parameter [{}] is not defined in environment variables. Application will be shut down.", InstanceNameEnvironmentVariableIndicator);
            throw new NullPointerException("The parameter [" + InstanceNameEnvironmentVariableIndicator + "] is not defined in environment variables. Application will be shut down.");
        } else if (uniqueIdInstance.isEmpty()) {
            log.error("The parameter [{}] defined in environment variables can not be empty. Application will be shut down.", InstanceNameEnvironmentVariableIndicator);
            throw new IllegalArgumentException("The parameter [" + InstanceNameEnvironmentVariableIndicator + "] defined in environment variables can not be empty. Application will be shut down.");
        }
        return uniqueIdInstance;
    }
}