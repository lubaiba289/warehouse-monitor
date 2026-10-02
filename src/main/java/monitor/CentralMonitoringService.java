package monitor;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

public class CentralMonitoringService implements Flow.Subscriber<Measurement>{
    private final Map<SensorType, Double> thresholdMap = new EnumMap<>(SensorType.class);
    private final Consumer<String> alarm;
    private Flow.Subscription subscription;

    public CentralMonitoringService(Map<SensorType, Double> thresholds,
                                    Consumer<String> alarm) {
        this.alarm = alarm;
        for (SensorType t : SensorType.values()) {
            this.thresholdMap.put(t, thresholds.getOrDefault(t, t.defaultThreshold));
        }
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(Measurement measurement) {
        double limit = thresholdMap.get(measurement.sensorType());
        if (measurement.value() > limit) {
            alarm.accept(String.format(
                    "ALARM! sensor=%s type=%s value=%.1f%s exceeds threshold %.1f%s at %s",
                    measurement.sensorId(), measurement.sensorType(),
                    measurement.value(), measurement.sensorType().unit,
                    limit, measurement.sensorType().unit, measurement.recordedTime()));
        } else {
            alarm.accept("OK");
        }

        if (subscription != null) subscription.request(1);

    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println("Monitoring stream error: " + throwable);
    }

    @Override
    public void onComplete() {
        System.out.println("Monitoring completed");
    }
}
