package software.amazon.samples.thingworx;

import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.client.things.VirtualThing;
import com.thingworx.communications.client.things.VirtualThingPropertyChangeListener;
import com.thingworx.metadata.PropertyDefinition;
import com.thingworx.types.BaseTypes;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import software.amazon.samples.configuration.awsAppconfig.AppConfigModule;
import software.amazon.samples.deviceShadow.ThingworxPropertyChangeCallback;
import software.amazon.samples.events.ThingworxPropertyChangeListener;
import software.amazon.samples.model.ThingModel;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Instance VirtualThing representing AWS connected device
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */

@Slf4j
@Getter
public class AwsConnectedThing extends VirtualThing {

    private final int processScanRequestTimeoutInMillis;
    private final String modelParametersDataTypeIndicator;
    private final VirtualThingPropertyChangeListener listener;

    public AwsConnectedThing(String name, String description, String identifier, ConnectedThingClient client,
                             AppConfigModule appConfigModule, ThingworxPropertyChangeCallback callback) {
        super(name, description, identifier, client);
        listener = new ThingworxPropertyChangeListener(callback);
        this.addPropertyChangeListener(listener);
        modelParametersDataTypeIndicator = appConfigModule.getModelParametersDataTypeIndicator();
        processScanRequestTimeoutInMillis = appConfigModule.getProcessScanRequestTimeoutInMillis();
    }

    @Override
    public void processScanRequest() throws Exception {
        super.updateSubscribedProperties(processScanRequestTimeoutInMillis);
    }

    public void updateProperties(ThingModel thingModel) {
        Map<String, Map<String, String>> thingModelParameters = thingModel.getModel();
        Set<String> parametersFromThingKeySet = this.getProperties().keySet();
        Set<String> parametersFromModelKeySet = thingModel.getModel().keySet();
        boolean shouldRemoveProperties = false;
        for (String thingParameterKey : parametersFromThingKeySet) {
            if (propertyNotInSet(thingParameterKey, parametersFromModelKeySet)) { // if property not in model then remove from thing
                log.debug("property [{}] from [{}] does not exist in the model.", thingParameterKey, thingModel.getDeviceName());
                shouldRemoveProperties = true;
            } else {
                String baseTypeString = thingModelParameters.get(thingParameterKey).get(modelParametersDataTypeIndicator);
                // if model data type does not match the thing data type for the same property remove the property from the model
                if (!baseTypeString.equalsIgnoreCase(this.getProperty(thingParameterKey).getPropertyDefinition().getBaseType().friendlyName())) {
                    log.debug("data type for property [{}] from [{}] does not match the data type on the model model.", thingParameterKey, thingModel.getDeviceName());
                    shouldRemoveProperties = true;
                }
            }
        }

        if (shouldRemoveProperties) {
            this.getProperties().clear();
            log.debug("clearing thing properties of thing {} since the model was updated", thingModel.getDeviceName());
        }

        for (String modelParameterKey : parametersFromModelKeySet) {
            if (propertyNotInSet(modelParameterKey, parametersFromThingKeySet)) { // if property not on the thing add from model
                log.debug("Adding property [{}] to [{}] since it was added to the model.", modelParameterKey, thingModel.getDeviceName());
                String baseTypeString = thingModelParameters.get(modelParameterKey).get(modelParametersDataTypeIndicator);
                BaseTypes baseType = BaseTypes.fromFriendlyName(baseTypeString);
                if (baseType == null) {
                    log.warn("Property [{}] couldn't be added to thing {}. Can't recognize type [{}] of the parameter", modelParameterKey, thingModel.getDeviceName(), baseTypeString);
                } else {
                    PropertyDefinition propertyDefinition = new PropertyDefinition(modelParameterKey, "", baseType);
                    this.defineProperty(propertyDefinition);
                }
            }
        }
    }

    private boolean propertyNotInSet(String property, Set<String> parametersSet) {
        for (String parameterName : parametersSet) {
            if (Objects.equals(property, parameterName)) {
                return false;
            }
        }
        return true;
    }
}

