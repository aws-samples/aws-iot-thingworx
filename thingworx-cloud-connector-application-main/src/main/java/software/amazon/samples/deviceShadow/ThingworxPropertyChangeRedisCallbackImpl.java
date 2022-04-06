package software.amazon.samples.deviceShadow;

import com.thingworx.communications.client.things.VirtualThingPropertyChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.samples.services.AwsIotService;
import software.amazon.samples.thingworx.AwsConnectedThing;

/**
 * Callback implementation for thingworx property change events
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */

@Slf4j
@Component(value = "thingworxCallBack")
public class ThingworxPropertyChangeRedisCallbackImpl implements ThingworxPropertyChangeCallback {

    @Autowired
    private AwsIotService awsIotService;

    @Override
    public void call(VirtualThingPropertyChangeEvent event) {
        AwsConnectedThing awsConnectedThing = ((AwsConnectedThing) event.getSource());

        JSONObject stateNode = new JSONObject();
        JSONObject reportedNode = new JSONObject();
        JSONObject changedPropertyNode = new JSONObject();
        try {
            changedPropertyNode.put(event.getPropertyDefinition().getName(), event.getPrimitiveValue().getValue());
            reportedNode.put("reported", changedPropertyNode);
            stateNode.put("state", reportedNode);
        } catch (JSONException e) {
            log.error("Could not create property change payload: {}", e.getMessage());
        }
        awsIotService.updateThingShadow(awsConnectedThing.getName(), stateNode); // updating device shadow
    }
}