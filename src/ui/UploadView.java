package ui;

import algorithm.FeatureExtractor;
import algorithm.GraphSimilarity;
import database.StudentDatabase;
import model.HandwritingGraph;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.io.File;

/**
 * Second UI Interface - Document Matcher
 * Allows uploading a suspect document and matching against database
 */
public class UploadView extends BorderPane {
    
    private StudentDatabase db;
    private FeatureExtractor extractor;
    private GraphSimilarity similarity;
    private ImageView imagePreview;
    private TextArea resultArea;
    private Label fileNameLabel;
    private File selectedFile;
    
    public UploadView() {
        db = StudentDatabase.getInstance();
        extractor = new FeatureExtractor();
        similarity = new GraphSimilarity();
        
        setupUI();
    }
    
    private void setupUI() {
        // Title
        Label title = new Label("Handwriting Analysis - Document Matcher");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
        // Upload area
        VBox uploadArea = createUploadArea();
        
        // Image preview
        imagePreview = new ImageView();
        imagePreview.setFitWidth(300);
        imagePreview.setFitHeight(200);
        imagePreview.setPreserveRatio(true);
        imagePreview.setStyle("-fx-border-color: #ccc; -fx-border-width: 2;");
        
        // Analyze button
        Button btnAnalyze = new Button("Analyze Handwriting");
        btnAnalyze.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        btnAnalyze.setOnAction(e -> analyzeDocument());
        
        // Results area
        resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setPrefHeight(200);
        resultArea.setPromptText("Analysis results will appear here...");
        
        // Layout
        HBox previewBox = new HBox(20, uploadArea, imagePreview);
        previewBox.setAlignment(Pos.CENTER);
        
        VBox centerBox = new VBox(20, previewBox, btnAnalyze, resultArea);
        centerBox.setPadding(new Insets(20));
        
        setTop(title);
        setCenter(centerBox);
        setPadding(new Insets(10));
    }
    
    private VBox createUploadArea() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-border-color: #ccc; -fx-border-width: 2; -fx-border-style: dashed;");
        
        Label dragLabel = new Label("Drag & Drop or Click to Upload");
        dragLabel.setStyle("-fx-font-size: 14px;");
        
        fileNameLabel = new Label("No file selected");
        fileNameLabel.setStyle("-fx-text-fill: #666;");
        
        Button btnBrowse = new Button("Browse Files");
        btnBrowse.setOnAction(e -> browseFile());
        
        box.getChildren().addAll(dragLabel, fileNameLabel, btnBrowse);
        
        // Drag and drop support
        box.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.ANY);
            }
            event.consume();
        });
        
        box.setOnDragDropped(event -> {
            var files = event.getDragboard().getFiles();
            if (files != null && !files.isEmpty()) {
                handleFile(files.get(0));
            }
            event.setDropCompleted(true);
            event.consume();
        });
        
        return box;
    }
    
    private void browseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Handwriting Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );
        
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            handleFile(file);
        }
    }
    
    private void handleFile(File file) {
        selectedFile = file;
        fileNameLabel.setText(file.getName());
        
        // Load and display image
        try {
            Image image = new Image("file:" + file.getAbsolutePath());
            imagePreview.setImage(image);
        } catch (Exception e) {
            imagePreview.setImage(null);
            resultArea.setText("Error loading image: " + e.getMessage());
        }
    }
    
    private void analyzeDocument() {
        if (selectedFile == null) {
            showAlert("No File", "Please upload a handwriting sample first.");
            return;
        }
        
        if (db.getStudentCount() == 0) {
            showAlert("No Data", "No students in database. Please add students first.");
            return;
        }
        
        resultArea.setText("Analyzing handwriting...\n");
        
        try {
            // Extract features from suspect document
            resultArea.appendText("1. Extracting features from document...\n");
            HandwritingGraph suspectGraph = extractor.extractFeatures(
                selectedFile.getAbsolutePath(), "SUSPECT");
            
            resultArea.appendText(String.format("   Extracted %d features\n", suspectGraph.getNodeCount()));
            
            // Display extracted features
            resultArea.appendText("\n2. Extracted Features:\n");
            for (var node : suspectGraph.getAllNodes()) {
                resultArea.appendText(String.format("   • %s: %.2f\n", 
                    node.getFeatureName(), node.getValue()));
            }
            
            // Compare with all students
            resultArea.appendText("\n3. Comparing with database students:\n");
            
            String[] studentIds = db.getAllStudentIds();
            HandwritingGraph[] studentGraphs = new HandwritingGraph[studentIds.length];
            String[] studentNames = new String[studentIds.length];
            
            for (int i = 0; i < studentIds.length; i++) {
                studentGraphs[i] = db.getStudentGraph(studentIds[i]);
                studentNames[i] = db.getStudentName(studentIds[i]);
            }
            
            GraphSimilarity.MatchResult result = similarity.findBestMatch(
                suspectGraph, studentGraphs, studentNames, studentIds);
            
            resultArea.appendText("\n4. Results:\n");
            resultArea.appendText("   ─────────────────────────────\n");
            
            if (result != null) {
                resultArea.appendText(String.format(
                    "   ✓ MATCH FOUND!\n\n" +
                    "   Student ID: %s\n" +
                    "   Student Name: %s\n" +
                    "   Confidence: %.1f%%\n",
                    result.getStudentId(),
                    result.getStudentName(),
                    result.getSimilarityScore() * 100
                ));
                
                // Provide interpretation
                if (result.getSimilarityScore() > 0.8) {
                    resultArea.appendText("\n   High confidence match - Strong evidence\n");
                } else if (result.getSimilarityScore() > 0.6) {
                    resultArea.appendText("\n   Moderate confidence - Further verification recommended\n");
                } else {
                    resultArea.appendText("\n   Low confidence - Not a strong match\n");
                }
            } else {
                resultArea.appendText("   No match found in database\n");
            }
            
            resultArea.appendText("\n   ─────────────────────────────\n");
            
        } catch (Exception e) {
            resultArea.appendText("\nError during analysis: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}