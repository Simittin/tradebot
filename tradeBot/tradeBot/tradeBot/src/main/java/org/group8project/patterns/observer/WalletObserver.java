package org.group8project.patterns.observer;

public interface WalletObserver {
    void onTrade(String type, double amount, double price, double newBalance);
}