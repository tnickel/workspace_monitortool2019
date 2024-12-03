package data;

import java.time.LocalDateTime;

public class Trade {
    private final LocalDateTime openTime;
    private final LocalDateTime closeTime;
    private final double lots;
    
    public Trade(LocalDateTime openTime, LocalDateTime closeTime, double lots) {
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.lots = lots;
    }
    
    public LocalDateTime getOpenTime() { 
        return openTime; 
    }
    
    public LocalDateTime getCloseTime() { 
        return closeTime; 
    }
    
    public double getLots() {
        return lots;
    }

    @Override
    public String toString() {
        return "Trade{" +
            "openTime=" + openTime +
            ", closeTime=" + closeTime +
            ", lots=" + lots +
            '}';
    }
}