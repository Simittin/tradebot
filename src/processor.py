import pandas as pd
import pandas_ta as ta
import os

def process_data(input_path, output_path, rsi_period=14, sma_period=50):
    """
    Ham veriyi okur, teknik indikatörleri ekler ve temizleyip kaydeder.
    
    Parametreler:
    - input_path: Ham verinin (CSV) yolu
    - output_path: İşlenmiş verinin kaydedileceği yol
    - rsi_period: RSI indikatörü için periyot (Örn: 14)
    - sma_period: Hareketli ortalama için periyot (Örn: 50)
    """
    
    print("\n--- [2/3] Veri İşleme ve Özellik Mühendisliği Başladı ---")
    
    # çektiğimiz veriyi okuma
    if not os.path.exists(input_path):
        print(f"HATA: Girdi dosyası bulunamadı -> {input_path}")
        print("Lütfen önce veri indirme adımını çalıştırın.")
        return None
        
    df = pd.read_csv(input_path)
    
    # Teknik İndikatörleri Hesapla 
    # Bu kısım Yapay Zekaya "Piyasanın Durumu" hakkında ipucu verir.
    # indikatörler: karmaşık olan fiyat hareketlerini matematiksel formüllerle özetleyip (yorumlayıp)
    # anlamlı hale getirir.
    
    # RSI (Momentum): Fiyatın hızını ölçer.
    # fiyatın ne kadar hızlı yükseldiğini/düştüğünü gösterir
    # 30 altı aşırı satım, 70 üstü aşırı alım sinyali verir
    df['RSI'] = df.ta.rsi(length=rsi_period)
    
    # SMA (Trend): Fiyatın ortalamaya göre yerini belirler.
    # fiyat ortalama üzerindeyse yükseliş trendi, altındaysa düşüş trendi
    df['SMA_50'] = df.ta.sma(length=50)
    df['EMA_20'] = df.ta.ema(length=20) # Fiyata daha duyarlı ortalama
    
    # Bollinger Bantları (Fiyatın sıkışmasını ölçer)
    bbands = df.ta.bbands(length=20, std=2)
    df['BB_LOWER'] = bbands.iloc[:, 0] # Alt bant (0. indeks)
    df['BB_UPPER'] = bbands.iloc[:, 2] # Üst bant (2. indeks)

    # Stochastic
    stoch = df.ta.stoch(k=14, d=3, smooth_k=3)
    # Genelde: [STOCHk, STOCHd]
    df['STOCH_K'] = stoch.iloc[:, 0]
    df['STOCH_D'] = stoch.iloc[:, 1]

    # MACD
    macd = df.ta.macd(fast=12, slow=26, signal=9)
    # pandas_ta genelde şu sırayla döner: [MACD Line, Histogram, Signal Line]
    # İsimler değişse bile iloc ile güvenli erişim sağlıyoruz:
    df['MACD'] = macd.iloc[:, 0]       # 1. Sütun: MACD Hattı
    df['MACD_SIGNAL'] = macd.iloc[:, 2] # 3. Sütun: Sinyal Hattı


    # 3. Hedef (Target) Oluşturma - "CEVAP ANAHTARI"
    # Yapay zeka neyi tahmin edecek?
    # Soru: "Bir saat sonraki kapanış fiyatı, şimdikinden yüksek mi olacak?"
    
    # shift(-1) ile gelecek verisini şimdiki satıra çekiyoruz (Geçici işlem)
    # next_close satırının mantığı burdaki promptta detaylı anlatılmıştır ---> https://gemini.google.com/app/170f04f92d2d78ce?hl=tr
    df['Next_Close'] = df['close'].shift(-1)
    
    # eğer gelecekte fiyat artarsa 1 düşere 0 etiketi ver
    df['Target'] = (df['Next_Close'] > df['close']).astype(int)
    
    # 4. Veri Temizliği (Data Cleaning)
    
    # İndikatör hesaplarken oluşan baştaki boşlukları (NaN) sil
    # Örn: 50 günlük ortalama için ilk 50 günün verisi boştur.
    df.dropna(inplace=True)
    
    # Veri Sızıntısını (Data Leakage) Önleme
    # Yapay zeka eğitim sırasında "Next_Close"u görürse kopya çeker.
    # Bu yüzden cevabı hesapladıktan sonra kopyayı yırtıp atıyoruz.
    df.drop(['Next_Close'], axis=1, inplace=True)
    
    # İşlenmiş veriyi kaydetme
    klasor_yolu = os.path.dirname(output_path)
    if klasor_yolu and not os.path.exists(klasor_yolu):
        os.makedirs(klasor_yolu)

    df.to_csv(output_path, index=False)
    
    print(f"BAŞARILI: İşlenmiş veri şuraya kaydedildi -> {output_path}")
    print(f"Eğitime Hazır Veri Sayısı: {len(df)} satır")
    print("Örnek Veri (Son 5 Satır):")
    print(df[['datetime', 'close', 'RSI', 'SMA_50', 'Target']].tail())
    
    return df