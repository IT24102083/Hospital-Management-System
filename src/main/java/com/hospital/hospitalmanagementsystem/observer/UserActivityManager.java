package com.hospital.hospitalmanagementsystem.observer;

import com.hospital.hospitalmanagementsystem.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service // A Spring-managed singleton to hold the list of observers
public class UserActivityManager implements UserActivitySubject {

    private final List<UserActivityObserver> observers = new ArrayList<>();

    @Override
    public void registerObserver(UserActivityObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(UserActivityObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String action, User user) {
        for (UserActivityObserver observer : observers) {
            observer.onUserActivity(action, user);
        }
    }
}