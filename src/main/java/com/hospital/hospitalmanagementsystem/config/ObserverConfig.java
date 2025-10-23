package com.hospital.hospitalmanagementsystem.config;

import com.hospital.hospitalmanagementsystem.observer.AuditLogObserver;
import com.hospital.hospitalmanagementsystem.observer.UserActivityManager;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class ObserverConfig {

    private final UserActivityManager userActivityManager;
    private final AuditLogObserver auditLogObserver;

    public ObserverConfig(UserActivityManager userActivityManager, AuditLogObserver auditLogObserver) {
        this.userActivityManager = userActivityManager;
        this.auditLogObserver = auditLogObserver;
    }

    @PostConstruct
    public void registerObservers() {
        // This connects the Observer to the Subject on startup
        userActivityManager.registerObserver(auditLogObserver);
    }
}