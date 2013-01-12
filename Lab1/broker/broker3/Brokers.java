import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * User: robert
 * Date: 11/01/13
 */
public class Brokers {
    private static HashMap<String, BrokerLocation> brokers;
    private static Brokers myBrokers = null;

    public static Brokers getInstance() throws IOException {
        if (myBrokers == null)
            myBrokers = new Brokers();

        return myBrokers;
    }

    //private Market() throws IOException {
    //    readMarket();
    //}

    public void addBroker(String brokerName, BrokerLocation brokerLoc) {
        if (brokers.containsKey(brokerName))
            System.out.println(brokerName + " already exists!");
        else
            brokers.put(brokerName, brokerLoc);
    }

    public BrokerLocation lookupBroker(String brokerName) {
        return brokers.containsKey(brokerName) ? brokers.get(brokerName) : null;
    }

}
