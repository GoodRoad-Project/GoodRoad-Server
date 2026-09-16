package goodroad.config;

import com.graphhopper.GraphHopper;
import com.graphhopper.config.Profile;
import com.graphhopper.json.Statement;
import com.graphhopper.util.CustomModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.graphhopper.json.Statement.Op.LIMIT;

@Configuration
public class GraphHopperConfig {

    @Value("${graphhopper.osm-file}")
    private String osmFile;

    @Value("${graphhopper.graph-location}")
    private String graphLocation;

    @Bean(destroyMethod = "close")
    public GraphHopper graphHopper() {

        GraphHopper hopper = new GraphHopper();

        hopper.setOSMFile(osmFile);
        hopper.setGraphHopperLocation(graphLocation);

        CustomModel customModel = new CustomModel();

        customModel.addToSpeed(
                Statement.If("true", LIMIT, "5")
        );

        hopper.setProfiles(
                new Profile("foot")
                        .setCustomModel(customModel)
        );

        hopper.setEncodedValuesString(
                "road_class," +
                        "road_environment," +
                        "surface," +
                        "max_slope," +
                        "average_slope"
        );

        hopper.importOrLoad();

        return hopper;
    }
}
