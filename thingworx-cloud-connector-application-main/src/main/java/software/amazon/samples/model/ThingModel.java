package software.amazon.samples.model;

import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * Thing data model
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */

@ToString
public class ThingModel {

    private final String name;
    private final String status;
    private final HashMap<String, Map<String, String>> attributes;

    public ThingModel(String name, String status, HashMap<String, Map<String, String>> attributes) {
        this.name = name;
        this.status = status;
        this.attributes = attributes;
    }

    public String getDeviceName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public Map<String, Map<String, String>> getModel() {
        return attributes;
    }

}
