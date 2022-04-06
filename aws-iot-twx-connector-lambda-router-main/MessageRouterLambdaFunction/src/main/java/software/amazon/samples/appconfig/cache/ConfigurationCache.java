package software.amazon.samples.appconfig.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.samples.appconfig.model.ConfigurationKey;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.appconfig.model.GetConfigurationResponse;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches AppConfig data locally
 *
 * @version 1.0 20 Oct 2021
 * @author Rafal Wysocki
 */

public class ConfigurationCache {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationCache.class + "::LAMBDA_BODY");
    private final ConcurrentHashMap<ConfigurationKey,
            ConfigurationCacheItem<GetConfigurationResponse>> cache = new ConcurrentHashMap<>();

    public ConfigurationCacheItem<GetConfigurationResponse> get(final ConfigurationKey key) {
        ConfigurationCacheItem<GetConfigurationResponse> configurationCacheItem = cache.get(key);
        logger.debug("Get AppConfig from cache: [{}] -> [{}]", key,
                     Optional.ofNullable(configurationCacheItem)
                             .map(ConfigurationCacheItem<GetConfigurationResponse>::getValue)
                             .map(GetConfigurationResponse::content)
                             .map(SdkBytes::asUtf8String)
                             .orElse("NULL"),
                     logger.isDebugEnabled());
        return configurationCacheItem;
    }

    public void put(final ConfigurationKey key,
                    final ConfigurationCacheItem<GetConfigurationResponse> value) {
        cache.put(key, value);//LOG
        logger.debug("Put AppConfig to cache: [{}] -> [{}]", key,
                     Optional.ofNullable(value)
                             .map(ConfigurationCacheItem<GetConfigurationResponse>::getValue)
                             .map(GetConfigurationResponse::content)
                             .map(SdkBytes::asUtf8String)
                             .orElse("NULL"),
                     logger.isDebugEnabled());
    }

    public Set<Map.Entry<ConfigurationKey, ConfigurationCacheItem<GetConfigurationResponse>>> entrySet() {
        return cache.entrySet();
    }
}
