package software.amazon.samples.services;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.samples.configuration.awsAppconfig.AppConfigModule;
import software.amazon.samples.deviceShadow.ThingworxPropertyChangeCallback;
import software.amazon.samples.thingworx.AwsConnectedThing;
import software.amazon.samples.thingworx.ThingworxClientSingleton;
import software.amazon.samples.mapper.JsonMessageModelMapper;
import software.amazon.samples.model.PayloadModel;
import software.amazon.samples.model.ThingModel;

import java.util.Map;
import java.util.Set;

/**
 * Spring boot service for binding and unbinding AWS device to thingworx
 * this service also sends data to thingworx coming from AWS device
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */

@Service
@Slf4j
public class ThingworxServiceImpl {

    @Autowired
    JsonMessageModelMapper mapper;
    @Autowired
    AppConfigModule appConfigModule;

    @Autowired
    @Qualifier("thingworxCallBack")
    private ThingworxPropertyChangeCallback callback;


    public void updateModel(ThingModel thingModel) {
        AwsConnectedThing thing = (AwsConnectedThing) ThingworxClientSingleton.INSTANCE.getConnectedThingClient().getThing(thingModel.getDeviceName());
        thing.updateProperties(thingModel);
        log.debug("updated Thing model for Thing {} to [{}]", thing.getName(), thingModel.getModel());
    }

    public void unbindThing(String thingName) {
        log.debug("going to unbind thing {}", thingName);
        AwsConnectedThing awsConnectedThing = (AwsConnectedThing) ThingworxClientSingleton.INSTANCE.getConnectedThingClient().getThing(thingName);
        if (awsConnectedThing != null) {
            ThingworxClientSingleton.INSTANCE.unBindThing(awsConnectedThing);
        } else {
            log.warn("could not unbind thing [{}] or it is already unbound", thingName);
        }
    }

    public void bindNewThing(String thingName) {
        if (ThingworxClientSingleton.INSTANCE.getConnectedThingClient().getThing(thingName) == null) { // if thing is not already bound then bind it
            log.debug("thing [{}] is not bound. going to bind it", thingName);
            AwsConnectedThing awsConnectedThing = new AwsConnectedThing(thingName, "", null, ThingworxClientSingleton.INSTANCE.getConnectedThingClient(), appConfigModule, callback);
            ThingworxClientSingleton.INSTANCE.bindThing(awsConnectedThing);
        }
    }

    public void sendPayloadToThingworx(PayloadModel payloadModel, ThingModel thingModel) {
        AwsConnectedThing awsConnectedThing = (AwsConnectedThing) ThingworxClientSingleton.INSTANCE.getConnectedThingClient().getThing(thingModel.getDeviceName());
        if (awsConnectedThing.isBound()) { // proceed if thing is bound
            if (awsConnectedThing.getProperties().isEmpty()) {
                log.warn("Payload will not be sent to thingworx, because thing model is not present in the messages");
            } else {
                Map<String, Map<String, String>> parametersToAdd = thingModel.getModel();
                Set<String> parametersNameSet = parametersToAdd.keySet();
                // removing the property change listener before updating values of properties because otherwise changes to thing properties will trigger the listener
                awsConnectedThing.removePropertyChangeListener(awsConnectedThing.getListener());
                if (awsConnectedThing.getPropertySubscriptions().isEmpty()) {
                    // this forces the virtual thing to load info table definition for all subscribed properties. if not called property change types will not be respected
                    awsConnectedThing.loadPropertySubscriptions();
                }
                try {
                    for (String parameterName : parametersNameSet) {
                        awsConnectedThing.setProperty(parameterName, payloadModel.getParameters().get(parameterName));
                    }
                    // re-enabling the listener so that changes from thingworx for all properties will be handled
                    awsConnectedThing.addPropertyChangeListener(awsConnectedThing.getListener());
                } catch (Exception e) {
                    log.error("Could not find parameter [{}] in payload.", parametersNameSet);
                }
                try {
                    awsConnectedThing.processScanRequest();
                    log.debug("Payload was sent to Thingworx. Payload: [{}]", payloadModel.toString());
                } catch (Exception eProcessing) {
                    log.error("Error updating subscribed properties for [{}]: " + eProcessing, awsConnectedThing.getName());
                }
            }
        } else {
            bindNewThing(thingModel.getDeviceName()); // bind thing if not bound already
        }
    }

    public JSONObject convertToJSONObject(String message) {
        return mapper.MessageStringToJSONObject(message);
    }

    public PayloadModel convertToPayloadModelObject(JSONObject json) {
        Object payload;
        if (json.isNull(appConfigModule.getThingTelemetryPayloadIndicator())) {
            return null;
        }
        try {
            payload = json.get(appConfigModule.getThingTelemetryPayloadIndicator());
        } catch (JSONException e) {
            log.error("Error processing conversion from json to PayloadModelObject. Couldn't convert json message [{}] to PayloadModel object. Payload is equals null.", json);
            return null;
        }
        return mapper.jsonToPayloadModel(String.valueOf(payload));
    }

    public ThingModel convertToThingModelObject(JSONObject json) {
        String thingName;
        String status;
        String attributesString;

        thingName = getJsonMessageParameterAndCheckIfItsNull(json, appConfigModule.getThingNameIndicator());
        status = getJsonMessageParameterAndCheckIfItsNull(json, appConfigModule.getThingStatusIndicator());
        attributesString = getJsonMessageParameterAndCheckIfItsNull(json, appConfigModule.getThingModelIndicator());

        return mapper.jsonToThingModel(thingName, status, attributesString);
    }

    private String getJsonMessageParameterAndCheckIfItsNull(JSONObject json, String parameter) {
        String thingName = null;
        if (json.isNull(parameter)) {
            return null;
        } else {
            try {
                thingName = json.get(parameter).toString();
            } catch (JSONException e) {
                log.error("Error processing conversion from json to [{}] String. Json message: [{}].", parameter, json);
            }
        }
        return thingName;
    }
}
