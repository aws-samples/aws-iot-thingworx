package software.amazon.samples;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.uuid.Generators;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.samples.appconfig.AppConfigModule;
import software.amazon.samples.fargate.FargateTaskManager;
import software.amazon.samples.redis.RedisModule;
import software.amazon.samples.redis.ThingworxClientApplication;
import software.amazon.samples.thingworx.connection.ThingworxConnection;
import software.amazon.samples.thingworx.connection.config.ThingworxClient;
import software.amazon.samples.thingworx.thingData.ThingworxThingFetcher;
import software.amazon.samples.thingworx.thingData.thing.ThingData;
import software.amazon.samples.thingworx.thingData.thing.ThingProperty;

import java.util.List;
import java.util.Map;

/**
 * Main application. It reads Things from Thingworx, put them to redis and lunch new Fargate tasks each running an instance of Thingworx client connector application.
 *
 * @author Krzysztof Lisowski
 * @version 1.0 20 Oct 2021
 */
public class ThingworxDataFetcherLambda implements RequestHandler<Map<String, Object>, String> {
    private static final Logger logger = LoggerFactory.getLogger(ThingworxDataFetcherLambda.class + "::LAMBDA_BODY");

    private static final String OK_STATE_RETURN = "200";

    private final RedisModule redisModule = RedisModule.getInstance();
    private static final FargateTaskManager farGateTaskManager = FargateTaskManager.getInstance();

    private static String THING_CONNECTOR_INSTANCE_REDIS_FIELD;
    private static String THING_MODEL_REDIS_FIELD;
    private static String PROPERTY_TYPE_REDIS_FIELD;
    private static String INSTANCE_PREFIX_NAME_REDIS_FIELD;

    private static String FARGATE_TASK_DEFINITION;
    private static String FARGATE_CLUSTER;
    private static String FARGATE_CONTAINER_NAME;
    private static String FARGATE_ENVIRONMENT_NAME;

    public static final String ENVIRONMENT = "ENVIRONMENT";

    /**
     * Main application
     *
     * @param event   event object that provides details about the invocation and the function
     * @param context context of function
     * @return status of lambda
     * @throws Exception
     */
    @SneakyThrows
    public String handleRequest(Map<String, Object> event, Context context) {
        try {
            logger.info("Lambda started", logger.isInfoEnabled());
            //Init app config
            AppConfigModule appConfigModule = AppConfigModule.getInstance();
            //Set fields for redis
            THING_CONNECTOR_INSTANCE_REDIS_FIELD = appConfigModule.getInstanceIndicator();
            THING_MODEL_REDIS_FIELD = appConfigModule.getThingModelIndicator();
            PROPERTY_TYPE_REDIS_FIELD = appConfigModule.getModelParametersDataTypeIndicator();
            INSTANCE_PREFIX_NAME_REDIS_FIELD = appConfigModule.getTwxCloudConnectorAppInstanceNamePrefix();
            logger.debug("The following keys will be inserted in to redis received from app config: {} with prefix: {}, {} with type field: {}",
                    THING_CONNECTOR_INSTANCE_REDIS_FIELD, INSTANCE_PREFIX_NAME_REDIS_FIELD,
                    THING_MODEL_REDIS_FIELD, PROPERTY_TYPE_REDIS_FIELD, logger.isDebugEnabled()
            );
            //Set config variables for farGate
            FARGATE_TASK_DEFINITION = appConfigModule.getTaskDefinition();
            FARGATE_CLUSTER = appConfigModule.getCluster();
            FARGATE_CONTAINER_NAME = appConfigModule.getContainerName();
            FARGATE_ENVIRONMENT_NAME = appConfigModule.getTwxCloudConnectorInstanceNameEnvironmentVariableIndicator();
            //Init connection to thingworx
            ThingworxConnection.getInstance().connect(
                    appConfigModule.getServerUrl(), appConfigModule.getApiKey(),
                    appConfigModule.isIgnoreSSLError(), appConfigModule.getReconnectInterval());

            ThingworxClient thingworxClient = ThingworxConnection.getInstance().getConnection();
            //Wait for connection to Thingworx
            thingworxClient.waitForConnection(appConfigModule.getWaitForConnectionTimeoutInMillis());
            //Init connection to redis
            redisModule.connect(appConfigModule.getConfigurationEndpoint());
            //Init network connection to farGate
            farGateTaskManager.createNetworkConfiguration(appConfigModule.getSecurityGroups(), appConfigModule.getSubnets());
            //Getting redis instances in list
            String connectorApplicationInstancesListName = appConfigModule.getTwxCloudConnectorAppInstanceListNameInRedis();
            List<ThingworxClientApplication> thingworxClientApplicationList = redisModule.getThingworxClientApplication(connectorApplicationInstancesListName);
            if (thingworxClientApplicationList.isEmpty()) {
                //If the list in redis is empty then create one thingworxClientApplicationList
                thingworxClientApplicationList.add(createNewInstanceOfThingworxClientApplication(connectorApplicationInstancesListName, 0));
                logger.debug("Redis list {} is either restarted or it's empty", connectorApplicationInstancesListName, logger.isDebugEnabled());
            }
            //Init class to read things from Thingworx
            ThingworxThingFetcher thingworxThingFetcher = new ThingworxThingFetcher(
                    thingworxClient, appConfigModule.getServiceCallTimeoutInMillis(),
                    appConfigModule.getTimeout(), appConfigModule.getAwsTags(),
                    appConfigModule.getAwsBoundPropertyCategoryName()
            );
            long numberOfThingsPerInstance = appConfigModule.getNumberOfThingsPerInstance();
            if (thingworxClient.isConnected()) {
                thingworxThingFetcher.readThingsDataFromThingworx();

                logger.info("Number of things to update in redis: {}", thingworxThingFetcher.getThingDataList().size(), logger.isInfoEnabled());
                //Loop through devices
                for (ThingData thing : thingworxThingFetcher.getThingDataList()) {
                    handleOneThing(connectorApplicationInstancesListName, thingworxClientApplicationList, numberOfThingsPerInstance, thing);
                }
                updateThingworxClientApplicationInstanceListInRedis(connectorApplicationInstancesListName, thingworxClientApplicationList, appConfigModule.getEnvironmentAppConfig());
            } else {
                throw new Exception("Couldn't connect to thingworx");
            }
        } catch (Exception e) {
            throw new Exception(e);
        } finally {
            this.redisModule.endConnection();
        }
        return OK_STATE_RETURN;
    }

    /**
     * Handles one Thing. Adds to redis and runs new instance if needed.
     *
     * @param connectorApplicationInstancesListName Thingworx client application redis list name
     * @param thingworxClientApplicationList        Thingworx client application list in redis
     * @param numberOfThingsPerInstance             number of Things per instance
     * @param thing                                 ThingData object
     */
    private void handleOneThing(final String connectorApplicationInstancesListName, final List<ThingworxClientApplication> thingworxClientApplicationList,
                                final long numberOfThingsPerInstance, final ThingData thing) {
        int twxClientInstanceIndex = 0;
        int i = 0;
        //Checking if Thing name is set in one of the Thingworx client application instances already in redis
        boolean isThingNameAssignedToTwxInstance = false;
        for (ThingworxClientApplication thingworxClientApplication : thingworxClientApplicationList) {
            if (thingworxClientApplication.getThingNames().contains(thing.getName())) {
                twxClientInstanceIndex = i;
                isThingNameAssignedToTwxInstance = true;
                break;
            }
            i++;
        }
        //If Thing name is not registered in any Thingworx client application instance then add Thing name to the Thingworx client application instance with free slots
        if (!isThingNameAssignedToTwxInstance) {
            twxClientInstanceIndex = getTwxClientInstanceIndexAndHandleRedisInstanceList(connectorApplicationInstancesListName, thingworxClientApplicationList, numberOfThingsPerInstance, thing, twxClientInstanceIndex);
        }
        //Checking if Thing name of Thing is same as connector application instance list name or one of Thingworx client application instances
        if (!checkIfThingNameIsNotSameAsInstanceList(thing.getName(), connectorApplicationInstancesListName)) {
            //Writing Thing name with modelData to redis
            setThingDataInRedis(
                    thing.getName(), thingworxClientApplicationList.get(twxClientInstanceIndex).getInstanceName(),
                    createJsonModelForProperties(thing.getThingProperties())
            );
        }
    }

    /**
     * Gets Thingworx client instance index and adds Thing to instance or creates new instance if needed.
     *
     * @param connectorApplicationInstancesListName Thingworx client application redis list name
     * @param thingworxClientApplicationList        Thingworx client application list in redis
     * @param numberOfThingsPerInstance             number of Things per instance
     * @param thing                                 ThingData object
     * @param twxClientInstanceIndex                Thingworx client application instance index in redis list
     * @return index of Thingworx client application instance in redis list
     */
    private int getTwxClientInstanceIndexAndHandleRedisInstanceList(final String connectorApplicationInstancesListName, final List<ThingworxClientApplication> thingworxClientApplicationList,
                                                                    final long numberOfThingsPerInstance, final ThingData thing, int twxClientInstanceIndex) {
        int i;
        i = 0;
        //Check if there exist Thingworx client application instance with available slot
        boolean canAddThingToExistingTwxClientInstance = false;
        for (ThingworxClientApplication thingworxClientApplication : thingworxClientApplicationList) {
            if (thingworxClientApplication.getThingNames().size() < numberOfThingsPerInstance) {
                canAddThingToExistingTwxClientInstance = true;
                twxClientInstanceIndex = i;
                break;
            }
            i++;
        }
        //Creating new Thingworx client application instance with "isDead" set to true
        if (!canAddThingToExistingTwxClientInstance) {
            //Create new thingworxClientApplicationList
            thingworxClientApplicationList.add(createNewInstanceOfThingworxClientApplication(connectorApplicationInstancesListName, thingworxClientApplicationList.size()));
            twxClientInstanceIndex = thingworxClientApplicationList.size() - 1;
        }
        //Checking if Thing name is same as connector application instance list name or one of Thingworx client application instances
        if (!checkIfThingNameIsNotSameAsInstanceList(thing.getName(), connectorApplicationInstancesListName)) {
            //Adding Thing name to Thingworx client application instance
            thingworxClientApplicationList.get(twxClientInstanceIndex).addThingName(thing.getName());
        }
        return twxClientInstanceIndex;
    }

    /**
     * Updates Thingworx client application instance list in redis
     *
     * @param instanceListName            Thingworx client application instance name
     * @param thingworxClientApplications Thingworx client application list
     * @throws JsonProcessingException
     */
    @SneakyThrows
    private void updateThingworxClientApplicationInstanceListInRedis(final String instanceListName, final List<ThingworxClientApplication> thingworxClientApplications,
                                                                     final String environmentAppConfig
    ) {
        for (ThingworxClientApplication thingworxClientApplication : thingworxClientApplications) {
            if (thingworxClientApplication.getInstanceIsDead()) {
                launchFargateTask(thingworxClientApplication.getInstanceName(), environmentAppConfig);
                thingworxClientApplication.setInstanceDead(false);
            }
            try {
                String instanceJsonFormat = new ObjectMapper().writeValueAsString(thingworxClientApplication);

                RedisModule.getInstance().updateThingworxClientApplicationInstanceElementInRedisList(
                        instanceListName, thingworxClientApplication.getIndexInRedisList(),
                        instanceJsonFormat
                );
                logger.debug("Updating Thingworx client application with value: {}", instanceJsonFormat);
            } catch (JsonProcessingException e) {
                logger.error("Couldn't parse object ThingworxClientApplication into json: {}", e, logger.isErrorEnabled());
                throw e;
            }
        }
    }

    /**
     * Launches Fargate task
     *
     * @param instanceName name of Thingworx client application instance
     */
    private void launchFargateTask(final String instanceName, final String environmentAppConfig) {
        farGateTaskManager.createRunTaskRequest(FARGATE_TASK_DEFINITION, FARGATE_CLUSTER, FARGATE_CONTAINER_NAME, FARGATE_ENVIRONMENT_NAME, instanceName, environmentAppConfig);
        farGateTaskManager.runTask();
    }

    /**
     * Creates new instance of Thingworx client application
     *
     * @param instanceListName          name of Thingworx client application instance list
     * @param numberOfExistingInstances number of existing instances in Thingworx client application instance list
     * @return ThingworxClientApplication instance
     * @throws JsonProcessingException
     */
    @SneakyThrows
    private ThingworxClientApplication createNewInstanceOfThingworxClientApplication(final String instanceListName, final int numberOfExistingInstances) {
        String instanceName = INSTANCE_PREFIX_NAME_REDIS_FIELD + "_" + Generators.timeBasedGenerator().generate();
        ThingworxClientApplication thingworxClientApplication = new ThingworxClientApplication();
        thingworxClientApplication.setInstanceName(instanceName);
        thingworxClientApplication.setInstanceDead(true);
        thingworxClientApplication.setIndexInRedisList(numberOfExistingInstances);

        if (RedisModule.getInstance().getString(instanceName) == null) {
            try {
                RedisModule.getInstance().addInstanceToRedisList(instanceListName, new ObjectMapper().writeValueAsString(thingworxClientApplication));
            } catch (JsonProcessingException e) {
                logger.error("Couldn't parse object ThingworxClientApplication into json: {}", e, logger.isErrorEnabled());
                throw e;
            }
        }

        logger.debug("Adding dead Thingworx client application instance to redis {}", instanceName, logger.isDebugEnabled());
        return thingworxClientApplication;
    }

    /**
     * Set Thing data in redis
     *
     * @param thingName     Thing name
     * @param thingInstance instance name that Thing is attached to
     * @param modelData     Thing model data
     * @throws JsonProcessingException
     */
    @SneakyThrows
    private void setThingDataInRedis(final String thingName, final String thingInstance, final ObjectNode modelData) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put(THING_CONNECTOR_INSTANCE_REDIS_FIELD, thingInstance);
        node.set(THING_MODEL_REDIS_FIELD, modelData);
        String jsonString;
        try {
            jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (JsonProcessingException e) {
            logger.error("Couldn't create json for Thing: {}, to send to redis: {}", thingName, e, logger.isErrorEnabled());
            throw e;
        }
        RedisModule.getInstance().setString(thingName, jsonString);
        String redisValue = RedisModule.getInstance().getString(thingName);
        logger.debug("Data send to redis about Things: ", logger.isDebugEnabled());
        logger.debug("key: {}, value: {}", thingName, redisValue, logger.isDebugEnabled());
    }

    /**
     * Checks if the n value of Thing is same as name of Thingworx client application instance list
     *
     * @param thingName        Thing name which is Id of device in Aws Iot
     * @param instanceListName name of instance list
     * @return false if Thing name is different then instanceListName
     */
    private boolean checkIfThingNameIsNotSameAsInstanceList(final String thingName, final String instanceListName) {
        if (thingName.equals(instanceListName)) {
            logger.warn("Thing name: {} of Thing is same as name of instance list: {}, couldn't add Thing to redis", thingName, instanceListName, logger.isWarnEnabled());
            return true;
        }
        return false;
    }

    /**
     * Parsing Thing properties into ObjectNode
     *
     * @param properties Thing properties
     * @return ObjectNode of Thing properties
     */
    private ObjectNode createJsonModelForProperties(final List<ThingProperty> properties) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode modelProperties = mapper.createObjectNode();
        for (ThingProperty thingProperty : properties) {
            ObjectNode node = mapper.createObjectNode();
            node.put(PROPERTY_TYPE_REDIS_FIELD, thingProperty.getBaseType());
            modelProperties.put(thingProperty.getName(), node);
        }
        return modelProperties;
    }
}
