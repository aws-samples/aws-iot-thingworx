# Message Router Lambda

This repository contains the source code for thingworx client connector message forwarder lambda function.
It reads data from AWS Kinesis Data Stream and forwards messages it received from it with Iot device payload to an instance
of thingworx client connector application that is assigned to each device. This is done by fetching the data with a key from Redis. 
The Redis cluster maintains a key for each Thing registered in thingworx with information about the Thing data shape and the instance of
cloud connector application that it is assigned to.

## Configuration

### IAM roles for lambda: 
Create a policy named "awsAppConfigAccessPolicy" with body taken from "awsAppConfigAccessPolicy.json" and add it to the IAM role attached to that lambda. The IAM role should have below policies:
- AWSLambdaKinesisExecutionRole
- AWSLambdaVPCAccessExecutionRole

### VPC
Make sure that this lambda is in the same VPC and security group as the one that Redis cluster is in.

### Endpoint Configuration settings
The configuration needed by this lambda function such as Redis must be correctly configured in AppConfig.

### Log levels
This lambda allows configuring log levels by passing an environment value named "LOG_LEVEL" with below values.

Levels of logging:
- DEBUG
- INFO
- WARN
- ERROR

### Production environment
In order to run this lambda in production mode an environment variable with the name "ENVIRONMENT" should be passed into it with value PROD

### Minimum execution requirements for executing this lambda
- Timeout: 70 s
- Memory: 768 mb

### Additional settings depends on required device count 
- Kinesis Data Steam needs at least 2 shards for 1000 devices
- Kinesis trigger in the Lambda needs a Batch Size equal to or greater than 1000

### AWS IoT Rules needed by Lambda
- rule 1 (measurement rule): 
  - Rule query statement: ```SELECT * FROM '/sample/topic'``` where '/sample/topic' is the topic where devices send measurement payload
  - Action 'Send a message to an Amazon Kinesis Stream':
    - Stream name: Your Kinesis stream name
    - Partition key (exactly this): ${clientid()}
    - Role: needs to write to Kinesis
- rule 2 (connected/disconnected status rule): 
  - Rule query statement: ```SELECT "value" as name, * FROM '$aws/events/presence/#'``` where you need to change name and value to values from AppConfig messageRouterLambda -> clientStatusMessageIndicator -> name and -> value
  - Action 'Send a message to an Amazon Kinesis Stream':
    - Stream name: Your Kinesis stream name
    - Partition key (exactly this): ${clientid()}
    - Role: needs to write to Kinesis

### AppConfig variables for this lambda
#### Redis configuration
- `redis` -> `configurationEndpoint` redis endpoint

### Status message payload
- `messageRouterLambda` -> 
  - `clientStatusMessageIndicator` ->
    - `name` the name of the additional, unique property in a config message
    - `value` the value of the additional, unique property in a config message

### Message payload between Redis information about the device, this Lambda and ThingWorx Connector App instance

- `thingworxClientConnectorSpringApp` -> 
  - `messageRouterLambdaPayloadStructure` ->
    - `thingTelemetryPayloadIndicator` the name of the property containing the payload of the device
    - `thingStatusIndicator` a property name with device status
    - `instanceIndicator` topic to send message (ThingWorx Connector App instance)
    - `thingModelIndicator` a property name that describes the data model
    - `thingNameIndicator` indicator for the thing name in the payload send to client connector application
  - `thingConnectionStatuses` ->
    - `connected` status name when device is connected
    - `disconnected` status name when device is disconnected
