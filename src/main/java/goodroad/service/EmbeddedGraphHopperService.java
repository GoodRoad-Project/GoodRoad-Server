package goodroad.service;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.shapes.GHPoint;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class EmbeddedGraphHopperService {

    private final GraphHopper graphHopper;

    public EmbeddedGraphHopperService(GraphHopper graphHopper) {
        this.graphHopper = graphHopper;
    }

    public ResponsePath route(
            GHPoint start,
            GHPoint end,
            CustomModel customModel,
            Locale locale
    ) {

        GHRequest request = new GHRequest()
                .addPoint(start)
                .addPoint(end)
                .setProfile("foot")
                .setLocale(locale);

        if (customModel != null) {
            request.setCustomModel(customModel);
        }

        GHResponse response = graphHopper.route(request);

        if (response.hasErrors()) {
            throw new IllegalStateException(
                    "GraphHopper routing failed: " + response.getErrors()
            );
        }

        return response.getBest();
    }
}