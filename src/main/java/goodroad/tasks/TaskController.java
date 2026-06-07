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

    @GetMapping("/completed")
    public List<TaskService.CompletedTaskView> completed(Authentication authentication) {
        return service.completed(authentication.getName());
    }

    @PostMapping
    public TaskService.TaskView create(@RequestBody TaskService.TaskCreateReq req) {
        return service.createTask(req);
    }
}
