package software.amazon.samples.appconfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;

import static software.amazon.samples.ThingworxDataFetcherLambda.ENVIRONMENT;

/**
 * Singleton class reads data from AppConfig
 *
 * @author Krzysztof Lisowski
 * @version 1.0 20 Oct 2021
 */
@Getter(AccessLevel.PRIVATE)
public class AppConfigModule {
    private static final Logger logger = LoggerFactory.getLogger(AppConfigModule.class + "::LAMBDA_BODY");

    private static final String APP_CONFIGURATION_FILE = "application.properties";
    private static final String APP_CONFIG_APPLICATION_PROPERTY = "appconfig.application";

    @Getter(AccessLevel.PUBLIC)
    private static final AppConfigModule instance = new AppConfigModule();

    // Variables with values from AppConfig. Change they according to what you need from config.
    //Thingworx configuration
    @Getter(AccessLevel.PUBLIC)
    private String serverUrl;
    @Getter(AccessLevel.PUBLIC)
    private String apiKey;
    @Getter(AccessLevel.PUBLIC)
    private boolean ignoreSSLError;
    @Getter(AccessLevel.PUBLIC)
    private int reconnectInterval;
    @Getter(AccessLevel.PUBLIC)
    private int serviceCallTimeoutInMillis;
    @Getter(AccessLevel.PUBLIC)
    private int waitForConnectionTimeoutInMillis;
    //Lambda configuration
    @Getter(AccessLevel.PUBLIC)
    private int numberOfThingsPerInstance;
    @Getter(AccessLevel.PUBLIC)
    private String twxCloudConnectorAppInstanceListNameInRedis;
    @Getter(AccessLevel.PUBLIC)
    private String twxCloudConnectorAppInstanceNamePrefix;
    //Lambda Thingworx configuration
    @Getter(AccessLevel.PUBLIC)
    private String awsBoundPropertyCategoryName;
    //Lambda Thingworx entity list service config configuration
    @Getter(AccessLevel.PUBLIC)
    private int timeout;
    @Getter(AccessLevel.PUBLIC)
    private String awsTags;
    //Redis configuration
    @Getter(AccessLevel.PUBLIC)
    private String configurationEndpoint;
    //Redis thing structure configuration
    @Getter(AccessLevel.PUBLIC)
    private String thingModelIndicator;
    @Getter(AccessLevel.PUBLIC)
    private String modelParametersDataTypeIndicator;
    @Getter(AccessLevel.PUBLIC)
    private String instanceIndicator;
    //Fargate configuration
    @Getter(AccessLevel.PUBLIC)
    private ArrayList<String> securityGroups;
    @Getter(AccessLevel.PUBLIC)
    private ArrayList<String> subnets;
    @Getter(AccessLevel.PUBLIC)
    private String taskDefinition;
    @Getter(AccessLevel.PUBLIC)
    private String cluster;
    @Getter(AccessLevel.PUBLIC)
    private String containerName;
    @Getter(AccessLevel.PUBLIC)
    private String twxCloudConnectorInstanceNameEnvironmentVariableIndicator;

    //App config environment
    @Getter(AccessLevel.PUBLIC)
    private String environmentAppConfig;
    /**
     * Constructor for AppConfigModule class
     */
    private AppConfigModule() {
        getConfig();
    }

    @SneakyThrows
    private void getConfig() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(APP_CONFIGURATION_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String application = properties.getProperty(APP_CONFIG_APPLICATION_PROPERTY);
        environmentAppConfig = System.getenv(ENVIRONMENT);
        logger.debug("Configuration from {}: {}={}, {}={}",
                APP_CONFIGURATION_FILE,
                APP_CONFIG_APPLICATION_PROPERTY, application,
                ENVIRONMENT, environmentAppConfig,
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
                new ConfigurationKey(application, environmentAppConfig, environmentAppConfig));
        final String appConfigResponse = response.content().asUtf8String();

        YAMLMapper mapper = new YAMLMapper();
        JsonNode node;
        try {
            node = mapper.readTree(appConfigResponse);
        } catch (JsonProcessingException e) {
            logger.error("Couldn't parse data from app config {}", e, logger.isErrorEnabled());
            throw e;
        }
        // Get values from AppConfig. Change they according to what you need from config.
        logger.debug("AppConfig pulled parameters:");
        //Thingworx configuration
        getDataFromAppConfigForThingworx(node);
        //Lambda configuration
        getDataFromAppConfigForLambdaConfiguration(node);
        //Redis configuration
        getDataFromAppConfigForRedisConfiguration(node);
        //Fargate configuration
        getDataFromAppConfigForFargateConfiguration(node);
    }

    /**
     * Reads Fargate data from AppConfig
     *
     * @param node json node
     * @throws IOException
     */
    @SneakyThrows
    private void getDataFromAppConfigForFargateConfiguration(JsonNode node) {
        JsonNode parentNode = node.path("thingworxDataFetcherLambda");
        parentNode = parentNode.path("fargateConfig");
        try {
            securityGroups = new ObjectMapper().readerFor(new TypeReference<ArrayList<String>>() {
            }).readValue((JsonNode) parentNode.withArray("securityGroups"));
        } catch (IOException e) {
            logger.error("Couldn't cast json of fargate security groups to arraylist: {}", e, logger.isDebugEnabled());
            throw e;
        }
        try {
            subnets = new ObjectMapper().readerFor(new TypeReference<ArrayList<String>>() {
            }).readValue((JsonNode) parentNode.withArray("subnets"));
        } catch (IOException e) {
            logger.error("Couldn't cast json of fargate subnets to arraylist: {}", e, logger.isDebugEnabled());
            throw e;
        }
        taskDefinition = parentNode.path("taskDefinition").asText();
        cluster = parentNode.path("cluster").asText();
        containerName = parentNode.path("containerName").asText();

        parentNode = node.path("thingworxClientConnectorSpringApp");
        twxCloudConnectorInstanceNameEnvironmentVariableIndicator = parentNode.path("twxCloudConnectorInstanceNameEnvironmentVariableIndicator").asText();

        logger.debug("securityGroups: {}", securityGroups, logger.isDebugEnabled());
        logger.debug("subnets: {}", subnets, logger.isDebugEnabled());
        logger.debug("taskDefinition: {}", taskDefinition, logger.isDebugEnabled());
        logger.debug("cluster: {}", cluster, logger.isDebugEnabled());
        logger.debug("containerName: {}", containerName, logger.isDebugEnabled());
        logger.debug("twxCloudConnectorInstanceNameEnvironmentVariableIndicator: {}", twxCloudConnectorInstanceNameEnvironmentVariableIndicator, logger.isDebugEnabled());
    }

    /**
     * Reads Redis data from AppConfig
     *
     * @param node json node
     */
    private void getDataFromAppConfigForRedisConfiguration(JsonNode node) {
        JsonNode parentNode = node.path("redis");
        configurationEndpoint = parentNode.path("configurationEndpoint").asText();
        //Redis thing structure configuration
        parentNode = node.path("thingworxClientConnectorSpringApp");
        parentNode = parentNode.path("messageRouterLambdaPayloadStructure");
        thingModelIndicator = parentNode.path("thingModelIndicator").asText();
        modelParametersDataTypeIndicator = parentNode.path("modelParametersDataTypeIndicator").asText();
        instanceIndicator = parentNode.path("instanceIndicator").asText();

        logger.debug("configurationEndpoint: {}", configurationEndpoint, logger.isDebugEnabled());
        logger.debug("thingModelIndicator: {}", thingModelIndicator, logger.isDebugEnabled());
        logger.debug("modelParametersDataTypeIndicator: {}", modelParametersDataTypeIndicator, logger.isDebugEnabled());
        logger.debug("instanceIndicator: {}", instanceIndicator, logger.isDebugEnabled());
    }

    /**
     * Reads Lambdas data from AppConfig
     *
     * @param node json node
     */
    private void getDataFromAppConfigForLambdaConfiguration(JsonNode node) {
        JsonNode parentNode = node.path("thingworxDataFetcherLambda");
        numberOfThingsPerInstance = parentNode.path("numberOfThingsPerInstance").asInt();
        twxCloudConnectorAppInstanceListNameInRedis = parentNode.path("twxCloudConnectorAppInstanceListNameInRedis").asText();
        twxCloudConnectorAppInstanceNamePrefix = parentNode.path("twxCloudConnectorAppInstanceNamePrefix").asText();
        //Lambda Thingworx entity list service config configuration
        parentNode = parentNode.path("thingworxConfig");
        awsBoundPropertyCategoryName = parentNode.path("awsBoundPropertyCategoryName").asText();
        //Lambda Thingworx configuration
        parentNode = parentNode.path("getEntityList");
        timeout = parentNode.path("timeout").asInt();
        awsTags = parentNode.path("awsTags").asText();

        logger.debug("numberOfThingsPerInstance: {}", numberOfThingsPerInstance, logger.isDebugEnabled());
        logger.debug("twxCloudConnectorAppInstanceListNameInRedis: {}", twxCloudConnectorAppInstanceListNameInRedis, logger.isDebugEnabled());
        logger.debug("twxCloudConnectorAppInstanceNamePrefix: {}", twxCloudConnectorAppInstanceNamePrefix, logger.isDebugEnabled());
        logger.debug("awsBoundPropertyCategoryName: {}", awsBoundPropertyCategoryName, logger.isDebugEnabled());
        logger.debug("timeout: {}", timeout, logger.isDebugEnabled());
        logger.debug("awsTags: {}", awsTags, logger.isDebugEnabled());
    }

    /**
     * Reads Thingworx data from AppConfig
     *
     * @param node json node
     */
    private void getDataFromAppConfigForThingworx(JsonNode node) {
        JsonNode parentNode = node.path("thingworx");
        serverUrl = parentNode.path("serverUrl").asText();
        apiKey = parentNode.path("apiKey").asText();
        ignoreSSLError = parentNode.path("ignoreSSLError").asBoolean();
        reconnectInterval = parentNode.path("reconnectInterval").asInt();
        serviceCallTimeoutInMillis = parentNode.path("serviceCallTimeoutInMillis").asInt();
        waitForConnectionTimeoutInMillis = parentNode.path("waitForConnectionTimeoutInMillis").asInt();

        logger.debug("serverUrl: {}", serverUrl, logger.isDebugEnabled());
        logger.debug("apiKey: {}", apiKey, logger.isDebugEnabled());
        logger.debug("ignoreSSLError: {}", ignoreSSLError, logger.isDebugEnabled());
        logger.debug("reconnectInterval: {}", reconnectInterval, logger.isDebugEnabled());
        logger.debug("serviceCallTimeoutInMillis: {}", serviceCallTimeoutInMillis, logger.isDebugEnabled());
        logger.debug("waitForConnectionTimeoutInMillis: {}", waitForConnectionTimeoutInMillis, logger.isDebugEnabled());
    }

    /**
     * Returns default client id
     *
     * @return client id
     */
    private String getDefaultClientId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns default client
     *
     * @return client
     */
    protected AppConfigClient getDefaultClient() {
        return AppConfigClient.create();
    }

    /**
     * Returns default cache item ttl
     *
     * @return cache item ttl
     */
    private Duration getDefaultCacheItemTtl() {
        return Duration.ofSeconds(30);
    }
}

