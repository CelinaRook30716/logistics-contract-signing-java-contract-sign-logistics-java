import java.nio.file.Path;

public final class ContractDemo {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("usage: ContractDemo contract.pdf cert.pem key.pem");
        String bucket = System.getenv().getOrDefault("INFRAI_BUCKET", "logistics-contracts");
        LogisticsSignService service = new LogisticsSignService(new InfraiClient(System.getenv("INFRAI_API_KEY")), bucket);
        System.out.println(service.signShipment("shipment-1001", Path.of(args[0]), Path.of(args[1]), Path.of(args[2])));
    }
}
