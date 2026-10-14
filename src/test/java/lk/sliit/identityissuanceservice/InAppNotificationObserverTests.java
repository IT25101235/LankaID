package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Notification_Mgmt.Model.Notification;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Model.UserNotification;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.InAppNotificationObserver;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.InAppNotificationEvent;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.NotificationObserver;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.NotificationPublisher;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Repository.NotificationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Repository.UserNotificationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Tests manual observer dispatch and real transactions, using fake database connections. */
class InAppNotificationObserverTests {

    private AnnotationConfigApplicationContext context;
    private NotificationPublisher publisher;
    private NotificationRepository notifications;
    private UserNotificationRepository deliveries;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        context = new AnnotationConfigApplicationContext(
                ObserverTestConfiguration.class, NotificationPublisher.class,
                InAppNotificationObserver.class, NotificationService.class
        );
        publisher = context.getBean(NotificationPublisher.class);
        notifications = context.getBean(NotificationRepository.class);
        deliveries = context.getBean(UserNotificationRepository.class);
        connection = context.getBean(DataSource.class).getConnection();

        when(notifications.save(any(Notification.class))).thenAnswer(invocation -> {
            // Storage must happen immediately, inside the publisher's transaction.
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(501);
            return notification;
        });
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void citizenEventSavesOneNotificationWithTheOriginalDetails() throws SQLException {
        publisher.sendToCitizen(25, null, "APPLICATION_APPROVED", "Your application was approved.");

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(saved.capture());
        assertNull(saved.getValue().getSentByStaffId());
        assertEquals("APPLICATION_APPROVED", saved.getValue().getNotificationType());
        assertEquals("Your application was approved.", saved.getValue().getMessage());
        assertEquals("N", saved.getValue().getBroadcast());
        assertEquals(List.of(25), savedRecipientIds());
        verify(connection).commit();
        verify(connection, never()).rollback();
    }

    @Test
    void broadcastReturnsTheActualCountAfterDuplicateAndNullRecipientsAreRemoved() {
        int count = publisher.sendToUsers(Arrays.asList(25, 26, 25, null), 99,
                "ADMIN_GENERAL", "Planned maintenance", true);

        assertEquals(2, count);
        assertEquals(List.of(25, 26), savedRecipientIds());
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(saved.capture());
        assertEquals(99, saved.getValue().getSentByStaffId());
        assertEquals("ADMIN_GENERAL", saved.getValue().getNotificationType());
        assertEquals("Planned maintenance", saved.getValue().getMessage());
        assertEquals("Y", saved.getValue().getBroadcast());
    }

    @Test
    void invalidRecipientsStillThrowTheOriginalValidationError() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> publisher.sendToUsers(null, 99, "ADMIN_GENERAL", "Message", true));

        assertEquals("No eligible recipients matched your selection.", failure.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> publisher.sendToUsers(List.of(), 99, "ADMIN_GENERAL", "Message", true));
        verifyNoInteractions(notifications, deliveries);
    }

    @Test
    void storageFailureReachesTheCallerAndRollsBackTheTransaction() throws SQLException {
        IllegalStateException failure = new IllegalStateException("Recipient storage failed");
        when(deliveries.saveAll(any())).thenThrow(failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> publisher.sendToCitizen(25, 99, "APPLICATION_APPROVED", "Approved"));

        assertSame(failure, thrown);
        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    @Test
    void notificationJoinsTheWorkflowTransactionAndRollsBackWithIt() throws SQLException {
        TransactionTemplate workflow = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));

        assertThrows(IllegalStateException.class, () -> workflow.executeWithoutResult(status -> {
            publisher.sendToCitizen(25, 99, "PAYMENT_SUCCESS", "Payment received");
            throw new IllegalStateException("The rest of the workflow failed");
        }));

        verify(notifications).save(any(Notification.class));
        verify(deliveries).saveAll(any());
        // Both the listener and publisher join the existing transaction: only one rollback.
        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    @Test
    void eachEventHasItsOwnDeliveryCount() throws SQLException {
        assertEquals(2, publisher.sendToUsers(List.of(25, 26), 99, "ADMIN_GENERAL", "First", true));
        assertEquals(1, publisher.sendToUsers(List.of(27), 99, "ADMIN_GENERAL", "Second", false));

        verify(notifications, times(2)).save(any(Notification.class));
        verify(connection, times(2)).commit();
    }

    @Test
    void missingObserverDoesNotReportASuccessfulDelivery() {
        NotificationPublisher withoutObserver = new NotificationPublisher(List.of());

        assertThrows(IllegalStateException.class,
                () -> withoutObserver.sendToUsers(List.of(25), 99, "ADMIN_GENERAL", "Message", false));
    }

    @Test
    void manualSubjectNotifiesMultipleObserversWithoutSpringEvents() {
        NotificationObserver first = mock(NotificationObserver.class);
        NotificationObserver second = mock(NotificationObserver.class);
        NotificationPublisher subject = new NotificationPublisher(List.of(first, second));
        InAppNotificationEvent event = new InAppNotificationEvent(List.of(25), null, "TEST", "Message", false);

        subject.notifyObservers(event);

        verify(first).update(event);
        verify(second).update(event);
    }

    @Test
    void removedObserverDoesNotReceiveFurtherUpdates() {
        NotificationObserver observer = mock(NotificationObserver.class);
        NotificationPublisher subject = new NotificationPublisher(List.of());
        InAppNotificationEvent event = new InAppNotificationEvent(List.of(25), null, "TEST", "Message", false);
        subject.addObserver(observer);
        subject.notifyObservers(event);
        subject.removeObserver(observer);
        subject.notifyObservers(event);

        verify(observer, times(1)).update(event);
    }

    @Test
    void registeringTheSameObserverAgainDoesNotDuplicateStorage() {
        publisher.addObserver(context.getBean(InAppNotificationObserver.class));
        publisher.addObserver(context.getBean(InAppNotificationObserver.class));

        publisher.sendToCitizen(25, null, "APPLICATION_APPROVED", "Approved");

        verify(notifications, times(1)).save(any(Notification.class));
        verify(deliveries, times(1)).saveAll(any());
    }

    @Test
    void removingTheStorageObserverFailsDeliveryAndRollsBack() throws SQLException {
        publisher.removeObserver(context.getBean(InAppNotificationObserver.class));

        assertThrows(IllegalStateException.class,
                () -> publisher.sendToCitizen(25, null, "APPLICATION_APPROVED", "Approved"));

        verifyNoInteractions(notifications, deliveries);
        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    @Test
    void failureInAnAdditionalObserverRollsBackEarlierStorage() throws SQLException {
        IllegalStateException failure = new IllegalStateException("Additional observer failed");
        publisher.addObserver(event -> { throw failure; });

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> publisher.sendToCitizen(25, null, "APPLICATION_APPROVED", "Approved")));

        verify(notifications).save(any(Notification.class));
        verify(deliveries).saveAll(any());
        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    @Test
    void simultaneousPublicationsKeepTheirMessageAndDeliveryCountSeparate() throws Exception {
        CountDownLatch bothObserversEntered = new CountDownLatch(2);
        NotificationPublisher subject = new NotificationPublisher(List.of(event -> {
            bothObserversEntered.countDown();
            try {
                if (!bothObserversEntered.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent publication did not arrive.");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            assertEquals(event.getUserIds().size() == 1 ? "First" : "Second", event.getMessage());
            event.setDeliveredCount(event.getUserIds().size());
        }));
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> subject.sendToUsers(List.of(25), null, "TEST", "First", false));
            var second = executor.submit(() -> subject.sendToUsers(List.of(26, 27), null, "TEST", "Second", false));

            assertEquals(1, first.get(10, TimeUnit.SECONDS).intValue());
            assertEquals(2, second.get(10, TimeUnit.SECONDS).intValue());
        } finally {
            executor.shutdownNow();
        }
    }

    private List<Integer> savedRecipientIds() {
        ArgumentCaptor<List<UserNotification>> saved = ArgumentCaptor.captor();
        verify(deliveries).saveAll(saved.capture());
        assertTrue(saved.getValue().stream().allMatch(item -> item.getId().getNotificationId().equals(501)));
        return saved.getValue().stream().map(item -> item.getId().getUserId()).toList();
    }

    @TestConfiguration
    @EnableTransactionManagement(proxyTargetClass = true)
    static class ObserverTestConfiguration {

        @Bean
        NotificationRepository notificationRepository() {
            return mock(NotificationRepository.class);
        }

        @Bean
        UserNotificationRepository userNotificationRepository() {
            return mock(UserNotificationRepository.class);
        }

        @Bean
        DataSource dataSource() throws SQLException {
            Connection connection = mock(Connection.class);
            when(connection.getAutoCommit()).thenReturn(true);
            DataSource dataSource = mock(DataSource.class);
            when(dataSource.getConnection()).thenReturn(connection);
            return dataSource;
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
