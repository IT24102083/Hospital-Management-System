package com.hospital.hospitalmanagementsystem.observer;

import com.hospital.hospitalmanagementsystem.model.User;

public interface UserActivityObserver {
    void onUserActivity(String action, User user);
}