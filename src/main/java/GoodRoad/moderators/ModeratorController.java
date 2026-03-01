package GoodRoad.moderators;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/moderators") // доступно только админу-модератору (см. Security Config)
public class ModeratorController {

    private final ModeratorService svc;

    public ModeratorController(ModeratorService svc) {
        this.svc = svc;
    }

    public record CreateModeratorReq(String firstName, String lastName, @NotBlank String phone, @NotBlank String password) {
    }

    @PostMapping
    public String create(@Valid @RequestBody CreateModeratorReq req) {
        return svc.create(req.firstName(), req.lastName(), req.phone(), req.password());
    }

    @DeleteMapping("/{id}")
    public void disable(@PathVariable String id) {
        svc.disable(id);
    }
}