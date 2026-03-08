package GoodRoad.users.moderators;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping()
public class ModeratorController {
    private final ModeratorService service;

    public ModeratorController(ModeratorService service) {
        this.service = service;
    }

    @PostMapping("/users/moderators")
    public String create(@RequestBody ModeratorService.createReq req) {
        return service.create(req.firstName(), req.lastName(), req.phone(), req.password());
    }

    @PutMapping("users/moderators/{id}")
    public void disable(@PathVariable String id) {
        service.disable(id);
    }
}
