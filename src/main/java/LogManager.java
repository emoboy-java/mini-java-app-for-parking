import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class LogManager implements AutoCloseable {
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final PrintWriter writer;
    private final Consumer<String> onLog;
    private LocalDate currentDate;
    private int dailyEntries, dailyExits;
    private long dailyRevenue;

    public LogManager(Consumer<String> onLog) throws IOException {
        this.onLog = onLog;

        //создание папки logs
        Path logsDir = Paths.get("logs");
        Files.createDirectories(logsDir);

        String fileName = "log_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
        Path logFile = logsDir.resolve(fileName);

        this.writer = new PrintWriter(new FileWriter(logFile.toFile(), true), true);
        this.currentDate = LocalDate.of(2027, 1, 1);
        write("Начало записей нового дня.");
    }

    public void logEntry(String plate, int spot, LocalDateTime time) {
        write(String.format("%s. Автомобиль %s заехал на место №%d.", formatDT(time), plate, spot));
        dailyEntries++;
    }

    public void logExit(String plate, LocalDateTime entry, LocalDateTime exit, long payment) {
        write(String.format("%s. Автомобиль %s выехал. Въезд: %s. Оплачено: %d ₽.",
                formatDT(exit), plate, formatDT(entry), payment));
        dailyExits++;
        dailyRevenue += payment;
    }

    public void checkDayChange(LocalDateTime time) {
        LocalDate today = time.toLocalDate();
        if (today.isAfter(currentDate)) {
            writeDailySummary();
            currentDate = today;
            dailyEntries = dailyExits = 0;
            dailyRevenue = 0;
            write("Начало записей нового дня.");
        }
    }

    public long getDailyRevenue() { return dailyRevenue; }

    @Override
    public void close() {
        writeDailySummary();
        writer.close();
    }

    private void writeDailySummary() {
        write(String.format("%s 23:59:59. Итог дня: въехало %d, выехало %d. Выручка: %d ₽.",
                formatDate(currentDate), dailyEntries, dailyExits, dailyRevenue));
    }

    private void write(String msg) {
        writer.println(msg);
        if (onLog != null) onLog.accept(msg);
    }

    private String formatDT(LocalDateTime t) { return t.format(DT_FMT); }
    private String formatDate(LocalDate d) { return d.format(D_FMT); }
}