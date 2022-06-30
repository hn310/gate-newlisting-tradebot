package trade;

import java.time.Instant;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.gate.gateapi.ApiException;
import io.gate.gateapi.models.CurrencyPair.TradeStatusEnum;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws ApiException, InterruptedException {
        SpotTrade spotTrade = new SpotTrade();
//        spotTrade.printUpcomingListing();

        // ****** WARNING: comment out this method after every run ******
        startTrade(spotTrade);
    }
    
    private static void startTrade(SpotTrade spotTrade) throws ApiException, InterruptedException {
        // define trade pair
        String currencyPair = "ZTG_USDT";
        logger.info("trade pair: " + currencyPair);
        String quoteCurrency = currencyPair.split("_")[0]; // get quote currency
        double totalFundInUsdt = 50.0;

        int amountPrecision = spotTrade.getAmountPrecision(currencyPair);
        String amountPrecisionFormat = "%." + amountPrecision + "f";
        int pricePrecision = spotTrade.getPricePrecision(currencyPair);
        String pricePrecisionFormat = "%." + pricePrecision + "f";

        // Đợi đến trước khi mở bán tầm 5s để biết giá ask thấp nhất
        long currentUnixTimestamp = Instant.now().getEpochSecond();
//        while (!spotTrade.isNearBuyTime(currentUnixTimestamp, currencyPair)) {
//            Thread.sleep(4500); // sleep 4.5s
//            currentUnixTimestamp = Instant.now().getEpochSecond();
//        }

        // It's buy time !!!
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
        boolean isTradable = TradeStatusEnum.TRADABLE.equals(spotTrade.getTradeStatus(currencyPair));
//        while (!isTradable) {
//            Thread.sleep(1); // max 900 requests/seconds -> retry every 1000ms/900 requests = 1ms
//            isTradable = TradeStatusEnum.TRADABLE.equals(spotTrade.getTradeStatus(currencyPair));
//        }

        // Thực hiện mua khi mở bán
        spotTrade.createBulkBuyOrder(currencyPair, buyAmounts, buyPrices);

        // check số lượng token nếu > 0 sẽ sell = x3,5,7,9 giá lowestAsk
        double availableQuoteCurrency = spotTrade.getAvailableAmount(quoteCurrency);
//        while (availableQuoteCurrency == 0) {
//            // sleep vài ms để đợi khớp lệnh
//            Thread.sleep(1);
//            availableQuoteCurrency = spotTrade.getAvailableAmount(quoteCurrency);
//        }
        List<String> sellAmounts = spotTrade.createSellAmounts(availableQuoteCurrency, sellPrices,
                amountPrecisionFormat);
        spotTrade.createBulkSellOrder(currencyPair, sellAmounts, sellPrices);
        logger.info("Finish startTrade()");
    }
}
