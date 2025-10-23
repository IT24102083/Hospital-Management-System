package com.hospital.hospitalmanagementsystem.observer;

import com.hospital.hospitalmanagementsystem.model.User;

public interface UserActivitySubject {
    void registerObserver(UserActivityObserver observer);
    void removeObserver(UserActivityObserver observer);
    void notifyObservers(String action, User user);
}