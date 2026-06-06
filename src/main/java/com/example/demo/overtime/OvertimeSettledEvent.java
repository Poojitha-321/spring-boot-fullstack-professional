package com.example.demo.overtime;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OvertimeSettledEvent extends ApplicationEvent {
    private final Long workerId;
    private final String month;
    private final double totalAmount;

    public OvertimeSettledEvent(Object source, Long workerId, String month, double totalAmount) {
        super(source);
        this.workerId = workerId;
        this.month = month;
        this.totalAmount = totalAmount;
    }
}
