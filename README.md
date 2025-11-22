# 🤖 AI Powered Crypto Trading Bot

Bu proje, Kripto para piyasalarında teknik analiz ve yapay zeka (Makine Öğrenmesi) kullanarak al-sat sinyalleri üreten modüler bir ticaret botudur. 

Proje, **Nesne Yönelimli Programlama (OOP)** prensipleri ve **Yazılım Tasarım Desenleri (Design Patterns)** kullanılarak, genişletilebilir ve sürdürülebilir bir mühendislik mimarisiyle geliştirilmiştir.

## 🚀 Özellikler

* **Otomatik Veri Toplama:** Yahoo Finance API üzerinden geçmiş verileri çeker.
* **Gelişmiş Veri İşleme:** RSI, MACD, Bollinger Bantları, SMA, EMA gibi teknik indikatörleri hesaplar.
* **Yapay Zeka Desteği:** `RandomForestClassifier` algoritması ile geçmiş verilerden öğrenerek gelecek trendini tahmin eder.
* **Çoklu Strateji Desteği:** Aynı anda birden fazla stratejiyi (Trend Takibi, Scalping, AI) çalıştırabilir.

## 🏗️ Mimari ve Tasarım Desenleri

Bu projede iki temel Tasarım Deseni (Design Pattern) kullanılmıştır:

### 1. Strategy Pattern (Strateji Deseni)
Botun karar mekanizması soyutlanmıştır. `TradingStrategy` arayüzü sayesinde, ana koda dokunmadan yeni stratejiler eklenebilir.
* **Kullanım:** `src/strategies.py`
* **Örnekler:** `MacdStrategy`, `BollingerStrategy`, `AiStrategy`

### 2. Observer Pattern (Gözlemci Deseni)
Piyasa verisi (`Subject`) ile Botlar (`Observers`) arasındaki bağlantı gevşek (decoupled) tutulmuştur. Veri güncellendiğinde, sisteme abone olan tüm botlar otomatik olarak tetiklenir.
* **Kullanım:** `src/market_observer.py`

## 📂 Dosya Yapısı

```text
TradeBot/
│
├── config.py                # Proje ayarları (Sembol, periyot vb.)
├── main.py                  # Projeyi başlatan ana dosya
├── requirements.txt         # Gerekli kütüphaneler
├── README.md                # Proje dokümantasyonu
│
├── data/                    # Veri depolama alanı (Otomatik oluşur)
│   ├── ham_veri.csv
│   └── islenmis_veri.csv
│
└── src/                     # Kaynak kodlar
    ├── data_loader.py       # Veri indirme modülü
    ├── processor.py         # Veri işleme ve Feature Engineering
    ├── ai_model.py          # Yapay zeka eğitim modülü
    ├── strategies.py        # Al-Sat stratejileri (Strategy Pattern)
    └── market_observer.py   # Piyasa dinleme sistemi (Observer Pattern)