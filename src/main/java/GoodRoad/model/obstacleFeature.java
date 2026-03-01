package GoodRoad.model;

import jakarta.persistence.*;
import lombok.*;

@Entity //для того, чтобы понимать, чтобы Spring Boot понимал, что работаем с таблицей
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class obstacleFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;

    private Double latitude;
    private Double lontitude;

    private Integer severetyEstimate;
    private Integer ReportsCount;
    private String lastReportedAt;

}
