package GoodRoad.model;

import jakarta.persistence.*;
import lombok.*;

@Entity //для того, чтобы понимать, чтобы Spring Boot понимал, что работаем с таблицей
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class obstacleReportPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reportId;
    private String url;
    private Long createdId;
}
