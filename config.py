import os

# ==========================================
# ⚙️ PROJE KONFİGÜRASYONU (AYARLAR)
# ==========================================

# --- 1. DİZİN VE DOSYA YÖNETİMİ ---
# Bu kod, projenin bilgisayarında nerede olduğunu otomatik algılar.
# Windows/Mac/Linux fark etmeksizin çalışmasını sağlar.
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# Verilerin saklanacağı 'data' klasörünün yolu
DATA_DIR = os.path.join(BASE_DIR, 'data')

# Dosya yolları
RAW_DATA_PATH = os.path.join(DATA_DIR, 'ham_veri.csv')           # İndirilen saf veri
PROCESSED_DATA_PATH = os.path.join(DATA_DIR, 'islenmis_veri.csv') # İşlenmiş (indikatörlü) veri
MODEL_PATH = os.path.join(DATA_DIR, 'ai_model.pkl')              # Eğitilen yapay zeka modeli

# --- 2. BORSA VERİ AYARLARI ---
SYMBOL = 'BTC-USD'       # İşlem paritesi (Yahoo Finance formatı)
INTERVAL = '1h'          # Zaman dilimi (1 saatlik mumlar)
PERIOD = '2y'            # Ne kadarlık geçmiş veri indirilsin?

# --- 3. İNDİKATÖR AYARLARI ---
# Stratejileri test ederken burayı değiştirebilirsin.
RSI_PERIOD = 14          # RSI için bakılacak mum sayısı
SMA_PERIOD = 50          # Hareketli ortalama uzunluğu

# --- 4. YAPAY ZEKA AYARLARI ---
TEST_SIZE = 0.2          # Verinin %20'si test, %80'i eğitim olacak
RANDOM_SEED = 42         # Sonuçların her denemede aynı çıkması için sabit sayı