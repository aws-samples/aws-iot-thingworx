package software.amazon.samples.configuration.awsAppconfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import software.amazon.samples.ThingworxConnectorApplication;
import software.amazon.samples.configuration.awsAppconfig.cache.ConfigurationCache;
import software.amazon.samples.configuration.awsAppconfig.model.ConfigurationKey;
import software.amazon.samples.configuration.awsAppconfig.utils.AppConfigUtility;
import software.amazon.awssdk.services.appconfig.AppConfigClient;
import software.amazon.awssdk.services.appconfig.model.GetConfigurationResponse;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * AppConfig module for mapping config data from AWS
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */

@Getter(AccessLevel.PRIVATE)
@Slf4j
public class AppConfigModule {

    final private Duration cacheItemTtl;
    final private AppConfigClient client;
    final private String clientId;
    final private ConfigurationCache configurationCache;

    // Variables with values from AppConfig. Change they according to what you need from config.
    @Getter(AccessLevel.PUBLIC)
    private String redisConfigurationEndpoint;
    @Getter(AccessLevel.PUBLIC)
    private String thingworxURL;
    @Getter(AccessLevel.PUBLIC)
    private String thingworxAppKey;
    @Getter(AccessLevel.PUBLIC)
    private Boolean ignoreSSLError;
    @Getter(AccessLevel.PUBLIC)
    private Integer reconnectInterval;
    @Getter(AccessLevel.PUBLIC)
    private Integer processScanRequestTimeoutInMillis;
    @Getter(AccessLevel.PUBLIC)
    private Integer waitForConnectionTimeoutInMillis;
    @Getter(AccessLevel.PUBLIC)
    private String instanceNameEnvironmentVariableIndicator;
    @Getter(AccessLevel.PUBLIC)
    private String thingStatusIndicator;
    @Getter(AccessLevel.PUBLIC)
    private String thingTelemetryPayloadIndicator;
    @Getter(AccessLevel.PUBLIC)
    private String thingNameIndicator;
    @Getter(AccessLevel.PUBLIC)
    private String thingModelIndicator;
    @Getter(AccessLevel.PUBLIC)
    private String modelParametersDataTypeIndicator;
    @Getter(AccessLevel.PUBLIC)
    private String connectedStatus;
    @Getter(AccessLevel.PUBLIC)
    private String disconnectedStatus;
    @Getter(AccessLevel.PUBLIC)
    private String propertyChangeQueueName;

    private final org.springframework.core.env.Environment springBootEnvironment;

    public AppConfigModule(Environment springBootEnvironment) throws JsonProcessingException {
        this(null, null, null, null, springBootEnvironment);
    }

    public AppConfigModule(final Duration cacheItemTtl,
                           final AppConfigClient client, final String clientId,
                           final ConfigurationCache configurationCache, final Environment springBootEnvironment) throws JsonProcessingException {
        this.cacheItemTtl = cacheItemTtl;
        this.client = client;
        this.clientId = clientId;
        this.configurationCache = configurationCache;
        this.springBootEnvironment = springBootEnvironment;
        getConfig();
    }

    public void getConfig() throws JsonProcessingException {
        final String awsApplication = springBootEnvironment.getProperty("awsAppConfig.application");
        final String awsEnvironment = System.getenv(ThingworxConnectorApplication.environmentNameIndicator);
        final String awsConfig = System.getenv(ThingworxConnectorApplication.environmentNameIndicator);

        AppConfigClient client = getOrDefault(this::getClient, this::getDefaultClient);
        ConfigurationCache cCache = getOrDefault(this::getConfigurationCache,
                ConfigurationCache::new);
        Duration duration = getOrDefault(this::getCacheItemTtl, this::getDefaultCacheItemTtl);
        String clientId = getOrDefault(this::getClientId, this::getDefaultClientId);


        final AppConfigUtility appConfigUtility = new AppConfigUtility(client, cCache, duration,
                clientId);

        final GetConfigurationResponse
                response = appConfigUtility.getConfiguration(
                new ConfigurationKey(awsApplication, awsEnvironment, awsConfig));
        final String appConfigResponse = response.content().asUtf8String();

        YAMLMapper yamlMapper = new YAMLMapper();
        JsonNode jsonNode;

        try {
            jsonNode = yamlMapper.readTree(appConfigResponse);
        } catch (JsonProcessingException ex) {
            log.error("Appconfig attribute error with message [" + ex.getMessage() + "]. check if attribute exist on AWS app config for [" + awsApplication + "] and environment [" + awsEnvironment + "]");
            throw ex;
        }

        JsonNode jsonResponseObjectRedis = jsonNode.path("redis");
        redisConfigurationEndpoint = jsonResponseObjectRedis.path("configurationEndpoint").asText();

        JsonNode jsonResponseObjectThingworx = jsonNode.path("thingworx");
        thingworxURL = jsonResponseObjectThingworx.path("serverUrl").asText();
        thingworxAppKey = jsonResponseObjectThingworx.path("apiKey").asText();
        ignoreSSLError = jsonResponseObjectThingworx.path("ignoreSSLError").asBoolean();
        reconnectInterval = jsonResponseObjectThingworx.path("reconnectInterval").asInt();
        processScanRequestTimeoutInMillis = jsonResponseObjectThingworx.path("processScanRequestTimeoutInMillis").asInt();
        waitForConnectionTimeoutInMillis = jsonResponseObjectThingworx.path("waitForConnectionTimeoutInMillis").asInt();

        JsonNode jsonResponseObjectThingworxClientConnectorApp = jsonNode.path("thingworxClientConnectorSpringApp");
        instanceNameEnvironmentVariableIndicator = jsonResponseObjectThingworxClientConnectorApp.path("twxCloudConnectorInstanceNameEnvironmentVariableIndicator").asText();

        JsonNode jsonResponseObjectProperty = jsonResponseObjectThingworxClientConnectorApp.path("messageRouterLambdaPayloadStructure");
        thingStatusIndicator = jsonResponseObjectProperty.path("thingStatusIndicator").asText();
        thingTelemetryPayloadIndicator = jsonResponseObjectProperty.path("thingTelemetryPayloadIndicator").asText();
        thingNameIndicator = jsonResponseObjectProperty.path("thingNameIndicator").asText();
        thingModelIndicator = jsonResponseObjectProperty.path("thingModelIndicator").asText();
        modelParametersDataTypeIndicator = jsonResponseObjectProperty.path("modelParametersDataTypeIndicator").asText();

        connectedStatus = jsonResponseObjectThingworxClientConnectorApp.path("thingConnectionStatuses").path("connected").asText();
        disconnectedStatus = jsonResponseObjectThingworxClientConnectorApp.path("thingConnectionStatuses").path("disconnected").asText();

        JsonNode jsonResponseObjectThingworxPropertyChangeConsumerApp = jsonNode.path("thingworxPropertyChangeConsumerSpringApp");
        propertyChangeQueueName = jsonResponseObjectThingworxPropertyChangeConsumerApp.path("propertyChangeQueue").asText();
    }

    private String getDefaultClientId() {
        return UUID.randomUUID().toString();
    }

    protected AppConfigClient getDefaultClient() {
        return AppConfigClient.create();
    }

    private Duration getDefaultCacheItemTtl() {
        return Duration.ofSeconds(30);
    }

    private <T> T getOrDefault(final Supplier<T> optionalGetter, final Supplier<T> defaultGetter) {
        return Optional.ofNullable(optionalGetter.get()).orElseGet(defaultGetter);
    }
}
