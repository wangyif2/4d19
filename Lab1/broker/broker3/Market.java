import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * User: robert
 * Date: 11/01/13
 */
public class Market {
    private static HashMap<String, Long> stocks;
    private static Market myMarket = null;
    private static String myMarketName;

    public static Market getInstance() throws IOException {
        if (myMarket == null) {
            //myMarket = new Market(OnlineBroker.MKT_NAME);
            myMarket = new Market("nasdaq");
        } else
            readMarket();

        return myMarket;
    }

    private Market(String marketName) throws IOException {
        myMarketName = marketName;
        readMarket();
    }

    private static void readMarket() throws IOException {
        stocks = new HashMap<String, Long>();

        BufferedReader bufferedReader = new BufferedReader(new FileReader(myMarketName));
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

    private void updateMarket(HashMap<String, Long> stocks) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(myMarketName));

        String line = "";

        for (Map.Entry<String, Long> stock : stocks.entrySet()) {
            Map.Entry pairs = (Map.Entry) stock;
            line += pairs.getKey() + " " + pairs.getValue().toString() + "\n";
        }

        bufferedWriter.write(line);
        bufferedWriter.close();
    }

    public Long lookUpStock(String symbol) {
        return stocks.containsKey(symbol) ? stocks.get(symbol) : null;
    }

    public void addStock(String symbol) throws IOException {
        stocks.put(symbol, (long) 0);
        updateMarket(stocks);
    }

    public void updateStock(String symbol, long price) throws IOException {
        stocks.put(symbol, price);
        updateMarket(stocks);
    }

    public void removeStock(String symbol) throws IOException {
        stocks.remove(symbol);
        updateMarket(stocks);
    }

    public class Stock {
        @Override
        public String toString() {
            return "Stock{" +
                    "stockName='" + stockName + '\'' +
                    ", stockPrice=" + stockPrice +
                    '}';
        }

        private String stockName;
        private long stockPrice;

        public Stock(String name, long price) {
            stockName = name;
            stockPrice = price;
        }

        public String getStockName() {
            return stockName;
        }

        public void setStockName(String stockName) {
            this.stockName = stockName;
        }

        public long getStockPrice() {
            return stockPrice;
        }

        public void setStockPrice(long stockPrice) {
            this.stockPrice = stockPrice;
        }
    }
}
