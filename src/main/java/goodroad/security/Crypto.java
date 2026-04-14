package goodroad.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class Crypto {

    private Crypto() {
    }

    public static String normPhone(String phone) {
        if (phone == null) {
            return "";
        }

        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() != 11) {
            return "";
        }

        char first = digits.charAt(0);
        if (first != '7' && first != '8') {
            return "";
        }

        if (first == '8') {
            return "7" + digits.substring(1);
        }

        return digits;
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