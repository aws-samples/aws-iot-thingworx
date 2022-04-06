# Thingworx Data Fetcher Lambda

This repository contains the source code for AWS lambda function that reads Things from Thingworx, put them to redis and lunch new Fargate tasks each running an instance of Thingworx client connector application. Each application is assigned to set number of devices and if there are more devices added in Thingworx than there are Fargate tasks available to handle those devices, this lambda will launch new Fargate task.

This lambda should be scheduled to run on set intervals so that is able to fetch new Things into Redis. The time interval for running this lambda is set in cloud formation script and can be changed directly in AWS console.

## Configuration
### IAM roles
Create a policy named `awsAppConfigAccessPolicy` with body taken from file `AppConfigAccessPolicy.json` and add it to the IAM role attached to that lambda. The IAM role should below policies:
- AmazonECS_FullAccess
- AWSLambdaVPCAccessExecutionRole

### VPC
Make sure this lambda is in the same VPC and security group as the one that Redis cluster is in.

### Thingworx
This lambda function requires below configuration in Thingworx:
- Each Thing has to be attached to a Tag with  `name` and `Vocabulary Terms` which should be similar to the value configured in AWS AppConfig variable `awsTags`. Default value of `awsTags` is `AWS_CONNECTED_THING:AWS_CONNECTED_THING`. 
- Each Thing created in Thingworx must be named with value set similar to the value used when connecting the Thing to AWS IoT Core
- Each of the properties defined on an AWS connected Thing in Thingworx must have a category value set indicating that the property value will be fetched from AWS IoT Core. This category value is configured in AppConfig variable `awsBoundPropertyCategoryName`. (Any other property of the Thing in Thingworx will not receive telemetry updates from IoT Core if the category name of it is not set as defined in `awsBoundPropertyCategoryName`).
  By default, vocabulary terms is set to `AWSConnectorBound` in AppConfig.
### Endpoint Configuration settings
The configuration needed by this lambda function such as Redis, Thingworx endpoints and the name of Fargate cluster used to launch client connector application instances must be correctly configured in AppConfig.

### Log levels
This lambda allows configuring log levels by passing an environment value named `LOG_LEVEL` with below values.

Levels of logging:
- DEBUG
- INFO
- WARN
- ERROR

### Production environment
In order to run this lambda in production mode an environment variable with the name `ENVIRONMENT` should be passed into it with value PROD

### Minimum execution requirements for executing this lambda
- Timeout: 15 min
- Memory: 1024 mb

### AppConfig variables for this lambda
#### Thingworx configuration 
- `serverUrl` Thingworx server url
- `apiKey` api Key for allowing the connection to Thingworx
- `ignoreSSLError` set SSL certificate error to true or false
- `reconnectInterval` interval between reconnects attempts
- `serviceCallTimeoutInMillis` timeout for Thingworx services
- `waitForConnectionTimeoutInMillis` connection wait timeout
- `awsBoundPropertyCategoryName` name category for property to detect properties to fetch with AWS
- `timeout` timeout for getEntityList service
- `awsTags` tag for Things to fetch Things from Thingworx

#### Redis configuration
- `configurationEndpoint` redis endpoint
- `thingModelIndicator` indicating the name of the filed for Thing model in payload sent by messageRouterLambda
- `modelParametersDataTypeIndicator` indicating the name of the filed for data type of parameters in payload sent by messageRouterLambda
- `instanceIndicator` indicating the name of the cloud connector application instance in payload sent by messageRouterLambda

#### Fargate configuration
- `securityGroups` Fargate security
- `subnets` Fargate subnets  
- `taskDefinition` task definition for cloud connector app
- `cluster` Fargate cluster name
- `containerName` container name assigned to Fargate tasks
- `twxCloudConnectorInstanceNameEnvironmentVariableIndicator` name of environment variable passed to cloud connector application instances in Fargate 
- `numberOfThingsPerInstance` number of Things assigned to each instance of cloud connector application in Fargate cluster
- `twxCloudConnectorAppInstanceListNameInRedis` the name of list variable in Redis holding information about client application connector instances
- `twxCloudConnectorAppInstanceNamePrefix` prefix for the name of client application instance


## Further information
This lambda automatically launches new instances of Thingworx cloud connector application into a Fargate cluster. This behaviour is controlled by setting how many devices per individual instance of the application is allowed. For example if there are 23 Things created in Thingworx and the value set in AppConfig for `numberOfDevicesPerInstance` is 10 then this lambda would launch 3 instances of the cloud connector application. The application only scales up meaning that existing task will not be stopped. This means if 10 Things are removed from Things we will still maintain 3 instances of application inside Fargate. If the value of `numberOfDevicesPerInstance` is changed, all data in redis must be cleared.   

## Estimated run time
Estimated run time for this lambda to read 10000 Things from Thingworx and putting that information into Redis then launching required Fargate tasks to run required instances of cloud connector application instances would be as below: (this assumption is on the bases of assigning 1000 Things to each instance of cloud application connector instances)
- Total lambda execution time: 76874.44 ms
- Time takes to read Things from Thingworx: 27677 ms