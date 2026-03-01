package GoodRoad.model;

import jakarta.persistence.*;
import lombok.*;

@Entity //для того, чтобы понимать, чтобы Spring Boot понимал, что работаем с таблицей
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class userObstaclePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String obstacleType;

    private Boolean avoid;
    private Integer maxAllowedSeverety;

}
