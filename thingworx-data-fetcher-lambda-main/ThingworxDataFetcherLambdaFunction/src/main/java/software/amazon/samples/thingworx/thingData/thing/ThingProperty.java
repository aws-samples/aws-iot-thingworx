package software.amazon.samples.thingworx.thingData.thing;

/**
 * Represents property of Thing
 *
 * @author Krzysztof Lisowski
 * @version 1.0 20 Oct 2021
 */
public class ThingProperty {
    private final String name;
    private final String baseType;

    /**
     * Constructor for class ThingProperty
     *
     * @param name     name of Thing property
     * @param baseType base type of property
     */
    public ThingProperty(final String name, final String baseType) {
        this.name = name;
        this.baseType = baseType;
    }

    /**
     * Returns name of Thing property
     *
     * @return name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns name of Thing property
     *
     * @return baseType
     */
    public String getBaseType() {
        return this.baseType;
    }
}
