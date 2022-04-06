package software.amazon.samples.thingworx.thingData;

import com.thingworx.relationships.RelationshipTypes;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.IntegerPrimitive;
import com.thingworx.types.primitives.StringPrimitive;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.samples.thingworx.connection.config.ThingworxClient;
import software.amazon.samples.thingworx.thingData.thing.ThingData;
import software.amazon.samples.thingworx.thingData.thing.ThingProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads Things data from Thingworx
 *
 * @author Krzysztof Lisowski
 * @version 1.0 20 Oct 2021
 */
public class ThingworxThingFetcher {
    private static final Logger logger = LoggerFactory.getLogger(ThingworxThingFetcher.class + "::LAMBDA_BODY");

    private final ThingworxClient client;

    private final int timeOutService;
    private final int getEntityListTimeOut;

    private final String awsTags;
    private final String propertyCategoryName;

    private final List<ThingData> thingDataList;

    /**
     * Constructor for class ThingworxThingFetcher
     *
     * @param client               Thingworx connection client
     * @param timeOutService       timeout for Thingworx services
     * @param getEntityListTimeOut timeout for Thingworx service "GetEntityList"
     * @param awsTags              tags name for fetching Things from Thingworx
     * @param propertyCategoryName Thing property category name for matching properties to read
     */
    public ThingworxThingFetcher(final ThingworxClient client, final int timeOutService,
                                 final int getEntityListTimeOut, final String awsTags,
                                 final String propertyCategoryName) {
        this.client = client;
        this.timeOutService = timeOutService;
        this.getEntityListTimeOut = getEntityListTimeOut;
        this.awsTags = awsTags;
        this.propertyCategoryName = propertyCategoryName;
        this.thingDataList = new ArrayList<>();

        logger.debug("Properties for class: {}", Object.class.getName(), logger.isDebugEnabled());
        logger.debug("Timeout service: {}", this.timeOutService, logger.isDebugEnabled());
        logger.debug("GetEntityList service time out: {}", this.getEntityListTimeOut, logger.isDebugEnabled());
        logger.debug("Tags name for fetching Things: {}", this.awsTags, logger.isDebugEnabled());
        logger.debug("Property category name: {}", this.propertyCategoryName, logger.isDebugEnabled());
    }

    /**
     * Reads Things from Thingworx and put them into ThingData list
     */
    public void readThingsDataFromThingworx() {
        final long startTime = System.currentTimeMillis();
        InfoTable thingsData = getInfoTableOfThingsCreatedWithAwsTag();

        if (thingsData != null && !thingsData.isEmpty()) {
            parseDataToThingDataList(thingsData);
        }

        final long endTime = System.currentTimeMillis();
        final long executionTime = endTime - startTime;
        logger.info("Reading things from Thingworx took: {} milliseconds", executionTime, logger.isInfoEnabled());
    }

    /**
     * Returns ThingData list
     *
     * @return ThingsData
     */
    public List<ThingData> getThingDataList() {
        return this.thingDataList;
    }

    /**
     * Reads Things names from Thingworx
     *
     * @return infoTable with Thing names
     * @throws Exception
     */
    @SneakyThrows
    private InfoTable getInfoTableOfThingsCreatedWithAwsTag() {
        try {
            long maxItems = 999999; //maximal number that can be set
            ValueCollection params = new ValueCollection();
            params.put("maxItems", new IntegerPrimitive(maxItems));
            params.put("type", new StringPrimitive("Thing"));
            params.put("tags", new StringPrimitive(awsTags));

            InfoTable infoTable = this.client.invokeService(RelationshipTypes.ThingworxEntityTypes.Resources, "EntityServices", "GetEntityList", params, this.getEntityListTimeOut);
            logger.debug("Successfully read Things from Thingworx created with Aws Tag: {}", awsTags, logger.isDebugEnabled());
            return infoTable;
        } catch (Exception e) {
            logger.error("Couldn't read Thing names from Thingworx created with Aws Tag: {}, ", awsTags, e, logger.isErrorEnabled());
            throw e;
        }
    }

    /**
     * Creates ThingData objects and add them to ThingData list
     *
     * @param infoTable infoTable with Things data returned by service "QueryImplementingThingsWithData"
     */
    private void parseDataToThingDataList(final InfoTable infoTable) {
        infoTable.getRows().forEach(row -> {
            String name = row.get("name").getStringValue();
            //Adding Things with name to ThingData
            ThingData thingData = new ThingData(name);
            InfoTable thingPropertyData = getPropertiesFromThing(name);
            List<ThingProperty> properties = getThingPropertiesFromInfoTable(thingPropertyData, name);
            thingData.addProperties(properties);
            this.thingDataList.add(thingData);
        });
    }

    /**
     * Reads properties of Thing from Thingworx
     *
     * @param thingName name of Thing
     * @return infoTable with Thing properties definitions
     * @throws Exception
     */
    @SneakyThrows
    private InfoTable getPropertiesFromThing(final String thingName) {
        ValueCollection params = new ValueCollection();
        params.put("category", new StringPrimitive(this.propertyCategoryName));

        try {
            InfoTable infoTable = this.client.invokeService(RelationshipTypes.ThingworxEntityTypes.Things, thingName, "GetPropertyDefinitions", params, this.timeOutService);
            logger.debug("Successfully read properties of Thing {}", thingName, logger.isDebugEnabled());
            return infoTable;
        } catch (Exception e) {
            logger.error("Couldn't read properties of Thing {}: {}", thingName, e, logger.isErrorEnabled());
            throw e;
        }
    }

    /**
     * Parses properties from infoTable of properties
     *
     * @param infoTable infoTable with properties data of Thing returned by service "GetPropertyDefinitions"
     * @param thingName name of Thing
     * @return ThingProperty list of Thing properties
     */
    private List<ThingProperty> getThingPropertiesFromInfoTable(final InfoTable infoTable, final String thingName) {
        List<ThingProperty> properties = new ArrayList<>();
        infoTable.getRows().forEach(row -> {
            String name = row.get("name").getStringValue();
            String baseType = row.get("baseType").getStringValue();
            //Adding property to Thing
            if (baseType.equals("NUMBER") || baseType.equals("STRING") || baseType.equals("INTEGER") || baseType.equals("BOOLEAN")) {
                properties.add(new ThingProperty(name, baseType));
            } else {
                logger.warn("Property {} of Thing {} is not supported type", baseType, thingName, logger.isWarnEnabled());
            }
        });
        return properties;
    }
}