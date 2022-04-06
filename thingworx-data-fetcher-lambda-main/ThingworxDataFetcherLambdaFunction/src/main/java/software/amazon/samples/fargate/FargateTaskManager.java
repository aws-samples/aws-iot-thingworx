package software.amazon.samples.fargate;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.AwsVpcConfiguration;
import software.amazon.awssdk.services.ecs.model.ContainerOverride;
import software.amazon.awssdk.services.ecs.model.KeyValuePair;
import software.amazon.awssdk.services.ecs.model.NetworkConfiguration;
import software.amazon.awssdk.services.ecs.model.RunTaskRequest;
import software.amazon.awssdk.services.ecs.model.TaskOverride;

import java.util.ArrayList;
import java.util.List;

import static software.amazon.samples.ThingworxDataFetcherLambda.ENVIRONMENT;

/**
 * Launches task of Thingworx instance in Fargate
 *
 * @author Krzysztof Lisowski
 * @version 1.0 20 Oct 2021
 */
public class FargateTaskManager {
    private static final Logger logger = LoggerFactory.getLogger(FargateTaskManager.class + "::LAMBDA_BODY");

    private static final String ASSIGN_PUBLIC_IP_IN_NETWORK_CONFIGURATION = "DISABLED";

    private static final int NUMBER_OF_TASKS_TO_RUN_IN_TASK_REQUEST = 1;
    private static final String LAUNCH_TYPE_IN_TASK_REQUEST = "FARGATE";
    private static final String STARTED_BY_IN_TASK_REQUEST = "InstanceThingworxRegistryRedisLambdaFunction";

    private static final FargateTaskManager instance = new FargateTaskManager();

    private NetworkConfiguration networkConfiguration = null;
    private DefaultCredentialsProvider defaultCredentialsProvider = null;
    private RunTaskRequest runTaskRequest = null;

    private FargateTaskManager() {
    }

    /**
     * Creates network configuration for Fargate
     *
     * @param securityGroups security group
     * @param subnets        subnets
     */
    public void createNetworkConfiguration(final ArrayList<String> securityGroups, final ArrayList<String> subnets) {
        if (networkConfiguration == null) {
            logger.debug("Creating network configuration with:", logger.isDebugEnabled());
            logger.debug("Public ip: {}", ASSIGN_PUBLIC_IP_IN_NETWORK_CONFIGURATION, logger.isDebugEnabled());
            logger.debug("Security groups: {}", securityGroups, logger.isDebugEnabled());
            logger.debug("Subnets: {}", subnets, logger.isDebugEnabled());

            this.networkConfiguration = NetworkConfiguration.builder()
                    .awsvpcConfiguration(
                            AwsVpcConfiguration.builder()
                                    .assignPublicIp(ASSIGN_PUBLIC_IP_IN_NETWORK_CONFIGURATION)
                                    .securityGroups(securityGroups)
                                    .subnets(subnets.toArray(new String[0]))
                                    .build()
                    ).build();
        }
        if (defaultCredentialsProvider == null) {
            logger.debug("Creating default credentials provider", logger.isDebugEnabled());
            this.defaultCredentialsProvider = DefaultCredentialsProvider.builder().build();
        }
    }

    /**
     * Creates run task request to launch new instance in Fargate
     *
     * @param taskDefinition                    task definition
     * @param cluster                           cluster
     * @param containerName                     container name
     * @param environmentName                   environment name variable indicator
     * @param environmentValueVariableIndicator environment value variable indicator
     * @param environmentValueAppConfig         environment value for app config
     */
    public void createRunTaskRequest(final String taskDefinition, final String cluster, final String containerName,
                                     final String environmentName, final String environmentValueVariableIndicator,
                                     final String environmentValueAppConfig) {
        logger.debug("Creating run task request with:", logger.isDebugEnabled());
        logger.debug("Task definition: {}", taskDefinition, logger.isDebugEnabled());
        logger.debug("Cluster: {}", cluster, logger.isDebugEnabled());
        logger.debug("Count: {}", NUMBER_OF_TASKS_TO_RUN_IN_TASK_REQUEST, logger.isDebugEnabled());
        logger.debug("Network configuration:{}", this.networkConfiguration, logger.isDebugEnabled());
        logger.debug("Launch type: {}", LAUNCH_TYPE_IN_TASK_REQUEST, logger.isDebugEnabled());
        logger.debug("Started by: {}", STARTED_BY_IN_TASK_REQUEST, logger.isDebugEnabled());
        logger.debug("Container name: {}", containerName, logger.isDebugEnabled());
        logger.debug("Environment name: {}", environmentName, logger.isDebugEnabled());
        logger.debug("Environment value: {}", environmentValueVariableIndicator, logger.isDebugEnabled());
        logger.debug("Environment name: {}", ENVIRONMENT, logger.isDebugEnabled());
        logger.debug("Environment value: {}", environmentValueAppConfig, logger.isDebugEnabled());

        //Creates environment variables for instances
        List<KeyValuePair> keyValuePairs = new ArrayList<>();
        keyValuePairs.add(KeyValuePair.builder()
                .name(environmentName)
                .value(environmentValueVariableIndicator)
                .build());
        keyValuePairs.add(KeyValuePair.builder()
                .name(ENVIRONMENT)
                .value(environmentValueAppConfig)
                .build());

        this.runTaskRequest = RunTaskRequest.builder()
                .taskDefinition(taskDefinition)
                .cluster(cluster)
                .count(NUMBER_OF_TASKS_TO_RUN_IN_TASK_REQUEST)
                .networkConfiguration(this.networkConfiguration)
                .launchType(LAUNCH_TYPE_IN_TASK_REQUEST)
                .startedBy(STARTED_BY_IN_TASK_REQUEST)
                .overrides(TaskOverride.builder()
                        .containerOverrides(
                                ContainerOverride.builder()
                                        .name(containerName)
                                        .environment(keyValuePairs)
                                        .build()
                        ).build()
                ).build();
    }

    /**
     * Runs task that launch new instance in Fargate
     *
     * @throws Exception
     */
    @SneakyThrows
    public void runTask() {
        if (this.defaultCredentialsProvider == null) {
            throw new Exception("defaultCredentialsProvider is null, can't create task");
        } else if (this.runTaskRequest == null) {
            throw new Exception("runTaskRequest is null, can't create task");
        } else {
            logger.debug("Running task with default credentials provider and task request", logger.isDebugEnabled());
            EcsClient.builder()
                    .credentialsProvider(this.defaultCredentialsProvider)
                    .build().runTask(this.runTaskRequest);

            runTaskRequest = null;
        }
    }

    /**
     * Returns instance of FargateTaskManager class
     *
     * @return FargateTaskManager
     */
    public static FargateTaskManager getInstance() {
        return instance;
    }
}
