# ThingworxCloudConnectorApplication

This repository contains the source code for AWS-Thingworx connector APP. Once running the application listens to messages passed into a redis topic that it is subscribed to.
The topic name is passed into the application as a Fargate environment variable. 
immediately after start the application initializes a connection to thingworx server and forwards messages that it receives from Redis topic to thingworx. 

This application will be launched automatically as a task inside a Fargate cluster controlled by `InstanceRegistryLambda` lambda function

### Environment Variables
Since this application is automatically run by "InstanceRegistryLambda" blow environment variables are passed to it from that lambda. But if testing locally please set below values:

- `ENVIRONMENT` (DEV, PROD)
- `THINGWORX_CONNECTOR_APP_INSTANCE_NAME` = any string value which represent subscribed topic. in PROD mode this topic name is generated and passed into the app by
  `InstanceRegistryLambda`


### AWS AppConfig
This application uses AWS AppConfig to pull all required configuration values. You need to have below parameters defined in AWS AppConfig:

- `redis` ->
    - `configurationEndpoint` redis endpoint ("redis://127.0.0.1:6379" - by default in develop environment)
- `thingworx` ->
    - `serverUrl` Thingworx server url
    - `apiKey` api Key for allowing the connection to Thingworx
    - `ignoreSSLError` set SSL certificate error to true or false
    - `reconnectInterval` interval between reconnects attempts
    - `processScanRequestTimeoutInMillis` timeout for UpdateSubscribedPropertyValues service
    - `waitForConnectionTimeoutInMillis` connection wait timeout
- `thingworxClientConnectorSpringApp` ->
    - `twxCloudConnectorInstanceNameEnvironmentVariableIndicator`  indicator of environment variable with name instance/topic
    - `messageRouterLambdaPayloadStructure` ->
        - `thingStatusIndicator` a property name with device status
        - `thingTelemetryPayloadIndicator` the name of the property containing the payload of the device
        - `thingNameIndicator` a device name/name of Thing in TWX
        - `thingModelIndicator` a property name that describes the data model
        - `modelParametersDataTypeIndicator` indicator of data type property
    - `thingConnectionStatuses` ->
        - `connected` status name when device is connected
        - `disconnected` status name when device is disconnected
- `thingworxPropertyChangeConsumerSpringApp` ->
    - `propertyChangeQueue` name of the property change queue

### Running the APP locally

- You need to have local instance of redis on address: redis://127.0.0.1:6379
- Define following system environment variables: 
  - `ENVIRONMENT` = DEV
  - `THINGWORX_CONNECTOR_APP_INSTANCE_NAME` = any string value which represent subscribed topic
- Provide aws credentials as system environment variable:
  - `AWS_ACCESS_KEY_ID` 
  - `AWS_SECRET_ACCESS_KEY` 
  - `AWS_SESSION_TOKEN` 
  - `AWS_REGION` 
- Setup AppConfig configuration as presented above.

### Recommended minimum resource requirements for supporting 1000 concurrently connected Things

- Memory = 1024 MB
- CPU = 1 Core