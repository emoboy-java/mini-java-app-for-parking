import javafx.scene.paint.Color;
import java.time.LocalDateTime;

public class Car {
    private final String plate;
    private final Color color;
    private LocalDateTime entryTime;
    private ParkingSpot currentSpot;
    private boolean parked;

    public Car(String plate) {
        this.plate = plate;
        this.color = Color.hsb(Math.random() * 360, 0.8, 0.9);
    }

    public String getPlate() { return plate; }
    public Color getColor() { return color; }

    public LocalDateTime getEntryTime() { return entryTime; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }

    public ParkingSpot getCurrentSpot() { return currentSpot; }
    public void setCurrentSpot(ParkingSpot spot) { this.currentSpot = spot; }

    public boolean isParked() { return parked; }
    public void setParked(boolean parked) { this.parked = parked; }
}