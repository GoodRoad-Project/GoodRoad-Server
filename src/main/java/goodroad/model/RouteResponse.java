package goodroad.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {
    private String id;
    private List<PathResponse> paths;
    private ResponseInfo info;
}

