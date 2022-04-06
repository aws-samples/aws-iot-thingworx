package software.amazon.samples;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.samples.module.AppConfigModule;
import software.amazon.samples.module.RedisModule;

import java.util.Base64;

/**
 * Handler for requests to Lambda function.
 *
 * @author Rafal Wysocki
 * @version 1.0 20 Oct 2021
 */

public class MessageRouterLambda implements RequestHandler<KinesisEvent, String> {

    private static final String STATUS_EVENT_TYPE_PROPERTY = "eventType";
    private static final String STATUS_CONNECTED_VALUE = "connected";
    private static final String STATUS_DISCONNECTED_VALUE = "disconnected";
    private static final String KINESIS_INFO = "kinesis";
    private static final String KINESIS_INFO_PARTITION_KEY_PROPERTY = "partitionKey";
    private static final String KINESIS_INFO_DATA = "data";

    private static final Logger logger = LoggerFactory.getLogger(
            MessageRouterLambda.class + "::LAMBDA_BODY");

    final AppConfigModule config = AppConfigModule.getInstance();
    final RedisModule redis = RedisModule.getInstance();

    @SneakyThrows
    @Override
    public String handleRequest(final KinesisEvent kinesisEvent, final Context context) {

        // Get event JSON from Kinesis
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.convertValue(kinesisEvent, JsonNode.class);

        JsonNode recordsArrayNode = rootNode.path("records");
        if (!thisIsNodeWithValue(recordsArrayNode)) {
            logger.error("Lambda triggered without 'records'!", logger.isErrorEnabled());
            throw new Exception("Lambda triggered without 'records'!");
        }
        if (!recordsArrayNode.isArray()) {
            logger.error("Lambda triggered with 'records', but 'records' is not an array!",
                         logger.isErrorEnabled());
            throw new Exception("Lambda triggered with 'records', but 'records' is not an array!");
        }

        int recordCount = recordsArrayNode.size();
        logger.info("Kinesis Event: {} records in the batch (version 6)", recordCount,
                    logger.isInfoEnabled());

        if (recordCount == 0) {
            logger.error("Lambda triggered with empty records array!", logger.isErrorEnabled());
            throw new Exception("Lambda triggered with empty records array!");
        }

        // Connect to the Redis
        if (!redis.connect(config.getRedisConfigurationEndpoint())) {
            logger.error("Can't connect to redis", logger.isErrorEnabled());
            throw new Exception("Can't connect to redis");
        }

        boolean everythingOk = true;
        try {

            // Loop through each record in Kinesis event
            int recordIndex = -1;
            for (final JsonNode recordNode : recordsArrayNode) {

                try {
                    logger.debug("======================== BEGIN MESSAGE ========================",
                                 logger.isDebugEnabled());
                    recordIndex++;

                    logger.debug("One record from Kinesis: {}", recordNode.toString(),
                                 logger.isDebugEnabled());

                    if (!thisIsNodeWithValue(recordNode)) {
                        logger.warn("Record {} from the Kinesis batch is empty", recordIndex,
                                    logger.isWarnEnabled());
                        continue;
                    }

                    // Get Kinesis info from the record
                    JsonNode kinesisInfo = getKinesisNodeFromJsonRow(recordNode);
                    if (!thisIsNodeWithValue(kinesisInfo)) {
                        logger.warn("Record {} from the Kinesis batch has no specific Kinesis info",
                                    recordIndex, logger.isWarnEnabled());
                        continue;
                    }

                    // Get partition key (client Id)
                    JsonNode partitionKeyNode = kinesisInfo.path(
                            KINESIS_INFO_PARTITION_KEY_PROPERTY);
                    if (!thisIsNodeWithValue(partitionKeyNode)) {
                        logger.warn("Record {} from the Kinesis batch has no partition key",
                                    recordIndex, logger.isWarnEnabled());
                        continue;
                    }
                    String partitionKey = partitionKeyNode.asText();
                    logger.debug("partitionKey=[{}]", partitionKey, logger.isDebugEnabled());

                    // Get payload from the record
                    JsonNode payload = getPayloadNodeFromKinesisNode(kinesisInfo);
                    logger.debug("Kinesis data payload: {}", payload,
                                 logger.isDebugEnabled());
                    if (!thisIsNodeWithValue(payload)) {
                        logger.warn("Row {} from the batch has no payload", recordIndex,
                                    logger.isWarnEnabled());
                        continue;
                    }

                    //handle extracted message
                    handleMessage(thisIsStatusMessage(payload), recordIndex, partitionKey, payload);

                    logger.debug("========================== END MESSAGE ========================",
                                 logger.isDebugEnabled());

                } catch (Exception e) {
                    everythingOk = false;
                    logger.error("Row {} from the batch throws exception: {}", recordIndex,
                                 e.getMessage(), logger.isErrorEnabled());
                }
            }
        } finally {
            redis.disconnect();
        }

        if (everythingOk) {
            logger.info("Kinesis Event handled successfully", logger.isInfoEnabled());
            return "200 OK";
        } else {
            throw new Exception("Kinesis Event handled with lambda internal error");
        }
    }


    @SneakyThrows
    private boolean handleMessage(final boolean isStatusMessage, final int recordIndex,
                                  final String partitionKey,
                                  final JsonNode payload) {

        if (isStatusMessage) {
            logger.debug("--> This is a status message", logger.isDebugEnabled());
        } else {
            logger.debug("--> This is a device metrics message", logger.isDebugEnabled());
        }

        String clientId = partitionKey;

        // Check clientId - exists?
        if (doesntHaveValue(clientId)) {
            logger.warn("Row {} from the batch with payload [ {} ] has no clientId",
                        recordIndex, payload, logger.isWarnEnabled());
            return false;
        }

        // Get data about the device from Redis according to clientId
        String deviceData = "";
        try {
            deviceData = redis.getString(clientId);
        } catch (Exception e) {
            logger.error(
                    "Row {} from the batch witch clientId={}, this key is reserved in the redis.",
                    recordIndex, clientId, logger.isErrorEnabled());
        }
        logger.debug("Row {} from the batch: clientId={}, payload={}, Redis deviceData={}",
                     recordIndex, clientId, payload, deviceData, logger.isDebugEnabled());

        if (deviceData == null || deviceData.isEmpty()) {
            logger.error("Data from not registered device [{}] - ignored", clientId,
                         logger.isErrorEnabled());
        } else {

            ObjectMapper mapper = new ObjectMapper();
            // Prepare parts of the message to send to topic according to payload type
            // (measurement from the device, or status from the AWS)
            JsonNode payloadJson = null;
            if (!isStatusMessage) {
                payloadJson = payload;
            }
            ObjectNode finalJson = (ObjectNode) mapper.readTree(deviceData);

            JsonNode topicNode = finalJson.path(config.getInstanceIndicator());
            if (!thisIsNodeWithValue(topicNode)) {
                logger.error(
                        "Data get from redis related to clientId={} (record {} from kinesis) has " +
                                "no topic to send data - ignored",
                        clientId, recordIndex, logger.isErrorEnabled());
                return false;
            }
            String topic = topicNode.asText();

            if (topic != null && !"".equals(topic.trim())) {

                String statusToSend = null;
                String statusFromPayload = "";

                if (isStatusMessage) {
                    JsonNode statusFromPayloadNode = payload.path(STATUS_EVENT_TYPE_PROPERTY);
                    if (!thisIsNodeWithValue(statusFromPayloadNode)) {
                        logger.error(
                                "Status message for the clientId={} (record {} from " +
                                        "kinesis) has no event type - ignored",
                                clientId, recordIndex, logger.isErrorEnabled());
                        return false;
                    }
                    statusFromPayload = statusFromPayloadNode.asText();

                    statusToSend = chooseStatusMessage(statusFromPayload);
                }

                // Send message only if this is a measurement message, 
                // or this is a status with recognized type
                if (!isStatusMessage || statusToSend != null) {

                    // Prepare message finally and send it
                    prepareJsonToSend(isStatusMessage, payloadJson, finalJson, statusToSend,
                                      clientId);
                    String redisPayload = finalJson.toString();
                    sendMessageToInstance(clientId, topic, redisPayload);

                } else {
                    logger.error("Device with clientId={} has message with unsupperted status {}" +
                                         " - ignored",
                                 clientId, statusFromPayload, logger.isErrorEnabled());
                }
            } else {
                logger.error(
                        "Device with clientId={} has no topic/instance to handle message - ignored",
                        clientId, logger.isErrorEnabled());
            }
        }
        return true;
    }

    private String chooseStatusMessage(final String statusFromPayload) {

        // If config message type is not recognized, return null 
        String returnedStatus = null;
        if (STATUS_CONNECTED_VALUE.equals(statusFromPayload)) {
            returnedStatus = config.getThingConnectionStatusConnected();
        }

        if (STATUS_DISCONNECTED_VALUE.equals(statusFromPayload)) {
            returnedStatus = config.getThingConnectionStatusDisconnected();
        }

        return returnedStatus;
    }

    private void sendMessageToInstance(final String clientId, final String topic,
                                       final String redisPayload) {

        // Send message and analyse count of subscribers.
        long messagesSent = redis.sendStringToTopic(topic, redisPayload);

        logger.debug(
                "Message from {} to topic {} sent. Subscribers {}, payload {}",
                clientId, topic,
                messagesSent, redisPayload, logger.isDebugEnabled());

        // If there is no subscriber, or if there is more than one subscriber, log error.
        // Should be precisely one subscriber.
        if (messagesSent == 0) {
            logger.error("Device {} sends payload to topic {}, " +
                                 "but it hasn't subscriber", clientId, topic,
                         logger.isErrorEnabled());
        } else if (messagesSent > 1) {
            logger.error("Device {} sends payload to topic {}, " +
                                 "but it has {} subscribers", clientId, topic,
                         messagesSent, logger.isErrorEnabled());
        }
    }

    private void prepareJsonToSend(final boolean isStatusMessage, final JsonNode payloadJSON,
                                   final ObjectNode finalJSON, final String statusToSend,
                                   final String clientId) {

        // Finally, prepare message from parts according to the message type
        finalJSON.remove(config.getInstanceIndicator());
        finalJSON.put(config.getThingNameIndicator(), clientId);
        if (isStatusMessage) {
            finalJSON.put(config.getThingTelemetryPayloadIndicator(), (String) null);
            finalJSON.put(config.getThingStatusIndicator(), statusToSend);
            finalJSON.set(config.getThingModelIndicator(), null);
        } else {
            finalJSON.set(config.getThingTelemetryPayloadIndicator(), payloadJSON);
            finalJSON.put(config.getThingStatusIndicator(), (Integer) null);
        }
    }

    private JsonNode getKinesisNodeFromJsonRow(final JsonNode record) {
        JsonNode kinesisNode = null;
        try {
            kinesisNode = record.path(KINESIS_INFO);
        } catch (Exception e) {
            logger.error("Message from Kinesis get data from row: {}", e.getMessage(),
                         logger.isErrorEnabled());
        }
        return kinesisNode;
    }


    private JsonNode getPayloadNodeFromKinesisNode(final JsonNode kinesis) {

        JsonNode payloadNode = null;
        try {
            JsonNode dataNode = kinesis.path(KINESIS_INFO_DATA);
            if (thisIsNodeWithValue(dataNode)) {
                byte[] decodedDataBytes = Base64.getDecoder().decode(dataNode.asText());
                String decodedData = new String(decodedDataBytes);
                ObjectMapper mapper = new ObjectMapper();
                payloadNode = mapper.readTree(decodedData);
            }
        } catch (Exception e) {
            logger.error("Message from Kinesis get payload exception: {}", e.getMessage(),
                         logger.isErrorEnabled());
        }
        return payloadNode;
    }

    private boolean doesntHaveValue(final String s) {
        return s == null || s.isEmpty() || s.trim().isEmpty();
    }

    private boolean thisIsStatusMessage(final JsonNode payload) {
        JsonNode typeIndicatorNode = payload.path(config.getClientStatusMessageIndicatorName());
        return thisIsNodeWithValue(
                typeIndicatorNode) && config.getClientStatusMessageIndicatorValue()
                                            .equals(typeIndicatorNode.asText());
    }

    private boolean thisIsNodeWithValue(final JsonNode node) {
        return node != null && !node.isMissingNode();
    }
}
