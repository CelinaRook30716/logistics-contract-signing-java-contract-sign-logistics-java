final class ShipmentDecision {
    static String next(String signatureState, boolean proofStored) {
        return "SIGNED".equals(signatureState) && proofStored ? "READY_FOR_DISPATCH" : "HOLD";
    }
}
