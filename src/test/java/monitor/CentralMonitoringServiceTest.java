package monitor;

import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CentralMonitoringServiceTest {

    @Test
    void alarmsOnlyAboveThreshold() {
        List<String> alarms = new ArrayList<>();
        var central = new CentralMonitoringService(Map.of(), alarms::add);
        central.onNext(MeasurementParser.parse(SensorType.TEMPERATURE, "sensor_id=t1; value=35"));
        central.onNext(MeasurementParser.parse(SensorType.TEMPERATURE, "sensor_id=t1; value=36"));
        central.onNext(MeasurementParser.parse(SensorType.HUMIDITY, "sensor_id=h1; value=50"));
        central.onNext(MeasurementParser.parse(SensorType.HUMIDITY, "sensor_id=h1; value=51"));
        assertEquals(4, alarms.size());
        assertTrue(alarms.get(1).contains("TEMPERATURE"));
        assertTrue(alarms.get(3).contains("HUMIDITY"));
    }

    @Test
    void endToEndOverUdp() throws Exception {
        BlockingQueue<String> alarms = new LinkedBlockingQueue<>();
        var central = new CentralMonitoringService(Map.of(), alarms::add);
        try (var warehouse = new WarehouseService(
                Map.of(SensorType.TEMPERATURE, 45344, SensorType.HUMIDITY, 45355))) {
            warehouse.subscribe(central);
            warehouse.start();

            send(45344, "sensor_id=t1; value=40");   // alarm
            send(45355, "sensor_id=h1; value=70");   // alarm

            String a1 = alarms.poll(3, TimeUnit.SECONDS);
            String a2 = alarms.poll(3, TimeUnit.SECONDS);
            assertNotNull(a1);
            assertNotNull(a2);
            assertTrue(a1.contains("t1") || a2.contains("t1"));
            assertTrue(a1.contains("h1") || a2.contains("h1"));
            assertNull(alarms.poll(300, TimeUnit.MILLISECONDS));
        }
    }

    private static void send(int port, String msg) {
        try (DatagramSocket s = new DatagramSocket()) {
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            s.send(new DatagramPacket(data, data.length, InetAddress.getLoopbackAddress(), port));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}