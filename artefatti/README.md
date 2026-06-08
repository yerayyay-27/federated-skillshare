Qui andranno inseriti tutti gli artefatti come: manuale sviluppatore, manualte utente, diaro del progetto, ecc...

Diario del progetto:
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

08/06/2026

Internal chat — completed the Interaction & Coordination block. The chat is tied to an exchange request and only opens once it's ACCEPTED, acting as the coordination channel between the two parties. Added the shared ChatMessage object and ChatService/Async; on the server, ChatRepository (stores each conversation in MapDB keyed by exchange request id) and ChatManager, which enforces that messages can only be sent/read if the request exists, is accepted, and the user is one of the two participants. Exposed via ChatServiceImpl (chatServlet at /app/chat). Built ChatGui (message list with sender and time, send field, and a Refresh button instead of real-time push), reachable from an "Open chat" button on accepted requests in ExchangeRequestsGui. Added ChatManager unit tests covering send/read, both participants writing, ordering, and rejection of non-participants, blank messages, unknown exchanges, and not-yet-accepted chats.

Reviews & reputation — completed the final spec block (Reputation & Feedback). A review is tied to an accepted exchange: only the two participants can review, each once per exchange, with a 1–5 rating and an optional comment. Added the shared Review and UserReputation objects and ReviewService/Async; on the server, ReviewRepository (MapDB) and ReviewManager, which validates the exchange is accepted and the reviewer is a participant, resolves the target as the other participant, blocks out-of-range ratings and double reviews, and computes each user's average rating and review list. Exposed via ReviewServiceImpl (reviewServlet at /app/reviews). On the client, built ReviewGui (star rating + comment, reachable from "Leave a review" on accepted requests) and ReputationGui (average rating, review count, and received reviews, reachable from "My reputation" on the home screen). Added ReviewManager unit tests covering targeting, rating range, non-participant and non-accepted rejection, double-review prevention, mutual reviews, average computation, and the canReview guard.