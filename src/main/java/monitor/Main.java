package monitor;

import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) throws Exception {

        CentralMonitoringService central = new CentralMonitoringService(
                Map.of(SensorType.TEMPERATURE, 35.0, SensorType.HUMIDITY, 50.0),
                System.out::println);

        WarehouseService warehouse = new WarehouseService(Map.of(SensorType.TEMPERATURE, 3344, SensorType.HUMIDITY, 3355));
        warehouse.subscribe(central);
        warehouse.start();


        Runtime.getRuntime().addShutdownHook(new Thread(warehouse::close));

        new CountDownLatch(1).await();
    }
}