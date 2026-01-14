package org.group8project.patterns.template;

import org.group8project.model.TradeSignal;
import org.group8project.service.WalletService;
import org.springframework.stereotype.Component;

@Component
public class SpotExecutor extends BaseTradeExecutor {

    private final WalletService walletService;
    private static final String SYMBOL = "BTC";
    private static final double TRADE_AMOUNT = 100.0; // Örn: 1000 USD'lik alım

    public SpotExecutor(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    protected boolean validateFunds(TradeSignal signal) {
        // Spot piyasada kural:
        // ALACAKSAN -> Cebinde USD var mı?
        // SATACAKSAN -> Cebinde Coin var mı?
        
        if (signal == TradeSignal.BUY) {
            boolean hasUsd = walletService.getUsdBalance() >= TRADE_AMOUNT;
            if (!hasUsd) System.out.println("DEBUG: Spot Alım için USD yetersiz.");
            return hasUsd;
        } 
        else if (signal == TradeSignal.SELL) {
            boolean hasCoin = walletService.getAssetAmount(SYMBOL) > 0;
            if (!hasCoin) System.out.println("DEBUG: Satılacak " + SYMBOL + " bulunamadı.");
            return hasCoin;
        }
        return false;
    }

    @Override
    protected void performExecution(TradeSignal signal, double price) {
        // BURAYA DİKKAT: Artık burada "if bakiye var mı" diye sormuyoruz!
        // Base class bunu bizim için halletti. Sadece işimizi yapıyoruz.
        
        System.out.println(">>> SPOT PİYASA EMRİ GİRİLİYOR <<<");
        
        if (signal == TradeSignal.BUY) {
            walletService.buy(SYMBOL, price, TRADE_AMOUNT);
        } else if (signal == TradeSignal.SELL) {
            walletService.sellAll(SYMBOL, price);
        }
    }
}