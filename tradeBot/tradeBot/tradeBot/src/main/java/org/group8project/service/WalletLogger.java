package org.group8project.service;

import org.group8project.patterns.observer.WalletObserver;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
public class WalletLogger implements WalletObserver {

    private final WalletService walletService;

    private final List<Map<String, Object>> logs = new LinkedList<>();

    public WalletLogger(@Lazy WalletService walletService) {
        this.walletService = walletService;
    }

    @PostConstruct
    public void init() {
        walletService.attach(this);
    }

    @Override
    public void onTrade(String type, double amount, double price, double newBalance) {

        Map<String, Object> log = new HashMap<>();
        log.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        log.put("type", type);
        log.put("amount", amount);
        log.put("price", price);
        log.put("balance", newBalance);

        logs.add(0, log);
        if (logs.size() > 50) logs.remove(logs.size() - 1);

        String fileLog = String.format("[%s] TYPE=%s | AMOUNT=%.2f | PRICE=%.2f | BALANCE=%.2f",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                type, amount, price, newBalance);

        writeToFile(fileLog);
    }

    private void writeToFile(String text) {
        try (FileWriter fw = new FileWriter("wallet.log", true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(text);
        } catch (Exception e) {
            System.err.println("Log yazılırken hata: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getLogs() {
        return logs;
    }
}
