package trade;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.gate.gateapi.ApiException;

public class Main {
    public static volatile boolean IS_TRADABLE = false;
    public static volatile boolean HAS_BOUGHT = false;
    public static volatile boolean HAS_SOLD = false;
    public static final double USDT_FUND = 20.0;
    
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws ApiException, InterruptedException {
        SpotTrade spotTrade = new SpotTrade();
//        spotTrade.printUpcomingListing();

        // ****** WARNING: comment out this method after every run ******
        String currencyPair = args[0];
//        String currencyPair = "TIFI_USDT";
        MultiThreadTrade t1 = new MultiThreadTrade("https://54.249.10.54/api/v4", currencyPair, spotTrade);
        MultiThreadTrade t2 = new MultiThreadTrade("https://54.65.90.250/api/v4", currencyPair, spotTrade);
        MultiThreadTrade t3 = new MultiThreadTrade("https://52.194.32.54/api/v4", currencyPair, spotTrade);
        MultiThreadTrade t4 = new MultiThreadTrade("https://52.198.110.169/api/v4", currencyPair, spotTrade);
        MultiThreadTrade t5 = new MultiThreadTrade("https://35.75.200.114/api/v4", currencyPair, spotTrade);
        MultiThreadTrade t6 = new MultiThreadTrade("https://54.150.2.137/api/v4", currencyPair, spotTrade);
        MultiThreadTrade t7 = new MultiThreadTrade("https://52.192.137.0/api/v4", currencyPair, spotTrade);
        MultiThreadTrade t8 = new MultiThreadTrade("https://3.114.10.13/api/v4", currencyPair, spotTrade);
        
        new Thread(t1).start();
        new Thread(t2).start();
        new Thread(t3).start();
        new Thread(t4).start();
        new Thread(t5).start();
        new Thread(t6).start();
        new Thread(t7).start();
        new Thread(t8).start();
    }
}
