package ui;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.View;
import algorithm.FeatureExtractor;
import database.StudentDatabase;
import images.BWImage;
import graph.ImageSkeletonisationGraph;
import model.FeatureEdge;
import model.FeatureNode;
import model.HandwritingGraph;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;

import java.io.File;

/**
 * Student Database Management view.
 * Provides CRUD operations for students and their handwriting graphs.
 *
 * Preloading is done on a background thread so the GUI appears immediately
 * instead of freezing while Zhang-Suen runs on all 5 sample images.
 */
public class DatabaseView extends BorderPane {

    private StudentDatabase db;
    private TableView<StudentRow> tableView;
    private ObservableList<StudentRow> studentData;
    private TextField txtName, txtId;
    private TextArea txtDetails;
    private Label loadingLabel;

    public DatabaseView() {
        db = StudentDatabase.getInstance();
        setupUI();

        if (db.getStudentCount() == 0) {
            loadingLabel.setText("Loading students... (running image pipeline)");
            preloadInBackground();
        } else {
            loadingLabel.setText("");
            refreshTable();
        }
    }


    private void preloadInBackground() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                db.preloadStudents();
                return null;
            }
        };
        task.setOnSucceeded(e -> { loadingLabel.setText(""); refreshTable(); });
        task.setOnFailed(e -> loadingLabel.setText(
            "Error loading students: " + task.getException().getMessage()));
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }


    private void setupUI() {
        Label title = new Label("Handwriting Analysis System - Student Database");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        loadingLabel = new Label("");
        loadingLabel.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");

        tableView = new TableView<>();
        setupTableColumns();

        GridPane formPane = createFormPane();

        txtDetails = new TextArea();
        txtDetails.setEditable(false);
        txtDetails.setPrefHeight(150);

        HBox buttonBox = createButtonBox();

        VBox topBox = new VBox(6, title, loadingLabel, buttonBox);
        topBox.setPadding(new Insets(10));

        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitPane.getItems().addAll(tableView, formPane, txtDetails);
        splitPane.setDividerPositions(0.5, 0.75);

        setTop(topBox);
        setCenter(splitPane);
    }

    private void setupTableColumns() {
        TableColumn<StudentRow, String> idCol = new TableColumn<>("Student ID");
        idCol.setCellValueFactory(cd -> cd.getValue().idProperty());
        idCol.setPrefWidth(100);

        TableColumn<StudentRow, String> nameCol = new TableColumn<>("Student Name");
        nameCol.setCellValueFactory(cd -> cd.getValue().nameProperty());
        nameCol.setPrefWidth(150);

        TableColumn<StudentRow, String> graphCol = new TableColumn<>("Graph Info");
        graphCol.setCellValueFactory(cd -> cd.getValue().graphInfoProperty());
        graphCol.setPrefWidth(200);

        tableView.getColumns().addAll(idCol, nameCol, graphCol);

        tableView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    showGraphDetails(newVal.getStudentId());
                    txtId.setText(newVal.getStudentId());
                    txtName.setText(newVal.getStudentName());
                }
            }
        );
    }

    private GridPane createFormPane() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Student ID:"), 0, 0);
        txtId = new TextField();
        txtId.setPromptText("e.g., S006");
        grid.add(txtId, 1, 0);

        grid.add(new Label("Student Name:"), 0, 1);
        txtName = new TextField();
        txtName.setPromptText("Full name");
        grid.add(txtName, 1, 1);

        return grid;
    }

    private HBox createButtonBox() {
        Button btnAdd            = new Button("Add Student");
        Button btnUpdate         = new Button("Update Student");
        Button btnDelete         = new Button("Delete Student");
        Button btnRefresh        = new Button("Refresh");
        Button btnViewGraph      = new Button("View Skeleton");
        Button btnVisualizeGraph = new Button("Visualize Graph");

        btnAdd.setOnAction(e -> addStudent());
        btnUpdate.setOnAction(e -> updateStudent());
        btnDelete.setOnAction(e -> deleteStudent());
        btnRefresh.setOnAction(e -> refreshTable());
        btnViewGraph.setOnAction(e -> viewGraph());
        btnVisualizeGraph.setOnAction(e -> visualizeGraph());

        HBox box = new HBox(10, btnAdd, btnUpdate, btnDelete,
                                btnRefresh, btnViewGraph, btnVisualizeGraph);
        box.setPadding(new Insets(10));
        return box;
    }


    private void addStudent() {
        String id   = txtId.getText().trim();
        String name = txtName.getText().trim();

        if (id.isEmpty() || name.isEmpty()) {
            showAlert("Error", "Please enter both Student ID and Name");
            return;
        }
        if (db.studentExists(id)) {
            showAlert("Error", "Student ID already exists!");
            return;
        }

        HandwritingGraph graph = new HandwritingGraph(id);
        graph.addNode(new FeatureNode("N1", "SlantAngle",    0.0));
        graph.addNode(new FeatureNode("N2", "LetterSpacing", 0.0));
        graph.addNode(new FeatureNode("N3", "StrokeDensity", 0.0));
        graph.addNode(new FeatureNode("N4", "LoopFrequency", 0.0));
        graph.addNode(new FeatureNode("N5", "PressureZone",  0.0));

        boolean ok = db.addStudent(id, name, "data/students/" + id + "/sample.png", graph);
        if (ok) { refreshTable(); clearForm(); showAlert("Success", "Student added successfully!"); }
        else      showAlert("Error", "Could not add student (database may be full)");
    }

    private void updateStudent() {
        String id   = txtId.getText().trim();
        String name = txtName.getText().trim();

        if (id.isEmpty() || name.isEmpty()) {
            showAlert("Error", "Please select a student to update");
            return;
        }
        boolean ok = db.updateStudentName(id, name);
        if (ok) { refreshTable(); showAlert("Success", "Student updated successfully!"); }
        else      showAlert("Error", "Student not found");
    }

    private void deleteStudent() {
        String id = txtId.getText().trim();
        if (id.isEmpty()) { showAlert("Error", "Please select a student to delete"); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setContentText("Are you sure you want to delete student " + id + "?");
        if (confirm.showAndWait().get() == ButtonType.OK) {
            if (db.removeStudent(id)) { refreshTable(); clearForm(); showAlert("Success", "Student deleted!"); }
        }
    }


    public void refreshTable() {
        studentData = FXCollections.observableArrayList();
        for (String id : db.getAllStudentIds()) {
            HandwritingGraph g = db.getStudentGraph(id);
            studentData.add(new StudentRow(id, db.getStudentName(id),
                String.format("%d nodes, %d edges", g.getNodeCount(), g.getEdgeCount())));
        }
        tableView.setItems(studentData);
    }

    private void showGraphDetails(String studentId) {
        HandwritingGraph g = db.getStudentGraph(studentId);
        if (g == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("=== Handwriting Graph for ").append(studentId).append(" ===\n\nNodes:\n");
        for (FeatureNode n : g.getAllNodes())
            sb.append(String.format("  * %s: %.2f%n", n.getFeatureName(), n.getValue()));
        sb.append("\nEdges:\n");
        for (FeatureEdge e : g.getAllEdges())
            sb.append(String.format("  * %s -> %s  (weight: %.2f, type: %s)%n",
                e.getSourceNode().getFeatureName(), e.getTargetNode().getFeatureName(),
                e.getWeight(), e.getRelationshipType()));
        txtDetails.setText(sb.toString());
    }


    /**
     * Opens a new window showing the student's skeletonized handwriting as a
     * red overlay on top of their original sample image.
     *
     * Features:
     *   - Skeleton runs on a background thread so the window appears immediately
     *   - Red skeleton pixels composited over the original greyscale sample
     *   - ScrollPane so the user can pan when zoomed in
     *   - Zoom In / Zoom Out / Reset buttons
     *   - Mouse scroll wheel zoom (Ctrl+scroll or plain scroll)
     */
    private void viewGraph() {
        StudentRow sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select a student first.");
            return;
        }

        String studentId = sel.getStudentId();
        HandwritingGraph hwg = db.getStudentGraph(studentId);
        String imagePath = db.getStudentImagePath(studentId);

        // ── Window ────────────────────────────────────────────────────────────
        Stage stage = new Stage();
        stage.setTitle("Skeleton Graph — " + sel.getStudentName());

        Label titleLabel = new Label(sel.getStudentName() + "  (" + studentId + ")");
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        TextArea featureArea = new TextArea();
        featureArea.setEditable(false);
        featureArea.setPrefHeight(110);
        StringBuilder fb = new StringBuilder("Extracted features\n");
        fb.append("----------------------------\n");
        for (FeatureNode n : hwg.getAllNodes())
            fb.append(String.format("  %-15s  %8.2f%n", n.getFeatureName(), n.getValue()));
        featureArea.setText(fb.toString());

        VBox topPanel = new VBox(8, titleLabel, featureArea);
        topPanel.setPadding(new Insets(10));

        Group canvasGroup = new Group();

        StackPane imageHolder = new StackPane(canvasGroup);
        imageHolder.setStyle("-fx-background-color: #1a1a1a;");

        ScrollPane scroll = new ScrollPane(imageHolder);
        scroll.setPannable(true);
        scroll.setFitToWidth(false);
        scroll.setFitToHeight(false);

        // ── Zoom ──────────────────────────────────────────────────────────────
        double[] scale = { 1.0 };

        Runnable applyZoom = () -> {
            canvasGroup.setScaleX(scale[0]);
            canvasGroup.setScaleY(scale[0]);
            if (!canvasGroup.getChildren().isEmpty()) {
                Canvas c = (Canvas) canvasGroup.getChildren().get(0);
                imageHolder.setMinSize(c.getWidth() * scale[0], c.getHeight() * scale[0]);
                imageHolder.setPrefSize(c.getWidth() * scale[0], c.getHeight() * scale[0]);
            }
        };

        Label zoomLabel = new Label("100%");
        zoomLabel.setMinWidth(50);
        zoomLabel.setStyle("-fx-font-size: 12px;");

        Button btnZoomIn  = new Button("+ Zoom In");
        Button btnZoomOut = new Button("- Zoom Out");
        Button btnReset   = new Button("Reset");

        btnZoomIn.setOnAction(e -> {
            scale[0] = Math.min(scale[0] * 1.25, 8.0);
            applyZoom.run();
            zoomLabel.setText(String.format("%.0f%%", scale[0] * 100));
        });
        btnZoomOut.setOnAction(e -> {
            scale[0] = Math.max(scale[0] * 0.8, 0.1);
            applyZoom.run();
            zoomLabel.setText(String.format("%.0f%%", scale[0] * 100));
        });
        btnReset.setOnAction(e -> {
            scale[0] = 1.0;
            applyZoom.run();
            zoomLabel.setText("100%");
        });

        scroll.setOnScroll(event -> {
            event.consume();
            double factor = event.getDeltaY() > 0 ? 1.15 : (1.0 / 1.15);
            scale[0] = Math.min(Math.max(scale[0] * factor, 0.1), 8.0);
            applyZoom.run();
            zoomLabel.setText(String.format("%.0f%%", scale[0] * 100));
        });

        Label legend = new Label(
            "  Red = junction node     Green = endpoint node     Blue = stroke edge");
        legend.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

        HBox bottomBar = new HBox(10, btnZoomIn, btnZoomOut, btnReset, zoomLabel, legend);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(8, 12, 8, 12));
        bottomBar.setStyle("-fx-border-color: #ddd; -fx-border-width: 1 0 0 0;");

        BorderPane root = new BorderPane();
        root.setTop(topPanel);
        root.setCenter(scroll);
        root.setBottom(bottomBar);

        stage.setScene(new Scene(root, 760, 700));
        stage.show();

        // ── Guard ─────────────────────────────────────────────────────────────
        if (imagePath == null || !new File(imagePath).exists()) {
            showErrorCanvas(canvasGroup, "Sample image not found: " + imagePath);
            return;
        }

        ProgressIndicator spinner = new ProgressIndicator();
        imageHolder.getChildren().add(spinner);

        Task<Canvas> task = new Task<>() {
            @Override
            protected Canvas call() {
                Image rawImage = new Image(
                    "file:" + new File(imagePath).getAbsolutePath());
                if (rawImage.getWidth() <= 0) return null;

                ImageSkeletonisationGraph isg =
                    new ImageSkeletonisationGraph(rawImage, studentId, 140, 20);

                return isg.getDisplayCanvas();
            }
        };

        task.setOnSucceeded(e -> {
            imageHolder.getChildren().remove(spinner);
            Canvas canvas = task.getValue();
            if (canvas == null) {
                showErrorCanvas(canvasGroup, "Failed to generate skeleton graph.");
                return;
            }
            canvasGroup.getChildren().setAll(canvas);
            imageHolder.setMinSize(canvas.getWidth(), canvas.getHeight());
            imageHolder.setPrefSize(canvas.getWidth(), canvas.getHeight());
            applyZoom.run();
        });

        task.setOnFailed(e -> {
            imageHolder.getChildren().remove(spinner);
            showErrorCanvas(canvasGroup,
                "Error: " + task.getException().getMessage());
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void showErrorCanvas(Group group, String message) {
        Canvas c = new Canvas(500, 200);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setFill(Color.rgb(240, 240, 240));
        gc.fillRect(0, 0, 500, 200);
        gc.setFill(Color.DARKRED);
        gc.setFont(Font.font(13));
        gc.fillText(message, 20, 60);
        group.getChildren().setAll(c);
        System.err.println("Skeleton viewer: " + message);
    }


    private void visualizeGraph() {
        StudentRow sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Graph graph = new SingleGraph("Graph");
        HandwritingGraph hwg = db.getStudentGraph(sel.getStudentId());

        for (FeatureNode n : hwg.getAllNodes())
            graph.addNode(n.getId())
                 .setAttribute("ui.label",
                     String.format("%s\nValue: %.2f", n.getFeatureName(), n.getValue()));

        for (FeatureEdge e : hwg.getAllEdges())
            graph.addEdge(e.getId(),
                          e.getSourceNode().getId(),
                          e.getTargetNode().getId())
                 .setAttribute("ui.label",
                     String.format("weight: %.2f\ntype: %s",
                         e.getWeight(), e.getRelationshipType()));

        FxViewer viewer = new FxViewer(graph, FxViewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        viewer.enableAutoLayout();
        View view = viewer.addDefaultView(false);

        GridPane gp = new GridPane();
        gp.add(new Label("Student " + sel.getStudentId() + "'s feature graph"), 0, 0);
        gp.add((javafx.scene.Node) view, 0, 1);

        Stage stage = new Stage();
        stage.setScene(new Scene(gp, 500, 500));
        stage.setTitle("Feature Graph — " + sel.getStudentName());
        stage.show();
    }

    private void clearForm() { txtId.clear(); txtName.clear(); txtDetails.clear(); }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public static class StudentRow {
        private final javafx.beans.property.StringProperty id;
        private final javafx.beans.property.StringProperty name;
        private final javafx.beans.property.StringProperty graphInfo;

        public StudentRow(String id, String name, String graphInfo) {
            this.id        = new javafx.beans.property.SimpleStringProperty(id);
            this.name      = new javafx.beans.property.SimpleStringProperty(name);
            this.graphInfo = new javafx.beans.property.SimpleStringProperty(graphInfo);
        }

        public String getStudentId()   { return id.get(); }
        public String getStudentName() { return name.get(); }
        public javafx.beans.property.StringProperty idProperty()        { return id; }
        public javafx.beans.property.StringProperty nameProperty()      { return name; }
        public javafx.beans.property.StringProperty graphInfoProperty() { return graphInfo; }
    }
}