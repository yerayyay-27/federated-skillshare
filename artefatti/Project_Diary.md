Project Diary:

First sprint: Yeray González Menéndez: "Product Owner"; Jorge Vigil Bravo: "Scrum Master"

06/06/2026 — Defined each member's tasks and started the project design.

Authentication (login): end-to-end RPC slice for "Access & Registration". Added the shared User object and AuthService/AuthServiceAsync, the server-side AuthServiceImpl (authServlet at /app/auth), the LoginGui screen, and set it as the entry point. Fixed an HTTP 503 caused by extending the legacy javax RemoteServiceServlet instead of the Jakarta variant — now a project-wide convention. Codebase standardized to English.

07/06/2026 — Registration & navigation.

Added RegisterGui (username/email/password with validation) wired to LoginGui, completing the login/registration flow. Added HomeGui as the post-login hub that receives the logged-in User and handles sign-out, establishing the navigation pattern for the authenticated area.

Announcement backend: serializable Announcement object, plus AnnouncementRepository (MapDB persistence) and AnnouncementManager (validation/business rules kept separate from persistence). Supports create, find by id, active listing, deactivation, and case-insensitive search; duplicate ids rejected atomically via putIfAbsent. Added repository/manager unit tests.

Announcement RPC layer: AnnouncementService/Async (shared) and AnnouncementServiceImpl (announcementsServlet at /app/announcements), delegating to AnnouncementManager. Added RPC tests — full suite at 24 passing.

Persistence (MapDB): migrated users from in-memory storage to MapDB via DatabaseCore (open collection → modify → commit), so users survive restarts. Passwords stay server-side, never sent to the client; a test account is seeded on first run. Verified persistence across a clean restart.

Profile management: extended User with bio and skill tags; introduced UserRepository as the single source of truth for user data. Added ProfileService/Async and ProfileServiceImpl (profileServlet at /app/profile) for updating bio/tags. Built ProfileGui, reachable from HomeGui. Backwards-compatible migration (only new collections added).

Refactor & tests: fixed a critical testability defect — UserRepository opened its MapDB collections in static fields, ignoring DatabaseCore.enableTestMode(). Reworked it to open them in the constructor and switched AuthServiceImpl/ProfileServiceImpl to constructor injection. Added unit tests for both services; the whole suite now runs against the in-memory database.

Exchange requests (Richiesta di Scambio): first part of Interaction & Coordination. Added the shared ExchangeRequest object (PENDING/ACCEPTED/REJECTED) and ExchangeService/Async; on the server, ExchangeRequestRepository + ExchangeRequestManager (auto-generated UUID ids, validates the target announcement, blocks self-requests and duplicate pending requests) exposed via ExchangeServiceImpl (exchangeServlet at /app/exchanges). Built ExchangeRequestsGui (received requests with Accept/Reject, sent requests with status) and a "Request exchange" action in the marketplace. Added manager unit tests.

Second sprint: Yeray González Menéndez: "Scrum Master"; Jorge Vigil Bravo: "Product Owner"

08/06/2026

Internal chat — completed the Interaction & Coordination block. The chat is tied to an exchange request and only opens once it's ACCEPTED, acting as the coordination channel between the two parties. Added the shared ChatMessage object and ChatService/Async; on the server, ChatRepository (stores each conversation in MapDB keyed by exchange request id) and ChatManager, which enforces that messages can only be sent/read if the request exists, is accepted, and the user is one of the two participants. Exposed via ChatServiceImpl (chatServlet at /app/chat). Built ChatGui (message list with sender and time, send field, and a Refresh button instead of real-time push), reachable from an "Open chat" button on accepted requests in ExchangeRequestsGui. Added ChatManager unit tests covering send/read, both participants writing, ordering, and rejection of non-participants, blank messages, unknown exchanges, and not-yet-accepted chats.

Reviews & reputation — completed the final spec block (Reputation & Feedback). A review is tied to an accepted exchange: only the two participants can review, each once per exchange, with a 1–5 rating and an optional comment. Added the shared Review and UserReputation objects and ReviewService/Async; on the server, ReviewRepository (MapDB) and ReviewManager, which validates the exchange is accepted and the reviewer is a participant, resolves the target as the other participant, blocks out-of-range ratings and double reviews, and computes each user's average rating and review list. Exposed via ReviewServiceImpl (reviewServlet at /app/reviews). On the client, built ReviewGui (star rating + comment, reachable from "Leave a review" on accepted requests) and ReputationGui (average rating, review count, and received reviews, reachable from "My reputation" on the home screen). Added ReviewManager unit tests covering targeting, rating range, non-participant and non-accepted rejection, double-review prevention, mutual reviews, average computation, and the canReview guard.

Profile photo: extended the user profile with persistent photo upload and preview support. Added browser-side image reading through the FileReader API, server-side image format and size validation, and MapDB persistence using a dedicated photos collection. Updated the profile RPC and GUI so users can upload and immediately preview their photo, with additional unit tests covering valid uploads, invalid formats, and unknown users.

09/06/2026 
 
Review flow — replaced the generic 500 on a second review with a clear explanation. Swapped the boolean canReview for getReviewBlockReason, which returns a human-readable reason (already reviewed, not accepted, not a participant, exchange gone) or null when allowed. ReviewGui now checks this on load and, if blocked, shows the reason and hides the form so the user can't attempt a disallowed review. Updated ReviewService/Async and ReviewServiceImpl, and adjusted the ReviewManager tests.

Third sprint: Yeray González Menéndez: "Product Owner"; Jorge Vigil Bravo: "Scrum Master"

10/06/2026

Demo database setup — prepared the persistent MapDB database for delivery testing. Temporarily removed the local database file from .gitignore.

Docker-based demo environment — ran the application through Docker Desktop and verified that the web interface was reachable at `localhost:8080`. Populated the persistent MapDB database through the application with two demo users and representative interactions involving profile setup, announcements, exchange requests, chat, and reviews. Reopened the application and confirmed that the stored data remained available across executions. The resulting database was versioned to provide a realistic dataset for future validation and the final delivery.

Artefatti - Produced the remaining project artifacts. We wrote the Developer Manual, an overview of the architecture and of how each feature was implemented, the design patterns adopted with their benefits, the User Manual, and the root README. We also created the use case diagram modelling the two actors (Visitor and Registered User) and their use cases. 

Distributed Systems Project

18/06/2026

Federated marketplace — announcement replication across instances.
We turned the centralized Skillshare into a federated peer-to-peer system where multiple independent instances cooperate. Each instance is an autonomous Skillshare server with its own users and its own MapDB database, configured from the outside (INSTANCE_ID, INSTANCE_URL, PEER_URLS, DATA_DIR) so the same code runs as any instance. We set up a two-instance deployment with Docker Compose (instance A on 8080, instance B on 8081), where instances reach each other over the internal Docker network by service name.
Instances cooperate by exchanging domain events (not raw database rows) over plain HTTP + JSON — server-to-server communication, distinct from the GWT RPC used between a browser and its own instance. We implemented:

FederationEvent (shared): the event exchanged between instances, with types AnnouncementCreated, AnnouncementUpdated and AnnouncementDeleted, plus the originating instance.
FederationClient (server): broadcasts an event to every known peer; if a peer is unreachable it logs and continues, so the local operation still succeeds (availability over immediate consistency).
FederationInboxServlet (server): a plain Jakarta servlet at /federation/inbox that receives events from peers and applies them to the local database.
Hooks in AnnouncementManager so that creating, updating or deleting an announcement broadcasts the corresponding event to peers.

As a result, an announcement created, edited or deleted on one instance is replicated to the others, so the marketplace spans the whole federation.
Key design decisions worth highlighting:

Events, not rows — instances notify each other of what happened, which keeps them loosely coupled and easy to extend to other event types.
No re-broadcast loop — the inbox applies received events directly through the repository, never through the manager's broadcast path, so receiving an event doesn't re-propagate it and cause an infinite loop between instances.
Idempotent delivery — creates ignore duplicate ids, deletes of an absent announcement are no-ops, and an update of an unknown announcement is stored; receiving the same event twice is harmless and replicas still converge.
Local-first / availability — a local action is never rolled back because a peer is offline; this is an explicit CAP trade-off (availability and eventual consistency over immediate consistency).

Known limitation (future work): if an instance is offline when an event is broadcast, it misses that event and its replica diverges until a later reconciliation mechanism (a pending-event queue or sync-on-reconnect) is added. We deliberately left this as future work.

Federated identity (user@instance). We extended the federation so users and announcements know which instance they belong to. Added an instance field to User (set from the local INSTANCE_ID when a user is loaded) and an originInstance field to Announcement (stamped with the local instance when created, preserved on update, and carried with the announcement when it federates). The marketplace now shows each owner as user@instance, making identity unambiguous across the federation. We also closed a federation-introduced security gap: editing/deleting an announcement is now allowed only when its origin instance matches the user's own instance, so a local "alice@inst-a" can no longer modify a remote "alice@inst-b" that happens to share the username. This user@instance model (inspired by ActivityPub/Mastodon) is the foundation for directing cross-instance actions such as exchange requests and chat to the correct instance.

Federation outbox and offline-instance handling.

We improved the announcement federation mechanism by adding a persistent outbox for outgoing federation events. Previously, if a peer instance was offline when an announcement event was broadcast, the local instance only logged the error and the remote instance permanently missed the update. Now, each outgoing delivery attempt is stored as an `OutgoingFederationEvent` with a target peer, delivery status, attempt count, timestamps and possible error information. The delivery status can be `PENDING`, `DELIVERED` or `FAILED`, and events are persisted in MapDB through a dedicated `FederationOutboxRepository`.

The `FederationClient` now creates one outbox entry per peer when broadcasting announcement events. Successful deliveries are marked as `DELIVERED`, while failed deliveries are marked as `FAILED` instead of being lost. This supports the distributed-systems choice of prioritizing availability over immediate consistency: the local operation still succeeds even if another instance is offline, and the failed event can be retried later to reach eventual consistency.

We tested this in the local Docker federation demo. With `instance-b` (`localhost:8081`) stopped, we created an announcement on `instance-a` (`localhost:8080`). The announcement was created locally, and the logs showed that delivery to `http://instance-b:8080` failed, confirming that the system remained available and recorded the failed federation attempt. We also verified that, when both instances are online, announcement replication works correctly between `localhost:8080` and `localhost:8081`.

Known limitation: retry is not automatic yet. The next step is to add a manual retry endpoint, for example `POST /federation/retry`, to trigger the existing retry mechanism after an offline instance comes back online.

Federation retry endpoint — manual recovery after offline peers.

We completed the offline-instance recovery flow by exposing a small manual retry endpoint for the federation outbox. After adding the persistent outbox, failed federation deliveries were no longer lost, but they still needed a way to be retried from the running application. To solve this, we added a plain Jakarta servlet, FederationRetryServlet, mapped to POST /federation/retry.

When this endpoint is called on an instance, it invokes the existing FederationClient.retryPending() method. This reloads pending or failed outgoing federation events from the MapDB-backed outbox and tries to deliver them again to their target peers. The endpoint returns a JSON response containing the operation status and the number of events successfully delivered, for example {"status":"ok","delivered":1}. Unsupported GET requests are rejected with HTTP 405.

This completes a simple but effective availability and eventual-consistency scenario for the local multi-instance demo. If instance-b (localhost:8081) is offline and an announcement is created on instance-a (localhost:8080), the local operation still succeeds and the failed federation event is stored in the outbox. When instance-b comes back online, we can call POST http://localhost:8080/federation/retry to resend the pending event. After refreshing localhost:8081, the missed announcement appears in the remote marketplace.

We also added unit tests for the retry servlet using a fake FederationClient, so the tests verify the servlet behavior without making real network calls. The test suite passed successfully after the change.

This implementation strengthens the distributed-systems design of the project: the system prioritizes availability over immediate consistency, stores failed events instead of losing them, and provides a controlled mechanism to make replicas converge later. Automatic background retry is still left as future work, but the current endpoint is enough to demonstrate offline recovery during the final discussion.

Automatic federation retry — background recovery and eventual consistency.

We improved the federation outbox by adding automatic background retry for failed and pending federation events. Previously, failed deliveries were persisted in the outbox and could be retried manually through `POST /federation/retry`, but this still required a manual action after an offline instance came back online.

To make the local multi-instance demo more realistic, we added a `FederationRetryScheduler`. Each application instance starts a single background scheduled task when the web application starts. By default, the scheduler runs every 10 seconds and calls the existing `FederationClient.retryPending()` method. This means that pending or failed outbox events are periodically retried without requiring the user to run a manual `curl` command.

The scheduler is integrated through a `FederationRetryLifecycleListener`, registered in `web.xml`. The listener starts the scheduler when the web application is initialized and stops it cleanly when the application is shut down. The retry behavior can be configured through `FEDERATION_RETRY_ENABLED` and `FEDERATION_RETRY_INTERVAL_SECONDS`, with safe defaults enabled and set to 10 seconds. Exceptions during retry are logged and contained so that future retry cycles can continue.

This strengthens the distributed behavior of the system. If `instance-b` (`localhost:8081`) is offline and an announcement is created on `instance-a` (`localhost:8080`), the local operation still succeeds and the failed delivery is stored in the outbox. When `instance-b` comes back online, `instance-a` automatically retries the stored event in the background, and the missed announcement eventually appears in the remote marketplace without manual intervention.

We also kept the manual `POST /federation/retry` endpoint for testing and debugging. Unit tests were added for the scheduler and lifecycle listener, avoiding real network calls. The full Maven test suite passed successfully with 123 tests and no failures.

This completes a clear availability and eventual-consistency scenario for the final discussion: the system remains available during peer failures and automatically converges once communication is restored.

Federated exchange requests — cross-instance request and status propagation.

We extended federation from the shared announcement marketplace to exchange requests. `ExchangeRequest` now records the requester and owner instance through `fromInstance` and `toInstance`, preserving unambiguous federated identities. New `ExchangeRequested`, `ExchangeAccepted` and `ExchangeRejected` domain events were added to `FederationEvent`.

When a user requests an announcement owned by another instance, the requester’s instance stores a local copy and sends the request through the existing federation client. The owner’s instance stores the incoming request under Received. Accepting or rejecting it sends the updated status back so the requester’s local replica eventually converges. Events reuse the persistent outbox and automatic retry scheduler, so temporary peer failures do not block the local operation and deliveries can complete when the peer returns.

Unit tests were added for local-versus-remote broadcasting, instance metadata, acceptance propagation and replica filtering. This change federates the exchange request and response workflow only; cross-instance chat and reviews remain future work and the complete two-instance Docker flow still requires manual verification.

Federated exchange request hardening — identity-safe requests and status propagation.

We hardened the federated exchange request workflow to make it safer and more consistent with the `user@instance` identity model. The self-request validation is now instance-aware: a user cannot request their own local announcement, but two users with the same local username on different instances are treated as different federated users. For example, `alice@inst-a` can request an announcement owned by `alice@inst-b`, while `alice@inst-a` is still prevented from requesting her own `alice@inst-a` announcement.

We also improved the exchange request interface so that received and sent requests display federated handles such as `alice@inst-a` and `bob@inst-b` instead of only local usernames. This makes cross-instance requests clearer and avoids confusion when different instances contain users with the same username.

Additional tests were added for same-username scenarios, rejection propagation, and federation inbox handling. The inbox tests cover storing exchange requests targeted at the local instance, ignoring requests for other instances, applying accepted and rejected statuses, and handling duplicate request delivery. The full Maven test suite passed successfully with 135 tests and no failures.

This strengthens the federated exchange workflow: users can now request announcements across instances, owners can accept or reject them, and the requester’s local replica converges to the correct status through the federation event system. Cross-instance chat and reviews remain future work.

Federation inbox idempotency — safe repeated delivery.

We improved the federation layer by adding stable event identifiers and a persistent inbox ledger for processed federation events. Since the system now uses a persistent outbox and automatic retry, federation messages may be delivered more than once. This follows an at-least-once delivery model, which is common in distributed systems, but it requires idempotency to avoid applying the same event multiple times.

To solve this, each `FederationEvent` now carries a unique `eventId`. This identifier is included in the JSON payload and preserved across failed attempts, manual retries and automatic retries. On the receiving side, we added a MapDB-backed `FederationInboxRepository` using the `processedFederationEvents` collection. Each processed event is stored with its event id, event type, origin instance and processing timestamp.

The inbox now checks the event id before applying an incoming federation event. If the event was already processed, the request returns successfully but the domain operation is not applied again. If the event is new, it is applied normally and then marked as processed. Importantly, failed applications are not marked as processed, so they can still be retried later.

This makes repeated delivery safe for announcement and exchange events. Duplicate announcement events no longer reapply changes unnecessarily, and repeated exchange request or status events do not create duplicate effects. Unit tests were added for event id generation, JSON round-trip preservation, processed-event persistence, duplicate handling, and failure cases. The full Maven test suite passed successfully with 144 tests and no failures.

This strengthens the reliability of the federated architecture: the project now combines availability, persistent outbox retry, automatic recovery and inbox idempotency to support eventual consistency across the local multi-instance demo.

Federated chat — cross-instance conversations for accepted exchanges.
We extended federation from announcements and exchange requests to the internal chat, so that the two participants of an accepted exchange can talk even when they live on different instances. Each ChatMessage now records the sender's home instance through senderInstance, preserving the user@instance federated identity, and a new ChatMessageCreated domain event was added to FederationEvent.
When a participant sends a message, the chat manager validates access in an instance-aware way: a user is recognised as a participant only when both the username and the home instance match, so a remote bob@inst-b is never mistaken for a local bob. The message is stored locally first (availability over consistency) and, if the other participant is on a different instance, sent to that instance through the existing federation client. The receiving inbox appends the message to its replica of the conversation and never re-broadcasts it, so both instances converge to the full conversation. Because chat storage only appends and has no natural deduplication key, repeated delivery is made safe by the existing eventId inbox ledger rather than by an idempotent write. The exchange request interface and chat view were updated to show federated handles such as alice@inst-a. Unit tests were added for local-versus-remote broadcasting, sender-instance stamping and the instance-aware participant check. Cross-instance reviews remain future work.
Logical clocks — deterministic message ordering with Lamport timestamps.
Federated chat introduced an ordering problem: each instance stored messages in arrival order, so two replicas of the same conversation could display them differently. We solved this with a Lamport logical clock per instance, persisted in MapDB so it survives restarts. Sending a message is a local event that advances the clock (tick), and receiving a message advances it to max(local, received) + 1 (update), so causality is respected: a reply always carries a strictly greater timestamp than the message it answers.
Each ChatMessage now carries its Lamport timestamp, and the chat manager returns messages ordered by the tuple (lamportTimestamp, senderInstance, timestamp). This produces a deterministic total order: messages that are causally related are ordered by logical time, while genuinely concurrent messages (equal timestamps) are ordered the same way on every instance by breaking ties on the instance id. The visible result is that both instances render an identical conversation. Unit tests cover the clock's tick/update/persistence semantics, ordering by logical time instead of insertion order, and the deterministic tie-break between concurrent messages. The clock is currently scoped to chat; the natural extensions are stamping every federation event and using the same logical time to resolve concurrent announcement updates with a deterministic last-writer-wins rule.

Federated reviews — cross-instance reputation with identity-safe participants.

We extended reviews and reputation to accepted exchanges whose participants belong to different instances. Each `Review` now records both the reviewer and reviewed user as `username@instance`, so users with the same local username remain distinct. Review validation resolves participants using both identity components, preserves the existing one-review-per-participant-and-exchange rule, and still treats older reviews without instance fields as local data.

Reviews are stored locally first and, when the reviewed user is remote, propagated through a new `ReviewCreated` federation event. This reuses the persistent outbox, manual and automatic retry, stable event ids, and processed-event inbox ledger. The reviewed user's instance validates the event origin, target instance, accepted exchange and participant identities before storing it, while duplicate delivery remains harmless. Federated reviews received on the user's home instance are included in the existing local reputation average and review list, and the review screens display federated handles without redesigning the UI.

Unit tests cover local compatibility, same-name users on different instances, self-review and duplicate prevention, event broadcasting, reputation filtering, incoming-event validation and inbox idempotency. Cross-instance reputation remains home-instance based rather than globally queried, and federation messages are still unauthenticated.

Login error feedback — readable reasons instead of a generic failure.
We reworked the login flow so that a failed sign-in shows the user a clear explanation rather than an opaque server error. Previously AuthService.login threw an IllegalArgumentException on bad credentials; reaching the browser through GWT-RPC, this surfaced as a generic failure with no usable message. Following the same pattern already used by reviews (getReviewBlockReason), login now returns a new serializable LoginResult that is either a success carrying the authenticated User or a failure carrying a human-readable reason, so the service no longer throws on the expected "wrong credentials" path. The server distinguishes the two failure cases explicitly, returning "No account found for this email." when the email is unknown and "Incorrect password." when the password does not match, plus a prompt when either field is empty. The login screen inspects the result and, on failure, displays the reason in the existing error label exactly as the review screen does, while the RPC failure callback is now reserved for genuine transport or server faults. The authentication service tests were updated to the new contract, asserting the specific reason for each failure instead of expecting a thrown exception.