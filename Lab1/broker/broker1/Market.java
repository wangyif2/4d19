import java.io.*;
import java.util.Arrays;
import java.util.HashMap;

/**
 * User: robert
 * Date: 11/01/13
 */
public class Market {
    private static HashMap<String, Long> stocks;
    private static Market myMarket = null;

    public static Market getInstance() throws IOException {
        if (myMarket == null)
            myMarket = new Market(OnlineBroker.MKT_NAME);
        else
            updateMarketStockPrice(OnlineBroker.MKT_NAME);

        return myMarket;
    }

    private Market(String marketName) throws IOException {
        updateMarketStockPrice(marketName);
    }

    private static void updateMarketStockPrice(String marketName) throws IOException {
        stocks = new HashMap<String, Long>();

        BufferedReader bufferedReader = new BufferedReader(new FileReader(marketName));
        String line;

        try {
            while ((line = bufferedReader.readLine()) != null) {
                String[] stock = line.split(" ");

                if (OnlineBroker.DEBUG) System.out.println("Added Stock: " + Arrays.toString(stock));
                stocks.put(stock[0], Long.parseLong(stock[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            bufferedReader.close();
        }
    }

    public Long lookUpStock(String symbol) {
        return stocks.containsKey(symbol) ? stocks.get(symbol) : 0;
    }
}
