package goodroad.model.gh;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphHopperResponse {
    private List<Path> paths;
    private Info info;
}