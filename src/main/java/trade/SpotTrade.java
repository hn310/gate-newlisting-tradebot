package trade;

import java.util.List;

import io.gate.gateapi.ApiClient;
import io.gate.gateapi.ApiException;
import io.gate.gateapi.api.SpotApi;
import io.gate.gateapi.models.CurrencyPair;
import io.gate.gateapi.models.CurrencyPair.TradeStatusEnum;
import io.gate.gateapi.models.Order;
import io.gate.gateapi.models.Ticker;

class SpotTrade {
    private SpotApi spotApi;

    public SpotTrade() {
        // Initialize API client
        ApiClient client = new ApiClient();
        client.setApiKeySecret(Config.API_KEY, Config.API_SECRET);
        this.spotApi = new SpotApi(client);
    }

    public void printUpcomingListing() throws ApiException {
        List<CurrencyPair> cps = spotApi.listCurrencyPairs();
        for (CurrencyPair cp : cps) {
            if (cp.getTradeStatus().equals(TradeStatusEnum.SELLABLE)) {
                System.out.println(cp.getBase() + "_" + cp.getQuote());
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

    public TradeStatusEnum getTradeStatus(String currencyPair) throws InterruptedException, ApiException {
        TradeStatusEnum currentTradeStatus = this.spotApi.getCurrencyPair(currencyPair).getTradeStatus();
        return currentTradeStatus;
    }

    public boolean isNearBuyTime(long currentUnixTimestamp, String currencyPair) throws ApiException {
        long tokenBuyStartTime = getBuyStartTime(currencyPair);
        return (currentUnixTimestamp + 10) > tokenBuyStartTime; // 10s before buy start time
    }

    public int getAmountPrecision(String currencyPair) throws ApiException {
        return this.spotApi.getCurrencyPair(currencyPair).getAmountPrecision();
    }

    public int getPricePrecision(String currencyPair) throws ApiException {
        return this.spotApi.getCurrencyPair(currencyPair).getPrecision();
    }
}
