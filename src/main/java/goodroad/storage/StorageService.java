package goodroad.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${yandex.storage.bucket}")
    private String bucket;

    public String uploadAvatar(MultipartFile file, String userId) {

        try {

            String ext = getExt(file.getOriginalFilename());

            String key = "avatars/" + userId + "/" + UUID.randomUUID() + ext;

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            return "https://storage.yandexcloud.net/"
                    + bucket + "/"
                    + key;

        } catch (Exception e) {
            throw new RuntimeException("Upload failed", e);
        }
    }

    public String uploadReviewPhoto(MultipartFile file, String userId) {

        try {

            String ext = getExt(file.getOriginalFilename());

            String key = "reviews/"
                    + userId
                    + "/"
                    + UUID.randomUUID()
                    + ext;

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            return "https://storage.yandexcloud.net/"
                    + bucket
                    + "/"
                    + key;

        } catch (Exception e) {
            throw new RuntimeException("Upload failed", e);
        }
    }

    private String getExt(String name) {
        if (name == null) return "";
        int i = name.lastIndexOf(".");
        return i == -1 ? "" : name.substring(i);
    }
}