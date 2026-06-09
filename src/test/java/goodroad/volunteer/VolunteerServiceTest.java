package goodroad.volunteer;

import goodroad.api.ApiErrors.ApiException;
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
    private HelpRequestRepo requests;

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
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000002"))).thenReturn(Optional.of(moderator));
        when(applications.findById(20L)).thenReturn(Optional.of(application));
        when(applications.save(application)).thenReturn(application);

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
        when(requests.findById(30L)).thenReturn(Optional.of(helpRequest(30L, requester, null, "OPEN")));

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
    void shouldWithdrawResponseWithoutPenalty() {
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
        assertEquals(80, volunteer.getTotalPoints());
        verify(users, never()).save(volunteer);
    }

    @Test
    void shouldSaveWalkRouteForAcceptedRequest() {
        UserEntity requester = user(1L, "USER", "+79990000001");
        UserEntity volunteer = user(2L, "VOLUNTEER", "+79990000002");
        HelpRequestEntity request = helpRequest(30L, requester, volunteer, "ACCEPTED");
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000001"))).thenReturn(Optional.of(requester));
        when(requests.findById(30L)).thenReturn(Optional.of(request));
        when(requests.save(request)).thenReturn(request);

        VolunteerService.HelpRequestResp result = service.setWalkRoute("+79990000001", "30", routeReq());

        assertEquals("ACCEPTED", result.status());
        assertEquals("59.93,30.31;59.93,30.32", request.getPlannedRoutePoints());
    }

    @Test
    void shouldCompleteWalkAndAddVolunteerPointsAfterBothParticipantsFinish() {
        UserEntity requester = user(1L, "USER", "+79990000001");
        UserEntity volunteer = user(2L, "VOLUNTEER", "+79990000002");
        HelpRequestEntity request = helpRequest(30L, requester, volunteer, "ACCEPTED");
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000001"))).thenReturn(Optional.of(requester));
        when(users.findByPhoneHash(Crypto.sha256Hex("79990000002"))).thenReturn(Optional.of(volunteer));
        when(requests.findById(30L)).thenReturn(Optional.of(request));
        when(requests.save(request)).thenReturn(request);

        VolunteerService.HelpRequestResp afterRequester = service.finishWalk("+79990000001", "30");
        VolunteerService.HelpRequestResp afterVolunteer = service.finishWalk("+79990000002", "30");

        assertFalse(afterRequester.completed());
        assertTrue(afterVolunteer.completed());
        assertEquals("COMPLETED", request.getStatus());
        assertEquals(100, volunteer.getTotalPoints());
        verify(users).save(volunteer);
    }

    private VolunteerService.HelpRequestReq helpRequestReq() {
        return new VolunteerService.HelpRequestReq(
                "Садовая улица, 12",
                "Невский проспект, 1",
                59.93,
                30.31,
                DATE_FORMAT.format(LocalDate.now().plusDays(1)),
                "12:30",
                "+79990000001",
                "@requester",
                "Нужно помочь дойти до остановки"
        );
    }

    private VolunteerService.WalkRouteReq routeReq() {
        return new VolunteerService.WalkRouteReq(null, List.of(
                new VolunteerService.RoutePointReq(59.93, 30.31),
                new VolunteerService.RoutePointReq(59.93, 30.32)
        ));
    }

    private UserEntity user(Long id, String role, String phone) {
        UserEntity user = UserEntity.builder()
                .firstName("Иван")
                .lastName("Петров")
                .phoneHash(Crypto.sha256Hex(Crypto.normPhone(phone)))
                .role(role)
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
        application.setCreatedAt(Instant.parse("2026-05-01T10:00:00Z"));
        return application;
    }

    private HelpRequestEntity helpRequest(Long id, UserEntity requester, UserEntity volunteer, String status) {
        HelpRequestEntity request = new HelpRequestEntity();
        request.setId(id);
        request.setRequester(requester);
        request.setVolunteer(volunteer);
        request.setFromAddress("Садовая улица, 12");
        request.setToAddress("Невский проспект, 1");
        request.setDate(LocalDate.now().plusDays(1));
        request.setTime(LocalTime.of(12, 30));
        request.setPhone("79990000001");
        request.setSocialNickname("@requester");
        request.setComment("Нужно помочь дойти до остановки");
        request.setStatus(status);
        request.setCreatedAt(Instant.parse("2026-05-01T10:00:00Z"));
        return request;
    }
}
