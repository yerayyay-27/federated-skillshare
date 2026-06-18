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
