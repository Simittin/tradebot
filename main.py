import config
from src.data_loader import load_data
from src.processor import process_data
from src.ai_model import train_model
# Yeni stratejileri import et
from src.strategies import MacdStrategy, BollingerStrategy, AiStrategy
from src.market_observer import MarketData, TraderBot

def main():
    print("=== BOT ORDUSU HAZIRLANIYOR ===")

    # 1. Veri Hazırla (Genişletilmiş İndikatörlerle)
    load_data(config.SYMBOL, config.PERIOD, config.INTERVAL, config.RAW_DATA_PATH)
    islenmis_veri = process_data(
        config.RAW_DATA_PATH, 
        config.PROCESSED_DATA_PATH,
        config.RSI_PERIOD,
        config.SMA_PERIOD
    )

    # 2. Yapay Zekayı Eğit (Tüm indikatörleri öğrensin)
    ai_model = train_model(config.PROCESSED_DATA_PATH, config.MODEL_PATH)

    # 3. Piyasayı Kur
    market = MarketData()

    # 4. BOTLARI YARAT (Farklı Stratejilerle)
    
    # Bot 1: Trend Takipçisi (Sadece MACD'ye bakar)
    bot_trend = TraderBot("Trend Avcısı (MACD)", MacdStrategy())
    
    # Bot 2: Scalper (Bollinger bantlarına göre hızlı al-sat yapar)
    bot_scalp = TraderBot("Scalper (Bollinger)", BollingerStrategy())
    
    # Bot 3: Yapay Zeka (Her şeye bakıp karar verir)
    bot_ai = TraderBot("Yapay Zeka (General)", AiStrategy(ai_model))

    # 5. Hepsini Piyasaya Bağla (Observer Pattern)
    market.attach(bot_trend)
    market.attach(bot_scalp)
    market.attach(bot_ai)

    # 6. Simülasyon: Piyasaya veri verelim
    print("\n>>> PİYASA TETİKLENİYOR...")
    market.set_new_data(islenmis_veri)

if __name__ == "__main__":
    main()