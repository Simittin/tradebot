from abc import ABC, abstractmethod

# ==========================================
#  OBSERVER PATTERN (GÖZLEMCİ DESENİ)
# ==========================================

# INTERFACES
class Observer(ABC):
    """
    Gözlemci Arayüzü: Piyasayı dinleyen herkes bu metoda sahip olmalı.
    """
    @abstractmethod
    def update(self, data):
        pass

class Subject:
    """
    Yayıncı (Subject) Arayüzü: Aboneleri (Gözlemcileri) yönetir.
    """
    def __init__(self):
        self._observers = [] # beni dinleyenlerin listesi

    def attach(self, observer):
        """Yeni bir abone ekle (Örn: Yeni bir bot)"""
        self._observers.append(observer)

    def detach(self, observer):
        """Abonelikten çıkar"""
        self._observers.remove(observer)

    def notify(self, data):
        """Tüm abonelere 'Veri Değişti!' diye bağır"""
        print(f"\n SİSTEM: Piyasa güncellendi, {len(self._observers)} aboneye haber veriliyor...")
        for observer in self._observers:
            observer.update(data)


# CONCRETE IMPLEMENTATIONS (CLASS)

class MarketData(Subject):
    """
    Piyasa Verisi (Subject). 
    Veri setini tutar ve yeni veri geldiğinde aboneleri tetikler.
    """
    def set_new_data(self, df):
        # Verinin son satırını alıp bilgi verelim
        son_fiyat = df['close'].iloc[-1]
        print(f" MARKET: Yeni veri yüklendi. Son Fiyat: {son_fiyat}")
        
        # Tüm botlara (Observerlara) veriyi gönder
        self.notify(df)


class TraderBot(Observer):
    """
    Al-Sat Botu (Observer).
    Piyasadan haber geldiğinde, kendi stratejisine göre karar verir.
    """
    def __init__(self, name, strategy):
        self.name = name
        self.strategy = strategy  # STRATEGY PATTERN burada birleşiyor!

    def update(self, df):
        print(f" [{self.name}] Veriyi aldı. Analiz başlıyor...")
        
        # Stratejiye sor: Ne yapayım?
        karar = self.strategy.analyze(df)
        
        # Kararı ekrana bas (veya işlem aç)
        if karar == "AL":
            print(f" [{self.name}] İŞLEM: ALIM EMRİ GİRİLDİ! ")
        elif karar == "SAT":
            print(f" [{self.name}] İŞLEM: SATIŞ EMRİ GİRİLDİ! ")
        else:
            print(f"zzz [{self.name}] Beklemede.")