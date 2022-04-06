package software.amazon.samples.configuration.awsAppconfig.utils;

import software.amazon.samples.configuration.awsAppconfig.cache.ConfigurationCache;
import software.amazon.samples.configuration.awsAppconfig.cache.ConfigurationCacheItem;
import software.amazon.samples.configuration.awsAppconfig.model.ConfigurationKey;
import software.amazon.awssdk.services.appconfig.AppConfigClient;
import software.amazon.awssdk.services.appconfig.model.GetConfigurationRequest;
import software.amazon.awssdk.services.appconfig.model.GetConfigurationResponse;

import java.time.Duration;
import java.util.Optional;

public class AppConfigUtility {

    private final AppConfigClient client;
    private final ConfigurationCache cache;
    private final Duration cacheItemTtl;
    private final String clientId;

    /**
     * Constructor for AppConfigUtility.
     *
     * @param client             client for retrieving configurations from the AppConfig API.
     * @param configurationCache cache for configurations returned by the AppConfig API.
     * @param cacheItemTtl       items are refreshed according to this ttl. default is 30 secs.
     * @param clientId           unique clientId that is sent to the AppConfig API. Default is a random UUID.
     */

    public AppConfigUtility(final AppConfigClient client,
                            final ConfigurationCache configurationCache,
                            final Duration cacheItemTtl,
                            final String clientId) {

        this.client = client;
        this.cache = configurationCache;
        this.cacheItemTtl = cacheItemTtl;
        this.clientId = clientId;

    }

    public GetConfigurationResponse getConfiguration(final ConfigurationKey configurationKey) {
        final ConfigurationCacheItem<GetConfigurationResponse> result = Optional.ofNullable(cache.get(configurationKey))
                .map(item -> {
                    if (item.isRefreshNeeded()) {
                        return getConfigurationFromApiAndApplyToCache(configurationKey, item, item.getValue().configurationVersion());
                    } else {
                        return item;
                    }
                }).orElseGet(() -> getConfigurationFromApiAndApplyToCache(configurationKey, null, null));
        if (result.getValue() == null && result.getException() != null) {
            throw result.getException();
        }
        return result.getValue();
    }

    protected ConfigurationCacheItem<GetConfigurationResponse> getConfigurationFromApiAndApplyToCache(
            final ConfigurationKey configurationKey,
            final ConfigurationCacheItem<GetConfigurationResponse> existingItem,
            final String version) {
        try {
            final GetConfigurationResponse result = client.getConfiguration(GetConfigurationRequest.builder()
                    .application(configurationKey.getApplication())
                    .environment(configurationKey.getEnvironment())
                    .configuration(configurationKey.getConfiguration())

                    .clientId(this.clientId)
                    .clientConfigurationVersion(version)
                    .build());

            final ConfigurationCacheItem<GetConfigurationResponse> item = new ConfigurationCacheItem<>(cacheItemTtl);
            if (result.content() != null) {
                item.setValue(result);
            }

            if (existingItem == null || existingItem.getValue() == null
                    || (item.getValue() != null
                    && !existingItem.getValue().configurationVersion().equals(item.getValue().configurationVersion()))) {
                item.calculateAndSetRefreshTime();
                cache.put(configurationKey, item);
                return item;
            } else {

                existingItem.calculateAndSetRefreshTime();
                cache.put(configurationKey, existingItem);
            }
            return existingItem;
        } catch (final RuntimeException e) {
            final ConfigurationCacheItem<GetConfigurationResponse> item = new ConfigurationCacheItem<>(cacheItemTtl);
            item.setException(e);

            if (existingItem == null || existingItem.getValue() == null
                    || (item.getValue() != null
                    && !existingItem.getValue().configurationVersion().equals(item.getValue().configurationVersion()))) {
                item.calculateAndSetRefreshTime();
                cache.put(configurationKey, item);
                return item;
            } else {

                existingItem.calculateAndSetRefreshTime();
                cache.put(configurationKey, existingItem);
            }
            return existingItem;
        }
    }
}
