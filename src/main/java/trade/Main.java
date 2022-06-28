package trade;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.gate.gateapi.ApiException;
import io.gate.gateapi.models.CurrencyPair.TradeStatusEnum;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws ApiException, InterruptedException {
        LOGGER.log(Level.INFO, "Start main method");
        String currencyPair = "PTS_USDT";
        Double amountOfUsdt = 10.0;

        SpotTrade spotTrade = new SpotTrade();
//        spotTrade.printUpcomingListing();

        int amountPrecision = spotTrade.getAmountPrecision(currencyPair);
        String amountPrecisionFormat = "%." + amountPrecision + "f";
        int pricePrecision = spotTrade.getPricePrecision(currencyPair);
        String pricePrecisionFormat = "%." + pricePrecision + "f";

        // Đợi đến trước khi mở bán tầm 5s để biết giá ask thấp nhất
//        long currentUnixTimestamp = Instant.now().getEpochSecond();
//        while (!spotTrade.isNearBuyTime(currentUnixTimestamp, currencyPair)) {
//           Thread.sleep(4500); // sleep 4.5s
//            currentUnixTimestamp = Instant.now().getEpochSecond();
//        }

        // It's buy time !!!
        // Lấy giá ask thấp nhất * 2 sẽ ra giá mình định mua
        Double lowestAsk = spotTrade.getLowestAsk(currencyPair);
        String buyPrice = String.format(pricePrecisionFormat, lowestAsk * 2);
        String buyAmount = String.format(amountPrecisionFormat, amountOfUsdt / Double.parseDouble(buyPrice));

        // Nếu mở bán thì status sẽ là tradable
        System.out.println("Start check status");
        boolean isTradable = TradeStatusEnum.TRADABLE.equals(spotTrade.getTradeStatus(currencyPair));
        while (!isTradable) {
            Thread.sleep(1); // max 900 requests/seconds -> retry every 1000ms/900 requests = 1ms
            isTradable = TradeStatusEnum.TRADABLE.equals(spotTrade.getTradeStatus(currencyPair));
        }
        // Thực hiện mua khi mở bán
        spotTrade.createBuyOrder(currencyPair, buyAmount, buyPrice);

        // loop để check số lượng token mình hiện đang có, nếu > 0 sẽ sell = x2 giá mua
    }
}
