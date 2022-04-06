package software.amazon.samples.services;

import com.amazonaws.services.iotdata.model.UpdateThingShadowRequest;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.samples.deviceShadow.AwsDeviceShadowExecutor;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Spring boot service for compiling and sending device shadow update message to AWS Iot Core
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */


@Service
@Slf4j
public class AwsIotServiceImpl implements AwsIotService {

    @Autowired
    AwsDeviceShadowExecutor awsDeviceShadowExecutor;

    @Override
    public void updateThingShadow(String thingName, JSONObject payload) {
        ByteBuffer payloadByteBuffer = ByteBuffer.wrap(payload.toString().getBytes(StandardCharsets.UTF_8));
        UpdateThingShadowRequest updateThingShadowRequest = new UpdateThingShadowRequest();
        updateThingShadowRequest.setThingName(thingName);
        updateThingShadowRequest.withPayload(payloadByteBuffer);

        log.debug("going to update device shadow for Thing [{}] with payload [{}]", thingName, payload);
        //delegate to dedicated thread so that thingworx does not have to wait
        awsDeviceShadowExecutor.getExecutorService().execute(awsDeviceShadowExecutor.getRunnableDeviceShadowUpdateTask(updateThingShadowRequest));
    }
}
