package software.amazon.samples.configuration.awsIotCore;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.EndpointToRegion;
import com.amazonaws.regions.Region;
import com.amazonaws.services.iot.AWSIot;
import com.amazonaws.services.iot.AWSIotClientBuilder;
import com.amazonaws.services.iot.model.DescribeEndpointRequest;
import com.amazonaws.services.iotdata.AWSIotData;
import com.amazonaws.services.iotdata.AWSIotDataClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Spring boot component for creating AWS IoT API connection
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */


@Component
public class AwsIotCoreConfig {

    @Bean
    public AWSIot getAwsIot() {
        return AWSIotClientBuilder.standard().withCredentials(new DefaultAWSCredentialsProviderChain()).build();
    }

    @Bean
    public AWSIotData getAwsIotData(AWSIot awsIot) {
        String iotDataEndpoint = awsIot.describeEndpoint((new DescribeEndpointRequest()).withEndpointType("iot:Data-ATS")).getEndpointAddress();// getting TLS endpoint with Amazon cert
        Region region = EndpointToRegion.guessRegionForEndpoint(iotDataEndpoint);
        return AWSIotDataClientBuilder.standard().withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(iotDataEndpoint, region.getName())).withCredentials(new DefaultAWSCredentialsProviderChain()).build();
    }
}
