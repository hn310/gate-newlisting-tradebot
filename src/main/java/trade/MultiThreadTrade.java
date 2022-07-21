package trade;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MultiThreadTrade implements Runnable {
    private static final Logger logger = LogManager.getLogger(MultiThreadTrade.class);

    private static final double buyMultiplier = 2.5;
    private static final double sellMultiplier = 4;

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

            int amountPrecision = spotTrade.getAmountPrecision(currencyPair);
            String amountPrecisionFormat = "%." + amountPrecision + "f";
            int pricePrecision = spotTrade.getPricePrecision(currencyPair);
            String pricePrecisionFormat = "%." + pricePrecision + "f";

            // Đợi đến trước khi mở bán tầm 5s để biết giá ask thấp nhất
            long buyStartTimeInMillis = spotTrade.getBuyStartTime(currencyPair);
            while (Instant.now().toEpochMilli() < (buyStartTimeInMillis - 5000)) {
                Thread.sleep(2000); // sleep 2s
            }

            // It's buy time !!!
            logger.info("It's buy time !!!");
            // bon chen thử đặt lệnh với giá thấp nhất, rồi sau đó lũy tiến dần
            double lowestAsk = spotTrade.getLowestAsk(currencyPair);
            logger.info("lowest ask: " + lowestAsk);
            String buyPrice = String.format(pricePrecisionFormat, lowestAsk * buyMultiplier);
            logger.info("buyPrice: " + buyPrice);
            String buyAmount = String.format(amountPrecisionFormat, Main.USDT_FUND / Double.parseDouble(buyPrice));
            logger.info("buyAmount: " + buyAmount);
            String sellPrice = String.format(pricePrecisionFormat, lowestAsk * sellMultiplier);
            logger.info("sellPrice: " + sellPrice);

            // Thực hiện spam mua trước khi mở bán 200ms
            boolean isBuyTime = false;
            while (!isBuyTime) {
                if (Instant.now().toEpochMilli() >= (buyStartTimeInMillis - 200)) {
                    isBuyTime = true;
                    while (spotTrade.getAvailableUsdt() >= Main.USDT_FUND) {
                        // single buy order
//                        spotTrade.createBuyOrder(currencyPair, buyAmount, buyPrice);

                        // bulk buy order
                        List<String> buyAmounts = Arrays.asList(buyAmount, buyAmount, buyAmount);
                        List<String> buyPrices = Arrays.asList(buyPrice, buyPrice, buyPrice);
                        spotTrade.createBulkBuyOrder(currencyPair, buyAmounts, buyPrices);
                        Thread.sleep(50);
                    }
                }
            }

            logger.info("Finished buy, start sell");
            // check số lượng token nếu > 0 sẽ sell = x3,5 giá lowestAsk
            double availableBaseCurrency = spotTrade.getAvailableAmount(baseCurrency);
            while (availableBaseCurrency == 0) {
                // sleep vài ms để đợi khớp lệnh
                Thread.sleep(20);
                availableBaseCurrency = spotTrade.getAvailableAmount(baseCurrency);
            }

            // single sell order
//            spotTrade.createSellOrder(currencyPair, String.format(amountPrecisionFormat, availableBaseCurrency),
//                    sellPrice);

            // bulk sell order
            String sellAmount = String.format(amountPrecisionFormat, availableBaseCurrency);
            List<String> sellAmounts = Arrays.asList(sellAmount, sellAmount, sellAmount);
            List<String> sellPrices = Arrays.asList(sellPrice, sellPrice, sellPrice);
            spotTrade.createBulkSellOrder(currencyPair, sellAmounts, sellPrices);
            logger.info("availableBaseCurrency: " + availableBaseCurrency);
            logger.info("Finish startTrade()");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
