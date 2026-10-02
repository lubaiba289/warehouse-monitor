package monitor;

import java.time.Instant;

public final class MeasurementParser {
    private MeasurementParser() {

    }

    public static Measurement parse(SensorType sensorType,
                                    String payload) {
        String sensorId = null;
        Double value = null;

        for (String part : payload.trim().split(";")) {
            String[] kv = part.split("=");
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String val = kv[1].trim();
            switch (key) {
                case "sensor_id" -> sensorId = val;
                case "value" -> {
                    try {
                        value = Double.parseDouble(val);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid value: " + val);
                    }
                }
                default -> { /* ignore unknown keys */ }
            }
        }
        if (sensorId == null || sensorId.isEmpty() || value == null) {
            throw new IllegalArgumentException("Malformed payload: " + payload);
        }
        return new Measurement(sensorId, sensorType, value, Instant.now());

    }
}
