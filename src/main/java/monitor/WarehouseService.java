package monitor;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class WarehouseService implements AutoCloseable{

    private final Map<SensorType, Integer> ports;
    private final List<DatagramSocket> sockets = new ArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    public volatile boolean running;
    private final SubmissionPublisher<Measurement> publisher = new SubmissionPublisher<>();



    public WarehouseService(Map<SensorType, Integer> sensorMap) {
        this.ports = sensorMap;
    }

    public void subscribe(Flow.Subscriber<Measurement> subscriber) {
        publisher.subscribe(subscriber);
    }

    public void start() throws java.net.SocketException {
        running = true;
        for (var entry : ports.entrySet()) {
            DatagramSocket socket = new DatagramSocket(entry.getValue());
            sockets.add(socket);
            executor.submit(() -> listen(socket, entry.getKey()));
            System.out.printf("Listening for %s on UDP %d%n",
                     entry.getKey(), entry.getValue());
        }
    }

    private void listen(DatagramSocket socket, SensorType type) {
        byte[] buf = new byte[1024];
        while (running && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String payload = new String(packet.getData(), 0, packet.getLength(),
                        StandardCharsets.UTF_8);
                publisher.submit(MeasurementParser.parse(type, payload));
            } catch (IllegalArgumentException e) {
                System.err.printf("Ignoring bad %s message: %s%n",
                        type, e.getMessage());
            } catch (java.io.IOException e) {
                if (running) System.err.println("Socket error: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        running = false;
        sockets.forEach(DatagramSocket::close);
        executor.shutdownNow();
        publisher.close();
    }
}
