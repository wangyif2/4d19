import java.io.*;
import java.util.Collection;
import java.util.HashMap;

/**
 * User: robert
 * Date: 11/01/13
 */
public class BrokerLookup {
    private static HashMap<String, BrokerLocation> brokers;
    private static BrokerLookup myBrokerLookup = null;

    public static BrokerLookup getInstance() throws IOException {
        if (myBrokerLookup == null) {
            myBrokerLookup = new BrokerLookup();
            brokers = new HashMap<String, BrokerLocation>();
        }

        return myBrokerLookup;
    }

    public void addBroker(String brokerName, BrokerLocation brokerLoc) {
        brokers.put(brokerName, brokerLoc);
    }

    public Collection<BrokerLocation> lookupBrokerLoc() {
        return brokers.values();
    }

    public BrokerLocation lookupBrokerLoc(String exchange) {
        return brokers.containsKey(exchange) ? brokers.get(exchange) : null;
    }
}
