package data;

import java.time.LocalDateTime;

public class Trade {
    private final LocalDateTime openTime;
    private final LocalDateTime closeTime;
    
    public Trade(LocalDateTime openTime, LocalDateTime closeTime) {
        this.openTime = openTime;
        this.closeTime = closeTime;
    }
    
    public LocalDateTime getOpenTime() { 
        return openTime; 
    }
    
    public LocalDateTime getCloseTime() { 
        return closeTime; 
    }

    @Override
    public String toString() {
        return "Trade{" +
            "openTime=" + openTime +
            ", closeTime=" + closeTime +
            '}';
    }
}