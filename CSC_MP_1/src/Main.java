import database.StudentDatabase;
import ui.ClassificationGraphView;
import ui.DatabaseView;
import ui.UploadView;
import classification.Classifier;
import classification.StyleOutput;
import model.HandwritingGraph;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.List;


import java.util.ArrayList;


/**
 * Single entry point for the Handwriting Analysis System.
 *
 * Absorbs the old Main(skeletonization).java and Main(classification).java —
 * delete those two files.
 *
 * The full image pipeline (RGB -> greyscale -> Niblack -> Zhang-Suen -> features)
 * now lives inside FeatureExtractor.extractFeatures(String, String), so this
 * class only wires the UI together.
 */
public class Main extends Application {

    private ClassificationGraphView classGraphView;
    private Label statusBar;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Handwriting Analysis System");

        this.classGraphView = new ClassificationGraphView();
        this.statusBar = new Label(statusText());
        statusBar.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 5;");

        // ── Tabs ──────────────────────────────────────────────────────────────
        TabPane tabPane = new TabPane();

        Tab dbTab = new Tab("Student Database");
        dbTab.setContent(new DatabaseView());   // DatabaseView preloads on its own
        dbTab.setClosable(false);

        Tab matchTab = new Tab("Document Matcher");
        matchTab.setContent(new UploadView());
        matchTab.setClosable(false);

        Tab classTab = new Tab("Classification Graph");
        classTab.setContent(classGraphView);
        classTab.setClosable(false);

        Tab aboutTab = new Tab("About");
        aboutTab.setContent(createAboutPane());
        aboutTab.setClosable(false);

        tabPane.getTabs().addAll(dbTab, matchTab, classTab, aboutTab);

        // Refresh classification graph when switching to that tab,
        // and always update the status bar
        tabPane.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldTab, newTab) -> {
                statusBar.setText(statusText());
                if (newTab == classTab) {
                    refreshClassificationGraph();
                }
            }
        );

        // ── Root layout ───────────────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setCenter(tabPane);
        root.setBottom(statusBar);

        primaryStage.setScene(new Scene(root, 900, 700));
        primaryStage.show();

        // Populate classification tab on startup
        refreshClassificationGraph();
    }


    /**
     * Retrieves all the studenst from the database 
     * Makes use of the gnn to classify studnts according to CURSIVE ,MIXED OR PRINT_BLOCK
     */
    public void refreshClassificationGraph() {
        StudentDatabase db = StudentDatabase.getInstance();
        if (db.getStudentCount() == 0) return;

        HandwritingGraph[] graphs = db.getAllGraphs();
        String[]           ids    = db.getAllStudentIds();

        // Build Lists for GNN
        List<HandwritingGraph> graphList = new ArrayList<>();
        List<String>           idList    = new ArrayList<>();
        List<String>           nameList  = new ArrayList<>();

        for (int i = 0; i < graphs.length; i++) {
            graphList.add(graphs[i]);
            idList.add(ids[i]);
            nameList.add(db.getStudentName(ids[i]));
        }

     // kNN edges — each student connects to 3 most similar neighbours
     // Produces varied, meaningful classifications with trained weights
     List<int[]> edges = new ArrayList<>();
     java.util.Set<String> added = new java.util.HashSet<>();
     int k = Math.min(3, graphList.size() - 1);

     for (int i = 0; i < graphList.size(); i++) {
         double[] distances = new double[graphList.size()];
         for (int j = 0; j < graphList.size(); j++) {
             if (i == j) { distances[j] = Double.MAX_VALUE; continue; }
             distances[j] = featureDistance(graphList.get(i), graphList.get(j));
         }
         for (int n = 0; n < k; n++) {
             int nearest = 0;
             for (int j = 1; j < distances.length; j++)
                 if (distances[j] < distances[nearest]) nearest = j;
             int a = Math.min(i, nearest);
             int b = Math.max(i, nearest);
             String key = a + "-" + b;
             if (added.add(key)) edges.add(new int[]{a, b});
             distances[nearest] = Double.MAX_VALUE;
         }
     }

     // GNN handles all cases — rule-based fallback is inside classifyBatchGNN
        StyleOutput[] results = Classifier.classifyBatchGNN(
            idList, nameList, graphList, edges);

        classGraphView.displayClassificationGraph(results, graphs);
    }

    private double featureDistance(HandwritingGraph a, HandwritingGraph b) {
        String[] feats = {"SlantAngle","LoopFrequency","LetterSpacing",
                          "PressureZone","StrokeDensity"};
        double sum = 0;
        for (String feat : feats) {
            double va = getFeatureValue(a, feat);
            double vb = getFeatureValue(b, feat);
            sum += Math.pow(va - vb, 2);
        }
        return Math.sqrt(sum);
    }

    private double getFeatureValue(HandwritingGraph g, String name) {
        for (int i = 0; i < g.getNodeCount(); i++) {
            model.FeatureNode n = g.getNode("N" + (i + 1));
            if (n != null && n.getFeatureName().equals(name)) return n.getValue();
        }
        return 0.0;
    }


    private String statusText() {
        return "Ready | Students in database: " +
               StudentDatabase.getInstance().getStudentCount();
    }

    private VBox createAboutPane() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.setAlignment(Pos.CENTER);

        Label title = new Label("Handwriting Analysis System");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextArea desc = new TextArea(
            "Detects handwriting similarity to assist in identifying exam fraud.\n\n" +
            "Pipeline\n" +
            "  1. Raw image loaded (PNG / JPG)\n" +
            "  2. RGB -> greyscale (weighted luminance)\n" +
            "  3. Greyscale -> binary (Niblack adaptive thresholding)\n" +
            "  4. Binary -> skeleton (Zhang-Suen thinning)\n" +
            "  5. Skeleton -> feature graph (5 nodes, 4 weighted edges)\n" +
            "  6. GNN similarity detection + style classification\n\n" +
            "Extracted features\n" +
            "  * Slant Angle      - average stroke direction (Sobel gradient)\n" +
            "  * Letter Spacing   - mean vertical-projection gap width\n" +
            "  * Stroke Density   - ink-pixel percentage\n" +
            "  * Loop Frequency   - horizontal-projection valley count\n" +
            "  * Pressure Zone    - ink-run-length variance\n\n" +
            "Graph structure\n" +
            "  Nodes = features above.  Edges = weighted relationships\n" +
            "  (spatial, structural, pressure) between feature pairs."
        );
        desc.setEditable(false);
        desc.setWrapText(true);
        desc.setPrefHeight(320);

        box.getChildren().addAll(title, desc);
        return box;
    }

    public static void main(String[] args) {
        launch(args);
    }
    
    
}