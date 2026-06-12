package goodroad.tasks;

import goodroad.obstacle.repository.*;
import goodroad.points.PointLedgerService;
import goodroad.security.Crypto;
import goodroad.tasks.repository.*;
import goodroad.users.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock UserRepo users;
    @Mock TaskRepo tasks;
    @Mock TaskTargetRepo targets;
    @Mock UserTaskTargetCompletionRepo targetCompletions;
    @Mock UserTaskCompletionRepo taskCompletions;
    @Mock ObstacleFeatureRepo features;
    @Mock PointLedgerService ledger;
    @InjectMocks TaskService service;

    @Test
    void shouldCreateReviewTaskWithDefaultPoints() {
        when(tasks.save(any(TaskEntity.class))).thenAnswer(inv -> { TaskEntity t = inv.getArgument(0); t.setId(50L); return t; });
        when(targets.save(any(TaskTargetEntity.class))).thenAnswer(inv -> { TaskTargetEntity target = inv.getArgument(0); target.setId(target.getTargetId()); return target; });
        when(targets.findByTaskIdInOrderBySortOrderAscIdAsc(List.of(50L))).thenReturn(List.of(target(1L, 50L, 1L), target(2L, 50L, 2L), target(3L, 50L, 3L)));

        TaskService.TaskView result = service.createTask(new TaskService.TaskCreateReq(
                "REVIEW", "Оцените три точки", 3, null, 59.93, 30.31,
                List.of(
                        new TaskService.TaskTargetCreateReq("OBSTACLE_FEATURE", 1L, "Садовая, 1", 59.93, 30.31),
                        new TaskService.TaskTargetCreateReq("OBSTACLE_FEATURE", 2L, "Садовая, 2", 59.94, 30.32),
                        new TaskService.TaskTargetCreateReq("OBSTACLE_FEATURE", 3L, "Садовая, 3", 59.95, 30.33)
                )
        ));

        assertEquals("50", result.id());
        assertEquals("REVIEW", result.activityType());
        assertEquals(30, result.points());
        assertEquals(3, result.targets().size());
    }

    @Test
    void shouldCompleteReviewTaskOnlyWhenAllTargetsDone() {
        UserEntity user = user(10L);
        TaskEntity task = task(50L, "REVIEW", 3, 30);
        TaskTargetEntity t1 = target(1L, 50L, 101L);
        TaskTargetEntity t2 = target(2L, 50L, 102L);
        TaskTargetEntity t3 = target(3L, 50L, 103L);
        when(users.findById(10L)).thenReturn(Optional.of(user));
        when(targets.findByTargetTypeAndTargetId("OBSTACLE_FEATURE", 103L)).thenReturn(List.of(t3));
        when(taskCompletions.existsByUserIdAndTaskId(10L, 50L)).thenReturn(false);
        when(targetCompletions.existsByUserIdAndTargetId(10L, 3L)).thenReturn(false);
        when(tasks.findById(50L)).thenReturn(Optional.of(task));
        when(targetCompletions.countByUserIdAndTaskId(10L, 50L)).thenReturn(3L);

        service.registerApprovedReview(10L, 103L, 900L);

        verify(targetCompletions).save(any(UserTaskTargetCompletionEntity.class));
        verify(taskCompletions).save(any(UserTaskCompletionEntity.class));
        assertEquals(1, user.getCompletedTasksCount());
        verify(ledger).earn(user, 30, "TASK_COMPLETED", "Оцените 3 точки", 50L, "REVIEW", 900L);
    }

    @Test
    void shouldHideCompletedTaskFromFeed() {
        UserEntity user = user(10L);
        TaskEntity visible = task(50L, "REVIEW", 3, 30);
        TaskEntity completed = task(51L, "REVIEW", 3, 30);
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000001"))).thenReturn(Optional.of(user));
        when(tasks.findByActivityTypeAndStatusOrderByCreatedAtDesc("REVIEW", "ACTIVE")).thenReturn(List.of(visible, completed));
        when(taskCompletions.existsByUserIdAndTaskId(10L, 50L)).thenReturn(false);
        when(taskCompletions.existsByUserIdAndTaskId(10L, 51L)).thenReturn(true);
        when(targets.findByTaskIdInOrderBySortOrderAscIdAsc(List.of(50L))).thenReturn(List.of(target(1L, 50L, 101L)));
        when(targetCompletions.findByUserIdAndTaskIdIn(10L, List.of(50L))).thenReturn(List.of());

        List<TaskService.TaskView> feed = service.feed("+79990000001", "REVIEW", null, null);

        assertEquals(1, feed.size());
        assertEquals("50", feed.get(0).id());
    }

    private UserEntity user(Long id) {
        UserEntity user = UserEntity.builder().phoneHash(Crypto.sha256Hex("79990000001")).role("USER").active(true).totalPoints(0).lifetimePoints(0).completedTasksCount(0).build();
        user.setId(id);
        return user;
    }
    private TaskEntity task(Long id, String type, int count, int points) {
        TaskEntity task = new TaskEntity();
        task.setId(id); task.setActivityType(type); task.setTitle("Оцените " + count + " точки"); task.setTargetCount(count); task.setPoints(points); task.setStatus("ACTIVE"); task.setCreatedAt(Instant.now());
        return task;
    }
    private TaskTargetEntity target(Long id, Long taskId, Long targetObjectId) {
        TaskTargetEntity target = new TaskTargetEntity();
        target.setId(id); target.setTaskId(taskId); target.setTargetType("OBSTACLE_FEATURE"); target.setTargetId(targetObjectId); target.setTitle("Цель " + targetObjectId); target.setSortOrder(id.intValue());
        return target;
    }
}
