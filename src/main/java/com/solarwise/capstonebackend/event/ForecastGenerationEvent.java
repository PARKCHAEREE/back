package com.solarwise.capstonebackend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
public class ForecastGenerationEvent extends ApplicationEvent {
    private final Long plantId;
    private final LocalDateTime targetTime;

    public ForecastGenerationEvent(Object source, Long plantId, LocalDateTime targetTime) {
        super(source);
        this.plantId = plantId;
        this.targetTime = targetTime;
    }
}
