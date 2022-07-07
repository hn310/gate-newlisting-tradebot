package trade;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.gate.gateapi.ApiClient;
import io.gate.gateapi.ApiException;
import io.gate.gateapi.GateApiException;
import io.gate.gateapi.api.SpotApi;
import io.gate.gateapi.models.BatchOrder;
import io.gate.gateapi.models.CurrencyPair;
import io.gate.gateapi.models.CurrencyPair.TradeStatusEnum;
import io.gate.gateapi.models.Order;
import io.gate.gateapi.models.SpotAccount;
import io.gate.gateapi.models.Ticker;

class SpotTrade {
    private static final Logger logger = LogManager.getLogger(SpotTrade.class);
    ApiClient client;

    private SpotApi spotApi;
    private double[] buyMultipliers = { 2.0, 2.5 };
    private double[] sellMultipliers = { 3.5, 4.5 };

    public SpotTrade() {
        // Initialize API client
        this.client = new ApiClient();
        this.client.setApiKeySecret(Config.API_KEY, Config.API_SECRET);
        this.client.setBasePath("https://52.198.110.169/api/v4");
        this.spotApi = new SpotApi(client);
    }

    public void printUpcomingListing() throws ApiException {
        List<CurrencyPair> cps = spotApi.listCurrencyPairs();
        for (CurrencyPair cp : cps) {
            if (cp.getTradeStatus().equals(TradeStatusEnum.BUYABLE)
                    || cp.getTradeStatus().equals(TradeStatusEnum.SELLABLE)) {
                ZonedDateTime buyStartTime = Instant.ofEpochMilli(cp.getBuyStart() * 1000)
                        .atZone(ZoneId.of("Asia/Bangkok"));
                System.out.println(cp.getBase() + "_" + cp.getQuote() + ". Status: " + cp.getTradeStatus());
                System.out.println("Buy Start Time: " + buyStartTime);
                System.out.println();
            }
        }
        System.out.println("Finish printUpcomingListing()!!");
        logger.info("Finish printUpcomingListing()!!");
        System.exit(0);
    }

    public double getLow24h(String currencyPair) throws ApiException {
        List<Ticker> tickers = spotApi.listTickers().currencyPair(currencyPair).execute();
        return Double.parseDouble(tickers.get(0).getLow24h());
    }

    public double getHigh24h(String currencyPair) throws ApiException {
        List<Ticker> tickers = spotApi.listTickers().currencyPair(currencyPair).execute();
        return Double.parseDouble(tickers.get(0).getHigh24h());
    }

    public double getLowestAsk(String currencyPair) throws ApiException {
        List<Ticker> tickers = spotApi.listTickers().currencyPair(currencyPair).execute();
        return Double.parseDouble(tickers.get(0).getLowestAsk());
    }

    public long getBuyStartTime(String currencyPair) throws ApiException {
        return spotApi.getCurrencyPair(currencyPair).getBuyStart();
    }

    public void createSellOrder(String currencyPair, String sellAmount, String sellPrice) throws ApiException {
        do {
            try {
                Order order = new Order();
                order.setAccount(Order.AccountEnum.SPOT);
                order.setAutoBorrow(false);
                order.setTimeInForce(Order.TimeInForceEnum.GTC);
                order.setType(Order.TypeEnum.LIMIT);
                order.setAmount(sellAmount);
                order.setPrice(sellPrice);
                order.setSide(Order.SideEnum.SELL);
                order.setCurrencyPair(currencyPair);
                this.spotApi.createOrder(order);
                Order result = this.spotApi.createOrder(order);

                logger.info("side: " + result.getSide() + ", create_time_ms: " + result.getCreateTimeMs() + ", amount: "
                        + result.getAmount() + ", price: " + result.getPrice() + ", filled price: "
                        + result.getFillPrice() + ", filled total: " + result.getFilledTotal());
            } catch (GateApiException gae) {
                logger.error(gae.getMessage());
                if (gae.getErrorLabel().equals("INVALID_CURRENCY")) {
                    continue;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            break;
        } while (true);
    }

    public void createBuyOrder(String currencyPair, String buyAmount, String buyPrice) {
        do {
            try {
                Order order = new Order();
                order.setAccount(Order.AccountEnum.SPOT);
                order.setAutoBorrow(false);
                order.setTimeInForce(Order.TimeInForceEnum.GTC);
                order.setType(Order.TypeEnum.LIMIT);
                order.setAmount(buyAmount);
                order.setPrice(buyPrice);
                order.setSide(Order.SideEnum.BUY);
                order.setCurrencyPair(currencyPair);
                Order result = this.spotApi.createOrder(order);

                logger.info("side: " + result.getSide() + ", create_time_ms: " + result.getCreateTimeMs() + ", amount: "
                        + result.getAmount() + ", price: " + result.getPrice() + ", filled price: "
                        + result.getFillPrice() + ", filled total: " + result.getFilledTotal());
            } catch (GateApiException gae) {
                logger.error(gae.getMessage());
                if (gae.getErrorLabel().equals("INVALID_CURRENCY")) {
                    continue;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            break;
        } while (true);
    }

    public void createBulkSellOrder(String currencyPair, List<String> sellAmounts, List<String> sellPrices) {
        try {
            long currentUnixTimestamp = Instant.now().getEpochSecond();
            List<Order> orders = new ArrayList<Order>();
            for (int i = 0; i < sellAmounts.size(); i++) {
                Order order = new Order();
                order.setText("t-" + currentUnixTimestamp + "-" + i);
                order.setAccount(Order.AccountEnum.SPOT);
                order.setAutoBorrow(false);
                order.setTimeInForce(Order.TimeInForceEnum.GTC);
                order.setType(Order.TypeEnum.LIMIT);
                order.setAmount(sellAmounts.get(i));
                order.setPrice(sellPrices.get(i));
                order.setSide(Order.SideEnum.SELL);
                order.setCurrencyPair(currencyPair);
                orders.add(order);
            }
            logger.info("current time before creating bulk sell requests: " + currentUnixTimestamp);
            List<BatchOrder> batchOrders = this.spotApi.createBatchOrders(orders);
            for (BatchOrder bo : batchOrders) {
                logger.info("side: " + bo.getSide() + ", text: " + bo.getText() + ", succeeded: " + bo.getSucceeded()
                        + ", label: " + bo.getLabel() + ", message: " + bo.getMessage() + ", create_time_ms: "
                        + bo.getCreateTimeMs() + ", amount: " + bo.getAmount() + ", price: " + bo.getPrice()
                        + ", filled price: " + bo.getFillPrice() + ", filled total: " + bo.getFilledTotal());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void createBulkBuyOrder(String currencyPair, List<String> buyAmounts, List<String> buyPrices) {
        try {
            long currentUnixTimestamp = Instant.now().getEpochSecond();
            List<Order> orders = new ArrayList<Order>();
            for (int i = 0; i < buyAmounts.size(); i++) {
                Order order = new Order();
                order.setText("t-" + currentUnixTimestamp + "-" + i);
                order.setAccount(Order.AccountEnum.SPOT);
                order.setAutoBorrow(false);
                order.setTimeInForce(Order.TimeInForceEnum.GTC);
                order.setType(Order.TypeEnum.LIMIT);
                order.setAmount(buyAmounts.get(i));
                order.setPrice(buyPrices.get(i));
                order.setSide(Order.SideEnum.BUY);
                order.setCurrencyPair(currencyPair);
                orders.add(order);
            }
            logger.info("current time before creating bulk buy requests: " + currentUnixTimestamp);
            List<BatchOrder> batchOrders = this.spotApi.createBatchOrders(orders);
            for (BatchOrder bo : batchOrders) {
                logger.info("side: " + bo.getSide() + ", text: " + bo.getText() + ", succeeded: " + bo.getSucceeded()
                        + ", label: " + bo.getLabel() + ", message: " + bo.getMessage() + ", create_time_ms: "
                        + bo.getCreateTimeMs() + ", amount: " + bo.getAmount() + ", price: " + bo.getPrice()
                        + ", filled price: " + bo.getFillPrice() + ", filled total: " + bo.getFilledTotal());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public List<String> createBuyPrices(double lowestAsk, String pricePrecisionFormat) {
        List<String> buyPrices = new ArrayList<String>();
        for (double multiplier : buyMultipliers) {
            buyPrices.add(String.format(pricePrecisionFormat, lowestAsk * multiplier));
        }
        return buyPrices;
    }

    public List<String> createBuyAmounts(double totalFundInUsdt, List<String> buyPrices, String amountPrecisionFormat) {
        double tradeAmount = totalFundInUsdt / this.buyMultipliers.length;
        List<String> buyAmounts = new ArrayList<String>();
        for (int i = 0; i < this.buyMultipliers.length; i++) {
            buyAmounts.add(String.format(amountPrecisionFormat, tradeAmount / Double.parseDouble(buyPrices.get(i))));
        }
        return buyAmounts;
    }

    public List<String> createSellPrices(double lowestAsk, String pricePrecisionFormat) {
        List<String> sellPrices = new ArrayList<String>();
        for (double sellMultiplier : this.sellMultipliers) {
            sellPrices.add(String.format(pricePrecisionFormat, lowestAsk * sellMultiplier));
        }
        return sellPrices;
    }

    public List<String> createSellAmounts(double availableBaseCurrency, List<String> sellPrices,
            String amountPrecisionFormat) {
        double tradeAmount = availableBaseCurrency / this.sellMultipliers.length;
        List<String> sellAmounts = new ArrayList<String>();
        for (int i = 0; i < this.sellMultipliers.length; i++) {
            sellAmounts.add(String.format(amountPrecisionFormat, tradeAmount / Double.parseDouble(sellPrices.get(i))));
        }
        return sellAmounts;
    }

    public TradeStatusEnum getTradeStatus(String currencyPair) throws InterruptedException, ApiException {
        TradeStatusEnum currentTradeStatus = this.spotApi.getCurrencyPair(currencyPair).getTradeStatus();
        return currentTradeStatus;
    }

    public boolean isNearBuyTime(long currentUnixTimestamp, String currencyPair) throws ApiException {
        long tokenBuyStartTime = getBuyStartTime(currencyPair);
        boolean isJustBeforeBuyStartTime = currentUnixTimestamp > (tokenBuyStartTime - 5); // 5s before buy start time
        boolean isAfterStartBuyTime = currentUnixTimestamp < (tokenBuyStartTime + 60); // < 60s after buy start time
        return isJustBeforeBuyStartTime && isAfterStartBuyTime;
    }

    public int getAmountPrecision(String currencyPair) throws ApiException {
        return this.spotApi.getCurrencyPair(currencyPair).getAmountPrecision();
    }

    public int getPricePrecision(String currencyPair) throws ApiException {
        return this.spotApi.getCurrencyPair(currencyPair).getPrecision();
    }

    public double getAvailableAmount(String baseCurrency) throws ApiException {
        List<SpotAccount> accounts = spotApi.listSpotAccounts().currency(baseCurrency).execute();
        return Double.parseDouble(accounts.get(0).getAvailable());
    }

    public void setBasePath(String basePath) {
        this.client.setBasePath(basePath);
        this.spotApi = new SpotApi(this.client);
    }

    public double getAvailableUsdt() throws ApiException {
        List<SpotAccount> accounts = this.spotApi.listSpotAccounts().currency("USDT").execute();
        return Double.parseDouble(accounts.get(0).getAvailable());
    }
}
