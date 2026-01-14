package org.group8project.service;

import org.group8project.patterns.observer.WalletObserver;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WalletService {

    private double usdBalance = 10000.0;
    private final Map<String, Double> assetBalance = new HashMap<>();
    private final List<WalletObserver> observers = new ArrayList<>();
    public void attach(WalletObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers(String type, double amount,double price) {
        for (WalletObserver observer : observers) {
            observer.onTrade(type,amount, price,usdBalance );
        }
    }


    public double getUsdBalance() { return usdBalance; }
    public double getAssetAmount(String symbol) { return assetBalance.getOrDefault(symbol, 0.0); }

    public boolean buy(String symbol, double price, double usdAmountToSpend) {
        if (usdBalance >= usdAmountToSpend) {
            double coinAmount = usdAmountToSpend / price;
            usdBalance -= usdAmountToSpend;
            assetBalance.put(symbol, getAssetAmount(symbol) + coinAmount);

            notifyObservers("BUY", usdAmountToSpend, price);

            System.out.println("[WALLET] ALINDI: -" + usdAmountToSpend + "$");
            return true;
        }
        return false;
    }


    public boolean sellAll(String symbol, double currentPrice) {
        double coinAmount = getAssetAmount(symbol);
        if (coinAmount > 0) {
            double revenue = coinAmount * currentPrice;
            usdBalance += revenue;
            assetBalance.put(symbol, 0.0);

            notifyObservers("SELL", revenue, currentPrice);

            System.out.println("[WALLET] SATILDI: +" + revenue + "$");
            return true;
        }
        return false;
    }
}