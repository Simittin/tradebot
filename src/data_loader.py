import yfinance as yf
import pandas as pd
import os

def load_data(symbol, period, interval, save_path):
    """
    Yahoo Finance'ten veriyi indirir, temizler ve belirtilen yola kaydeder.
    
    Parametreler:
    - symbol: 'BTC-USD' gibi parite ismi
    - period: '2y' gibi süre
    - interval: '1h' gibi mum aralığı
    - save_path: Dosyanın kaydedileceği tam yol (örn: data/ham_veri.csv)
    """
    
    print(f"\n--- [1/3] Veri İndirme Başlatıldı: {symbol} ---")
    
    # request atarak gerekli veriyi çekiyoruz 
    try:
        df = yf.download(tickers=symbol, period=period, interval=interval, auto_adjust=True)
    except Exception as e:
        print(f"HATA: Veri indirilemedi! Detay: {e}")
        return None

    # boş veri atarsa hata
    if df.empty:
        print("HATA: Yahoo Finance boş veri döndürdü. Sembolü veya interneti kontrol edin.")
        return None

    # 3. MultiIndex (İç içe sütun) Düzeltmesi
    # yfinance bazen sütunları (Price, Open) şeklinde iki katmanlı verir.
    if isinstance(df.columns, pd.MultiIndex):
        df.columns = df.columns.get_level_values(0)

    
    df = df.reset_index()               # tarihileri bir sütuna koyar
    df.columns = df.columns.str.lower() # sütun isimlerini küçült(olası büyük küçük harf karışıklığı için)

    # tarih sütunu ismini değiştir (Date -> datetime)
    if 'date' in df.columns:
        df = df.rename(columns={'date': 'datetime'})

    # gerekli olan veriler 
    # datetime --> tarih saat  volume --> işlem hacmi
    # open     --> açılış fiyatı close --> kapanış fiyatı
    # high     --> en yüksek fiyat low   --> en düşük fiyat 
    gerekli_sutunlar = ['datetime', 'open', 'high', 'low', 'close', 'volume']
    
    # Sadece veri setinde mevcut olan sütunları seç (Hata almamak için)
    # gelen veride başka sütunlar da ol(abilir) bunları atmak için ve bize gerekli olanları almak için
    mevcut = [col for col in gerekli_sutunlar if col in df.columns]
    df = df[mevcut]

    # Kaydedilecek klasör (data/) yoksa, kod patlamasın diye otomatik oluştur.
    klasor_yolu = os.path.dirname(save_path)
    if klasor_yolu and not os.path.exists(klasor_yolu):
        os.makedirs(klasor_yolu)
        print(f"Bilgi: '{klasor_yolu}' klasörü oluşturuldu.")

    # excel dosyasına kaydetme
    df.to_csv(save_path, index=False)
    
    print(f"BAŞARILI: Veri şuraya kaydedildi -> {save_path}")
    print(f"İndirilen Veri Boyutu: {len(df)} satır")
    
    return df