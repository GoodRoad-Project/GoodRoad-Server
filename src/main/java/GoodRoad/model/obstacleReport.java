package GoodRoad.model;

import jakarta.persistence.*;
import lombok.*;

@Entity //для того, чтобы понимать, чтобы Spring Boot понимал, что работаем с таблицей
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class obstacleReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long featureId;
    private Long authorId;

    private Integer severety;
    private String text;
    private String createdAt;
    private String status;
}