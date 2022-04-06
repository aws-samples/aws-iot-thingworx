package software.amazon.samples.configuration.awsAppconfig.cache;

import lombok.extern.slf4j.Slf4j;
import software.amazon.samples.configuration.awsAppconfig.model.ConfigurationKey;
import software.amazon.awssdk.services.appconfig.model.GetConfigurationResponse;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AppConfig configuration cache
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */

@Slf4j
public class ConfigurationCache {
    private final ConcurrentHashMap<ConfigurationKey, ConfigurationCacheItem<GetConfigurationResponse>> cache
            = new ConcurrentHashMap<>();

    public ConfigurationCacheItem<GetConfigurationResponse> get(final ConfigurationKey key) {
        log.debug("Getting configuration from cache config for key : {}", key);
        return cache.get(key);
    }

    public void put(final ConfigurationKey key, final ConfigurationCacheItem<GetConfigurationResponse> value) {
        log.debug("Putting configuration item into cache config for : [{}] and value : [{}]", key, value);
        cache.put(key, value);
    }

    public Set<Map.Entry<ConfigurationKey, ConfigurationCacheItem<GetConfigurationResponse>>> entrySet() {
        return cache.entrySet();
    }
}
