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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class VolunteerService {
    private static final int VOLUNTEER_CANCEL_PENALTY = 50;
    private static final int WALK_REWARD = 100;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final UserRepo users;
    private final VolunteerApplicationRepo applications;
    private final VolunteerApplicationPhotoRepo applicationPhotos;
    private final VolunteerUserStateRepo states;
    private final HelpRequestRepo requests;
    private final VolunteerComplaintRepo complaints;
    private final SosNotificationRepo sosNotifications;
    private final StorageService storageService;

    public VolunteerService(
            UserRepo users,
            VolunteerApplicationRepo applications,
            VolunteerApplicationPhotoRepo applicationPhotos,
            VolunteerUserStateRepo states,
            HelpRequestRepo requests,
            VolunteerComplaintRepo complaints,
            SosNotificationRepo sosNotifications,
            StorageService storageService
    ) {
        this.users = users;
        this.applications = applications;
        this.applicationPhotos = applicationPhotos;
        this.states = states;
        this.requests = requests;
        this.complaints = complaints;
        this.sosNotifications = sosNotifications;
        this.storageService = storageService;
    }

    public record VolunteerMenuResp(boolean volunteer, String applicationStatus, String rejectReason, Instant volunteerBannedUntil) {}
    public record CreateVolunteerApplicationReq(String dobroUrl, String phone, String socialNickname, List<String> certificatePhotoUrls) {}
    public record PhotoUploadResp(String photoUrl) {}
    public record VolunteerApplicationResp(String id, String applicantId, String applicantName, String dobroUrl, String phone, String socialNickname, List<String> certificatePhotoUrls, String status, String moderatorComment, Instant createdAt, Instant moderatedAt) {}
    public record RejectApplicationReq(String reason) {}
    public record HelpRequestReq(String fromAddress, String toAddress, String date, String time, String phone, String socialNickname, String comment) {}
    public record HelpRequestResp(String id, String requesterId, String volunteerId, String fromAddress, String toAddress, String date, String time, String phone, String socialNickname, String comment, String status, boolean contactsVisible, boolean canStart, boolean started, boolean completed, Instant createdAt) {}
    public record SosReq(String comment) {}
    public record LocationReq(Double latitude, Double longitude) {}
    public record RoutePointReq(Double latitude, Double longitude) {}
    public record WalkRouteReq(
            @JsonProperty("points") @JsonAlias("encodedPoints") String encodedPoints,
            @JsonProperty("routePoints") List<RoutePointReq> routePoints
    ) {}
    public record ComplaintReq(String text) {}
    public record ComplaintResp(String id, String requestId, String authorId, String targetId, String text, String status, String moderatorComment, Instant createdAt, Instant resolvedAt) {}
    public record ResolveComplaintReq(String guiltyUserId, String moderatorComment) {}
    public record SosResp(String id, String requestId, String reason, String comment, String status, String moderatorComment, String requesterName, String volunteerName, String requesterPhone, String volunteerPhone, String requesterSocial, String volunteerSocial, Instant createdAt, Instant resolvedAt) {}
    public record ResolveSosReq(String moderatorComment) {}

    @Transactional(readOnly = true)
    public VolunteerMenuResp getMenu(String phoneFromAuth) {
        UserEntity user = findCurrent(phoneFromAuth);
        VolunteerUserStateEntity state = states.findById(user.getId()).orElse(null);
        VolunteerApplicationEntity last = applications.findFirstByApplicantIdOrderByCreatedAtDesc(user.getId()).orElse(null);
        return new VolunteerMenuResp(
                isVolunteer(user),
                last == null ? null : last.getStatus(),
                last == null ? null : last.getModeratorComment(),
                state == null ? null : state.getVolunteerBannedUntil()
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
        getOrCreateState(app.getApplicant());
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
        requireNotBanned(volunteer);
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
        requireNotBanned(volunteer);
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
        subtractPoints(volunteer, VOLUNTEER_CANCEL_PENALTY);
        request.setVolunteer(null);
        request.setAcceptedAt(null);
        request.setStatus("OPEN");
        return toHelpResp(requests.save(request), volunteer, false);
    }

    @Transactional
    public HelpRequestResp startWalk(String phoneFromAuth, String id, WalkRouteReq routeReq) {
        UserEntity user = findCurrent(phoneFromAuth);
        HelpRequestEntity request = findRequest(id);
        requireParticipant(request, user);
        if (!"ACCEPTED".equals(request.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_NOT_ACCEPTED", "Walk can start only for accepted request");
        }
        if (routeReq != null) {
            saveWalkRoute(request, routeReq);
        }
        Instant now = Instant.now();
        if (request.getRequester().getId().equals(user.getId())) {
            request.setRequesterStartedAt(now);
        } else {
            request.setVolunteerStartedAt(now);
        }
        if (request.getRequesterStartedAt() != null && request.getVolunteerStartedAt() != null && request.getStartedAt() == null) {
            request.setStartedAt(now);
        }
        return toHelpResp(requests.save(request), user, true);
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
    public HelpRequestResp updateLocation(String phoneFromAuth, String id, LocationReq req) {
        UserEntity user = findCurrent(phoneFromAuth);
        HelpRequestEntity request = findRequest(id);
        requireParticipant(request, user);
        if (request.getStartedAt() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "WALK_NOT_STARTED", "Walk is not started");
        }
        if (req == null) {
            throw bad("LOCATION_INVALID", "Location is invalid");
        }
        validateCoordinates(req.latitude(), req.longitude());
        if (request.getRequester().getId().equals(user.getId())) {
            request.setRequesterLatitude(req.latitude());
            request.setRequesterLongitude(req.longitude());
        } else {
            request.setVolunteerLatitude(req.latitude());
            request.setVolunteerLongitude(req.longitude());
        }
        if (isFarFromPlannedRoute(request, req.latitude(), req.longitude())) {
            createSos(request, user, "ROUTE_DEVIATION", "Participant is more than one kilometer away from the planned route");
        }
        return toHelpResp(requests.save(request), user, true);
    }

    @Transactional
    public SosResp sendSos(String phoneFromAuth, String id, SosReq req) {
        UserEntity user = findCurrent(phoneFromAuth);
        HelpRequestEntity request = findRequest(id);
        requireParticipant(request, user);
        return createSos(request, user, "MANUAL", req == null ? null : req.comment());
    }

    @Transactional
    public HelpRequestResp finishWalk(String phoneFromAuth, String id) {
        UserEntity user = findCurrent(phoneFromAuth);
        HelpRequestEntity request = findRequest(id);
        requireParticipant(request, user);
        if (request.getStartedAt() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "WALK_NOT_STARTED", "Walk is not started");
        }
        Instant now = Instant.now();
        if (Duration.between(request.getStartedAt(), now).toHours() >= 5) {
            createSos(request, user, "TIME_LIMIT", "Walk lasts more than five hours");
        }
        if (request.getRequester().getId().equals(user.getId())) {
            request.setRequesterFinishedAt(now);
        } else {
            request.setVolunteerFinishedAt(now);
        }
        if (request.getRequesterFinishedAt() != null && request.getVolunteerFinishedAt() != null) {
            request.setStatus("COMPLETED");
            request.setCompletedAt(now);
            UserEntity volunteer = request.getVolunteer();
            volunteer.setTotalPoints(volunteer.getTotalPoints() + WALK_REWARD);
            users.save(volunteer);
        }
        return toHelpResp(requests.save(request), user, true);
    }

    @Transactional
    public ComplaintResp createComplaint(String phoneFromAuth, String requestId, ComplaintReq req) {
        UserEntity author = findCurrent(phoneFromAuth);
        HelpRequestEntity request = findRequest(requestId);
        requireParticipant(request, author);
        String text = InputRules.trimToNull(req == null ? null : req.text());
        if (text == null) {
            throw bad("COMPLAINT_TEXT_EMPTY", "Complaint text is empty");
        }
        if (request.getVolunteer() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_HAS_NO_VOLUNTEER", "Complaint is available after a volunteer accepts the request");
        }
        VolunteerComplaintEntity complaint = new VolunteerComplaintEntity();
        complaint.setRequest(request);
        complaint.setAuthor(author);
        complaint.setTarget(request.getRequester().getId().equals(author.getId()) ? request.getVolunteer() : request.getRequester());
        complaint.setText(text);
        return toComplaintResp(complaints.save(complaint));
    }

    @Transactional(readOnly = true)
    public List<ComplaintResp> listPendingComplaints() {
        return complaints.findByStatusOrderByCreatedAtAsc("PENDING").stream().map(this::toComplaintResp).toList();
    }

    @Transactional
    public ComplaintResp resolveComplaint(String moderatorPhone, String id, ResolveComplaintReq req) {
        UserEntity moderator = requireModerator(moderatorPhone);
        VolunteerComplaintEntity complaint = complaints.findById(parseId(id, "COMPLAINT_ID_INVALID", "Complaint id is invalid"))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMPLAINT_NOT_FOUND", "Complaint not found"));
        UserEntity guilty = users.findById(parseId(req == null ? null : req.guiltyUserId(), "GUILTY_USER_ID_INVALID", "Guilty user id is invalid"))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "GUILTY_USER_NOT_FOUND", "Guilty user not found"));
        applyPenalty(complaint.getRequest(), guilty);
        complaint.setStatus("RESOLVED");
        complaint.setGuiltyUser(guilty);
        complaint.setModerator(moderator);
        complaint.setModeratorComment(InputRules.trimToNull(req.moderatorComment()));
        complaint.setResolvedAt(Instant.now());
        return toComplaintResp(complaints.save(complaint));
    }

    @Transactional(readOnly = true)
    public List<SosResp> listSosNotifications() {
        return sosNotifications.findByStatusInOrderByCreatedAtDesc(List.of("OPEN", "CONFIRMED")).stream().map(this::toSosResp).toList();
    }

    @Transactional(readOnly = true)
    public List<SosResp> listAllSosNotifications() {
        return sosNotifications.findAllByOrderByCreatedAtDesc().stream().map(this::toSosResp).toList();
    }

    @Transactional
    public SosResp confirmSos(String moderatorPhone, String id, ResolveSosReq req) {
        return updateSosStatus(moderatorPhone, id, "CONFIRMED", req);
    }

    @Transactional
    public SosResp markSosFalseAlarm(String moderatorPhone, String id, ResolveSosReq req) {
        return updateSosStatus(moderatorPhone, id, "FALSE_ALARM", req);
    }

    @Transactional
    public SosResp resolveSos(String moderatorPhone, String id, ResolveSosReq req) {
        return updateSosStatus(moderatorPhone, id, "RESOLVED", req);
    }

    private SosResp updateSosStatus(String moderatorPhone, String id, String status, ResolveSosReq req) {
        UserEntity moderator = requireModerator(moderatorPhone);
        SosNotificationEntity sos = sosNotifications.findById(parseId(id, "SOS_ID_INVALID", "SOS id is invalid"))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SOS_NOT_FOUND", "SOS notification not found"));
        if ("RESOLVED".equals(sos.getStatus()) || "FALSE_ALARM".equals(sos.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SOS_ALREADY_CLOSED", "SOS notification is already closed");
        }
        if ("FALSE_ALARM".equals(status) && "CONFIRMED".equals(sos.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SOS_ALREADY_CONFIRMED", "Confirmed SOS cannot be marked as false alarm");
        }
        sos.setStatus(status);
        sos.setModerator(moderator);
        sos.setModeratorComment(InputRules.trimToNull(req == null ? null : req.moderatorComment()));
        if ("RESOLVED".equals(status) || "FALSE_ALARM".equals(status)) {
            sos.setResolvedAt(Instant.now());
        }
        return toSosResp(sosNotifications.save(sos));
    }

    private void fillRequest(HelpRequestEntity request, HelpRequestReq req) {
        request.setFromAddress(InputRules.requireAddressText(req.fromAddress(), "FROM_ADDRESS_INVALID", "From address"));
        request.setToAddress(InputRules.requireAddressText(req.toAddress(), "TO_ADDRESS_INVALID", "To address"));
        request.setDate(parseDate(req.date()));
        request.setTime(parseTime(req.time()));
        String phone = Crypto.normPhone(req.phone());
        if (phone.isEmpty()) {
            throw bad("PHONE_INVALID", "Phone is invalid");
        }
        request.setPhone(phone);
        request.setSocialNickname(InputRules.trimToNull(req.socialNickname()));
        String comment = InputRules.trimToNull(req.comment());
        if (comment == null) {
            throw bad("HELP_COMMENT_EMPTY", "Help comment is empty");
        }
        request.setComment(comment);
    }

    private VolunteerApplicationResp toApplicationResp(VolunteerApplicationEntity app) {
        return new VolunteerApplicationResp(
                app.getId().toString(),
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

    private HelpRequestResp toHelpResp(HelpRequestEntity request, UserEntity viewer, boolean allowOwnContacts) {
        boolean participant = isParticipant(request, viewer);
        boolean contactsVisible = allowOwnContacts && participant && request.getVolunteer() != null;
        boolean canStart = participant && "ACCEPTED".equals(request.getStatus()) && scheduledTime(request).minus(30, ChronoUnit.MINUTES).isBefore(Instant.now());
        return new HelpRequestResp(
                request.getId().toString(),
                request.getRequester().getId().toString(),
                request.getVolunteer() == null ? null : request.getVolunteer().getId().toString(),
                request.getFromAddress(),
                request.getToAddress(),
                request.getDate().format(DATE_FORMAT),
                request.getTime().toString(),
                contactsVisible ? request.getPhone() : null,
                contactsVisible ? request.getSocialNickname() : null,
                request.getComment(),
                request.getStatus(),
                contactsVisible,
                canStart,
                request.getStartedAt() != null,
                request.getCompletedAt() != null,
                request.getCreatedAt()
        );
    }

    private ComplaintResp toComplaintResp(VolunteerComplaintEntity complaint) {
        return new ComplaintResp(
                complaint.getId().toString(),
                complaint.getRequest().getId().toString(),
                complaint.getAuthor().getId().toString(),
                complaint.getTarget().getId().toString(),
                complaint.getText(),
                complaint.getStatus(),
                complaint.getModeratorComment(),
                complaint.getCreatedAt(),
                complaint.getResolvedAt()
        );
    }


    private void saveWalkRoute(HelpRequestEntity request, WalkRouteReq req) {
        List<RoutePointReq> points = extractRoutePoints(req);
        if (points.size() < 2) {
            throw bad("ROUTE_POINTS_INVALID", "Route must contain at least two valid points");
        }
        StringJoiner joiner = new StringJoiner(";");
        for (RoutePointReq point : points) {
            validateCoordinates(point.latitude(), point.longitude());
            joiner.add(point.latitude() + "," + point.longitude());
        }
        request.setPlannedRoutePoints(joiner.toString());
    }

    private List<RoutePointReq> extractRoutePoints(WalkRouteReq req) {
        if (req == null) {
            throw bad("ROUTE_EMPTY", "Route is empty");
        }
        if (req.routePoints() != null && !req.routePoints().isEmpty()) {
            return req.routePoints();
        }
        String encoded = InputRules.trimToNull(req.encodedPoints());
        if (encoded != null) {
            return decodePolyline(encoded);
        }
        throw bad("ROUTE_EMPTY", "Route is empty");
    }

    private List<RoutePointReq> decodePolyline(String encoded) {
        List<RoutePointReq> points = new ArrayList<>();
        int index = 0;
        int lat = 0;
        int lon = 0;
        while (index < encoded.length()) {
            int[] latResult = decodePolylineValue(encoded, index);
            lat += latResult[0];
            index = latResult[1];
            if (index >= encoded.length()) {
                throw bad("ROUTE_POINTS_INVALID", "Encoded route is invalid");
            }
            int[] lonResult = decodePolylineValue(encoded, index);
            lon += lonResult[0];
            index = lonResult[1];
            points.add(new RoutePointReq(lat / 100000.0, lon / 100000.0));
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
        return new int[]{delta, index};
    }

    private boolean isFarFromPlannedRoute(HelpRequestEntity request, double latitude, double longitude) {
        List<RoutePointReq> route = parseStoredRoute(request.getPlannedRoutePoints());
        if (route.size() < 2) {
            return false;
        }
        double minDistance = Double.MAX_VALUE;
        for (int i = 1; i < route.size(); i++) {
            RoutePointReq a = route.get(i - 1);
            RoutePointReq b = route.get(i);
            minDistance = Math.min(minDistance, distanceToSegmentMeters(latitude, longitude, a.latitude(), a.longitude(), b.latitude(), b.longitude()));
        }
        return minDistance > 1000;
    }

    private List<RoutePointReq> parseStoredRoute(String raw) {
        String value = InputRules.trimToNull(raw);
        if (value == null) {
            return List.of();
        }
        List<RoutePointReq> points = new ArrayList<>();
        for (String part : value.split(";")) {
            String[] coords = part.split(",");
            if (coords.length != 2) {
                return List.of();
            }
            try {
                points.add(new RoutePointReq(Double.parseDouble(coords[0]), Double.parseDouble(coords[1])));
            } catch (NumberFormatException e) {
                return List.of();
            }
        }
        return points;
    }

    private double distanceToSegmentMeters(double pointLat, double pointLon, double startLat, double startLon, double endLat, double endLon) {
        double refLat = Math.toRadians(pointLat);
        double px = lonToMeters(pointLon, refLat);
        double py = latToMeters(pointLat);
        double ax = lonToMeters(startLon, refLat);
        double ay = latToMeters(startLat);
        double bx = lonToMeters(endLon, refLat);
        double by = latToMeters(endLat);
        double dx = bx - ax;
        double dy = by - ay;
        if (dx == 0 && dy == 0) {
            return Math.hypot(px - ax, py - ay);
        }
        double t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        double closestX = ax + t * dx;
        double closestY = ay + t * dy;
        return Math.hypot(px - closestX, py - closestY);
    }

    private double latToMeters(double lat) {
        return Math.toRadians(lat) * 6371000.0;
    }

    private double lonToMeters(double lon, double refLat) {
        return Math.toRadians(lon) * 6371000.0 * Math.cos(refLat);
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw bad("LOCATION_INVALID", "Location is invalid");
        }
    }

    private SosResp createSos(HelpRequestEntity request, UserEntity sender, String reason, String comment) {
        SosNotificationEntity sos = new SosNotificationEntity();
        sos.setRequest(request);
        sos.setSender(sender);
        sos.setReason(reason);
        sos.setComment(InputRules.trimToNull(comment));
        sos.setStatus("OPEN");
        return toSosResp(sosNotifications.save(sos));
    }

    private SosResp toSosResp(SosNotificationEntity sos) {
        HelpRequestEntity request = sos.getRequest();
        return new SosResp(
                sos.getId().toString(),
                request.getId().toString(),
                sos.getReason(),
                sos.getComment(),
                sos.getStatus(),
                sos.getModeratorComment(),
                joinName(request.getRequester()),
                request.getVolunteer() == null ? null : joinName(request.getVolunteer()),
                request.getPhone(),
                volunteerContact(request.getVolunteer(), true),
                request.getSocialNickname(),
                volunteerContact(request.getVolunteer(), false),
                sos.getCreatedAt(),
                sos.getResolvedAt()
        );
    }

    private String volunteerContact(UserEntity volunteer, boolean phone) {
        if (volunteer == null) {
            return null;
        }
        return applications.findFirstByApplicantIdOrderByCreatedAtDesc(volunteer.getId())
                .filter(app -> "APPROVED".equals(app.getStatus()))
                .map(app -> phone ? app.getPhone() : app.getSocialNickname())
                .orElse(null);
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double radius = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * radius * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private void applyPenalty(HelpRequestEntity request, UserEntity guilty) {
        VolunteerUserStateEntity state = getOrCreateState(guilty);
        boolean guiltyVolunteer = request.getVolunteer() != null && request.getVolunteer().getId().equals(guilty.getId());
        if (guiltyVolunteer) {
            int warnings = state.getVolunteerWarnings() + 1;
            state.setVolunteerWarnings(warnings);
            if (warnings == 1) {
                subtractPoints(guilty, 50);
            } else if (warnings == 2) {
                subtractPoints(guilty, 75);
            } else {
                subtractPoints(guilty, 100);
                state.setVolunteerWarnings(0);
                state.setVolunteerBannedUntil(Instant.now().plus(Duration.ofDays(7)));
            }
        } else {
            int warnings = state.getRequesterWarnings() + 1;
            state.setRequesterWarnings(warnings);
            if (warnings == 1) {
                subtractPoints(guilty, 25);
            } else if (warnings == 2) {
                subtractPoints(guilty, 50);
            } else {
                subtractPoints(guilty, 75);
                state.setRequesterWarnings(0);
            }
        }
        states.save(state);
    }

    private VolunteerUserStateEntity getOrCreateState(UserEntity user) {
        return states.findById(user.getId()).orElseGet(() -> {
            VolunteerUserStateEntity state = new VolunteerUserStateEntity();
            state.setUser(user);
            return states.save(state);
        });
    }

    private void subtractPoints(UserEntity user, int points) {
        user.setTotalPoints(Math.max(0, user.getTotalPoints() - points));
        users.save(user);
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

    private void requireNotBanned(UserEntity volunteer) {
        VolunteerUserStateEntity state = states.findById(volunteer.getId()).orElse(null);
        if (state != null && state.getVolunteerBannedUntil() != null && state.getVolunteerBannedUntil().isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "VOLUNTEER_BANNED", "Volunteer is temporarily banned");
        }
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

    private Instant scheduledTime(HelpRequestEntity request) {
        return LocalDateTime.of(request.getDate(), request.getTime()).atZone(ZoneId.systemDefault()).toInstant();
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
