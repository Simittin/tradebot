package org.group8project.patterns.template;

import org.group8project.model.TradeSignal;

public abstract class BaseTradeExecutor {

    // TEMPLATE METHOD: Akışın patronu burası. Değiştirilemez (final).
    public final void executeTrade(TradeSignal signal, double price) {
        if (signal == TradeSignal.HOLD) {
            return;
        }

        System.out.println("--- İşlem Döngüsü Başlatılıyor ---");

        // ADIM 1: Bakiye Kontrolü (Soyut adım - Alt sınıf kuralı belirler)
        // Sinyali gönderiyoruz ki neyi kontrol edeceğini bilsin (USD mi? Coin mi?)
        if (validateFunds(signal)) {
            
            // ADIM 2: İşlemi Gerçekleştir (Soyut adım - Alt sınıf uygular)
            performExecution(signal, price);
            
            // ADIM 3: Logla (Ortak adım)
            logTransaction(signal, price);
            
        } else {
            System.out.println("HATA: Yetersiz Bakiye veya Teminat! İşlem iptal edildi.");
        }
    }

    // --- SOYUT ADIMLAR (Alt sınıflar doldurmak ZORUNDA) ---

    // Artık bu metot abstract! Her piyasa kendi kuralını koymalı.
    protected abstract boolean validateFunds(TradeSignal signal);

    protected abstract void performExecution(TradeSignal signal, double price);

    // --- ORTAK ADIMLAR (Herkes aynısını kullanır) ---

    private void logTransaction(TradeSignal signal, double price) {
        System.out.println("LOG: Veritabanına kayıt atıldı -> " + signal + " @ " + price);
    }
}