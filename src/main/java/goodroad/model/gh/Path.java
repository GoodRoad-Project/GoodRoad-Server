package goodroad.model.gh;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Path {
    private Double distance;
    private Long time;
    private String points;

    @JsonProperty("points_encoded")
    private Boolean pointsEncoded = false;
}