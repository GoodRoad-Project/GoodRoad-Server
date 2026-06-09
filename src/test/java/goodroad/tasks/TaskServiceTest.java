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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {
    @Mock UserRepo users;
    @Mock TaskRepo tasks;
    @Mock TaskTargetRepo targets;
    @Mock UserTaskTargetCompletionRepo targetCompletions;
    @Mock UserTaskCompletionRepo taskCompletions;
    @Mock ObstacleFeatureRepo features;
    @Mock goodroad.volunteer.repository.HelpRequestRepo helpRequests;
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
    void shouldCompleteVolunteerTaskOnlyAfterCompletedWalksNotAfterAccepting() {
        UserEntity volunteer = user(10L);
        TaskEntity task = task(80L, "VOLUNTEER", 3, 120);
        task.setAssignedUserId(10L);
        TaskTargetEntity acceptedTarget = target(1L, 80L, 101L);
        acceptedTarget.setTargetType("HELP_REQUEST");
        TaskTargetEntity completedTarget = target(2L, 80L, 102L);
        completedTarget.setTargetType("HELP_REQUEST");
        goodroad.volunteer.repository.HelpRequestEntity accepted = helpRequest(101L, 59.93, 30.31, LocalDate.now().plusDays(2), LocalTime.of(10, 0));
        accepted.setStatus("ACCEPTED");
        accepted.setVolunteer(volunteer);

        when(users.findByPhoneHash(Crypto.sha256Hex("79990000001"))).thenReturn(Optional.of(volunteer));
        when(tasks.findByActivityTypeAndStatusAndAssignedUserIdOrderByCreatedAtDesc("REVIEW", "ACTIVE", 10L)).thenReturn(List.of());
        when(tasks.findByActivityTypeAndStatusAndAssignedUserIdOrderByCreatedAtDesc("VOLUNTEER", "ACTIVE", 10L)).thenReturn(List.of(task));
        when(targets.findByTaskIdOrderBySortOrderAscIdAsc(80L)).thenReturn(List.of(acceptedTarget, completedTarget));
        when(targetCompletions.findByUserIdAndTaskIdIn(10L, List.of(80L))).thenReturn(List.of());
        when(helpRequests.findById(101L)).thenReturn(Optional.of(accepted));
        when(helpRequests.findById(102L)).thenReturn(Optional.empty());
        when(helpRequests.findByStatusOrderByDateAscTimeAscCreatedAtAsc("OPEN")).thenReturn(List.of());
        when(tasks.findByActivityTypeAndStatusOrderByCreatedAtDesc("VOLUNTEER", "ACTIVE")).thenReturn(List.of(task));
        when(taskCompletions.existsByUserIdAndTaskId(10L, 80L)).thenReturn(false);
        when(targets.findByTaskIdInOrderBySortOrderAscIdAsc(List.of(80L))).thenReturn(List.of(acceptedTarget, completedTarget));
        when(targetCompletions.findByUserIdAndTaskIdIn(10L, List.of(80L))).thenReturn(List.of());

        List<TaskService.TaskView> feed = service.feed("+79990000001", "VOLUNTEER", 59.93, 30.31);

        assertEquals(1, feed.size());
        assertEquals(0, feed.get(0).completedCount());
        assertEquals("ACTIVE", acceptedTarget.getStatus());
        assertEquals("UNAVAILABLE", completedTarget.getStatus());
        verify(taskCompletions, never()).save(any());
    }

    @Test
    void shouldReplaceVolunteerTargetAcceptedByAnotherVolunteer() {
        UserEntity volunteer = user(10L);
        UserEntity other = user(11L);
        TaskEntity task = task(80L, "VOLUNTEER", 3, 120);
        task.setAssignedUserId(10L);
        TaskTargetEntity unavailable = target(1L, 80L, 101L);
        unavailable.setTargetType("HELP_REQUEST");
        TaskTargetEntity kept = target(2L, 80L, 102L);
        kept.setTargetType("HELP_REQUEST");
        goodroad.volunteer.repository.HelpRequestEntity acceptedByOther = helpRequest(101L, 59.93, 30.31, LocalDate.now().plusDays(2), LocalTime.of(10, 0));
        acceptedByOther.setStatus("ACCEPTED");
        acceptedByOther.setVolunteer(other);
        goodroad.volunteer.repository.HelpRequestEntity openKept = helpRequest(102L, 59.931, 30.311, LocalDate.now().plusDays(2), LocalTime.of(11, 0));
        goodroad.volunteer.repository.HelpRequestEntity replacement = helpRequest(103L, 59.932, 30.312, LocalDate.now().plusDays(2), LocalTime.of(12, 0));

        when(users.findByPhoneHash(Crypto.sha256Hex("79990000001"))).thenReturn(Optional.of(volunteer));
        when(tasks.findByActivityTypeAndStatusAndAssignedUserIdOrderByCreatedAtDesc("REVIEW", "ACTIVE", 10L)).thenReturn(List.of());
        when(tasks.findByActivityTypeAndStatusAndAssignedUserIdOrderByCreatedAtDesc("VOLUNTEER", "ACTIVE", 10L)).thenReturn(List.of(task));
        when(targets.findByTaskIdOrderBySortOrderAscIdAsc(80L)).thenReturn(List.of(unavailable, kept));
        when(targetCompletions.findByUserIdAndTaskIdIn(10L, List.of(80L))).thenReturn(List.of());
        when(helpRequests.findById(101L)).thenReturn(Optional.of(acceptedByOther));
        when(helpRequests.findById(102L)).thenReturn(Optional.of(openKept));
        when(helpRequests.findByStatusOrderByDateAscTimeAscCreatedAtAsc("OPEN")).thenReturn(List.of(replacement));
        when(targets.save(any(TaskTargetEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tasks.findByActivityTypeAndStatusOrderByCreatedAtDesc("VOLUNTEER", "ACTIVE")).thenReturn(List.of(task));
        when(taskCompletions.existsByUserIdAndTaskId(10L, 80L)).thenReturn(false);
        when(targets.findByTaskIdInOrderBySortOrderAscIdAsc(List.of(80L))).thenReturn(List.of(unavailable, kept));

        service.feed("+79990000001", "VOLUNTEER", 59.93, 30.31);

        assertEquals("UNAVAILABLE", unavailable.getStatus());
        ArgumentCaptor<TaskTargetEntity> captor = ArgumentCaptor.forClass(TaskTargetEntity.class);
        verify(targets, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(t -> Objects.equals(t.getTargetId(), 103L) && "HELP_REQUEST".equals(t.getTargetType())));
    }

    @Test
    void shouldKeepVolunteerTaskPartialWhenNoReplacementExists() {
        UserEntity volunteer = user(10L);
        UserEntity other = user(11L);
        TaskEntity task = task(80L, "VOLUNTEER", 3, 120);
        task.setAssignedUserId(10L);
        TaskTargetEntity unavailable = target(1L, 80L, 101L);
        unavailable.setTargetType("HELP_REQUEST");
        goodroad.volunteer.repository.HelpRequestEntity acceptedByOther = helpRequest(101L, 59.93, 30.31, LocalDate.now().plusDays(2), LocalTime.of(10, 0));
        acceptedByOther.setStatus("ACCEPTED");
        acceptedByOther.setVolunteer(other);

        when(users.findByPhoneHash(Crypto.sha256Hex("79990000001"))).thenReturn(Optional.of(volunteer));
        when(tasks.findByActivityTypeAndStatusAndAssignedUserIdOrderByCreatedAtDesc("REVIEW", "ACTIVE", 10L)).thenReturn(List.of());
        when(tasks.findByActivityTypeAndStatusAndAssignedUserIdOrderByCreatedAtDesc("VOLUNTEER", "ACTIVE", 10L)).thenReturn(List.of(task));
        when(targets.findByTaskIdOrderBySortOrderAscIdAsc(80L)).thenReturn(List.of(unavailable));
        when(targetCompletions.findByUserIdAndTaskIdIn(10L, List.of(80L))).thenReturn(List.of());
        when(helpRequests.findById(101L)).thenReturn(Optional.of(acceptedByOther));
        when(helpRequests.findByStatusOrderByDateAscTimeAscCreatedAtAsc("OPEN")).thenReturn(List.of());
        when(tasks.findByActivityTypeAndStatusOrderByCreatedAtDesc("VOLUNTEER", "ACTIVE")).thenReturn(List.of(task));
        when(taskCompletions.existsByUserIdAndTaskId(10L, 80L)).thenReturn(false);
        when(targets.findByTaskIdInOrderBySortOrderAscIdAsc(List.of(80L))).thenReturn(List.of(unavailable));

        List<TaskService.TaskView> feed = service.feed("+79990000001", "VOLUNTEER", 59.93, 30.31);

        assertEquals("ACTIVE", task.getStatus());
        assertEquals("UNAVAILABLE", unavailable.getStatus());
        assertEquals(1, feed.size());
        assertEquals(3, feed.get(0).targetCount());
        assertEquals(0, feed.get(0).targets().size());
    }

    @Test
    void shouldGeneratePersonalReviewTasksInsteadOfOneGlobalTask() {
        UserEntity user = user(10L);
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000001"))).thenReturn(Optional.of(user));
        when(tasks.findByActivityTypeAndStatusAndAssignedUserIdOrderByCreatedAtDesc("REVIEW", "ACTIVE", 10L)).thenReturn(List.of());
        when(tasks.save(any(TaskEntity.class))).thenAnswer(inv -> {
            TaskEntity task = inv.getArgument(0);
            task.setId(task.getTargetCount() == 3 ? 301L : task.getTargetCount() == 5 ? 305L : 310L);
            return task;
        });
        when(targets.save(any(TaskTargetEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        List<ObstacleFeatureEntity> lowReviewed = new ArrayList<>();
        for (long i = 1; i <= 10; i++) lowReviewed.add(feature(i, 59.93 + i * 0.0001, 30.31 + i * 0.0001, 0));
        when(features.findLowReviewedByBbox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), eq(50))).thenReturn(lowReviewed);
        when(tasks.findByActivityTypeAndStatusOrderByCreatedAtDesc("REVIEW", "ACTIVE")).thenReturn(List.of());

        service.feed("+79990000001", "REVIEW", 59.93, 30.31);

        verify(tasks, atLeastOnce()).save(argThat(task -> Objects.equals(task.getAssignedUserId(), 10L) && "REVIEW".equals(task.getActivityType())));
    }

    private UserEntity user(Long id) {
        UserEntity user = UserEntity.builder().phoneHash(Crypto.sha256Hex("79990000001")).role("USER").active(true).totalPoints(0).lifetimePoints(0).completedTasksCount(0).build();
        user.setId(id);
        return user;
    }

    private TaskEntity task(Long id, String type, int count, int points) {
        TaskEntity task = new TaskEntity();
        task.setId(id);
        task.setActivityType(type);
        task.setTitle(type.equals("VOLUNTEER") ? "Помогите трем людям с прогулкой рядом с вами" : "Оцените " + count + " точки");
        task.setTargetCount(count);
        task.setPoints(points);
        task.setStatus("ACTIVE");
        task.setCreatedAt(Instant.now());
        return task;
    }

    private goodroad.volunteer.repository.HelpRequestEntity helpRequest(Long id, double lat, double lon, LocalDate date, LocalTime time) {
        goodroad.volunteer.repository.HelpRequestEntity request = new goodroad.volunteer.repository.HelpRequestEntity();
        request.setId(id);
        request.setRequester(user(99L));
        request.setFromAddress("Старт " + id);
        request.setToAddress("Финиш " + id);
        request.setStartLatitude(lat);
        request.setStartLongitude(lon);
        request.setDate(date);
        request.setTime(time);
        request.setPhone("79990000001");
        request.setComment("Нужна помощь");
        request.setStatus("OPEN");
        request.setCreatedAt(Instant.now().minus(Duration.ofHours(30)));
        return request;
    }

    private TaskTargetEntity target(Long id, Long taskId, Long targetObjectId) {
        TaskTargetEntity target = new TaskTargetEntity();
        target.setId(id);
        target.setTaskId(taskId);
        target.setTargetType("OBSTACLE_FEATURE");
        target.setTargetId(targetObjectId);
        target.setTitle("Цель " + targetObjectId);
        target.setSortOrder(id.intValue());
        target.setStatus("ACTIVE");
        return target;
    }

    private ObstacleFeatureEntity feature(Long id, double lat, double lon, int reviewsCount) {
        ObstacleFeatureEntity feature = new ObstacleFeatureEntity();
        feature.setId(id);
        feature.setLat(lat);
        feature.setLon(lon);
        feature.setCity("СПб");
        feature.setStreet("Садовая");
        feature.setHouse(id.toString());
        feature.setReviewsCount(reviewsCount);
        return feature;
    }
}
