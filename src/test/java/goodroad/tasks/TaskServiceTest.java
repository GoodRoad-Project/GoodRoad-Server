package goodroad.tasks;

import goodroad.api.ApiErrors.ApiException;
import goodroad.obstacle.repository.ObstacleFeatureEntity;
import goodroad.obstacle.repository.ObstacleFeatureRepo;
import goodroad.points.PointLedgerService;
import goodroad.tasks.repository.TaskEntity;
import goodroad.tasks.repository.TaskGenerationStateEntity;
import goodroad.tasks.repository.TaskGenerationStateRepo;
import goodroad.tasks.repository.TaskRepo;
import goodroad.tasks.repository.TaskTargetEntity;
import goodroad.tasks.repository.TaskTargetRepo;
import goodroad.tasks.repository.UserTaskCompletionEntity;
import goodroad.tasks.repository.UserTaskCompletionRepo;
import goodroad.tasks.repository.UserTaskTargetCompletionEntity;
import goodroad.tasks.repository.UserTaskTargetCompletionRepo;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import goodroad.volunteer.repository.HelpRequestRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {
    @Mock UserRepo users;
    @Mock TaskRepo tasks;
    @Mock TaskTargetRepo targets;
    @Mock UserTaskTargetCompletionRepo targetCompletions;
    @Mock UserTaskCompletionRepo taskCompletions;
    @Mock ObstacleFeatureRepo features;
    @Mock HelpRequestRepo helpRequests;
    @Mock PointLedgerService ledger;
    @Mock TaskGenerationStateRepo generationStates;
    @InjectMocks TaskService service;

    @Test
    void feedOnlyReadsTasksAndNeverGeneratesThem() {
        UserEntity user = user(10L, "USER");
        TaskEntity task = task(80L, "REVIEW", 3, 30);
        task.setAssignedUserId(10L);
        when(users.findByPhoneHash(any())).thenReturn(Optional.of(user));
        when(tasks.findByActivityTypeAndStatusOrderByCreatedAtDesc("REVIEW", "ACTIVE")).thenReturn(List.of(task));
        when(taskCompletions.existsByUserIdAndTaskId(10L, 80L)).thenReturn(false);
        when(targets.findByTaskIdInOrderBySortOrderAscIdAsc(List.of(80L))).thenReturn(List.of());
        when(targetCompletions.findByUserIdAndTaskIdIn(10L, List.of(80L))).thenReturn(List.of());

        List<TaskService.TaskView> result = service.feed("+79990000001", "REVIEW", 59.93, 30.31);

        assertEquals(1, result.size());
        verify(tasks, never()).save(any());
        verifyNoInteractions(features, generationStates);
    }

    @Test
    void rejectsUnknownActivityFilterInsteadOfReturningAllTasks() {
        when(users.findByPhoneHash(any())).thenReturn(Optional.of(user(10L, "USER")));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.feed("+79990000001", "UNKNOWN", null, null)
        );

        assertEquals("TASK_ACTIVITY_INVALID", exception.code());
    }

    @Test
    void generationCreatesPersonalReviewTasks() {
        UserEntity user = user(10L, "USER");
        when(users.findByPhoneHashForUpdate(any())).thenReturn(Optional.of(user));
        when(generationStates.findById(10L)).thenReturn(Optional.empty());
        when(tasks.findByStatusAndAssignedUserIdOrderByCreatedAtDesc("ACTIVE", 10L))
                .thenReturn(List.of(), List.of(task(301L, "REVIEW", 3, 30), task(305L, "REVIEW", 5, 50)));
        when(tasks.findByActivityTypeAndStatusAndAssignedUserIdOrderByCreatedAtDesc("REVIEW", "ACTIVE", 10L))
                .thenReturn(List.of());
        when(tasks.save(any(TaskEntity.class))).thenAnswer(invocation -> {
            TaskEntity saved = invocation.getArgument(0);
            saved.setId(300L + saved.getTargetCount());
            return saved;
        });
        when(targets.save(any(TaskTargetEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        List<ObstacleFeatureEntity> nearby = new ArrayList<>();
        for (long id = 1; id <= 25; id++) {
            nearby.add(feature(id, 59.93 + id * 0.00001, 30.31 + id * 0.00001));
        }
        when(features.findLowReviewedByBbox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), eq(50))).thenReturn(nearby);

        TaskService.GenerationResult result = service.generateForCurrentLocation(
                "+79990000001",
                new TaskService.GenerationReq(59.93, 30.31)
        );

        assertFalse(result.skipped());
        assertEquals(2, result.createdTasks());
        verify(tasks, atLeastOnce()).save(org.mockito.ArgumentMatchers.argThat(
                generated -> generated.isAutoGenerated() && Long.valueOf(10L).equals(generated.getAssignedUserId())
        ));
        verify(generationStates).save(any(TaskGenerationStateEntity.class));
    }

    @Test
    void generationIsThrottledWhenLocationDidNotChange() {
        UserEntity user = user(10L, "USER");
        TaskGenerationStateEntity state = new TaskGenerationStateEntity();
        state.setUserId(10L);
        state.setLatitude(59.93);
        state.setLongitude(30.31);
        state.setGeneratedAt(Instant.now());
        when(users.findByPhoneHashForUpdate(any())).thenReturn(Optional.of(user));
        when(generationStates.findById(10L)).thenReturn(Optional.of(state));

        TaskService.GenerationResult result = service.generateForCurrentLocation(
                "+79990000001",
                new TaskService.GenerationReq(59.9301, 30.3101)
        );

        assertTrue(result.skipped());
        assertEquals("LOCATION_UNCHANGED", result.reason());
        verify(tasks, never()).save(any());
        verifyNoInteractions(features);
    }

    @Test
    void approvedReviewCompletesTargetAndAwardsTaskPoints() {
        UserEntity user = user(10L, "USER");
        TaskEntity task = task(80L, "REVIEW", 1, 30);
        task.setAssignedUserId(10L);
        TaskTargetEntity target = target(1L, 80L, 101L);
        when(users.findByIdForUpdate(10L)).thenReturn(Optional.of(user));
        when(targets.findByTargetTypeAndTargetId("OBSTACLE_FEATURE", 101L)).thenReturn(List.of(target));
        when(tasks.findById(80L)).thenReturn(Optional.of(task));
        when(taskCompletions.existsByUserIdAndTaskId(10L, 80L)).thenReturn(false);
        when(targetCompletions.existsByUserIdAndTargetId(10L, 1L)).thenReturn(false);
        when(targetCompletions.countByUserIdAndTaskId(10L, 80L)).thenReturn(1L);

        service.registerApprovedReview(10L, 101L, 500L);

        verify(targetCompletions).save(any(UserTaskTargetCompletionEntity.class));
        verify(taskCompletions).save(any(UserTaskCompletionEntity.class));
        verify(ledger).earn(user, 30, "TASK_COMPLETED", task.getTitle(), 80L, "REVIEW", 500L);
    }

    private UserEntity user(Long id, String role) {
        UserEntity user = UserEntity.builder()
                .role(role)
                .active(true)
                .totalPoints(0)
                .lifetimePoints(0)
                .completedTasksCount(0)
                .build();
        user.setId(id);
        return user;
    }

    private TaskEntity task(Long id, String type, int count, int points) {
        TaskEntity task = new TaskEntity();
        task.setId(id);
        task.setActivityType(type);
        task.setTitle("Задание " + id);
        task.setTargetCount(count);
        task.setPoints(points);
        task.setStatus("ACTIVE");
        task.setCenterLatitude(59.93);
        task.setCenterLongitude(30.31);
        task.setCreatedAt(Instant.now());
        return task;
    }

    private TaskTargetEntity target(Long id, Long taskId, Long featureId) {
        TaskTargetEntity target = new TaskTargetEntity();
        target.setId(id);
        target.setTaskId(taskId);
        target.setTargetType("OBSTACLE_FEATURE");
        target.setTargetId(featureId);
        target.setTitle("Цель");
        target.setStatus("ACTIVE");
        return target;
    }

    private ObstacleFeatureEntity feature(Long id, double latitude, double longitude) {
        ObstacleFeatureEntity feature = new ObstacleFeatureEntity();
        feature.setId(id);
        feature.setLat(latitude);
        feature.setLon(longitude);
        feature.setCity("Санкт-Петербург");
        feature.setStreet("Садовая");
        feature.setHouse(id.toString());
        feature.setReviewsCount(0);
        return feature;
    }
}
