package goodroad.validation;

import goodroad.api.ApiErrors.ApiException;
import org.springframework.http.HttpStatus;

import java.util.regex.Pattern;

public final class InputRules {

    private static final Pattern CYRILLIC_TEXT = Pattern.compile("^(?=.*\\p{IsCyrillic})[\\p{IsCyrillic} -]+$");
    private static final Pattern DIGITS = Pattern.compile("^\\d+$");
    private static final Pattern ADDRESS_TEXT = Pattern.compile("^(?=.*[\\p{L}\\d])[\\p{L}\\d .,/()№#\\-–—]+$");

    private InputRules() {
    }

    public static String requireCyrillicText(String value, String code, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null || !CYRILLIC_TEXT.matcher(normalized).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, code, fieldName + " is invalid");
        }
        return normalized;
    }

    public static String requireDigits(String value, String code, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null || !DIGITS.matcher(normalized).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, code, fieldName + " is invalid");
        }
        return normalized;
    }

    public static String requireAddressText(String value, String code, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null || !ADDRESS_TEXT.matcher(normalized).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, code, fieldName + " is invalid");
        }
        return normalized;
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        return normalized;
    }
}