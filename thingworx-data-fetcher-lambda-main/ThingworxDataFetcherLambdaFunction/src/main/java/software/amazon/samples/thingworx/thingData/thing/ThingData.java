package software.amazon.samples.thingworx.thingData.thing;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents Thing data
 *
 * @author Krzysztof Lisowski
 * @version 1.0 20 Oct 2021
 */
public class ThingData {
    private final String name;

    private final List<ThingProperty> thingProperties;

    /**
     * Constructor for class
     *
     * @param name name of Thing which is Id of device in Aws Iot
     */
    public ThingData(final String name) {
        this.name = name;
        this.thingProperties = new ArrayList<>();
    }

    /**
     * Adds ThingProperties to Thing
     *
     * @param properties list of ThingProperty
     */
    public void addProperties(final List<ThingProperty> properties) {
        this.thingProperties.addAll(properties);
    }

    /**
     * Returns Thing name which is Id of device in Aws Iot
     *
     * @return name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns ThingProperties of Thing
     *
     * @return list of ThingProperties
     */
    public List<ThingProperty> getThingProperties() {
        return this.thingProperties;
    }
}
