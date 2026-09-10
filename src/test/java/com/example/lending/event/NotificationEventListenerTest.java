package com.example.lending.event;

import com.example.lending.domain.customer.Customer;
import com.example.lending.domain.loan.Loan;
import com.example.lending.domain.notification.NotificationEventType;
import com.example.lending.service.NotificationService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.*;

class NotificationEventListenerTest {
    @Test
    void delegatesPublishedLendingEventToNotificationService() {
        NotificationService service = mock(NotificationService.class);
        NotificationEventListener listener = new NotificationEventListener(service);
        Customer customer = Customer.builder().id(1L).build();
        Loan loan = Loan.builder().id(2L).build();
        LendingNotificationEvent event = new LendingNotificationEvent(
                NotificationEventType.LOAN_DISBURSED, customer, loan, Map.of("key", "value"));

        listener.on(event);

        verify(service).notify(NotificationEventType.LOAN_DISBURSED, customer, loan, event.variables());
    }

    @Test
    void containsDeliveryFailureSoCoreEventHandlingDoesNotPropagateIt() {
        NotificationService service = mock(NotificationService.class);
        doThrow(new IllegalStateException("provider unavailable"))
                .when(service).notify(any(), any(), any(), anyMap());
        NotificationEventListener listener = new NotificationEventListener(service);

        listener.on(new LendingNotificationEvent(NotificationEventType.OVERDUE_NOTICE,
                Customer.builder().id(1L).build(), Loan.builder().id(2L).build(), Map.of()));

        verify(service).notify(eq(NotificationEventType.OVERDUE_NOTICE), any(), any(), anyMap());
    }
}
