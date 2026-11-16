public class ParkingSpot {
    private final int number;
    private Car car;

    public ParkingSpot(int number) {
        this.number = number;
    }

    public int getNumber() { return number; }
    public Car getCar() { return car; }
    public void setCar(Car car) { this.car = car; }
    public boolean isOccupied() { return car != null; }
}