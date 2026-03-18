package goodroad.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

public final class Crypto {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?\\d+$");

    private Crypto() {
    }

    public static String normPhone(String phone) {
        if (phone == null) {
            return "";
        }

        String normalized = phone.trim();
        if (normalized.isEmpty() || !PHONE_PATTERN.matcher(normalized).matches()) {
            return "";
        }

        return normalized.charAt(0) == '+' ? normalized.substring(1) : normalized;
    }

    public static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(b.length * 2);
            for (byte x : b) {
                sb.append(String.format("%02x", x));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}