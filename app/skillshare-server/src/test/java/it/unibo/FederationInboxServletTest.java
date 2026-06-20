package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FederationInboxServletTest {

    private String localInstance;
    private String remoteInstance;
    private CountingAnnouncementRepository announcementRepository;
    private CountingExchangeRequestRepository exchangeRepository;
    private CountingReviewRepository reviewRepository;
    private FederationInboxRepository inboxRepository;
    private FederationInboxServlet servlet;

    @BeforeEach
    void setUp() {
        DatabaseCore.enableTestMode();
        localInstance = FederationConfig.get().getInstanceId();
        remoteInstance = localInstance + "-remote";
        announcementRepository = new CountingAnnouncementRepository();
        exchangeRepository = new CountingExchangeRequestRepository();
        reviewRepository = new CountingReviewRepository();
        inboxRepository = new FederationInboxRepository();
        servlet = new FederationInboxServlet(
                announcementRepository,
                exchangeRepository,
                new ChatRepository(),
                reviewRepository,
                inboxRepository);
    }

    @AfterEach
    void tearDown() {
        DatabaseCore.disableTestMode();
    }

    @Test
    void exchangeRequestedForThisInstanceIsStored() {
        ExchangeRequest request = remoteRequest("exchange-local-target", localInstance);
        FederationEvent event = FederationEvent.exchangeRequested(remoteInstance, request);

        assertTrue(servlet.applyEvent(event));

        assertEquals(request.getId(), exchangeRepository.findById(request.getId()).getId());
        assertTrue(inboxRepository.hasProcessed(event.getEventId()));
    }

    @Test
    void exchangeRequestedForAnotherInstanceIsIgnoredAndRecorded() {
        ExchangeRequest request = remoteRequest("exchange-other-target", "another-instance");
        FederationEvent event = FederationEvent.exchangeRequested(remoteInstance, request);

        assertTrue(servlet.applyEvent(event));

        assertNull(exchangeRepository.findById(request.getId()));
        assertTrue(inboxRepository.hasProcessed(event.getEventId()));
    }

    @Test
    void duplicateExchangeAcceptedUpdatesRequesterReplicaOnce() {
        ExchangeRequest request = requesterReplica("exchange-accepted");
        exchangeRepository.save(request);
        FederationEvent event = FederationEvent.exchangeAccepted(remoteInstance, request);

        assertTrue(servlet.applyEvent(event));
        assertFalse(servlet.applyEvent(event));

        assertEquals(ExchangeRequest.STATUS_ACCEPTED,
                exchangeRepository.findById(request.getId()).getStatus());
        assertEquals(1, exchangeRepository.getUpdateCalls());
    }

    @Test
    void duplicateExchangeRejectedUpdatesRequesterReplicaOnce() {
        ExchangeRequest request = requesterReplica("exchange-rejected");
        exchangeRepository.save(request);
        FederationEvent event = FederationEvent.exchangeRejected(remoteInstance, request);

        assertTrue(servlet.applyEvent(event));
        assertFalse(servlet.applyEvent(event));

        assertEquals(ExchangeRequest.STATUS_REJECTED,
                exchangeRepository.findById(request.getId()).getStatus());
        assertEquals(1, exchangeRepository.getUpdateCalls());
    }

    @Test
    void duplicateExchangeRequestDeliveryRunsSaveOnce() {
        ExchangeRequest request = remoteRequest("exchange-duplicate", localInstance);
        FederationEvent event = FederationEvent.exchangeRequested(remoteInstance, request);

        assertTrue(servlet.applyEvent(event));
        assertFalse(servlet.applyEvent(event));

        assertEquals(1, exchangeRepository.listAll().size());
        assertEquals(1, exchangeRepository.getSaveCalls());
    }

    @Test
    void duplicateAnnouncementCreatedRunsSaveOnce() {
        Announcement announcement = new Announcement(
                "announcement-duplicate", "alice", "Java", "Spanish",
                "Basics", "Evenings", true);
        announcement.setOriginInstance(remoteInstance);
        FederationEvent event = FederationEvent.announcementCreated(remoteInstance, announcement);

        assertTrue(servlet.applyEvent(event));
        assertFalse(servlet.applyEvent(event));

        assertEquals(1, announcementRepository.listAll().size());
        assertEquals(1, announcementRepository.getSaveCalls());
    }

    @Test
    void failedApplicationIsNotMarkedProcessed() {
        FederationInboxRepository failureLedger = new FederationInboxRepository();
        FederationInboxServlet failingServlet = new FederationInboxServlet(
                new FailingAnnouncementRepository(), exchangeRepository, failureLedger);
        Announcement announcement = new Announcement(
                "announcement-failure", "alice", "Java", "Spanish",
                "Basics", "Evenings", true);
        FederationEvent event = FederationEvent.announcementCreated(remoteInstance, announcement);

        assertThrows(IllegalStateException.class, () -> failingServlet.applyEvent(event));

        assertFalse(failureLedger.hasProcessed(event.getEventId()));
    }

    @Test
    void eventWithoutIdIsRejectedAndNotRecorded() {
        FederationEvent event = FederationEvent.announcementDeleted(remoteInstance, "announcement-1");
        event.setEventId(null);

        assertThrows(IllegalArgumentException.class, () -> servlet.applyEvent(event));

        assertFalse(inboxRepository.hasProcessed(null));
    }

    @Test
    void validReviewForLocalUserIsStored() {
        ExchangeRequest exchange = acceptedReviewExchange("review-exchange", localInstance);
        exchangeRepository.save(exchange);
        Review review = remoteReview("review-1", exchange.getId(), localInstance);
        FederationEvent event = FederationEvent.reviewCreated(remoteInstance, review);

        assertTrue(servlet.applyEvent(event));

        assertEquals(1, reviewRepository.listAll().size());
        assertEquals("bob@" + localInstance, reviewRepository.listAll().get(0).getToHandle());
        assertTrue(inboxRepository.hasProcessed(event.getEventId()));
    }

    @Test
    void reviewForAnotherInstanceIsIgnoredAndRecorded() {
        Review review = remoteReview("review-other", "exchange-other", "another-instance");
        FederationEvent event = FederationEvent.reviewCreated(remoteInstance, review);

        assertTrue(servlet.applyEvent(event));

        assertTrue(reviewRepository.listAll().isEmpty());
        assertTrue(inboxRepository.hasProcessed(event.getEventId()));
    }

    @Test
    void malformedReviewIsRejectedAndNotRecorded() {
        FederationEvent event = FederationEvent.reviewCreated(remoteInstance, null);

        assertThrows(IllegalArgumentException.class, () -> servlet.applyEvent(event));

        assertFalse(inboxRepository.hasProcessed(event.getEventId()));
    }

    @Test
    void duplicateReviewEventStoresReviewOnce() {
        ExchangeRequest exchange = acceptedReviewExchange("review-duplicate-exchange", localInstance);
        exchangeRepository.save(exchange);
        Review review = remoteReview("review-duplicate", exchange.getId(), localInstance);
        FederationEvent event = FederationEvent.reviewCreated(remoteInstance, review);

        assertTrue(servlet.applyEvent(event));
        assertFalse(servlet.applyEvent(event));

        assertEquals(1, reviewRepository.listAll().size());
        assertEquals(1, reviewRepository.getSaveCalls());
    }

    @Test
    void reviewForPendingExchangeIsRejectedAndNotRecorded() {
        ExchangeRequest exchange = acceptedReviewExchange("review-pending", localInstance);
        exchange.setStatus(ExchangeRequest.STATUS_PENDING);
        exchangeRepository.save(exchange);
        Review review = remoteReview("review-pending-id", exchange.getId(), localInstance);
        FederationEvent event = FederationEvent.reviewCreated(remoteInstance, review);

        assertThrows(IllegalArgumentException.class, () -> servlet.applyEvent(event));

        assertTrue(reviewRepository.listAll().isEmpty());
        assertFalse(inboxRepository.hasProcessed(event.getEventId()));
    }

    @Test
    void reviewFromNonParticipantIsRejectedAndNotRecorded() {
        ExchangeRequest exchange = acceptedReviewExchange("review-non-participant", localInstance);
        exchangeRepository.save(exchange);
        Review review = remoteReview("review-non-participant-id", exchange.getId(), localInstance);
        review.setFromUsername("carol");
        FederationEvent event = FederationEvent.reviewCreated(remoteInstance, review);

        assertThrows(IllegalArgumentException.class, () -> servlet.applyEvent(event));

        assertTrue(reviewRepository.listAll().isEmpty());
        assertFalse(inboxRepository.hasProcessed(event.getEventId()));
    }

    @Test
    void reviewOriginMustMatchReviewerInstance() {
        ExchangeRequest exchange = acceptedReviewExchange("review-origin", localInstance);
        exchangeRepository.save(exchange);
        Review review = remoteReview("review-origin-id", exchange.getId(), localInstance);
        FederationEvent event = FederationEvent.reviewCreated("spoofed-instance", review);

        assertThrows(IllegalArgumentException.class, () -> servlet.applyEvent(event));
        assertFalse(inboxRepository.hasProcessed(event.getEventId()));
    }

    private ExchangeRequest remoteRequest(String id, String targetInstance) {
        ExchangeRequest request = new ExchangeRequest(
                id, "announcement-1", "Piano", "alice", "bob", "hello",
                ExchangeRequest.STATUS_PENDING);
        request.setFromInstance(remoteInstance);
        request.setToInstance(targetInstance);
        return request;
    }

    private ExchangeRequest requesterReplica(String id) {
        ExchangeRequest request = remoteRequest(id, remoteInstance);
        request.setFromInstance(localInstance);
        return request;
    }

    private ExchangeRequest acceptedReviewExchange(String id, String reviewedInstance) {
        ExchangeRequest request = new ExchangeRequest(
                id, "announcement-review", "Piano", "alice", "bob", "hello",
                ExchangeRequest.STATUS_ACCEPTED);
        request.setFromInstance(remoteInstance);
        request.setToInstance(reviewedInstance);
        return request;
    }

    private Review remoteReview(String id, String exchangeId, String reviewedInstance) {
        Review review = new Review(
                id, exchangeId, "alice", "bob", 5, "Excellent", System.currentTimeMillis());
        review.setFromInstance(remoteInstance);
        review.setToInstance(reviewedInstance);
        return review;
    }

    private static class CountingAnnouncementRepository extends AnnouncementRepository {
        private int saveCalls;

        @Override
        public boolean save(Announcement announcement) {
            saveCalls++;
            return super.save(announcement);
        }

        int getSaveCalls() { return saveCalls; }
    }

    private static class FailingAnnouncementRepository extends AnnouncementRepository {
        @Override
        public boolean save(Announcement announcement) {
            throw new IllegalStateException("simulated apply failure");
        }
    }

    private static class CountingExchangeRequestRepository extends ExchangeRequestRepository {
        private int saveCalls;
        private int updateCalls;

        @Override
        public boolean save(ExchangeRequest request) {
            saveCalls++;
            return super.save(request);
        }

        @Override
        public void update(ExchangeRequest request) {
            updateCalls++;
            super.update(request);
        }

        int getSaveCalls() { return saveCalls; }
        int getUpdateCalls() { return updateCalls; }
    }

    private static class CountingReviewRepository extends ReviewRepository {
        private int saveCalls;

        @Override
        public boolean save(Review review) {
            saveCalls++;
            return super.save(review);
        }

        int getSaveCalls() { return saveCalls; }
    }
}
