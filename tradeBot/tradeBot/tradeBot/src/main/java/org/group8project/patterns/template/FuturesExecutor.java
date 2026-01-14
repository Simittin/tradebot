package org.group8project.patterns.template;

import org.group8project.model.TradeSignal;
import org.springframework.stereotype.Component;

@Component
public class FuturesExecutor extends BaseTradeExecutor {

    @Override
    protected boolean validateFunds(TradeSignal signal) {
        // Vadeli işlemlerde kural farklıdır:
        // Yön ne olursa olsun (Long/Short), "Initial Margin" (Başlangıç Teminatı) gerekir.
        double requiredMargin = 50.0; // Örn: Kaldıraçlı işlem için 50$ yeterli
        
        // Mock bir kontrol (Gerçekte WalletService'den marjin bakiyesine bakılır)
        System.out.println("DEBUG: Futures cüzdanında teminat kontrol ediliyor...");
        return true; // Şimdilik hep var sayalım
    }

    @Override
    protected void performExecution(TradeSignal signal, double price) {
        System.out.println(">>> FUTURES (KALDIRAÇLI) POZİSYON AÇILIYOR <<<");
        
        if (signal == TradeSignal.BUY) {
            System.out.println("İşlem: LONG Pozisyonu Aç (10x Kaldıraç)");
        } else if (signal == TradeSignal.SELL) {
            System.out.println("İşlem: SHORT Pozisyonu Aç (10x Kaldıraç)");
        }
    }
}