package trade;

import java.time.Instant;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.gate.gateapi.models.CurrencyPair.TradeStatusEnum;

public class MultiThreadTrade implements Runnable {
    private static final Logger logger = LogManager.getLogger(MultiThreadTrade.class);

    private SpotTrade spotTrade;
    private String currencyPair;

    public MultiThreadTrade(String basePath, String currencyPair, SpotTrade spotTrade) {
        this.spotTrade = spotTrade;
        this.spotTrade.setBasePath(basePath);
        this.currencyPair = currencyPair;
    }

    @Override
    public void run() {
        try {
            logger.info("-------------------------------------------------------------");
            logger.info("Start startTrade()");
            // define trade pair
            logger.info("trade pair: " + currencyPair);
            String baseCurrency = currencyPair.split("_")[0]; // get base currency ETH/USDT -> base: ETH, quote: USDT
            double totalFundInUsdt = 20.0;

            int amountPrecision = spotTrade.getAmountPrecision(currencyPair);
            String amountPrecisionFormat = "%." + amountPrecision + "f";
            int pricePrecision = spotTrade.getPricePrecision(currencyPair);
            String pricePrecisionFormat = "%." + pricePrecision + "f";

            // Đợi đến trước khi mở bán tầm 5s để biết giá ask thấp nhất
            long currentUnixTimestamp = Instant.now().getEpochSecond();
            while (!spotTrade.isNearBuyTime(currentUnixTimestamp, currencyPair)) {
                Thread.sleep(2000); // sleep 2s
                currentUnixTimestamp = Instant.now().getEpochSecond();
            }

            // It's buy time !!!
            logger.info("It's buy time !!!");
            // bon chen thử đặt lệnh với giá thấp nhất, rồi sau đó lũy tiến dần
            double lowestAsk = spotTrade.getLowestAsk(currencyPair);
            logger.info("lowest ask: " + lowestAsk);
            List<String> buyPrices = spotTrade.createBuyPrices(lowestAsk, pricePrecisionFormat);
            logger.info("buyPrices: " + String.join(", ", buyPrices));
            List<String> buyAmounts = spotTrade.createBuyAmounts(totalFundInUsdt, buyPrices, amountPrecisionFormat);
            logger.info("buyAmounts: " + String.join(", ", buyAmounts));
            List<String> sellPrices = spotTrade.createSellPrices(lowestAsk, pricePrecisionFormat);
            logger.info("sellPrices: " + String.join(", ", sellPrices));

            // Nếu mở bán thì status sẽ là tradable
            while (!Main.IS_TRADABLE) {
                Thread.sleep(5); // max 900 requests/seconds -> retry every 1000ms/900 requests = 1ms
                Main.IS_TRADABLE = TradeStatusEnum.TRADABLE.equals(spotTrade.getTradeStatus(currencyPair));
            }
            logger.info("is tradable !!!");

            // Thực hiện mua khi mở bán
            while(!Main.HAS_BOUGHT) {
                spotTrade.createBulkBuyOrder(currencyPair, buyAmounts, buyPrices);
            }

            // check số lượng token nếu > 0 sẽ sell = x3,5,7,9 giá lowestAsk
            double availableBaseCurrency = spotTrade.getAvailableAmount(baseCurrency);
            while (availableBaseCurrency == 0) {
                // sleep vài ms để đợi khớp lệnh
                Thread.sleep(5);
                availableBaseCurrency = spotTrade.getAvailableAmount(baseCurrency);
            }
            List<String> sellAmounts = spotTrade.createSellAmounts(availableBaseCurrency, sellPrices,
                    amountPrecisionFormat);
            spotTrade.createBulkSellOrder(currencyPair, sellAmounts, sellPrices);
            logger.info("sellAmounts: " + String.join(", ", sellAmounts));
            logger.info("Finish startTrade()");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
