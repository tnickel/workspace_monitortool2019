package data;



import java.util.Comparator;

public class TradeComparator implements Comparator<Trade> {
    @Override
    public int compare(Trade trade1, Trade trade2) {
        return trade1.getOpenTime().compareTo(trade2.getOpenTime());
    }
}