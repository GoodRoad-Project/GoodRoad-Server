package goodroad.validation;

import goodroad.api.ApiErrors.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Service
public class UploadValidationService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    public VerifiedUpload validate(MultipartFile file, UploadPurpose purpose) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_EMPTY", "Выберите непустой файл для загрузки");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Размер файла не должен превышать 10 МБ");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "FILE_READ_FAILED", "Не удалось прочитать загруженный файл");
        }

        DetectedType detectedType = detectType(bytes);
        if (!purpose.allowedTypes.contains(detectedType)) {
            throw new ApiException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "FILE_CONTENT_TYPE_INVALID",
                    purpose == UploadPurpose.VOLUNTEER_CERTIFICATE
                    ? "Сертификат должен быть файлом JPEG или PNG"
                            : "Изображение должно быть файлом JPEG, PNG или WEBP"
            );
        }

        return new VerifiedUpload(bytes, detectedType.contentType, detectedType.extension);
    }

    private DetectedType detectType(byte[] bytes) {
        if (startsWith(bytes, new int[] {0xFF, 0xD8, 0xFF})) {
            return DetectedType.JPEG;
        }
        if (startsWith(bytes, new int[] {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
            return DetectedType.PNG;
        }
        if (bytes.length >= 12
                && ascii(bytes, 0, 4).equals("RIFF")
                && ascii(bytes, 8, 4).equals("WEBP")) {
            return DetectedType.WEBP;
        }
        return DetectedType.UNKNOWN;
    }

    private boolean startsWith(byte[] bytes, int[] signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if ((bytes[index] & 0xFF) != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }

    public enum UploadPurpose {
        AVATAR(Set.of(DetectedType.JPEG, DetectedType.PNG, DetectedType.WEBP)),
        REVIEW_PHOTO(Set.of(DetectedType.JPEG, DetectedType.PNG, DetectedType.WEBP)),
        VOLUNTEER_CERTIFICATE(Set.of(DetectedType.JPEG, DetectedType.PNG));

        private final Set<DetectedType> allowedTypes;

        UploadPurpose(Set<DetectedType> allowedTypes) {
            this.allowedTypes = allowedTypes;
        }
    }

    private enum DetectedType {
        JPEG("image/jpeg", ".jpg"),
        PNG("image/png", ".png"),
        WEBP("image/webp", ".webp"),
        UNKNOWN("application/octet-stream", "");

        private final String contentType;
        private final String extension;

        DetectedType(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }
    }

    public record VerifiedUpload(byte[] bytes, String contentType, String extension) {
    }
}
