package goodroad.storage;

import goodroad.api.ApiErrors.ApiException;
import goodroad.validation.UploadValidationService;
import goodroad.validation.UploadValidationService.UploadPurpose;
import goodroad.validation.UploadValidationService.VerifiedUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3Client;
    private final UploadValidationService uploadValidator;

    @Value("${yandex.storage.bucket}")
    private String bucket;

    public String uploadAvatar(MultipartFile file, String userId) {

        VerifiedUpload verified = uploadValidator.validate(file, UploadPurpose.AVATAR);

        try {

            String key = "avatars/" + userId + "/" + UUID.randomUUID() + verified.extension();

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(verified.contentType())
                            .contentLength((long) verified.bytes().length)
                            .build(),
                    RequestBody.fromBytes(verified.bytes())
            );

            return "https://storage.yandexcloud.net/"
                    + bucket + "/"
                    + key;

        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "STORAGE_UNAVAILABLE", "File storage is unavailable");
        }
    }

    public String uploadReviewPhoto(MultipartFile file, String userId) {

        VerifiedUpload verified = uploadValidator.validate(file, UploadPurpose.REVIEW_PHOTO);

        try {

            String key = "reviews/"
                    + userId
                    + "/"
                    + UUID.randomUUID()
                    + verified.extension();

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(verified.contentType())
                            .contentLength((long) verified.bytes().length)
                            .build(),
                    RequestBody.fromBytes(verified.bytes())
            );

            return "https://storage.yandexcloud.net/"
                    + bucket
                    + "/"
                    + key;

        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "STORAGE_UNAVAILABLE", "File storage is unavailable");
        }
    }

    public String uploadVolunteerCertificate(MultipartFile file, String userId) {

        VerifiedUpload verified = uploadValidator.validate(file, UploadPurpose.VOLUNTEER_CERTIFICATE);

        try {

            String key = "volunteer-certificates/"
                    + userId
                    + "/"
                    + UUID.randomUUID()
                    + verified.extension();

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(verified.contentType())
                            .contentLength((long) verified.bytes().length)
                            .build(),
                    RequestBody.fromBytes(verified.bytes())
            );

            return "https://storage.yandexcloud.net/"
                    + bucket
                    + "/"
                    + key;

        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "STORAGE_UNAVAILABLE", "File storage is unavailable");
        }
    }
}