package software.amazon.samples.services;

import org.json.JSONObject;

/**
 * Interface fo AWS Iot service
 * this service also sends data to thingworx coming from AWS device
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */

public interface AwsIotService {

    void updateThingShadow(String thingName, JSONObject payload);
}
