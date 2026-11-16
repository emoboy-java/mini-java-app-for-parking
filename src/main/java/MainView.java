import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.Group;
import javafx.stage.Stage;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class MainView {
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final int COLS = 5, ROWS = 4;
    private static final double SPOT_W = 80, SPOT_H = 100, GAP = 10, MARGIN = 20;

    private final Stage stage;
    private final Pane absoluteRoot;
    private final BorderPane root;
    private final Pane parkingPane;
    private final FlowPane availablePane;
    private final TextArea logArea;

    private final Label clockLbl, freeLbl, occupiedLbl, dailyRevLbl, totalRevLbl, infoLbl;
    private final ParkingManager manager;
    private final TimeManager timeManager;
    private final LogManager logManager;

    private Car dragCar;
    private Node dragNode;
    private boolean wasParked;
    private ParkingSpot originalSpot;
    private final Map<ParkingSpot, Rectangle> spotMap = new HashMap<>();

    public MainView(Stage stage) {
        this.stage = stage;
        absoluteRoot = new Pane();
        root = new BorderPane();
        absoluteRoot.getChildren().add(root);

        // Верхняя панель
        HBox topBar = new HBox(20);
        freeLbl = createLabel("Свободно: 20");
        occupiedLbl = createLabel("Занято: 0");
        clockLbl = createLabel("Время: 16.11.2026 08:00:00");
        dailyRevLbl = createLabel("Выручка за сутки: 0 ₽");
        totalRevLbl = createLabel("Общая выручка: 0 ₽");
        topBar.getChildren().addAll(freeLbl, occupiedLbl, clockLbl, dailyRevLbl, totalRevLbl);
        topBar.setStyle("-fx-padding: 10; -fx-background-color: #f0f0f0;");

        infoLbl = new Label("Кликните на машину на парковке для информации");
        VBox topContainer = new VBox(5, topBar, infoLbl);
        root.setTop(topContainer);

        // Центр
        VBox center = new VBox(10);
        parkingPane = new Pane();
        parkingPane.setPrefSize(500, 450);
        parkingPane.setStyle("-fx-background-color: #dcdcdc; -fx-border-color: black;");

        availablePane = new FlowPane();
        availablePane.setPrefSize(500, 120);
        availablePane.setStyle("-fx-background-color: #e8e8e8; -fx-border-color: black;");
        availablePane.setHgap(10);
        availablePane.setVgap(10);
        availablePane.setPadding(new Insets(10));

        center.getChildren().addAll(parkingPane, availablePane);
        root.setCenter(center);

        // Журнал
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        root.setBottom(logArea);

        Scene scene = new Scene(absoluteRoot, 800, 850);
        stage.setScene(scene);
        stage.setTitle("Парковка аэропорта");

        try {
            logManager = new LogManager(msg -> Platform.runLater(() -> logArea.appendText(msg + "\n")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        manager = new ParkingManager(logManager);
        timeManager = new TimeManager(this::onTimeTick);

        initSpots();
        refreshAvailableCars();

        stage.setOnCloseRequest(e -> {
            timeManager.stop();
            logManager.close();
        });

        timeManager.start();
    }

    public void show() { stage.show(); }

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setMinWidth(Label.USE_PREF_SIZE);
        return lbl;
    }

    private void initSpots() {
        for (int i = 0; i < 20; i++) {
            int row = i / COLS, col = i % COLS;
            double x = col * (SPOT_W + GAP) + MARGIN;
            double y = row * (SPOT_H + GAP) + MARGIN;

            ParkingSpot spot = manager.getSpots().get(i);
            Rectangle rect = new Rectangle(x, y, SPOT_W, SPOT_H);
            rect.setFill(Color.LIGHTGRAY);
            rect.setStroke(Color.BLACK);

            Text num = new Text(x + SPOT_W / 2 - 5, y + SPOT_H / 2 + 5, String.valueOf(spot.getNumber()));
            parkingPane.getChildren().addAll(rect, num);
            spotMap.put(spot, rect);
        }
    }

    private void refreshAvailableCars() {
        availablePane.getChildren().clear();
        for (Car car : manager.getAvailableCars()) {
            availablePane.getChildren().add(createCarNode(car));
        }
    }

    private Node createCarNode(Car car) {
        Group group = new Group();
        Rectangle body = new Rectangle(60, 30);
        body.setFill(car.getColor());
        body.setStroke(Color.BLACK);
        body.setArcWidth(10);
        body.setArcHeight(10);

        Text plate = new Text(car.getPlate());
        plate.setX(5);
        plate.setY(20);
        plate.setFont(Font.font(10));

        group.getChildren().addAll(body, plate);

        group.setOnMousePressed(e -> startDrag(car, group, e));
        group.setOnMouseDragged(this::doDrag);
        group.setOnMouseReleased(this::endDrag);

        group.setOnMouseClicked(e -> {
            if (car.isParked()) {
                LocalDateTime now = timeManager.getTime();
                long mins = Duration.between(car.getEntryTime(), now).toMinutes();
                long cost = PaymentCalculator.calculate(car.getEntryTime(), now);
                infoLbl.setText(String.format("Машина: %s | Въезд: %s | Длительность: %d мин | Стоимость: %d ₽",
                        car.getPlate(), car.getEntryTime().format(DT_FMT), mins, cost));
            }
        });

        return group;
    }

    private void startDrag(Car car, Node node, javafx.scene.input.MouseEvent e) {
        dragCar = car;
        dragNode = node;
        wasParked = car.isParked();
        originalSpot = car.getCurrentSpot();

        Bounds b = node.localToScene(node.getBoundsInLocal());
        ((Pane) node.getParent()).getChildren().remove(node);
        absoluteRoot.getChildren().add(node);
        node.setLayoutX(b.getMinX());
        node.setLayoutY(b.getMinY());
        node.toFront();
    }

    private void doDrag(javafx.scene.input.MouseEvent e) {
        if (dragNode != null) {
            dragNode.setLayoutX(e.getSceneX() - dragNode.getLayoutBounds().getWidth() / 2);
            dragNode.setLayoutY(e.getSceneY() - dragNode.getLayoutBounds().getHeight() / 2);
        }
    }

    private void endDrag(javafx.scene.input.MouseEvent e) {
        if (dragNode == null) return;

        absoluteRoot.getChildren().remove(dragNode);
        Bounds carB = dragNode.localToScene(dragNode.getBoundsInLocal());
        double cx = carB.getMinX() + carB.getWidth() / 2;
        double cy = carB.getMinY() + carB.getHeight() / 2;

        boolean placed = false;
        for (ParkingSpot spot : manager.getSpots()) {
            Rectangle r = spotMap.get(spot);
            Bounds sb = r.localToScene(r.getBoundsInLocal());
            if (sb.contains(cx, cy) && !spot.isOccupied()) {
                if (manager.parkCar(dragCar, spot, timeManager.getTime())) {
                    parkingPane.getChildren().add(dragNode);
                    dragNode.setLayoutX(r.getX() + r.getWidth() / 2 - dragNode.getLayoutBounds().getWidth() / 2);
                    dragNode.setLayoutY(r.getY() + r.getHeight() / 2 - dragNode.getLayoutBounds().getHeight() / 2);
                    placed = true;
                    updateCounters();
                }
                break;
            }
        }

        if (!placed) {
            Bounds parkB = parkingPane.localToScene(parkingPane.getBoundsInLocal());
            if (!parkB.intersects(carB) && wasParked) {
                manager.exitCar(dragCar, timeManager.getTime());
                refreshAvailableCars();
                updateCounters();
                updateTotalRevenue();
                infoLbl.setText("Машина выехала. Кликните на другую для информации.");
            } else {
                // Возврат на прежнее место
                if (wasParked && originalSpot != null) {
                    Rectangle r = spotMap.get(originalSpot);
                    parkingPane.getChildren().add(dragNode);
                    dragNode.setLayoutX(r.getX() + r.getWidth() / 2 - dragNode.getLayoutBounds().getWidth() / 2);
                    dragNode.setLayoutY(r.getY() + r.getHeight() / 2 - dragNode.getLayoutBounds().getHeight() / 2);
                } else {
                    availablePane.getChildren().add(dragNode);
                }
            }
        }

        dragCar = null;
        dragNode = null;
    }

    private void onTimeTick(LocalDateTime time) {
        Platform.runLater(() -> {
            clockLbl.setText("Время: " + time.format(DT_FMT));
            logManager.checkDayChange(time);
            dailyRevLbl.setText("Выручка за сутки: " + logManager.getDailyRevenue() + " ₽");
            updateTotalRevenue();
        });
    }

    private void updateCounters() {
        freeLbl.setText("Свободно: " + manager.getFreeCount());
        occupiedLbl.setText("Занято: " + manager.getOccupiedCount());
    }

    private void updateTotalRevenue() {
        totalRevLbl.setText("Общая выручка: " + manager.getTotalRevenue() + " ₽");
    }
}