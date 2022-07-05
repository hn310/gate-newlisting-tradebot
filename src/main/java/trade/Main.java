package trade;

import io.gate.gateapi.ApiException;

public class Main {
    public static final double USDT_FUND = 20.0;

    static final String[] IPs = { "https://54.249.10.54/api/v4", "https://54.65.90.250/api/v4",
            "https://52.194.32.54/api/v4", "https://52.198.110.169/api/v4", "https://35.75.200.114/api/v4",
            "https://54.150.2.137/api/v4", "https://52.192.137.0/api/v4", "https://3.114.10.13/api/v4",
            "https://52.193.165.155/api/v4", "https://13.114.18.125/api/v4", "https://3.114.87.146/api/v4",
            "https://18.177.37.64/api/v4", "https://18.177.66.24/api/v4" };

    public static void main(String[] args) throws ApiException, InterruptedException {
        SpotTrade spotTrade = new SpotTrade();
//        spotTrade.printUpcomingListing();

        // ****** WARNING: comment out this method after every run ******
        String currencyPair = args[0];
//        String currencyPair = "SRT_USDT";

        for (String ip : IPs) {
            MultiThreadTrade t = new MultiThreadTrade(ip, currencyPair, spotTrade);
            new Thread(t).start();
        }
    }
}
