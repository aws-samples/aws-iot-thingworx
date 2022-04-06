package software.amazon.samples;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import software.amazon.samples.configuration.awsAppconfig.AppConfigModule;
import software.amazon.samples.thingworx.ThingworxClientSingleton;

/**
 * Spring boot main application launcher
 *
 * @author Maciej Kiciński
 * @version 1.0
 * @since 2021-11-04
 */

@SpringBootApplication
@Slf4j
public class ThingworxConnectorApplication implements ApplicationRunner {

    public static final String environmentNameIndicator = "ENVIRONMENT";

    @Autowired
    AppConfigModule appConfigModule;

    public static void main(String[] args) {
        if(System.getenv(environmentNameIndicator) != null) {
            SpringApplication application = new SpringApplication(ThingworxConnectorApplication.class);
            String[] profile = new String[] {System.getenv(environmentNameIndicator)};
            application.setAdditionalProfiles(profile);
            log.info("going to run spring boot with profile: {}", (Object) profile);
            application.run(args);
        }else {
            log.error("failed loading spring boot profile");
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        // instantiate thingworx client
        ThingworxClientSingleton.INSTANCE.generateThingworxClientInstance(appConfigModule);
    }
}
