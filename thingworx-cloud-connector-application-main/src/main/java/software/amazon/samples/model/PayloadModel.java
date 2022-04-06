package software.amazon.samples.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;


/**
 * Payload model received from device
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */

@Setter
@Getter
@ToString
public class PayloadModel {
    Map<String, Object> parameters = new HashMap<>();

    @JsonAnySetter
    void setParameters(String key, Object value) {
        parameters.put(key, value);
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

}
