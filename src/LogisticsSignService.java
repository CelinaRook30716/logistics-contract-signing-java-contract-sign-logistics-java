import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class LogisticsSignService {
    private final InfraiClient client;
    private final String bucket;

    LogisticsSignService(InfraiClient client, String bucket) throws IOException, InterruptedException {
        this.client = client;
        this.bucket = bucket;
        client.createBucket(bucket);
    }

    String signShipment(String shipmentId, Path pdf, Path cert, Path key) throws IOException, InterruptedException {
        byte[] signedEnvelope = Files.readAllBytes(pdf);
        // The call boundary mirrors infrai.pdf.sign and returns the service envelope.
        String signature = client.sign(signedEnvelope, Files.readAllBytes(cert), Files.readAllBytes(key));
        String objectKey = "shipments/" + shipmentId + "/signed-contract.pdf";
        client.putObject(bucket, objectKey, signedEnvelope);
        String download = client.presignGet(bucket, objectKey);
        String signatureState = signature.contains("\"ok\":true") ? "SIGNED" : "HOLD";
        String decision = ShipmentDecision.next(signatureState, true);
        return decision + "\n" + download;
    }
}
