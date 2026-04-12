package goodroad.users.moderators;

import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/moderators")
public class ModeratorController {
    private final ModeratorService service;

    public ModeratorController(ModeratorService service) {
        this.service = service;
    }

    @PostMapping("")
    public String create(@RequestBody ModeratorService.createReq req) {
        return service.create(req.firstName(), req.lastName(), req.phone(), req.password());
    }

    @PutMapping("/{id}")
    public void disable(@PathVariable String id) {
        service.disable(id);
    }

    @GetMapping("/all")
    public List<ModeratorService.ModeratorView> getAllModerators() {
        return service.getAllModerators();
    }
}