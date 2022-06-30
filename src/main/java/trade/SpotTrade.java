package trade;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import io.gate.gateapi.ApiClient;
import io.gate.gateapi.ApiException;
import io.gate.gateapi.api.SpotApi;
import io.gate.gateapi.models.CurrencyPair;
import io.gate.gateapi.models.CurrencyPair.TradeStatusEnum;
import io.gate.gateapi.models.Order;
import io.gate.gateapi.models.SpotAccount;
import io.gate.gateapi.models.Ticker;

class SpotTrade {
    private SpotApi spotApi;
    private int buyIntervalNumber = 5; // divide fund into n equal orders
    private int sellIntervalNumber = 4; // x3, x5, x7, x9

    public SpotTrade() {
        // Initialize API client
        ApiClient client = new ApiClient();
        client.setApiKeySecret(Config.API_KEY, Config.API_SECRET);
        this.spotApi = new SpotApi(client);
    }

    public void printUpcomingListing() throws ApiException {
        List<CurrencyPair> cps = spotApi.listCurrencyPairs();
        for (CurrencyPair cp : cps) {
            if (cp.getTradeStatus().equals(TradeStatusEnum.BUYABLE) || cp.getTradeStatus().equals(TradeStatusEnum.SELLABLE)) {
                ZonedDateTime buyStartTime = Instant.ofEpochMilli(cp.getBuyStart()*1000).atZone(ZoneId.of("Asia/Bangkok"));
                System.out.println(cp.getBase() + "_" + cp.getQuote() + ". Status: " + cp.getTradeStatus());
                System.out.println("Buy Start Time: " + buyStartTime);
                System.out.println();
            }
        }
        System.out.println("Finish printUpcomingListing()!!");
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
    }

    public void createBuyOrder(String currencyPair, String buyAmount, String buyPrice) throws ApiException {
        Order order = new Order();
        order.setAccount(Order.AccountEnum.SPOT);
        order.setAutoBorrow(false);
        order.setTimeInForce(Order.TimeInForceEnum.GTC);
        order.setType(Order.TypeEnum.LIMIT);
        order.setAmount(buyAmount);
        order.setPrice(buyPrice);
        order.setSide(Order.SideEnum.BUY);
        order.setCurrencyPair(currencyPair);
        this.spotApi.createOrder(order);
    }

    public void createBulkSellOrder(String currencyPair, List<String> sellAmounts, List<String> sellPrices)
            throws ApiException {
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
        this.spotApi.createBatchOrders(orders);
    }

    public void createBulkBuyOrder(String currencyPair, List<String> buyAmounts, List<String> buyPrices)
            throws ApiException {
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
        this.spotApi.createBatchOrders(orders);
    }

    public List<String> createBuyPrices(double lowestAsk, String pricePrecisionFormat) {
        double multiplier = 1.0;
        List<String> buyPrices = new ArrayList<String>();
        for (int i = 0; i < this.buyIntervalNumber; i++) {
            buyPrices.add(String.format(pricePrecisionFormat, lowestAsk * multiplier));
            multiplier += 0.3; // 1.0 ~ 3.0
        }
        return buyPrices;
    }

    public List<String> createBuyAmounts(double totalFundInUsdt, List<String> buyPrices, String amountPrecisionFormat) {
        double tradeAmount = totalFundInUsdt / this.buyIntervalNumber;
        List<String> buyAmounts = new ArrayList<String>();
        for (int i = 0; i < this.buyIntervalNumber; i++) {
            buyAmounts.add(String.format(amountPrecisionFormat, tradeAmount / Double.parseDouble(buyPrices.get(i))));
        }
        return buyAmounts;
    }

    public List<String> createSellPrices(double lowestAsk, String pricePrecisionFormat) {
        double multiplier = 3.0;
        List<String> sellPrices = new ArrayList<String>();
        for (int i = 0; i < this.sellIntervalNumber; i++) {
            sellPrices.add(String.format(pricePrecisionFormat, lowestAsk * multiplier));
            multiplier += 2; // x3, x5, x7, x9
        }
        return sellPrices;
    }

    public List<String> createSellAmounts(double availableQuoteCurrency, List<String> sellPrices,
            String amountPrecisionFormat) {
        double tradeAmount = availableQuoteCurrency / this.sellIntervalNumber;
        List<String> sellAmounts = new ArrayList<String>();
        for (int i = 0; i < this.sellIntervalNumber; i++) {
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
        boolean isJustBeforeBuyStartTime = currentUnixTimestamp > (tokenBuyStartTime - 10); // 10s before buy start time
        boolean isAfterStartBuyTime = currentUnixTimestamp < (tokenBuyStartTime + 60); // < 60s after buy start time
        return isJustBeforeBuyStartTime && isAfterStartBuyTime;
    }

    public int getAmountPrecision(String currencyPair) throws ApiException {
        return this.spotApi.getCurrencyPair(currencyPair).getAmountPrecision();
    }

    public int getPricePrecision(String currencyPair) throws ApiException {
        return this.spotApi.getCurrencyPair(currencyPair).getPrecision();
    }

    public double getAvailableAmount(String quoteCurrency) throws ApiException {
        List<SpotAccount> accounts = spotApi.listSpotAccounts().currency(quoteCurrency).execute();
        return Double.parseDouble(accounts.get(0).getAvailable());
    }
}
