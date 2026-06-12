package goodroad.volunteer;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import goodroad.api.ApiErrors.ApiException;
import goodroad.model.Role;
import goodroad.security.Crypto;
import goodroad.storage.StorageService;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import goodroad.validation.InputRules;
import goodroad.volunteer.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import goodroad.points.PointLedgerService;
import goodroad.tasks.TaskService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class VolunteerService {
    private static final int WALK_REWARD = 100;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final UserRepo users;
    private final VolunteerApplicationRepo applications;
    private final VolunteerApplicationPhotoRepo applicationPhotos;
    private final HelpRequestRepo requests;
    private final StorageService storageService;

    @Autowired(required = false)
    private PointLedgerService pointLedger;

    @Autowired(required = false)
    private TaskService taskService;

    public VolunteerService(
            UserRepo users,
            VolunteerApplicationRepo applications,
            VolunteerApplicationPhotoRepo applicationPhotos,
            HelpRequestRepo requests,
            StorageService storageService
    ) {
        this.users = users;
        this.applications = applications;
        this.applicationPhotos = applicationPhotos;
        this.requests = requests;
        this.storageService = storageService;
    }

    public record VolunteerMenuResp(boolean volunteer, String applicationStatus, String rejectReason) {}
    public record CreateVolunteerApplicationReq(String dobroUrl, String phone, String socialNickname, List<String> certificatePhotoUrls) {}
    public record PhotoUploadResp(String photoUrl) {}
    public record VolunteerApplicationResp(String id, String applicantId, String applicantName, String dobroUrl, String phone, String socialNickname, List<String> certificatePhotoUrls, String status, String moderatorComment, Instant createdAt, Instant moderatedAt) {}
    public record RejectApplicationReq(String reason) {}
    public record HelpRequestReq(String fromAddress, String toAddress, String date, String time, String phone, String socialNickname, String comment) {}
    public record HelpRequestResp(String id, String requesterId, String volunteerId, String fromAddress, String toAddress, String date, String time, String phone, String socialNickname, String comment, String status, boolean contactsVisible, boolean completed, Instant createdAt) {}
    public record RoutePointReq(Double latitude, Double longitude) {}
    public record WalkRouteReq(
            @JsonProperty("points") @JsonAlias("encodedPoints") String encodedPoints,
            @JsonProperty("routePoints") List<RoutePointReq> routePoints
    ) {}

    @Transactional(readOnly = true)
    public VolunteerMenuResp getMenu(String phoneFromAuth) {
        UserEntity user = findCurrent(phoneFromAuth);
        VolunteerApplicationEntity last = applications.findFirstByApplicantIdOrderByCreatedAtDesc(user.getId()).orElse(null);
        return new VolunteerMenuResp(
                isVolunteer(user),
                last == null ? null : last.getStatus(),
                last == null ? null : last.getModeratorComment()
        );
    }

    @Transactional
    public VolunteerApplicationResp createApplication(String phoneFromAuth, CreateVolunteerApplicationReq req) {
        if (req == null) {
            throw bad("VOLUNTEER_APPLICATION_EMPTY", "Application is empty");
        }
        UserEntity user = findCurrent(phoneFromAuth);
        if (isVolunteer(user)) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_VOLUNTEER", "User is already volunteer");
        }
        applications.findFirstByApplicantIdOrderByCreatedAtDesc(user.getId())
                .filter(app -> "PENDING".equals(app.getStatus()))
                .ifPresent(app -> { throw new ApiException(HttpStatus.CONFLICT, "APPLICATION_ALREADY_PENDING", "Application is already pending"); });

        VolunteerApplicationEntity app = new VolunteerApplicationEntity();
        app.setApplicant(user);
        app.setDobroUrl(requireUrl(req.dobroUrl(), "DOBRO_URL_INVALID", "Dobro.ru link is invalid"));
        app.setPhone(Crypto.normPhone(req.phone()));
        if (app.getPhone().isEmpty()) {
            throw bad("PHONE_INVALID", "Phone is invalid");
        }
        app.setSocialNickname(InputRules.trimToNull(req.socialNickname()));
        applications.save(app);

        if (req.certificatePhotoUrls() != null) {
            for (String rawUrl : req.certificatePhotoUrls()) {
                String url = requireUrl(rawUrl, "CERTIFICATE_URL_INVALID", "Certificate URL is invalid");
                VolunteerApplicationPhotoEntity photo = new VolunteerApplicationPhotoEntity();
                photo.setApplication(app);
                photo.setUrl(url);
                app.getPhotos().add(photo);
            }
        }
        return toApplicationResp(app);
    }

    @Transactional
    public PhotoUploadResp uploadCertificate(String phoneFromAuth, MultipartFile file) {
        UserEntity user = findCurrent(phoneFromAuth);
        String url = storageService.uploadVolunteerCertificate(file, user.getId().toString());
        return new PhotoUploadResp(url);
    }

    @Transactional(readOnly = true)
    public List<VolunteerApplicationResp> listPendingApplications() {
        return applications.findByStatusOrderByCreatedAtAsc("PENDING").stream().map(this::toApplicationResp).toList();
    }

    @Transactional
    public VolunteerApplicationResp approveApplication(String moderatorPhone, String id) {
        UserEntity moderator = requireModerator(moderatorPhone);
        VolunteerApplicationEntity app = findApplication(id);
        if (!"PENDING".equals(app.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "APPLICATION_ALREADY_PROCESSED", "Application already processed");
        }
        app.setStatus("APPROVED");
        app.setModerator(moderator);
        app.setModeratedAt(Instant.now());
        app.getApplicant().setRole(Role.VOLUNTEER.name());
        users.save(app.getApplicant());
        applications.save(app);
        return toApplicationResp(app);
    }

    @Transactional
    public VolunteerApplicationResp rejectApplication(String moderatorPhone, String id, RejectApplicationReq req) {
        UserEntity moderator = requireModerator(moderatorPhone);
        VolunteerApplicationEntity app = findApplication(id);
        String reason = InputRules.trimToNull(req == null ? null : req.reason());
        if (reason == null) {
            throw bad("REJECT_REASON_EMPTY", "Reject reason is empty");
        }
        if (!"PENDING".equals(app.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "APPLICATION_ALREADY_PROCESSED", "Application already processed");
        }
        app.setStatus("REJECTED");
        app.setModeratorComment(reason);
        app.setModerator(moderator);
        app.setModeratedAt(Instant.now());
        return toApplicationResp(applications.save(app));
    }

    @Transactional
    public HelpRequestResp createHelpRequest(String phoneFromAuth, HelpRequestReq req) {
        if (req == null) {
            throw bad("HELP_REQUEST_EMPTY", "Help request is empty");
        }
        UserEntity requester = findCurrent(phoneFromAuth);
        HelpRequestEntity request = new HelpRequestEntity();
        request.setRequester(requester);
        fillRequest(request, req);
        return toHelpResp(requests.save(request), requester, false);
    }

    @Transactional(readOnly = true)
    public List<HelpRequestResp> listOwnRequests(String phoneFromAuth) {
        UserEntity user = findCurrent(phoneFromAuth);
        return requests.findByRequesterIdOrderByDateDescTimeDescCreatedAtDesc(user.getId()).stream()
                .map(request -> toHelpResp(request, user, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HelpRequestResp> listMyWards(String phoneFromAuth) {
        UserEntity volunteer = requireVolunteer(phoneFromAuth);
        return requests.findByVolunteerIdOrderByDateDescTimeDescCreatedAtDesc(volunteer.getId()).stream()
                .map(request -> toHelpResp(request, volunteer, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HelpRequestResp> listAvailableRequests(String phoneFromAuth, Double latitude, Double longitude) {
        UserEntity volunteer = requireVolunteer(phoneFromAuth);
        return requests.findByStatusOrderByDateAscTimeAscCreatedAtAsc("OPEN").stream()
                .filter(request -> !request.getRequester().getId().equals(volunteer.getId()))
                .sorted(Comparator.comparing(HelpRequestEntity::getDate).thenComparing(HelpRequestEntity::getTime))
                .map(request -> toHelpResp(request, volunteer, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public HelpRequestResp getHelpRequest(String phoneFromAuth, String id) {
        UserEntity user = findCurrent(phoneFromAuth);
        return toHelpResp(findRequest(id), user, true);
    }

    @Transactional
    public HelpRequestResp cancelOwnRequest(String phoneFromAuth, String id) {
        UserEntity user = findCurrent(phoneFromAuth);
        HelpRequestEntity request = findRequest(id);
        requireRequester(request, user);
        if ("COMPLETED".equals(request.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_COMPLETED", "Completed request cannot be cancelled");
        }
        request.setStatus("CANCELLED");
        request.setCancelledAt(Instant.now());
        return toHelpResp(requests.save(request), user, true);
    }

    @Transactional
    public void deleteOwnRequest(String phoneFromAuth, String id) {
        UserEntity user = findCurrent(phoneFromAuth);
        HelpRequestEntity request = findRequest(id);
        requireRequester(request, user);
        if ("COMPLETED".equals(request.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_COMPLETED", "Completed request cannot be deleted");
        }
        if (request.getVolunteer() == null && "OPEN".equals(request.getStatus())) {
            requests.delete(request);
            return;
        }
        request.setStatus("CANCELLED");
        request.setCancelledAt(Instant.now());
        requests.save(request);
    }

    @Transactional
    public HelpRequestResp acceptRequest(String phoneFromAuth, String id) {
        UserEntity volunteer = requireVolunteer(phoneFromAuth);
        HelpRequestEntity request = findRequest(id);
        if (!"OPEN".equals(request.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_NOT_OPEN", "Help request is not open");
        }
        if (request.getRequester().getId().equals(volunteer.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OWN_REQUEST_ACCEPT", "Volunteer cannot accept own request");
        }
        request.setVolunteer(volunteer);
        request.setStatus("ACCEPTED");
        request.setAcceptedAt(Instant.now());
        return toHelpResp(requests.save(request), volunteer, true);
    }

    @Transactional
    public HelpRequestResp withdrawResponse(String phoneFromAuth, String id) {
        UserEntity volunteer = requireVolunteer(phoneFromAuth);
        HelpRequestEntity request = findRequest(id);
        requireVolunteerOfRequest(request, volunteer);
        if (!"ACCEPTED".equals(request.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_CANNOT_WITHDRAW", "Response cannot be withdrawn");
        }
        request.setVolunteer(null);
        request.setAcceptedAt(null);
        request.setStatus("OPEN");
        return toHelpResp(requests.save(request), volunteer, false);
    }

    @Transactional
    public HelpRequestResp setWalkRoute(String phoneFromAuth, String id, WalkRouteReq req) {
        UserEntity user = findCurrent(phoneFromAuth);
        HelpRequestEntity request = findRequest(id);
        requireParticipant(request, user);
        if (!"ACCEPTED".equals(request.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_NOT_ACCEPTED", "Route can be saved only for accepted request");
        }
        saveWalkRoute(request, req);
        return toHelpResp(requests.save(request), user, true);
    }

    @Transactional
    public HelpRequestResp finishWalk(String phoneFromAuth, String id) {
        UserEntity user = findCurrent(phoneFromAuth);
        HelpRequestEntity request = findRequest(id);
        requireParticipant(request, user);
        if (!"ACCEPTED".equals(request.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_NOT_ACCEPTED", "Walk can be finished only for accepted request");
        }
        Instant now = Instant.now();
        if (request.getRequester().getId().equals(user.getId())) {
            request.setRequesterFinishedAt(now);
        } else {
            request.setVolunteerFinishedAt(now);
        }
        if (request.getRequesterFinishedAt() != null && request.getVolunteerFinishedAt() != null) {
            request.setStatus("COMPLETED");
            request.setCompletedAt(now);
            UserEntity volunteer = request.getVolunteer();
            if (pointLedger != null) {
                pointLedger.earn(volunteer, WALK_REWARD, "VOLUNTEER_WALK_COMPLETED", "Завершена волонтерская прогулка", null, "HELP_REQUEST", request.getId());
            } else {
                volunteer.setTotalPoints(volunteer.getTotalPoints() + WALK_REWARD);
                users.save(volunteer);
            }
            if (taskService != null) {
                taskService.registerCompletedHelp(volunteer.getId(), request.getId());
            }
        }
        return toHelpResp(requests.save(request), user, true);
    }

    private VolunteerApplicationResp toApplicationResp(VolunteerApplicationEntity app) {
        return new VolunteerApplicationResp(
                app.getId() == null ? null : app.getId().toString(),
                app.getApplicant().getId().toString(),
                joinName(app.getApplicant()),
                app.getDobroUrl(),
                app.getPhone(),
                app.getSocialNickname(),
                app.getPhotos().stream().map(VolunteerApplicationPhotoEntity::getUrl).toList(),
                app.getStatus(),
                app.getModeratorComment(),
                app.getCreatedAt(),
                app.getModeratedAt()
        );
    }

    private HelpRequestResp toHelpResp(HelpRequestEntity request, UserEntity viewer, boolean details) {
        boolean requester = request.getRequester().getId().equals(viewer.getId());
        boolean assignedVolunteer = request.getVolunteer() != null && request.getVolunteer().getId().equals(viewer.getId());
        boolean contactsVisible = requester || assignedVolunteer;
        return new HelpRequestResp(
                request.getId() == null ? null : request.getId().toString(),
                request.getRequester().getId().toString(),
                request.getVolunteer() == null ? null : request.getVolunteer().getId().toString(),
                request.getFromAddress(),
                request.getToAddress(),
                DATE_FORMAT.format(request.getDate()),
                request.getTime().toString(),
                contactsVisible ? request.getPhone() : null,
                contactsVisible ? request.getSocialNickname() : null,
                request.getComment(),
                request.getStatus(),
                contactsVisible,
                "COMPLETED".equals(request.getStatus()),
                request.getCreatedAt()
        );
    }

    private void fillRequest(HelpRequestEntity request, HelpRequestReq req) {
        String fromAddress = InputRules.trimToNull(req.fromAddress());
        String toAddress = InputRules.trimToNull(req.toAddress());
        String comment = InputRules.trimToNull(req.comment());
        if (fromAddress == null) {
            throw bad("FROM_ADDRESS_EMPTY", "From address is empty");
        }
        if (toAddress == null) {
            throw bad("TO_ADDRESS_EMPTY", "To address is empty");
        }
        if (comment == null) {
            throw bad("COMMENT_EMPTY", "Comment is empty");
        }
        String phone = Crypto.normPhone(req.phone());
        if (phone.isEmpty()) {
            throw bad("PHONE_INVALID", "Phone is invalid");
        }
        request.setFromAddress(fromAddress);
        request.setToAddress(toAddress);
        request.setDate(parseDate(req.date()));
        request.setTime(parseTime(req.time()));
        request.setPhone(phone);
        request.setSocialNickname(InputRules.trimToNull(req.socialNickname()));
        request.setComment(comment);
    }

    private void saveWalkRoute(HelpRequestEntity request, WalkRouteReq req) {
        List<RoutePointReq> points = readRoutePoints(req);
        if (points.size() < 2) {
            throw bad("ROUTE_POINTS_INVALID", "Route must contain at least two points");
        }
        StringJoiner joiner = new StringJoiner(";");
        for (RoutePointReq point : points) {
            validateCoordinates(point.latitude(), point.longitude());
            joiner.add(point.latitude() + "," + point.longitude());
        }
        request.setPlannedRoutePoints(joiner.toString());
    }

    private List<RoutePointReq> readRoutePoints(WalkRouteReq req) {
        if (req == null) {
            throw bad("ROUTE_POINTS_INVALID", "Route is empty");
        }
        if (req.routePoints() != null && !req.routePoints().isEmpty()) {
            return req.routePoints();
        }
        String encoded = InputRules.trimToNull(req.encodedPoints());
        if (encoded == null) {
            throw bad("ROUTE_POINTS_INVALID", "Route is empty");
        }
        return decodePolyline(encoded);
    }

    private List<RoutePointReq> decodePolyline(String encoded) {
        List<RoutePointReq> points = new ArrayList<>();
        int index = 0;
        int lat = 0;
        int lng = 0;
        while (index < encoded.length()) {
            int[] latResult = decodePolylineValue(encoded, index);
            lat += latResult[0];
            index = latResult[1];
            int[] lngResult = decodePolylineValue(encoded, index);
            lng += lngResult[0];
            index = lngResult[1];
            points.add(new RoutePointReq(lat / 1e5, lng / 1e5));
        }
        return points;
    }

    private int[] decodePolylineValue(String encoded, int index) {
        int result = 0;
        int shift = 0;
        int b;
        do {
            if (index >= encoded.length()) {
                throw bad("ROUTE_POINTS_INVALID", "Encoded route is invalid");
            }
            b = encoded.charAt(index++) - 63;
            result |= (b & 0x1f) << shift;
            shift += 5;
        } while (b >= 0x20);
        int delta = (result & 1) != 0 ? ~(result >> 1) : result >> 1;
        return new int[] { delta, index };
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw bad("LOCATION_INVALID", "Location is invalid");
        }
    }

    private UserEntity findCurrent(String phoneFromAuth) {
        String phoneNorm = Crypto.normPhone(phoneFromAuth);
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_PHONE_NOT_FOUND", "User with given phone not found");
        }
        return users.findByPhoneHash(Crypto.sha256Hex(phoneNorm))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_PHONE_NOT_FOUND", "User with given phone not found"));
    }

    private UserEntity requireModerator(String phoneFromAuth) {
        UserEntity user = findCurrent(phoneFromAuth);
        if (!Role.MODERATOR.name().equals(user.getRole()) && !Role.MODERATOR_ADMIN.name().equals(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "MODERATOR_REQUIRED", "Moderator rights required");
        }
        return user;
    }

    private UserEntity requireVolunteer(String phoneFromAuth) {
        UserEntity user = findCurrent(phoneFromAuth);
        if (!isVolunteer(user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "VOLUNTEER_REQUIRED", "Volunteer rights required");
        }
        return user;
    }

    private boolean isVolunteer(UserEntity user) {
        return Role.VOLUNTEER.name().equals(user.getRole());
    }

    private void requireRequester(HelpRequestEntity request, UserEntity user) {
        if (!request.getRequester().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "REQUEST_OWNER_REQUIRED", "Request owner rights required");
        }
    }

    private void requireVolunteerOfRequest(HelpRequestEntity request, UserEntity user) {
        if (request.getVolunteer() == null || !request.getVolunteer().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "REQUEST_VOLUNTEER_REQUIRED", "Request volunteer rights required");
        }
    }

    private void requireParticipant(HelpRequestEntity request, UserEntity user) {
        if (!isParticipant(request, user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "REQUEST_PARTICIPANT_REQUIRED", "Request participant rights required");
        }
    }

    private boolean isParticipant(HelpRequestEntity request, UserEntity user) {
        return request.getRequester().getId().equals(user.getId())
                || request.getVolunteer() != null && request.getVolunteer().getId().equals(user.getId());
    }

    private VolunteerApplicationEntity findApplication(String id) {
        return applications.findById(parseId(id, "APPLICATION_ID_INVALID", "Application id is invalid"))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND", "Application not found"));
    }

    private HelpRequestEntity findRequest(String id) {
        return requests.findById(parseId(id, "HELP_REQUEST_ID_INVALID", "Help request id is invalid"))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "HELP_REQUEST_NOT_FOUND", "Help request not found"));
    }

    private Long parseId(String raw, String code, String msg) {
        try {
            return Long.parseLong(raw);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, code, msg);
        }
    }

    private LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw bad("DATE_INVALID", "Date must have dd-MM-yyyy format");
        }
    }

    private LocalTime parseTime(String raw) {
        try {
            return LocalTime.parse(raw);
        } catch (DateTimeParseException e) {
            throw bad("TIME_INVALID", "Time must have HH:mm format");
        }
    }

    private String requireUrl(String raw, String code, String msg) {
        String value = InputRules.trimToNull(raw);
        if (value == null || !(value.startsWith("http://") || value.startsWith("https://"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, code, msg);
        }
        return value;
    }

    private ApiException bad(String code, String msg) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, msg);
    }

    private String joinName(UserEntity user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();
        return (first + " " + last).trim();
    }
}