package software.amazon.samples.events;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.springframework.context.ApplicationContext;
import software.amazon.samples.configuration.awsAppconfig.AppConfigModule;
import software.amazon.samples.services.ThingworxServiceImpl;
import software.amazon.samples.model.PayloadModel;
import software.amazon.samples.model.ThingModel;

import java.util.Objects;

/**
 * Redis topic subscriber
 * this handles redis events with device data coming from lambda
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */

@Slf4j
public class RedisTopicListener implements MessageListener<String> {

    private final ApplicationContext applicationContext;
    private final RedissonClient redissonClient;
    private final String CONNECTED;
    private final String DISCONNECTED;

    public RedisTopicListener(ApplicationContext applicationContext, RedissonClient redissonClient, AppConfigModule appConfigModule) {
        this.applicationContext = applicationContext;
        this.redissonClient = redissonClient;
        this.CONNECTED = appConfigModule.getConnectedStatus();
        this.DISCONNECTED = appConfigModule.getDisconnectedStatus();
    }

    @Override
    public void onMessage(CharSequence charSequence, String message) {
        log.debug("Received Message from Topic: " + message);
        ThingworxServiceImpl thingWorxService = null;
        try {
            thingWorxService = applicationContext.getBean(ThingworxServiceImpl.class);
        } catch (Exception e) {
            log.error("Cannot handle message from topic because Spring boot application context is shutdown");
            redissonClient.shutdown();
            System.exit(1);
        }
        JSONObject json = thingWorxService.convertToJSONObject(message);
        PayloadModel payloadModel = thingWorxService.convertToPayloadModelObject(json);
        ThingModel thingModel = thingWorxService.convertToThingModelObject(json);
        boolean statusIsNotNull = Objects.nonNull(thingModel.getStatus());
        if (thingModel.getDeviceName() != null) { // checking if device name exist in payload
            if (statusIsNotNull) { // if message type is thing stats
                if (thingModel.getStatus().equals(DISCONNECTED)) { // if thing status is disconnected then unbind the thing
                    log.debug("requesting to unbind thing [{}]from thingworx", thingModel.getDeviceName());
                    thingWorxService.unbindThing(thingModel.getDeviceName());
                }
                if (thingModel.getStatus().equals(CONNECTED)) { // if thing status is connected then bind the thing
                    log.debug("requesting to bind thing [{}] to thingworx", thingModel.getDeviceName());
                    thingWorxService.bindNewThing(thingModel.getDeviceName());
                }
            } else {
                log.debug("going to send payload for thing [{}] to thingworx", thingModel.getDeviceName());
                thingWorxService.bindNewThing(thingModel.getDeviceName());
                thingWorxService.updateModel(thingModel);
                thingWorxService.sendPayloadToThingworx(payloadModel, thingModel);
            }
        } else {
            log.error("Cannot send payload to thingworx thing because name is missing in the message.");
        }
    }
}