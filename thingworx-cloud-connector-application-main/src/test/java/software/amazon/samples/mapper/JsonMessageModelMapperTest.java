/*
package pl.ttpsc.thingworxconnector.mapper;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class JsonMessageModelMapperTest {

    @Test
    void jsonToPayloadModel() {
        //given

        //when

        //then
    }

    @Test
    void jsonToThingModel() {

    }

    @Test
    void shouldConvertStringMessageToJSONObject() {
        //given
        JsonMessageModelMapper jsonMessageModelMapper = new JsonMessageModelMapper();
        String stringMessage = "{\n" +
                "  \"payload\": {\n" +
                "    \"temp\": 222,\n" +
                "    \"_id_\": \"GFDSGFDgfdgfd\"\n" +
                "  },\n" +
                "  \"deviceName\": \"Station_1\",\n" +
                "  \"deviceModelName\": \"Demo Wind Farm Asset Model\",\n" +
                "  \"deviceModel\": {\n" +
                "    \"temp\": {\n" +
                "      \"type\": \"INTEGER\"\n" +
                "  }\n" +
                "}\n" +
                "}";
        //when
        JSONObject result = jsonMessageModelMapper.MessageStringToJSONObject(stringMessage);
        //then
        assertThat(result, jsonEquals(stringMessage));
    }
}*/
