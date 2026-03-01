package GoodRoad.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class Crypto {

    private Crypto() {
    }

    public static String normPhone(String phone) { // выкидываем все кроме цифр
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[^0-9]", "");
    }

    public static String sha256Hex(String s) { // возвращаем ex-троку
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