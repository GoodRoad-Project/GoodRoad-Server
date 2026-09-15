package goodroad.tasks;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService service;

    @GetMapping
    public List<TaskService.TaskView> feed(Authentication authentication,
                                           @RequestParam(required = false) String activityType,
                                           @RequestParam(required = false) Double latitude,
                                           @RequestParam(required = false) Double longitude) {
        return service.feed(authentication.getName(), activityType, latitude, longitude);
    }

    @PostMapping("/generation")
    public TaskService.GenerationResult generate(
            Authentication authentication,
            @RequestBody TaskService.GenerationReq request
    ) {
        return service.generateForCurrentLocation(authentication.getName(), request);
    }

    @GetMapping("/completed")
    public List<TaskService.CompletedTaskView> completed(Authentication authentication) {
        return service.completed(authentication.getName());
    }

}
