package goodroad.validation;

import goodroad.api.ApiErrors.ApiException;
import org.springframework.http.HttpStatus;

public final class GeoUtils {
    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    public static Coordinates requireCoordinates(Double latitude, Double longitude, String code) {
        if (latitude == null || longitude == null
                || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new ApiException(HttpStatus.BAD_REQUEST, code, "Координаты должны содержать допустимые широту и долготу");
        }
        return new Coordinates(latitude, longitude);
    }

    public static Coordinates parseLatLon(String value, String fieldName) {
        if (value == null) {
            throw invalidPoint(fieldName);
        }
        String[] parts = value.split(",", -1);
        if (parts.length != 2) {
            throw invalidPoint(fieldName);
        }
        try {
            return requireCoordinates(
                    Double.parseDouble(parts[0].trim()),
                    Double.parseDouble(parts[1].trim()),
                    "ROUTE_POINT_INVALID"
            );
        } catch (NumberFormatException e) {
            throw invalidPoint(fieldName);
        }
    }

    public static double distanceKm(double firstLat, double firstLon, double secondLat, double secondLon) {
        double latitudeDelta = Math.toRadians(secondLat - firstLat);
        double longitudeDelta = Math.toRadians(secondLon - firstLon);
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(firstLat)) * Math.cos(Math.toRadians(secondLat))
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static ApiException invalidPoint(String fieldName) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "ROUTE_POINT_INVALID",
                "Поле " + fieldName + " должно иметь формат latitude,longitude"
        );
    }

    public record Coordinates(double latitude, double longitude) {
        public String asLatLon() {
            return latitude + "," + longitude;
        }
    }
}
