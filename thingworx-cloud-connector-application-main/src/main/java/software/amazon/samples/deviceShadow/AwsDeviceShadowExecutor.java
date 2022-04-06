package software.amazon.samples.deviceShadow;

import com.amazonaws.services.iotdata.AWSIotData;
import com.amazonaws.services.iotdata.model.ResourceNotFoundException;
import com.amazonaws.services.iotdata.model.UpdateThingShadowRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Spring boot component for executing deviceShadow updates
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */


@Slf4j
@Component
public class AwsDeviceShadowExecutor {

    @Autowired
    AWSIotData awsIotData;

    ExecutorService executorservice = Executors.newFixedThreadPool(10);// running device shadow updates with 10 threads

    public ExecutorService getExecutorService() {
        return executorservice;
    }

    public Runnable getRunnableDeviceShadowUpdateTask(UpdateThingShadowRequest updateThingShadowRequest) {
        return () -> {
            try {
                awsIotData.updateThingShadow(updateThingShadowRequest);
            } catch (ResourceNotFoundException e) {
                log.warn("Device was not found in AWS Iot core. Shadow update was not successful. Aws error: {}", e.getMessage());
            }
        };
    }
}
