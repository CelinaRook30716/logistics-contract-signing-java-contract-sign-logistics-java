public final class ShipmentDecisionTest {
    public static void main(String[] args) {
        String ready = ShipmentDecision.next("SIGNED", true);
        String hold = ShipmentDecision.next("SIGNED", false);
        if (!"READY_FOR_DISPATCH".equals(ready)) throw new AssertionError(ready);
        if (!"HOLD".equals(hold)) throw new AssertionError(hold);
        System.out.println("ShipmentDecisionTest passed: signed proof controls dispatch");
    }
}
