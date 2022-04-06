package software.amazon.samples.redis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents instance of Thingworx client application
 *
 * @author Krzysztof Lisowski
 * @version 1.0 20 Oct 2021
 */
@JsonIgnoreProperties(value = {"indexInRedisList"})
public class ThingworxClientApplication {

    private String instanceName;
    private final List<String> thingNames;
    private boolean instanceIsDead;
    private int indexInRedisList;

    /**
     * Constructor for class ThingworxClientApplication
     */
    public ThingworxClientApplication() {
        this.thingNames = new ArrayList<>();
    }

    /**
     * Returns instance name of Thingworx client application
     *
     * @return name
     */
    public String getInstanceName() {
        return this.instanceName;
    }

    /**
     * Set instance name of Thingworx client application
     *
     * @param instanceName instance name of Thingworx client application
     */
    public void setInstanceName(final String instanceName) {
        this.instanceName = instanceName;
    }

    /**
     * Returns list of Thing names that are set on that instance
     *
     * @return names
     */
    public List<String> getThingNames() {
        return this.thingNames;
    }

    /**
     * Returns status of isDead of Thingworx client application in Fargate
     *
     * @return status
     */
    public boolean getInstanceIsDead() {
        return this.instanceIsDead;
    }

    /**
     * Sets status of isDead of Thingworx client application
     *
     * @param instanceIsDead status
     */
    public void setInstanceDead(final boolean instanceIsDead) {
        this.instanceIsDead = instanceIsDead;
    }

    /**
     * Returns redis list index of Thingworx client application instance
     *
     * @return index
     */
    public int getIndexInRedisList() {
        return indexInRedisList;
    }

    /**
     * Sets redis list index of Thingworx client application instance
     *
     * @param indexInRedisList index
     */
    public void setIndexInRedisList(final int indexInRedisList) {
        this.indexInRedisList = indexInRedisList;
    }

    /**
     * Adds thing name to Thingworx client application instance
     *
     * @param thingName name
     */
    public void addThingName(final String thingName) {
        this.thingNames.add(thingName);
    }
}

