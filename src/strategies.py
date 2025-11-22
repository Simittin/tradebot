from abc import ABC, abstractmethod
import pandas as pd

# --- ARAYÜZ ---
class TradingStrategy(ABC):
    @abstractmethod
    def analyze(self, df: pd.DataFrame) -> str:
        pass

# --- 1. MACD STRATEJİSİ (Trend Takipçisi) ---
class MacdStrategy(TradingStrategy):
    """
    MACD çizgisi, Sinyal çizgisini yukarı keserse AL, aşağı keserse SAT.
    Genelde büyük trendleri yakalar.
    """
    def analyze(self, df: pd.DataFrame) -> str:
        # Son veriyi al
        macd = df['MACD'].iloc[-1]
        signal = df['MACD_SIGNAL'].iloc[-1]
        
        # Bir önceki veriyi al (Kesişimi anlamak için düne bakmalıyız)
        prev_macd = df['MACD'].iloc[-2]
        prev_signal = df['MACD_SIGNAL'].iloc[-2]

        # Kesişim Kontrolü (Crossover)
        # Bugün MACD sinyalin üstüne çıkmış VE dün altındaymış = AL SİNYALİ
        if macd > signal and prev_macd < prev_signal:
            return "AL (MACD Kesişimi)"
        elif macd < signal and prev_macd > prev_signal:
            return "SAT (MACD Kesişimi)"
        else:
            return "BEKLE"

# --- 2. BOLLINGER STRATEJİSİ (Tepki Alımı) ---
class BollingerStrategy(TradingStrategy):
    """
    Fiyat alt bandı delerse 'Ucuz' diyip ALIR.
    Fiyat üst bandı delerse 'Pahalı' diyip SATAR.
    """
    def analyze(self, df: pd.DataFrame) -> str:
        price = df['close'].iloc[-1]
        lower = df['BB_LOWER'].iloc[-1]
        upper = df['BB_UPPER'].iloc[-1]

        if price < lower:
            return "AL (Alt Bant Delindi - Ucuz)"
        elif price > upper:
            return "SAT (Üst Bant Delindi - Pahalı)"
        else:
            return "BEKLE"

# --- 3. YAPAY ZEKA (General Manager) ---
class AiStrategy(TradingStrategy):
    def __init__(self, model):
        self.model = model

    def analyze(self, df: pd.DataFrame) -> str:
        # Modelin eğitimi sırasında kullandığımız TÜM sütunları vermeliyiz
        features = df[[
            'SMA_50', 'EMA_20', 
            'RSI', 
            'MACD', 'MACD_SIGNAL', 
            'STOCH_K', 'STOCH_D', 
            'BB_UPPER', 'BB_LOWER'
        ]].tail(1)
        
        try:
            prediction = self.model.predict(features)
            proba = self.model.predict_proba(features) # Emin olma oranı
            
            guven = proba[0][1] if prediction[0] == 1 else proba[0][0]
            
            if prediction[0] == 1:
                return f"AL (AI Güveni: %{guven*100:.1f})"
            else:
                return f"SAT (AI Güveni: %{guven*100:.1f})"
        except Exception as e:
            return "BEKLE (Hata)"