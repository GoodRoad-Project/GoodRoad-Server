package GoodRoad.model;

import jakarta.persistence.*;
import lombok.*;

@Entity //для того, чтобы понимать, чтобы Spring Boot понимал, что работаем с таблицей
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class user {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String secondName;
    private String lastName;

    private String phoneEncrypted;
    private String phoneHash;

    private String passwordHash;
    private String photoUrl;

    private String role;
    private Boolean isActive;
}
