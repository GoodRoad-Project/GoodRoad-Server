package GoodRoad.users.moderators;

import GoodRoad.users.repository.UserEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public List<UserEntity> getAllModerators() {
        return service.getAllModerators();
    }

    @DeleteMapping("/{id}")
    public void deleteModerator(@PathVariable String id) {
        service.deleteModerator(id);
    }


}
