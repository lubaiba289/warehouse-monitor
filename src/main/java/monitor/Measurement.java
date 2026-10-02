package monitor;

import java.time.Instant;

public record Measurement (String sensorId,
                           SensorType sensorType,
                           Double value,
                           Instant recordedTime) {

}

