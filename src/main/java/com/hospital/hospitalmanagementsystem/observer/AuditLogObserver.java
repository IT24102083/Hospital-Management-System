package com.hospital.hospitalmanagementsystem.observer;

import com.hospital.hospitalmanagementsystem.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component // Mark as a Spring bean so we can find it
public class AuditLogObserver implements UserActivityObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogObserver.class);

    @Override
    public void onUserActivity(String action, User user) {
        LOGGER.info(
                "[AUDIT LOG] Admin action performed. Action: '{}', User ID: {}, Username: '{}'",
                action,
                user.getId(),
                user.getUsername()
        );
    }
}