package goodroad.validation;

import goodroad.api.ApiErrors.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrustedUrlServiceTest {
    private final TrustedUrlService service = new TrustedUrlService("goodroad-bucket");

    @Test
    void acceptsDobroHttpsProfile() {
        assertEquals(
                "https://dobro.ru/volunteer/123",
                service.requireDobroProfileUrl("https://dobro.ru/volunteer/123")
        );
    }

    @Test
    void rejectsLookalikeDobroDomain() {
        assertThrows(ApiException.class, () -> service.requireDobroProfileUrl(
                "https://dobro.ru.evil.example/payload.exe"
        ));
        assertThrows(ApiException.class, () -> service.requireDobroProfileUrl(
                "https://attacker@dobro.ru/volunteer/123"
        ));
    }

    @Test
    void acceptsOnlyCurrentUsersUploadedCertificate() {
        String expected = "https://storage.yandexcloud.net/goodroad-bucket/volunteer-certificates/10/cert.jpg";
        assertEquals(expected, service.requireOwnedStorageUrl(
                expected, "volunteer-certificates", 10L, "CERTIFICATE_URL_INVALID"
        ));

        assertThrows(ApiException.class, () -> service.requireOwnedStorageUrl(
                "https://storage.yandexcloud.net/goodroad-bucket/volunteer-certificates/11/malware.jpg",
                "volunteer-certificates",
                10L,
                "CERTIFICATE_URL_INVALID"
        ));
        assertThrows(ApiException.class, () -> service.requireOwnedStorageUrl(
                "https://storage.yandexcloud.net/goodroad-bucket/volunteer-certificates/10/%2e%2e/reviews/file.jpg",
                "volunteer-certificates",
                10L,
                "CERTIFICATE_URL_INVALID"
        ));
    }
}
