package ui;

import classification.HandwritingStyle;
import classification.StyleOutput;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import model.HandwritingGraph;
import model.FeatureNode;


/**
 * JavaFX panel that visualises the handwriting classification results for all
 * students loaded from the database.

 */
public class ClassificationGraphView extends VBox {

    //Style colour constants 
    private static final String PRINT_COL   = "#378ADD";
    private static final String CURSIVE_COL = "#7F77DD";
    private static final String MIXED_COL   = "#EF9F27";

    /**
     * Used to colour edges and nodes in the per-student radial graph
     * and the feature progress bars.
     */
    private static final Color[] FEATURE_COLORS = {
        Color.web("#378ADD"), Color.web("#7F77DD"), Color.web("#EF9F27"),
        Color.web("#1D9E75"), Color.web("#D85A30")
    };

    /**
     *labels for the five features shown in the UI.
     */
    private static final String[] FEATURE_NAMES = {
        "Slant", "Loops", "Spacing", "Pressure", "Density"
    };

    /**
     * Internal keys used to look up 
     */
    private static final String[] FEATURE_KEYS = {
        "SlantAngle", "LoopFrequency", "LetterSpacing",
        "PressureZone", "StrokeDensity"
    };

    /**
     * Minimum raw value per feature used in min-max normalisation to [0, 100].
     */
    private static final double[] FEAT_MIN = {60,  0,  3, 10, 1};

    /**
     * Maximum raw value per feature used in min-max normalisation to [0, 100].
     * */
    private static final double[] FEAT_MAX = {140, 15, 30, 90, 12};

    /**
     * Builds the initial placeholder view shown before any student data is loaded.
     */
    public ClassificationGraphView() {
        setPadding(new Insets(15));
        setSpacing(10);
        Label title = new Label("Handwriting Classification Graph");
        title.setStyle("-fx-font-size:18px;-fx-font-weight:bold;");
        Label hint = new Label("Load students from the database to see results.");
        hint.setStyle("-fx-font-size:12px;-fx-text-fill:#999;");
        getChildren().addAll(title, hint);
    }

    /**
     * Clears the panel and renders the full two-tab classification UI.
     * @param results array of one per student;
     * @param graphs  parallel array of objects used to
     *                normalise raw feature values for the progress bars
     */
    public void displayClassificationGraph(StyleOutput[] results,HandwritingGraph[] graphs) {
        getChildren().clear();

        if (results == null || results.length == 0) {
            getChildren().add(new Label("No students to classify."));
            return;
        }

        Label title = new Label("Handwriting Classification Graph  —  GNN");
        title.setStyle("-fx-font-size:18px;-fx-font-weight:bold;");
        getChildren().add(title);

        TabPane tabs = new TabPane();
        tabs.setPrefHeight(650);

        // Tab 1: bar chart + similarity table
        Tab chartTab = new Tab("Style Distribution", buildChartPage(results));
        chartTab.setClosable(false);

        // Tab 2: inter-student GNN message-passing graph + breakdown cards
        Tab gnnTab = new Tab("GNN Graph", buildGNNGraphPage(results, graphs));
        gnnTab.setClosable(false);

        tabs.getTabs().addAll(chartTab, gnnTab);
        getChildren().add(tabs);
    }

    /**
     * Builds the GNN Graph tab content: a circular canvas where each student is
     * a node connected to all others by faint edges followed by a per-student feature-breakdown card.

     * @param results classification outputs 
     * @param graphs  used to compute normalised feature scores for the cards
     * @return a wrapping the canvas and the student cards
     */
    private ScrollPane buildGNNGraphPage(StyleOutput[] results,
                                          HandwritingGraph[] graphs) {
        int N = results.length;
        double W = 700, H = 500;
        Canvas canvas = new Canvas(W, H);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Light grey background
        gc.setFill(Color.web("#f9fafb"));
        gc.fillRect(0, 0, W, H);

        double cx = W / 2, cy = H / 2;
        double radius = Math.min(W, H) * 0.36;
        double[] nx = new double[N];
        double[] ny = new double[N];
        for (int i = 0; i < N; i++) {
            double angle = (2 * Math.PI / N) * i - Math.PI / 2;
            nx[i] = cx + radius * Math.cos(angle);
            ny[i] = cy + radius * Math.sin(angle);
        }

        // Full-mesh faint edges represent GNN message passing between all students
        gc.setLineWidth(0.8);
        gc.setGlobalAlpha(0.15);
        gc.setStroke(Color.web("#6b7280"));
        for (int i = 0; i < N; i++)
            for (int j = i + 1; j < N; j++)
                gc.strokeLine(nx[i], ny[i], nx[j], ny[j]);
        gc.setGlobalAlpha(1.0);

        // Draw each student node 
        for (int i = 0; i < N; i++) {
            if (results[i] == null) continue;

            Color col = switch (results[i].getStyle()) {
                case PRINT_BLOCK -> Color.web(PRINT_COL);
                case CURSIVE     -> Color.web(CURSIVE_COL);
                default          -> Color.web(MIXED_COL);
            };


            double ringR = 28 + (results[i].getSimilarityScore() / 100.0) * 10;
            gc.setFill(col.deriveColor(0, 1, 1, 0.2));
            gc.fillOval(nx[i] - ringR, ny[i] - ringR, ringR * 2, ringR * 2);

            // Solid main node
            gc.setFill(col);
            gc.fillOval(nx[i] - 22, ny[i] - 22, 44, 44);

            // Student first name + similarity % rendered inside the node
            gc.setFill(Color.WHITE);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 8));
            String[] parts = results[i].getStudentname().split(" ");
            gc.fillText(parts[0], nx[i], ny[i] - 3);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 9));
            gc.fillText(String.format("%.0f%%",
                    results[i].getSimilarityScore()), nx[i], ny[i] + 9);

            // Style tag label drawn just below the node
            gc.setFill(col);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            gc.fillText(results[i].getTag(), nx[i], ny[i] + 38);
        }

        // Canvas title and subtitle at the top
        gc.setFill(Color.web("#374151"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("GNN Message Passing Graph", cx, 24);
        gc.setFont(Font.font("Arial", 10));
        gc.setFill(Color.web("#9ca3af"));
        gc.fillText("Edges show which students influenced each other's classification",
                cx, 40);

        // Legend in the bottom-left corner
        double lx = 20, ly = H - 70;
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        gc.setFill(Color.web("#374151"));
        gc.fillText("Legend", lx + 30, ly);
        String[][] legend = {{"PRINT", PRINT_COL}, {"CURSIVE", CURSIVE_COL},
                             {"MIXED",  MIXED_COL}};
        for (int i = 0; i < legend.length; i++) {
            gc.setFill(Color.web(legend[i][1]));
            gc.fillOval(lx, ly + 10 + i * 18, 12, 12);
            gc.setFill(Color.web("#374151"));
            gc.setFont(Font.font("Arial", 10));
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText(legend[i][0], lx + 18, ly + 20 + i * 18);
        }


        VBox page = new VBox(12);
        page.setPadding(new Insets(16));
        page.getChildren().add(canvas);

        Label breakdownTitle = new Label("Per-student GNN classification breakdown");
        breakdownTitle.setStyle("-fx-font-size:13px;-fx-font-weight:bold;");
        page.getChildren().add(breakdownTitle);

        for (int i = 0; i < results.length; i++) {
            if (results[i] == null) continue;
            double[] norm = normalise(graphs[i]);
            page.getChildren().add(buildStudentCard(results[i], norm));
        }

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;");
        return scroll;
    }

    /**
     * Builds one student card: a mini radial node graph on the left and a column
     * of feature progress bars on the right, with the student's name, style
     * badge, and GNN similarity score.
     *
     * @param student    classification result for this student
     * @param normScores five normalised feature scores (0–100), 
     * @return a styled  ready to add to a layout
     */
    private HBox buildStudentCard(StyleOutput student, double[] normScores) {

        HBox card = new HBox(16);
        card.setStyle("-fx-background-color:white;"
                    + "-fx-background-radius:10;"
                    + "-fx-border-color:#e5e7eb;"
                    + "-fx-border-radius:10;"
                    + "-fx-padding:12;");
        card.setAlignment(Pos.CENTER_LEFT);

        // Left radial node graph for this student
        Canvas canvas = new Canvas(220, 220);
        drawNodeGraph(canvas.getGraphicsContext2D(), student, normScores, 220, 220);
        card.getChildren().add(canvas);

        // Right info panel
        VBox info = new VBox(8);
        info.setPrefWidth(280);
        info.setAlignment(Pos.TOP_LEFT);

        // Name + coloured style badge on the same row
        HBox nameRow = new HBox(10);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(student.getStudentname());
        nameLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;");

        Label styleBadge = new Label(student.getTag());
        styleBadge.setStyle("-fx-background-color:" + badgeBgColor(student.getStyle())
                          + ";-fx-text-fill:"        + badgeTextColor(student.getStyle())
                          + ";-fx-padding:2 10;"
                          + "-fx-background-radius:20;"
                          + "-fx-font-size:11px;-fx-font-weight:bold;");
        nameRow.getChildren().addAll(nameLabel, styleBadge);
        info.getChildren().add(nameRow);

        // GNN similarity confidence as a percentage
        Label simLabel = new Label(String.format("Similarity: %.0f%%",
                                   student.getSimilarityScore()));
        simLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#6b7280;");
        info.getChildren().add(simLabel);

        // Small italic note showing the score originates from GNN embeddings
        Label gnnLabel = new Label("GNN-updated embeddings");
        gnnLabel.setStyle("-fx-font-size:10px;-fx-text-fill:#9ca3af;"
                        + "-fx-font-style:italic;");
        info.getChildren().add(gnnLabel);

        // One progress bar per feature
        for (int i = 0; i < FEATURE_NAMES.length; i++) {
            info.getChildren().add(
                featureBar(FEATURE_NAMES[i], normScores[i],
                           toHex(FEATURE_COLORS[i]))
            );
        }

        card.getChildren().add(info);
        return card;
    }
    
    /**
     * Creates a labelled horizontal progress bar for one handwriting feature.
     * The filled width is proportional to clamped to [0, 100]
     * and scaled to a maximum of 240 px.
     *
     * @param name  display label 
     * @param pct   normalised score in [0, 100]
     * @param color CSS hex fill colour for the bar
     * @return a  containing the label row and the track
     */
    private VBox featureBar(String name, double pct, String color) {
        VBox box = new VBox(2);

        // Label row: feature name left-aligned, percentage right-aligned
        HBox labelRow = new HBox();
        labelRow.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(name);
        lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#6b7280;");
        Label val = new Label(String.format("%.0f%%", pct));
        val.setStyle("-fx-font-size:11px;-fx-text-fill:#374151;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        labelRow.getChildren().addAll(lbl, spacer, val);

        // Grey background track
        StackPane track = new StackPane();
        track.setPrefHeight(6);
        track.setStyle("-fx-background-color:#f3f4f6;-fx-background-radius:3;");

        // Coloured fill bar — 100 % maps to 240 px wide
        double fillPct = Math.max(0, Math.min(100, pct));
        HBox fill = new HBox();
        fill.setPrefHeight(6);
        fill.setStyle("-fx-background-color:" + color + ";-fx-background-radius:3;");
        fill.setPrefWidth(fillPct * 2.4);

        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().add(fill);

        box.getChildren().addAll(labelRow, track);
        return box;
    }


    /**
     * Draws a  node graph for a single student onto the given
     * @param gc      the target graphics context
     * @param student classification result (supplies name, style, similarity)
     * @param scores  five normalised score
     * @param w       canvas width in pixels
     * @param h       canvas height in pixels
     */
    private void drawNodeGraph(GraphicsContext gc, StyleOutput student,
                                double[] scores, double w, double h) {
        double cx = w / 2, cy = h / 2;
        double r  = Math.min(w, h) * 0.36;
        int    n  = FEATURE_NAMES.length;

        gc.clearRect(0, 0, w, h);

        // Spoke edges — thickness proportional to each feature's score
        for (int i = 0; i < n; i++) {
            double angle = (2 * Math.PI / n) * i - Math.PI / 2;
            double nx = cx + r * Math.cos(angle);
            double ny = cy + r * Math.sin(angle);
            gc.setStroke(FEATURE_COLORS[i]);
            gc.setLineWidth(1.0 + (scores[i] / 100.0) * 4.0);
            gc.setGlobalAlpha(0.6);
            gc.strokeLine(cx, cy, nx, ny);
            gc.setGlobalAlpha(1.0);
        }

        // Feature nodes — radius grows with the score
        for (int i = 0; i < n; i++) {
            double angle = (2 * Math.PI / n) * i - Math.PI / 2;
            double nx    = cx + r * Math.cos(angle);
            double ny    = cy + r * Math.sin(angle);
            double nr    = 16 + (scores[i] / 100.0) * 10;

            gc.setFill(FEATURE_COLORS[i]);
            gc.fillOval(nx - nr, ny - nr, nr * 2, nr * 2);

            // Feature name and score as white text inside the node
            gc.setFill(Color.WHITE);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 8));
            gc.fillText(FEATURE_NAMES[i], nx, ny - 2);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 9));
            gc.fillText(String.format("%.0f%%", scores[i]), nx, ny + 9);
        }

        // Central student node — colour determined by the classification style
        Color centre = switch (student.getStyle()) {
            case PRINT_BLOCK -> Color.web(PRINT_COL);
            case CURSIVE     -> Color.web(CURSIVE_COL);
            default          -> Color.web(MIXED_COL);
        };
        double cr = 26;
        gc.setFill(centre);
        gc.fillOval(cx - cr, cy - cr, cr * 2, cr * 2);

        // Student name (first and optional second word) + similarity score
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        String[] parts = student.getStudentname().split(" ");
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 8));
        gc.fillText(parts[0], cx, cy - 4);
        if (parts.length > 1) gc.fillText(parts[1], cx, cy + 6);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 9));
        gc.fillText(String.format("%.0f%%", student.getSimilarityScore()),
                    cx, cy + 17);
    }

    /**
     * Builds the Style Distribution tab: a bar chart of style counts above a
     * scrollable results table.
     * @param results classification results to display
     * @return a containing the chart and the table
     */
    private VBox buildChartPage(StyleOutput[] results) {
        VBox page = new VBox(16);
        page.setPadding(new Insets(16));

        page.getChildren().add(buildBarChart(results));

        Label tableTitle = new Label("Student Results");
        tableTitle.setStyle("-fx-font-size:14px;-fx-font-weight:bold;");
        page.getChildren().addAll(tableTitle, buildTable(results));

        return page;
    }

    /**
     * Constructs a barChart with three bars: PRINT, CURSIVE, MIXED.
     *
     * Custom bar colours are applied via a scene-property listener because
     * has been attached to a live scene
     * @param results classification results to tally
     * @return a styled and coloured bar chart
     */
    private BarChart<String, Number> buildBarChart(StyleOutput[] results) {
        int print = 0, cursive = 0, mixed = 0;
        for (StyleOutput r : results) {
            if (r == null) continue;
            switch (r.getStyle()) {
                case PRINT_BLOCK: print++;   break;
                case CURSIVE:     cursive++;  break;
                default:          mixed++;    break;
            }
        }

        int maxCount = Math.max(print, Math.max(cursive, mixed));

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Handwriting Style");

        NumberAxis yAxis = new NumberAxis(0, maxCount + 1, 1);
        yAxis.setLabel("Number of Students");
        yAxis.setMinorTickCount(0);
        // Show integers only — no decimal tick labels
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
            @Override public String toString(Number n) {
                return String.valueOf(n.longValue());
            }
        });

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Handwriting Style Distribution");
        chart.setLegendVisible(false);
        chart.setPrefHeight(320);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("PRINT",   print));
        series.getData().add(new XYChart.Data<>("CURSIVE", cursive));
        series.getData().add(new XYChart.Data<>("MIXED",   mixed));
        chart.getData().add(series);

        // Apply custom bar colours once the chart is part of a live scene
        chart.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                chart.lookupAll(".data0.chart-bar")
                     .forEach(node -> node.setStyle("-fx-bar-fill:" + PRINT_COL   + ";"));
                chart.lookupAll(".data1.chart-bar")
                     .forEach(node -> node.setStyle("-fx-bar-fill:" + CURSIVE_COL + ";"));
                chart.lookupAll(".data2.chart-bar")
                     .forEach(node -> node.setStyle("-fx-bar-fill:" + MIXED_COL   + ";"));
            }
        });

        return chart;
    }

    /**
     * Builds a three-column showing each student's name,
     * style tag, and similarity score formatted as a percentage.
     *
     * @param results classification results to populate the table
     * @return a ready-to-display
     */
    private TableView<String[]> buildTable(StyleOutput[] results) {
        TableView<String[]> table = new TableView<>();
        table.setPrefHeight(250);

        TableColumn<String[], String> nameCol = new TableColumn<>("Student");
        nameCol.setCellValueFactory(c ->
            new javafx.beans.property.SimpleStringProperty(c.getValue()[0]));
        nameCol.setPrefWidth(200);

        TableColumn<String[], String> styleCol = new TableColumn<>("Style");
        styleCol.setCellValueFactory(c ->
            new javafx.beans.property.SimpleStringProperty(c.getValue()[1]));
        styleCol.setPrefWidth(120);

        TableColumn<String[], String> simCol = new TableColumn<>("Similarity");
        simCol.setCellValueFactory(c ->
            new javafx.beans.property.SimpleStringProperty(c.getValue()[2]));
        simCol.setPrefWidth(100);

        table.getColumns().addAll(nameCol, styleCol, simCol);

        for (StyleOutput r : results) {
            if (r == null) continue;
            table.getItems().add(new String[]{
                r.getStudentname(),
                r.getTag(),
                String.format("%.0f%%", r.getSimilarityScore())
            });
        }

        return table;
    }
    
    /**
     * Min-max normalises all five raw feature values from a student's graph to
     * the range [0, 100] for use in progress bars and node graphs.
     *
     * @param graph the student's {@link HandwritingGraph}
     * @return five normalised scores in {@link #FEATURE_KEYS} order
     */
    private double[] normalise(HandwritingGraph graph) {
        double[] out = new double[FEATURE_KEYS.length];
        for (int i = 0; i < FEATURE_KEYS.length; i++) {
            double val = getFeature(graph, FEATURE_KEYS[i]);
            out[i] = Math.max(0, Math.min(100,
                (val - FEAT_MIN[i]) / (FEAT_MAX[i] - FEAT_MIN[i]) * 100));
        }
        return out;
    }

    /**
     * Iterates the graph's nodes and returns the value of the
     * first node whose feature name matches 
     * Returns  if no match is found.
     *
     * @param graph the student's
     * @param name  feature name to find
     * @return the raw feature value, or  if absent
     */
    private double getFeature(HandwritingGraph graph, String name) {
        for (int i = 0; i < graph.getNodeCount(); i++) {
            FeatureNode node = graph.getNode("N" + (i + 1));
            if (node != null && node.getFeatureName().equals(name))
                return node.getValue();
        }
        return 0.0;
    }

    /**
     * Returns the pill background colour for a style badge.
     *
     * @param style the classified handwriting style
     * @return CSS hex colour string
     */
    private String badgeBgColor(HandwritingStyle style) {
        return switch (style) {
            case PRINT_BLOCK -> "#dbeafe";
            case CURSIVE     -> "#ede9fe";
            default          -> "#fef3c7";
        };
    }

    /**
     * Returns the text colour for a style badge pill.
     *
     * @param style the classified handwriting style
     * @return CSS hex colour string
     */
    private String badgeTextColor(HandwritingStyle style) {
        return switch (style) {
            case PRINT_BLOCK -> "#1e40af";
            case CURSIVE     -> "#5b21b6";
            default          -> "#92400e";
        };
    }

    /**
     * Converts a JavaFX  to a lowercase CSS hex string
     * @param c the colour to convert
     * @return a 7-character hex colour string including the leading 
     */
    private String toHex(Color c) {
        return String.format("#%02x%02x%02x",
            (int)(c.getRed()   * 255),
            (int)(c.getGreen() * 255),
            (int)(c.getBlue()  * 255));
    }
}