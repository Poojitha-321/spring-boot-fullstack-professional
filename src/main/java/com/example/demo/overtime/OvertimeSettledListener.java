package com.example.demo.overtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class OvertimeSettledListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOvertimeSettled(OvertimeSettledEvent event) {
        try {
            // SMS would be sent here via SMS provider
            log.info("SMS SENT: Worker {} overtime for {} settled. Amount: ₹{}",
                event.getWorkerId(), event.getMonth(), event.getTotalAmount());
        } catch (Exception e) {
            // SMS failure should NOT affect settlement data
            log.error("SMS failed for worker {} but settlement is complete: {}",
                event.getWorkerId(), e.getMessage());
        }
    }
}
