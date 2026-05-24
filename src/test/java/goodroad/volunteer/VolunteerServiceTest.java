package goodroad.volunteer;

import goodroad.security.Crypto;
import goodroad.storage.StorageService;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import goodroad.volunteer.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VolunteerServiceTest {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Mock
    private UserRepo users;

    @Mock
    private VolunteerApplicationRepo applications;

    @Mock
    private VolunteerApplicationPhotoRepo applicationPhotos;

    @Mock
    private VolunteerUserStateRepo states;

    @Mock
    private HelpRequestRepo requests;

    @Mock
    private VolunteerComplaintRepo complaints;

    @Mock
    private SosNotificationRepo sosNotifications;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private VolunteerService service;

    @Test
    void shouldCreateVolunteerApplicationWithCertificateLinks() {
        UserEntity user = user(1L, "USER", "+79990000001");
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000001"))).thenReturn(Optional.of(user));
        when(applications.findFirstByApplicantIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(applications.save(any(VolunteerApplicationEntity.class))).thenAnswer(invocation -> {
            VolunteerApplicationEntity application = invocation.getArgument(0);
            application.setId(10L);
            application.setCreatedAt(Instant.now());
            return application;
        });

        VolunteerService.VolunteerApplicationResp result = service.createApplication(
                "+79990000001",
                new VolunteerService.CreateVolunteerApplicationReq(
                        "https://dobro.ru/volunteer/1",
                        "+79990000001",
                        "@volunteer",
                        List.of("https://storage.yandexcloud.net/bucket/cert-1.jpg")
                )
        );

        assertEquals("10", result.id());
        assertEquals("PENDING", result.status());
        assertEquals("79990000001", result.phone());
        assertEquals(List.of("https://storage.yandexcloud.net/bucket/cert-1.jpg"), result.certificatePhotoUrls());
    }

    @Test
    void shouldApproveApplicationAndPromoteUserToVolunteer() {
        UserEntity applicant = user(1L, "USER", "+79990000001");
        UserEntity moderator = user(2L, "MODERATOR", "+79990000002");
        VolunteerApplicationEntity application = application(20L, applicant, "PENDING");
        VolunteerUserStateEntity state = state(applicant);
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000002"))).thenReturn(Optional.of(moderator));
        when(applications.findById(20L)).thenReturn(Optional.of(application));
        when(applications.save(application)).thenReturn(application);
        when(states.findById(1L)).thenReturn(Optional.empty());
        when(states.save(any(VolunteerUserStateEntity.class))).thenReturn(state);

        VolunteerService.VolunteerApplicationResp result = service.approveApplication("+79990000002", "20");

        assertEquals("APPROVED", result.status());
        assertEquals("VOLUNTEER", applicant.getRole());
        assertNotNull(application.getModeratedAt());
        verify(users).save(applicant);
    }

    @Test
    void shouldRejectApplicationWithModeratorReason() {
        UserEntity applicant = user(1L, "USER", "+79990000001");
        UserEntity moderator = user(2L, "MODERATOR", "+79990000002");
        VolunteerApplicationEntity application = application(20L, applicant, "PENDING");
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000002"))).thenReturn(Optional.of(moderator));
        when(applications.findById(20L)).thenReturn(Optional.of(application));
        when(applications.save(application)).thenReturn(application);

        VolunteerService.VolunteerApplicationResp result = service.rejectApplication(
                "+79990000002",
                "20",
                new VolunteerService.RejectApplicationReq("Не хватает сертификата")
        );

        assertEquals("REJECTED", result.status());
        assertEquals("Не хватает сертификата", result.moderatorComment());
        assertEquals(moderator, application.getModerator());
    }

    @Test
    void shouldUploadVolunteerCertificateToStorage() {
        UserEntity user = user(1L, "USER", "+79990000001");
        MockMultipartFile file = new MockMultipartFile("file", "cert.png", "image/png", new byte[] {1, 2, 3});
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000001"))).thenReturn(Optional.of(user));
        when(storageService.uploadVolunteerCertificate(file, "1")).thenReturn("https://storage.yandexcloud.net/bucket/cert.png");

        VolunteerService.PhotoUploadResp result = service.uploadCertificate("+79990000001", file);

        assertEquals("https://storage.yandexcloud.net/bucket/cert.png", result.photoUrl());
    }

    @Test
    void shouldCreateHelpRequestAndHideContactsForNonParticipantVolunteer() {
        UserEntity requester = user(1L, "USER", "+79990000001");
        UserEntity volunteer = user(2L, "VOLUNTEER", "+79990000002");
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000001"))).thenReturn(Optional.of(requester));
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000002"))).thenReturn(Optional.of(volunteer));
        when(requests.save(any(HelpRequestEntity.class))).thenAnswer(invocation -> {
            HelpRequestEntity request = invocation.getArgument(0);
            request.setId(30L);
            request.setCreatedAt(Instant.now());
            return request;
        });
        when(requests.findById(30L)).thenAnswer(invocation -> Optional.of(helpRequest(30L, requester, null, "OPEN")));

        VolunteerService.HelpRequestResp created = service.createHelpRequest("+79990000001", helpRequestReq());
        VolunteerService.HelpRequestResp details = service.getHelpRequest("+79990000002", "30");

        assertEquals("30", created.id());
        assertEquals("OPEN", created.status());
        assertNull(details.phone());
        assertNull(details.socialNickname());
        assertFalse(details.contactsVisible());
    }

    @Test
    void shouldAcceptRequestAndShowContactsToVolunteer() {
        UserEntity requester = user(1L, "USER", "+79990000001");
        UserEntity volunteer = user(2L, "VOLUNTEER", "+79990000002");
        HelpRequestEntity request = helpRequest(30L, requester, null, "OPEN");
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000002"))).thenReturn(Optional.of(volunteer));
        when(states.findById(2L)).thenReturn(Optional.empty());
        when(requests.findById(30L)).thenReturn(Optional.of(request));
        when(requests.save(request)).thenReturn(request);

        VolunteerService.HelpRequestResp result = service.acceptRequest("+79990000002", "30");

        assertEquals("ACCEPTED", result.status());
        assertEquals("2", result.volunteerId());
        assertEquals("79990000001", result.phone());
        assertEquals("@requester", result.socialNickname());
        assertTrue(result.contactsVisible());
    }

    @Test
    void shouldWithdrawResponseAndSubtractFiftyPoints() {
        UserEntity requester = user(1L, "USER", "+79990000001");
        UserEntity volunteer = user(2L, "VOLUNTEER", "+79990000002");
        volunteer.setTotalPoints(80);
        HelpRequestEntity request = helpRequest(30L, requester, volunteer, "ACCEPTED");
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000002"))).thenReturn(Optional.of(volunteer));
        when(requests.findById(30L)).thenReturn(Optional.of(request));
        when(requests.save(request)).thenReturn(request);

        VolunteerService.HelpRequestResp result = service.withdrawResponse("+79990000002", "30");

        assertEquals("OPEN", result.status());
        assertNull(request.getVolunteer());
        assertEquals(30, volunteer.getTotalPoints());
        verify(users).save(volunteer);
    }

    @Test
    void shouldStartWalkOnlyAfterBothParticipantsConfirm() {
        UserEntity requester = user(1L, "USER", "+79990000001");
        UserEntity volunteer = user(2L, "VOLUNTEER", "+79990000002");
        HelpRequestEntity request = helpRequest(30L, requester, volunteer, "ACCEPTED");
        request.setDate(LocalDate.now());
        request.setTime(LocalTime.now().minusMinutes(10));
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000001"))).thenReturn(Optional.of(requester));
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000002"))).thenReturn(Optional.of(volunteer));
        when(requests.findById(30L)).thenReturn(Optional.of(request));
        when(requests.save(request)).thenReturn(request);

        VolunteerService.HelpRequestResp afterRequester = service.startWalk("+79990000001", "30", routeReq());
        VolunteerService.HelpRequestResp afterVolunteer = service.startWalk("+79990000002", "30", null);

        assertFalse(afterRequester.started());
        assertTrue(afterVolunteer.started());
        assertNotNull(request.getStartedAt());
        assertEquals("59.93,30.31;59.93,30.32", request.getPlannedRoutePoints());
    }

    @Test
    void shouldCreateSosWhenParticipantIsMoreThanOneKilometerAwayFromRoute() {
        UserEntity requester = user(1L, "USER", "+79990000001");
        UserEntity volunteer = user(2L, "VOLUNTEER", "+79990000002");
        HelpRequestEntity request = helpRequest(30L, requester, volunteer, "ACCEPTED");
        request.setStartedAt(Instant.now());
        request.setPlannedRoutePoints("59.93,30.31;59.93,30.32");
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000002"))).thenReturn(Optional.of(volunteer));
        when(requests.findById(30L)).thenReturn(Optional.of(request));
        when(requests.save(request)).thenReturn(request);
        when(applications.findFirstByApplicantIdOrderByCreatedAtDesc(2L)).thenReturn(Optional.empty());
        when(sosNotifications.save(any(SosNotificationEntity.class))).thenAnswer(invocation -> {
            SosNotificationEntity sos = invocation.getArgument(0);
            sos.setId(100L);
            sos.setCreatedAt(Instant.now());
            return sos;
        });

        service.updateLocation("+79990000002", "30", new VolunteerService.LocationReq(59.95, 30.31));

        verify(sosNotifications).save(argThat(sos -> "ROUTE_DEVIATION".equals(sos.getReason())));
    }

    @Test
    void shouldNotCreateSosWhenParticipantIsCloseToRoute() {
        UserEntity requester = user(1L, "USER", "+79990000001");
        UserEntity volunteer = user(2L, "VOLUNTEER", "+79990000002");
        HelpRequestEntity request = helpRequest(30L, requester, volunteer, "ACCEPTED");
        request.setStartedAt(Instant.now());
        request.setPlannedRoutePoints("59.93,30.31;59.93,30.32");
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000002"))).thenReturn(Optional.of(volunteer));
        when(requests.findById(30L)).thenReturn(Optional.of(request));
        when(requests.save(request)).thenReturn(request);

        service.updateLocation("+79990000002", "30", new VolunteerService.LocationReq(59.9305, 30.315));

        verifyNoInteractions(sosNotifications);
    }

    @Test
    void shouldFinishWalkAndAddRewardAfterBothParticipantsFinish() {
        UserEntity requester = user(1L, "USER", "+79990000001");
        UserEntity volunteer = user(2L, "VOLUNTEER", "+79990000002");
        volunteer.setTotalPoints(20);
        HelpRequestEntity request = helpRequest(30L, requester, volunteer, "ACCEPTED");
        request.setStartedAt(Instant.now());
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000001"))).thenReturn(Optional.of(requester));
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000002"))).thenReturn(Optional.of(volunteer));
        when(requests.findById(30L)).thenReturn(Optional.of(request));
        when(requests.save(request)).thenReturn(request);

        VolunteerService.HelpRequestResp afterRequester = service.finishWalk("+79990000001", "30");
        VolunteerService.HelpRequestResp afterVolunteer = service.finishWalk("+79990000002", "30");

        assertFalse(afterRequester.completed());
        assertTrue(afterVolunteer.completed());
        assertEquals("COMPLETED", request.getStatus());
        assertEquals(120, volunteer.getTotalPoints());
        verify(users).save(volunteer);
    }

    @Test
    void shouldApplyVolunteerComplaintPenaltyAndWeekBanOnThirdWarning() {
        UserEntity requester = user(1L, "USER", "+79990000001");
        UserEntity volunteer = user(2L, "VOLUNTEER", "+79990000002");
        UserEntity moderator = user(3L, "MODERATOR", "+79990000003");
        volunteer.setTotalPoints(200);
        HelpRequestEntity request = helpRequest(30L, requester, volunteer, "ACCEPTED");
        VolunteerComplaintEntity complaint = complaint(40L, request, requester, volunteer);
        VolunteerUserStateEntity state = state(volunteer);
        state.setVolunteerWarnings(2);
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000003"))).thenReturn(Optional.of(moderator));
        when(complaints.findById(40L)).thenReturn(Optional.of(complaint));
        when(users.findById(2L)).thenReturn(Optional.of(volunteer));
        when(states.findById(2L)).thenReturn(Optional.of(state));
        when(states.save(state)).thenReturn(state);
        when(complaints.save(complaint)).thenReturn(complaint);

        VolunteerService.ComplaintResp result = service.resolveComplaint(
                "+79990000003",
                "40",
                new VolunteerService.ResolveComplaintReq("2", "Волонтер не пришел")
        );

        assertEquals("RESOLVED", result.status());
        assertEquals(100, volunteer.getTotalPoints());
        assertEquals(0, state.getVolunteerWarnings());
        assertNotNull(state.getVolunteerBannedUntil());
        assertTrue(state.getVolunteerBannedUntil().isAfter(Instant.now().plus(Duration.ofDays(6))));
    }

    @Test
    void shouldRejectAvailableRequestsForBannedVolunteer() {
        UserEntity volunteer = user(2L, "VOLUNTEER", "+79990000002");
        VolunteerUserStateEntity state = state(volunteer);
        state.setVolunteerBannedUntil(Instant.now().plus(Duration.ofDays(1)));
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000002"))).thenReturn(Optional.of(volunteer));
        when(states.findById(2L)).thenReturn(Optional.of(state));

        assertThrows(RuntimeException.class, () -> service.listAvailableRequests("+79990000002", 59.93, 30.31));
    }


    @Test
    void shouldConfirmSosAndKeepItOpenUntilResolved() {
        UserEntity requester = user(1L, "USER", "+79990000001");
        UserEntity volunteer = user(2L, "VOLUNTEER", "+79990000002");
        UserEntity moderator = user(3L, "MODERATOR", "+79990000003");
        HelpRequestEntity request = helpRequest(30L, requester, volunteer, "ACCEPTED");
        SosNotificationEntity sos = sos(100L, request, requester, "MANUAL", "OPEN");
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000003"))).thenReturn(Optional.of(moderator));
        when(sosNotifications.findById(100L)).thenReturn(Optional.of(sos));
        when(sosNotifications.save(sos)).thenReturn(sos);
        when(applications.findFirstByApplicantIdOrderByCreatedAtDesc(2L)).thenReturn(Optional.empty());

        VolunteerService.SosResp result = service.confirmSos(
                "+79990000003",
                "100",
                new VolunteerService.ResolveSosReq("Связались с участниками")
        );

        assertEquals("CONFIRMED", result.status());
        assertEquals("CONFIRMED", sos.getStatus());
        assertEquals(moderator, sos.getModerator());
        assertEquals("Связались с участниками", sos.getModeratorComment());
        assertNull(sos.getResolvedAt());
    }

    @Test
    void shouldMarkSosAsFalseAlarmAndCloseIt() {
        UserEntity requester = user(1L, "USER", "+79990000001");
        UserEntity volunteer = user(2L, "VOLUNTEER", "+79990000002");
        UserEntity moderator = user(3L, "MODERATOR", "+79990000003");
        HelpRequestEntity request = helpRequest(30L, requester, volunteer, "ACCEPTED");
        SosNotificationEntity sos = sos(100L, request, requester, "MANUAL", "OPEN");
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000003"))).thenReturn(Optional.of(moderator));
        when(sosNotifications.findById(100L)).thenReturn(Optional.of(sos));
        when(sosNotifications.save(sos)).thenReturn(sos);
        when(applications.findFirstByApplicantIdOrderByCreatedAtDesc(2L)).thenReturn(Optional.empty());

        VolunteerService.SosResp result = service.markSosFalseAlarm(
                "+79990000003",
                "100",
                new VolunteerService.ResolveSosReq("Ошибочное нажатие")
        );

        assertEquals("FALSE_ALARM", result.status());
        assertEquals("FALSE_ALARM", sos.getStatus());
        assertNotNull(sos.getResolvedAt());
    }

    @Test
    void shouldResolveConfirmedSos() {
        UserEntity requester = user(1L, "USER", "+79990000001");
        UserEntity volunteer = user(2L, "VOLUNTEER", "+79990000002");
        UserEntity moderator = user(3L, "MODERATOR", "+79990000003");
        HelpRequestEntity request = helpRequest(30L, requester, volunteer, "ACCEPTED");
        SosNotificationEntity sos = sos(100L, request, volunteer, "ROUTE_DEVIATION", "CONFIRMED");
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000003"))).thenReturn(Optional.of(moderator));
        when(sosNotifications.findById(100L)).thenReturn(Optional.of(sos));
        when(sosNotifications.save(sos)).thenReturn(sos);
        when(applications.findFirstByApplicantIdOrderByCreatedAtDesc(2L)).thenReturn(Optional.empty());

        VolunteerService.SosResp result = service.resolveSos(
                "+79990000003",
                "100",
                new VolunteerService.ResolveSosReq("Участники в безопасности")
        );

        assertEquals("RESOLVED", result.status());
        assertEquals("RESOLVED", sos.getStatus());
        assertNotNull(sos.getResolvedAt());
    }

    @Test
    void shouldListOnlyOpenAndConfirmedSosInActiveModerationList() {
        when(sosNotifications.findByStatusInOrderByCreatedAtDesc(List.of("OPEN", "CONFIRMED"))).thenReturn(List.of());

        service.listSosNotifications();

        verify(sosNotifications).findByStatusInOrderByCreatedAtDesc(List.of("OPEN", "CONFIRMED"));
        verify(sosNotifications, never()).findAllByOrderByCreatedAtDesc();
    }

    private VolunteerService.HelpRequestReq helpRequestReq() {
        return new VolunteerService.HelpRequestReq(
                "Санкт-Петербург, Садовая улица, дом 12",
                "Санкт-Петербург, Невский проспект, дом 1",
                LocalDate.now().plusDays(1).format(DATE_FORMAT),
                "12:30",
                "+79990000001",
                "@requester",
                "Нужно помочь пройти маршрут"
        );
    }

    private VolunteerService.WalkRouteReq routeReq() {
        return new VolunteerService.WalkRouteReq(
                null,
                List.of(
                        new VolunteerService.RoutePointReq(59.93, 30.31),
                        new VolunteerService.RoutePointReq(59.93, 30.32)
                )
        );
    }

    private UserEntity user(Long id, String role, String phone) {
        UserEntity user = UserEntity.builder()
                .firstName("Имя" + id)
                .lastName("Фамилия" + id)
                .phoneHash(Crypto.sha256Hex(Crypto.normPhone(phone)))
                .role(role)
                .passHash("hash")
                .active(true)
                .totalPoints(0)
                .build();
        user.setId(id);
        return user;
    }

    private VolunteerApplicationEntity application(Long id, UserEntity applicant, String status) {
        VolunteerApplicationEntity application = new VolunteerApplicationEntity();
        application.setId(id);
        application.setApplicant(applicant);
        application.setDobroUrl("https://dobro.ru/volunteer/1");
        application.setPhone("79990000001");
        application.setSocialNickname("@volunteer");
        application.setStatus(status);
        application.setCreatedAt(Instant.now());
        return application;
    }

    private VolunteerUserStateEntity state(UserEntity user) {
        VolunteerUserStateEntity state = new VolunteerUserStateEntity();
        state.setUser(user);
        state.setUserId(user.getId());
        return state;
    }

    private HelpRequestEntity helpRequest(Long id, UserEntity requester, UserEntity volunteer, String status) {
        HelpRequestEntity request = new HelpRequestEntity();
        request.setId(id);
        request.setRequester(requester);
        request.setVolunteer(volunteer);
        request.setFromAddress("Санкт-Петербург, Садовая улица, дом 12");
        request.setToAddress("Санкт-Петербург, Невский проспект, дом 1");
        request.setDate(LocalDate.now().plusDays(1));
        request.setTime(LocalTime.of(12, 30));
        request.setPhone("79990000001");
        request.setSocialNickname("@requester");
        request.setComment("Нужно помочь пройти маршрут");
        request.setStatus(status);
        request.setCreatedAt(Instant.now());
        return request;
    }


    private SosNotificationEntity sos(Long id, HelpRequestEntity request, UserEntity sender, String reason, String status) {
        SosNotificationEntity sos = new SosNotificationEntity();
        sos.setId(id);
        sos.setRequest(request);
        sos.setSender(sender);
        sos.setReason(reason);
        sos.setStatus(status);
        sos.setComment("Комментарий SOS");
        sos.setCreatedAt(Instant.now());
        return sos;
    }

    private VolunteerComplaintEntity complaint(Long id, HelpRequestEntity request, UserEntity author, UserEntity target) {
        VolunteerComplaintEntity complaint = new VolunteerComplaintEntity();
        complaint.setId(id);
        complaint.setRequest(request);
        complaint.setAuthor(author);
        complaint.setTarget(target);
        complaint.setText("Жалоба");
        complaint.setStatus("PENDING");
        complaint.setCreatedAt(Instant.now());
        return complaint;
    }
}
