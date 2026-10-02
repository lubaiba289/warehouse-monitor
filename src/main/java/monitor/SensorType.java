package monitor;

public enum SensorType {
    TEMPERATURE("°C", 35.0),
    HUMIDITY("%", 50.0);

    public final String unit;
    public final double defaultThreshold;

    SensorType(String unit, double defaultThreshold) {
        this.unit = unit;
        this.defaultThreshold = defaultThreshold;
    }

}
