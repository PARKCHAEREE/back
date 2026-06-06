package com.solarwise.capstonebackend.event;

import com.solarwise.capstonebackend.entity.Anomaly;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AnomalyCreatedEvent extends ApplicationEvent {
    private final Anomaly anomaly;

    public AnomalyCreatedEvent(Object source, Anomaly anomaly) {
        super(source);
        this.anomaly = anomaly;
    }
}
