package software.amazon.samples.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import software.amazon.samples.model.PayloadModel;
import software.amazon.samples.model.ThingModel;

import java.util.HashMap;
import java.util.Map;


/**
 * Json Object mapper for converting device payload to virtual thing property
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */

@Component
@Slf4j
public class JsonMessageModelMapper {

    public PayloadModel jsonToPayloadModel(String json) {
        PayloadModel result = null;
        try {
            result = new ObjectMapper().readValue(json, PayloadModel.class);
        } catch (JsonProcessingException e) {
            log.error("Error processing conversion from json to PayloadModel. Payload in json format: [{}]", json);
        }
        return result;
    }

    public ThingModel jsonToThingModel(String name, String model, String attributesJSON) {
        HashMap<String, Map<String, String>> attributes = new HashMap<>();
        if (attributesJSON != null) {
            try {
                attributes = new ObjectMapper().readValue(attributesJSON, attributes.getClass());
            } catch (JsonProcessingException e) {
                log.error("Error processing conversion from json to ThingModel. Thing name: [{}], thing model: [{}], attributes in json format: [{}]", name, model, attributesJSON);
            }
        }
        return new ThingModel(name, model, attributes);
    }

    public JSONObject MessageStringToJSONObject(String message) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(message);
        } catch (JSONException e) {
            log.error("Error processing conversion message from string to JSONObject. Message in String format: [{}]", message);
        }
        return jsonObject;
    }
}


