package goodroad.validation;

import goodroad.api.ApiErrors.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

@Service
public class TrustedUrlService {
    private static final String STORAGE_HOST = "storage.yandexcloud.net";
    private static final Set<String> DOBRO_HOSTS = Set.of("dobro.ru", "www.dobro.ru");

    private final String storageBucket;

    public TrustedUrlService(@Value("${yandex.storage.bucket}") String storageBucket) {
        this.storageBucket = storageBucket;
    }

    public String requireDobroProfileUrl(String rawUrl) {
        URI uri = parseHttpsUrl(rawUrl, "DOBRO_URL_INVALID", "Укажите корректную HTTPS-ссылку на профиль dobro.ru");
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (!DOBRO_HOSTS.contains(host) || uri.getPort() != -1 || uri.getUserInfo() != null) {
            throw invalid("DOBRO_URL_INVALID", "Ссылка должна вести на домен dobro.ru");
        }
        return uri.normalize().toString();
    }

    public String requireOwnedStorageUrl(String rawUrl, String directory, Long userId, String code) {
        URI uri = parseHttpsUrl(rawUrl, code, "Ссылка на файл имеет неверный формат");
        String expectedPrefix = "/" + storageBucket + "/" + directory + "/" + userId + "/";
        String rawPath = uri.getRawPath() == null ? "" : uri.getRawPath().toLowerCase(Locale.ROOT);
        boolean containsEncodedSeparator = rawPath.contains("%2f") || rawPath.contains("%5c") || rawPath.contains("%2e");
        if (!STORAGE_HOST.equalsIgnoreCase(uri.getHost())
                || uri.getPort() != -1
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null
                || containsEncodedSeparator
                || uri.normalize().getPath() == null
                || !uri.normalize().getPath().startsWith(expectedPrefix)
                || uri.normalize().getPath().equals(expectedPrefix)) {
            throw invalid(code, "Разрешены только файлы, загруженные текущим пользователем через GoodRoad");
        }
        return uri.normalize().toString();
    }

    private URI parseHttpsUrl(String rawUrl, String code, String message) {
        String value = InputRules.trimToNull(rawUrl);
        if (value == null || value.length() > 512) {
            throw invalid(code, message);
        }
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw invalid(code, message);
            }
            return uri;
        } catch (URISyntaxException e) {
            throw invalid(code, message);
        }
    }

    private ApiException invalid(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }
}
