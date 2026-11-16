import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParkingManager {
    private static final int TOTAL_SPOTS = 20;
    private static final int INITIAL_CARS = 12;
    private static final String LETTERS = "ABCEHKMOPTXY";

    private final List<ParkingSpot> spots;
    private final List<Car> availableCars;
    private final List<Car> parkedCars;
    private final LogManager logManager;
    private final Random random = new Random();

    private int totalEntries, totalExits;
    private long totalRevenue;

    public ParkingManager(LogManager logManager) {
        this.logManager = logManager;
        this.spots = new ArrayList<>(TOTAL_SPOTS);
        this.availableCars = new ArrayList<>(INITIAL_CARS);
        this.parkedCars = new ArrayList<>();

        for (int i = 1; i <= TOTAL_SPOTS; i++) spots.add(new ParkingSpot(i));
        for (int i = 0; i < INITIAL_CARS; i++) availableCars.add(generateCar());
    }

    private Car generateCar() {
        StringBuilder sb = new StringBuilder(7);
        sb.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        sb.append(random.nextInt(10));
        sb.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        sb.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        sb.append(random.nextInt(10));
        sb.append(random.nextInt(10));
        return new Car(sb.toString());
    }

    public boolean parkCar(Car car, ParkingSpot spot, LocalDateTime time) {
        if (spot.isOccupied()) return false;

        car.setParked(true);
        car.setCurrentSpot(spot);
        car.setEntryTime(time);
        spot.setCar(car);

        availableCars.remove(car);
        parkedCars.add(car);
        totalEntries++;

        logManager.logEntry(car.getPlate(), spot.getNumber(), time);
        return true;
    }

    public long exitCar(Car car, LocalDateTime time) {
        if (!car.isParked()) return 0;

        long payment = PaymentCalculator.calculate(car.getEntryTime(), time);
        logManager.logExit(car.getPlate(), car.getEntryTime(), time, payment);

        car.getCurrentSpot().setCar(null);
        car.setParked(false);
        car.setCurrentSpot(null);

        parkedCars.remove(car);
        availableCars.add(generateCar());

        totalExits++;
        totalRevenue += payment;
        return payment;
    }

    public List<ParkingSpot> getSpots() { return spots; }
    public List<Car> getAvailableCars() { return availableCars; }
    public int getFreeCount() { return TOTAL_SPOTS - parkedCars.size(); }
    public int getOccupiedCount() { return parkedCars.size(); }
    public long getTotalRevenue() { return totalRevenue; }
}