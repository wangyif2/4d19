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

    public static Market getInstance(String exchange) throws IOException {
        if (myMarket == null)
            myMarket = new Market();

        readMarket(exchange);

        return myMarket;
    }

    private static void readMarket(String exchange) throws IOException {
        stocks = new HashMap<String, Long>();

        BufferedReader bufferedReader = new BufferedReader(new FileReader(exchange));
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

    private void updateMarket(String exchange, HashMap<String, Long> stocks) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(exchange));

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

    public void addStock(String exchange, String symbol) throws IOException {
        stocks.put(symbol, (long) 0);
        updateMarket(exchange, stocks);
    }

    public void updateStock(String exchange, String symbol, long price) throws IOException {
        stocks.put(symbol, price);
        updateMarket(exchange, stocks);
    }

    public void removeStock(String exchange, String symbol) throws IOException {
        stocks.remove(symbol);
        updateMarket(exchange, stocks);
    }
}
