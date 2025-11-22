import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report
import joblib # Modeli diske kaydetmek için (Save/Load)
import os

def train_model(data_path, model_save_path):
    """
    İşlenmiş veriyi yükler, Yapay Zeka modelini eğitir, test eder ve kaydeder.
    Geriye eğitilmiş modeli döndürür.
    """
    print("\n--- [3/4] Yapay Zeka Eğitimi Başladı ---")

    # 1. Veri Kontrolü ve Yükleme
    if not os.path.exists(data_path):
        print(f"HATA: Veri dosyası bulunamadı -> {data_path}")
        return None

    df = pd.read_csv(data_path)

    # 2. Girdiler (Features) ve Çıktı (Target) Ayrımı
    # Model hangi sütunlara bakarak karar verecek?
    # processor.py'de RSI ve SMA üretmiştik. Target da orada.
    feature_cols = [
        'SMA_50', 'EMA_20', 
        'RSI', 
        'MACD', 'MACD_SIGNAL', 
        'STOCH_K', 'STOCH_D', 
        'BB_UPPER', 'BB_LOWER'
    ]
    
    X = df[feature_cols] # Sorular (RSI kaç? SMA kaç?)
    y = df['Target']     # Cevap (Yükseldi mi? 1/0)

    print(f"Eğitimde Kullanılan Özellikler: {feature_cols}")

    # 3. Veriyi Bölme (Eğitim vs Test)
    # shuffle=False ÇOK KRİTİK: Zaman serisi verisinde karıştırma yapılmaz.
    # Geçmiş verilerle eğitip, gelecek verilerle test etmeliyiz.
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, shuffle=False)

    print(f"Eğitim Verisi: {len(X_train)} satır")
    print(f"Test Verisi: {len(X_test)} satır")

    # 4. Modeli Kur ve Eğit (Random Forest)
    # Bir sürü karar ağacı oluşturup ortak karar alan güçlü bir algoritma.
    print("Model öğreniyor... (Bu işlem bilgisayar hızına göre sürebilir)")
    model = RandomForestClassifier(n_estimators=100, min_samples_split=10, random_state=42)
    model.fit(X_train, y_train)

    # 5. Test Et (Sınav Vakti)
    predictions = model.predict(X_test)
    basari = accuracy_score(y_test, predictions)

    print(f"\n>>> MODEL BAŞARISI (Accuracy): %{basari * 100:.2f}")
    
    # Detaylı Rapor (Precision, Recall vb.)
    # 1 (Yükseliş) tahminlerinde ne kadar başarılı?
    print("Detaylı Sınıflandırma Raporu:")
    print(classification_report(y_test, predictions))

    # 6. Modeli Kaydet
    # Eğitilen beyni bir dosyaya (.pkl) kaydediyoruz.
    # Böylece her çalıştırdığımızda tekrar eğitmek zorunda kalmayız.
    klasor = os.path.dirname(model_save_path)
    if klasor and not os.path.exists(klasor):
        os.makedirs(klasor)

    joblib.dump(model, model_save_path)
    print(f"Model başarıyla kaydedildi -> {model_save_path}")

    return model