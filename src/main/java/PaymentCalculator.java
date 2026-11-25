import java.time.Duration;
import java.time.LocalDateTime;

public class PaymentCalculator {
    private static final long FREE_MINUTES = 20;
    private static final long HOURLY_RATE = 200;
    private static final long DAILY_THRESHOLD_SECONDS = 12 * 3600;
    private static final long DAY_SECONDS = 24 * 3600;

    public static long calculate(LocalDateTime entry, LocalDateTime exit) {
        long seconds = Duration.between(entry, exit).getSeconds();

        //бесплатно до 20 минут включительно
        if (seconds <= FREE_MINUTES * 60) return 0;

        //до 12 часов: почасовая оплата
        if (seconds <= DAILY_THRESHOLD_SECONDS) {
            return (long) Math.ceil(seconds / 3600.0) * HOURLY_RATE;
        }

        //после 12 часов: посуточная оплата
        return (long) Math.ceil(seconds / (double) DAY_SECONDS) * HOURLY_RATE;
    }
}