import javafx.animation.AnimationTimer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.function.Consumer;

public class TimeManager {
    private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 11, 25, 8, 0, 0);
    private final Instant realStartTime;
    private LocalDateTime currentTime;
    private final Consumer<LocalDateTime> onTick;
    private final AnimationTimer timer;

    public TimeManager(Consumer<LocalDateTime> onTick) {
        this.onTick = onTick;
        this.realStartTime = Instant.now();
        this.currentTime = START_TIME;

        this.timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsedSeconds = Duration.between(realStartTime, Instant.now()).getSeconds();
                LocalDateTime newTime = START_TIME.plusMinutes(elapsedSeconds);
                if (!newTime.equals(currentTime)) {
                    currentTime = newTime;
                    onTick.accept(currentTime);
                }
            }
        };
    }

    public void start() { timer.start(); }
    public void stop() { timer.stop(); }
    public LocalDateTime getTime() { return currentTime; }
}