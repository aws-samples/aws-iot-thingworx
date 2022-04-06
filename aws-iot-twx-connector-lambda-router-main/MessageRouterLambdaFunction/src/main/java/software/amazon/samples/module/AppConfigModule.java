package software.amazon.samples.module;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.samples.appconfig.cache.ConfigurationCache;
import software.amazon.samples.appconfig.model.ConfigurationKey;
import software.amazon.samples.appconfig.utils.AppConfigUtility;
import software.amazon.awssdk.services.appconfig.AppConfigClient;
import software.amazon.awssdk.services.appconfig.model.GetConfigurationResponse;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

/**
 * Wrapper with singleton for AppConfig access.
 *
 * @author Rafal Wysocki
 * @version 1.0 20 Oct 2021
 */

@Getter(AccessLevel.PRIVATE)
public class AppConfigModule {

    // AppConfig access
    private static final String APP_CONFIGURATION_FILE = "application.properties";
    private static final String APPCONFIG_APPLICATION_PROPERTY = "appconfig.application";

    // Redis
    private static final String REDIS = "redis";
    private static final String CONFIGURATION_ENDPOINT = "configurationEndpoint";

    // Communication between this Lambda and EC2 instances
    private static final String THINGWORX_CLIENT_CONNECTOR_SPRING_APP
            = "thingworxClientConnectorSpringApp";
    private static final String MESSAGE_ROUTER_LAMBDA_PAYLOAD_STRUCTURE
            = "messageRouterLambdaPayloadStructure";
    private static final String THING_TELEMETRY_PAYLOAD_INDICATOR
            = "thingTelemetryPayloadIndicator";
    private static final String THING_STATUS_INDICATOR = "thingStatusIndicator";
    private static final String INSTANCE_INDICATOR = "instanceIndicator";
    private static final String THING_MODEL_INDICATOR = "thingModelIndicator";
    private static final String THING_NAME_INDICATOR = "thingNameIndicator";
    private static final String THING_CONNECTION_STATUSES = "thingConnectionStatuses";
    private static final String THING_CONNECTION_STATUS_CONNECTED = "connected";
    private static final String THING_CONNECTION_STATUS_DISCONNECTED = "disconnected";

    // Specific for this lambda
    private static final String MESSAGE_ROUTER_LAMBDA = "messageRouterLambda";
    private static final String CLIENT_STATUS_MESSAGE_INDICATOR = "clientStatusMessageIndicator";
    private static final String CLIENT_STATUS_MESSAGE_INDICATOR_NAME = "name";
    private static final String CLIENT_STATUS_MESSAGE_INDICATOR_VALUE = "value";


    private static final Logger logger = LoggerFactory.getLogger(
            AppConfigModule.class + "::LAMBDA_BODY");

    @Getter(AccessLevel.PUBLIC)
    private static final AppConfigModule instance = new AppConfigModule();

    // Variables with values from AppConfig. Change they according to what you need from config.

    // Redis
    @Getter(AccessLevel.PUBLIC)
    private String redisConfigurationEndpoint;

    // Communication between this Lambda and EC2 instances
    @Getter(AccessLevel.PUBLIC)
    private String thingTelemetryPayloadIndicator;
    @Getter(AccessLevel.PUBLIC)
    private String thingStatusIndicator;
    @Getter(AccessLevel.PUBLIC)
    private String instanceIndicator;
    @Getter(AccessLevel.PUBLIC)
    private String thingModelIndicator;
    @Getter(AccessLevel.PUBLIC)
    private String thingNameIndicator;

    @Getter(AccessLevel.PUBLIC)
    private String thingConnectionStatusConnected;
    @Getter(AccessLevel.PUBLIC)
    private String thingConnectionStatusDisconnected;

    // Specific for this lambda
    @Getter(AccessLevel.PUBLIC)
    private String clientStatusMessageIndicatorName;
    @Getter(AccessLevel.PUBLIC)
    private String clientStatusMessageIndicatorValue;


    private AppConfigModule() {
        getConfig();
    }

    @SneakyThrows
    private void getConfig() {

        final String appConfigResponse = getAppConfigContent();

        YAMLMapper mapper = new YAMLMapper();
        JsonNode rootNode = mapper.readTree(appConfigResponse);


        // Get values from AppConfig. Change they according to what you need from config.
        getRedisConfiguration(rootNode);
        getTwxCloudConnectorConfiguration(rootNode);
        getSpecificConfigurationForThisLambda(rootNode);

        logAllConfigurationVariablesInDebugMode();
    }

    private void logAllConfigurationVariablesInDebugMode() {

        logger.debug("AppConfig: redisConfigurationEndpoint={}", redisConfigurationEndpoint,
                     logger.isDebugEnabled());
        logger.debug("AppConfig: thingTelemetryPayloadIndicator={}", thingTelemetryPayloadIndicator,
                     logger.isDebugEnabled());
        logger.debug("AppConfig: thingStatusIndicator={}", thingStatusIndicator,
                     logger.isDebugEnabled());
        logger.debug("AppConfig: instanceIndicator={}", instanceIndicator,
                     logger.isDebugEnabled());
        logger.debug("AppConfig: thingModelIndicator={}", thingModelIndicator,
                     logger.isDebugEnabled());
        logger.debug("AppConfig: thingConnectionStatusConnected={}", thingConnectionStatusConnected,
                     logger.isDebugEnabled());
        logger.debug("AppConfig: thingConnectionStatusDisconnected={}",
                     thingConnectionStatusDisconnected,
                     logger.isDebugEnabled());
        logger.debug("AppConfig: clientStatusMessageIndicatorName={}",
                     clientStatusMessageIndicatorName,
                     logger.isDebugEnabled());
        logger.debug("AppConfig: clientStatusMessageIndicatorValue={}",
                     clientStatusMessageIndicatorValue,
                     logger.isDebugEnabled());
    }

    private void getSpecificConfigurationForThisLambda(final JsonNode rootNode) {
        JsonNode statusIndicatorNode = rootNode
                .path(MESSAGE_ROUTER_LAMBDA)
                .path(CLIENT_STATUS_MESSAGE_INDICATOR);
        clientStatusMessageIndicatorName = statusIndicatorNode
                .path(CLIENT_STATUS_MESSAGE_INDICATOR_NAME).asText();
        clientStatusMessageIndicatorValue = statusIndicatorNode
                .path(CLIENT_STATUS_MESSAGE_INDICATOR_VALUE).asText();
    }

    private void getTwxCloudConnectorConfiguration(final JsonNode rootNode) {
        JsonNode thingworxClientNode = rootNode
                .path(THINGWORX_CLIENT_CONNECTOR_SPRING_APP);
        JsonNode propertyNode = thingworxClientNode
                .path(MESSAGE_ROUTER_LAMBDA_PAYLOAD_STRUCTURE);
        thingTelemetryPayloadIndicator = propertyNode
                .path(THING_TELEMETRY_PAYLOAD_INDICATOR).asText();
        thingStatusIndicator = propertyNode
                .path(THING_STATUS_INDICATOR).asText();
        instanceIndicator = propertyNode
                .path(INSTANCE_INDICATOR).asText();
        thingModelIndicator = propertyNode
                .path(THING_MODEL_INDICATOR).asText();
        thingNameIndicator = propertyNode
                .path(THING_NAME_INDICATOR).asText();

        JsonNode statusNode = thingworxClientNode
                .path(THING_CONNECTION_STATUSES);
        thingConnectionStatusConnected = statusNode
                .path(THING_CONNECTION_STATUS_CONNECTED).asText();
        thingConnectionStatusDisconnected = statusNode
                .path(THING_CONNECTION_STATUS_DISCONNECTED).asText();
    }

    private void getRedisConfiguration(final JsonNode rootNode) {
        JsonNode redisEndpointNode = rootNode
                .path(REDIS);
        redisConfigurationEndpoint = redisEndpointNode
                .path(CONFIGURATION_ENDPOINT).asText();
    }

    private String getAppConfigContent() {

        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(APP_CONFIGURATION_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }


        String application = properties.getProperty(APPCONFIG_APPLICATION_PROPERTY);
        String environment = System.getenv("ENVIRONMENT");

        logger.debug("Configuration from {}: {}={}, {}={}",
                     APP_CONFIGURATION_FILE,
                     APPCONFIG_APPLICATION_PROPERTY, application,
                     "ENVIRONMENT", environment,
                     logger.isDebugEnabled());

        AppConfigClient client = getDefaultClient();
        ConfigurationCache cCache = new ConfigurationCache();
        Duration duration = getDefaultCacheItemTtl();
        String clientId = getDefaultClientId();

        final AppConfigUtility appConfigUtility = new AppConfigUtility(client,
                                                                       cCache,
                                                                       duration,
                                                                       clientId);

        final GetConfigurationResponse response = appConfigUtility.getConfiguration(
                new ConfigurationKey(application, environment, environment));
        final String appConfigResponse = response.content().asUtf8String();
        return appConfigResponse;
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
}
