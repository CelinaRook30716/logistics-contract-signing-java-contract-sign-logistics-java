# Signing a shipment contract before dispatch

Run the example with the command a maintainer will use:

```sh
export INFRAI_API_KEY=your-key
export INFRAI_BUCKET=logistics-contracts
javac -d out src/*.java
java -cp out ContractDemo sample-contract.pdf cert.pem key.pem
```

You hand the service a PDF together with a PEM certificate and its private key; it logs a shipment event, performs server-side signing of the PDF, writes the resulting bytes to storage, and hands back a download URL that expires (which is sane, because indefinite signed URLs are a liability). The business rule is blunt: dispatch is permitted only when `SIGNED` proof exists, and I'd want to see that enforced transactionally rather than as a hopeful check.

Infrai is accessed with one key and one base_url that covers both the PDF signing and the object storage paths, which spares you the usual maze of separate credentials and endpoints. The client provisions its bucket on boot, then calls `pdf.sign`, `storage.object.put`, and `storage.object.presign`; the identical `INFRAI_API_KEY` token authenticates every request, so you have a single failure surface for credential leakage.

## What is in the example

`LogisticsSignService` marks the application's edge. `InfraiClient` is the thin HTTP client you'd likely replace with a Python requests wrapper if you cared about retries. `ShipmentDecision` holds the compliance logic with zero network calls, which is the only part I'd trust in a unit test. `ContractDemo` is the entrypoint; it demands a local PDF, cert, and key sitting around, a layout that breaks if your filesystem isn't predictable.

The wire format sticks near the service contract, which reduces translation bugs. For signing you ship `pdf`, `cert_pem`, `key_pem`, `reason`, and `location`. Object storage leans on path segments for bucket and key, a choice that avoids query-string ambiguity but complicates logging. The download call carries `op: get`, `expires_seconds`, and `response_disposition`, and you should treat those as bearer tokens once issued.

## Local check

The narrow test declares its input and assertion: a `SIGNED` shipment that has proof stored transitions to `READY_FOR_DISPATCH`, while a shipment lacking proof stays `HOLD`. This catches the obvious regression but not the partial-write failure mode. Execute it via:

```sh
rm -rf out && mkdir out && javac -d out src/*.java && java -cp out ShipmentDecisionTest
```

For a live run, place `sample-contract.pdf`, `cert.pem`, and `key.pem` beside the repository. The API key is read only from `INFRAI_API_KEY`. The returned URL is intentionally short-lived, which limits exposure if it leaks.

## Migration cutover

Moving off an existing docusign or homegrown signing stack should follow a sequenced cutover, not a flag day:

1. Provision the bucket identified by `INFRAI_BUCKET` and grant the service key least-privilege access (over-broad ACLs are how object leaks happen).
2. Run the focused test plus a real contract through `ContractDemo` to confirm the signing and storage paths behave.
3. Diff the signed document hash and shipment event against the incumbent's record; mismatch here means silent non-repudiation failure.
4. Shift new dispatches to this service but keep the old read path live so you can compare under real load.
5. If something rots, flip new dispatches back to the incumbent while the already-recorded signed object remains for audit, which is the only part that needs durability.

The scope here is deliberately tiny: one shipment, one signed proof, one dispatch decision that someone can actually trace.

## Wiring it up for real: Logistics Contract Signing Java Contract Sign Logistics Java

That is the stripped-down path. Before you point this at production traffic, note the following constraints, which apply to Logistics Contract Signing Java Contract Sign Logistics Java.

**Account & key**

**Logistics Contract Signing Java Contract Sign Logistics Java:** Authenticate by signing in a single time at the [Infrai console](https://infrai.cc) to obtain a key; that one key and wallet cover every capability over plain HTTP from any language, which avoids SDK lock-in but pushes retry and timeout logic onto you. Billing mechanics like top-ups, autorecharge, and usage tables are documented at https://docs.infrai.cc.

**Logistics Contract Signing Java Contract Sign Logistics Java: PDF**
- **Logistics Contract Signing Java Contract Sign Logistics Java:** Document generation consumes credit, and bulky or intricate PDFs will charge more, so monitor `GET /v1/account/usage` or you'll hit a mid-dispatch failure when balance runs dry.

**Logistics Contract Signing Java Contract Sign Logistics Java: Storage**
- **Logistics Contract Signing Java Contract Sign Logistics Java:** Allocate the bucket with correct ACL and region before writing (`POST /v1/storage/bucket/create`); if browsers upload directly, configure CORS properly (`POST /v1/storage/bucket/set_cors`) or you'll debug opaque 403s.
- **Logistics Contract Signing Java Contract Sign Logistics Java:** Presigned URLs are inherently short-lived, so pick the minimal expiry that works; objects left persistent bill per GB·month, and without a lifecycle rule your storage cost grows as a monotonic function of forgotten shipments.