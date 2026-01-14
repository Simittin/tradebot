package org.group8project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * PROJE GİRİŞ NOKTASI
 * @SpringBootApplication anotasyonu, Spring'in alt paketlerdeki
 * (controller, service, patterns...) tüm sınıfları otomatik taramasını sağlar.
 */
@SpringBootApplication
@EnableScheduling
public class Main {
    public static void main(String[] args) {
        // Uygulamayı başlatır
        SpringApplication.run(Main.class, args);
    }
}