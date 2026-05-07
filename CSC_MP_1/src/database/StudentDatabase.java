package database;

import algorithm.FeatureExtractor;
import model.FeatureEdge;
import model.FeatureNode;
import model.HandwritingGraph;

/**
 * Singleton database holding all student records and their handwriting graphs.
 * Uses parallel fixed-size arrays (no Java collections) as required.
 */
public class StudentDatabase {

    private static final int MAX_STUDENTS = 100;
    private static StudentDatabase instance;

    private String[]          studentIds;
    private String[]          studentNames;
    private String[]          studentImagePaths;
    private HandwritingGraph[] graphs;
    private int               studentCount;

    private StudentDatabase() {
        studentIds        = new String[MAX_STUDENTS];
        studentNames      = new String[MAX_STUDENTS];
        studentImagePaths = new String[MAX_STUDENTS];
        graphs            = new HandwritingGraph[MAX_STUDENTS];
        studentCount      = 0;
    }

    public static StudentDatabase getInstance() {
        if (instance == null) instance = new StudentDatabase();
        return instance;
    }


    public boolean addStudent(String studentId, String name,
                              String imagePath, HandwritingGraph graph) {
        if (studentCount >= MAX_STUDENTS) {
            System.out.println("Database full.");
            return false;
        }
        if (studentExists(studentId)) {
            System.out.println("Student " + studentId + " already exists.");
            return false;
        }
        studentIds[studentCount]        = studentId;
        studentNames[studentCount]      = name;
        studentImagePaths[studentCount] = imagePath;
        graphs[studentCount]            = graph;
        studentCount++;
        System.out.println("Student added: " + studentId);
        return true;
    }

    public String getStudentName(String studentId) {
        int i = findStudentIndex(studentId);
        return i == -1 ? null : studentNames[i];
    }

    public HandwritingGraph getStudentGraph(String studentId) {
        int i = findStudentIndex(studentId);
        return i == -1 ? null : graphs[i];
    }

    public String[] getAllStudentIds() {
        String[] ids = new String[studentCount];
        for (int i = 0; i < studentCount; i++) ids[i] = studentIds[i];
        return ids;
    }

    /**
     * Returns all HandwritingGraphs in the same order as getAllStudentIds().
     * Used by Main.refreshClassificationGraph() and GNNBridge.
     */
    public HandwritingGraph[] getAllGraphs() {
        HandwritingGraph[] result = new HandwritingGraph[studentCount];
        for (int i = 0; i < studentCount; i++) result[i] = graphs[i];
        return result;
    }

    public boolean updateStudentName(String studentId, String newName) {
        int i = findStudentIndex(studentId);
        if (i == -1) { System.out.println("Student " + studentId + " not found."); return false; }
        studentNames[i] = newName;
        System.out.println("Student " + studentId + " name updated to: " + newName);
        return true;
    }

    public boolean updateStudentImagePath(String studentId, String newPath) {
        int i = findStudentIndex(studentId);
        if (i == -1) { System.out.println("Student " + studentId + " not found."); return false; }
        studentImagePaths[i] = newPath;
        return true;
    }

    public boolean updateStudentNode(String studentId, String nodeId,
                                     String newFeatureName, double newValue) {
        int i = findStudentIndex(studentId);
        if (i == -1) { System.out.println("Student " + studentId + " not found."); return false; }
        return graphs[i].updateNode(nodeId, newFeatureName, newValue);
    }

    public boolean removeStudent(String studentId) {
        int i = findStudentIndex(studentId);
        if (i == -1) { System.out.println("Student " + studentId + " not found."); return false; }
        for (int j = i; j < studentCount - 1; j++) {
            studentIds[j]        = studentIds[j + 1];
            studentNames[j]      = studentNames[j + 1];
            studentImagePaths[j] = studentImagePaths[j + 1];
            graphs[j]            = graphs[j + 1];
        }
        studentIds[--studentCount]        = null;
        studentNames[studentCount]        = null;
        studentImagePaths[studentCount]   = null;
        graphs[studentCount]              = null;
        System.out.println("Student " + studentId + " removed.");
        return true;
    }


    public int     getStudentCount()            { return studentCount; }
    public boolean studentExists(String id)     { return findStudentIndex(id) != -1; }

    /**
     * Preloads 5 sample students.
     * FeatureExtractor now runs the full pipeline internally
     * (RGB -> greyscale -> Niblack -> Zhang-Suen -> features).
     */
    public void preloadStudents() {
        FeatureExtractor extractor = new FeatureExtractor();

        HandwritingGraph g1 = extractor.extractFeatures("data/students/S001/sample.png", "S001");
        HandwritingGraph g2 = extractor.extractFeatures("data/students/S002/sample.png", "S002");
        HandwritingGraph g3 = extractor.extractFeatures("data/students/S003/sample.png", "S003");
        HandwritingGraph g4 = extractor.extractFeatures("data/students/S004/sample.png", "S004");
        HandwritingGraph g5 = extractor.extractFeatures("data/students/S005/sample.png", "S005");
        HandwritingGraph g6 = extractor.extractFeatures("data/students/S006/sample.png", "S006");
        HandwritingGraph g7 = extractor.extractFeatures("data/students/S007/sample.png", "S007");

        

        addDefaultEdges(g1); addDefaultEdges(g2); addDefaultEdges(g3);
        addDefaultEdges(g4); addDefaultEdges(g5); addDefaultEdges(g6);
        addDefaultEdges(g7);

        addStudent("S001", "Alice Dlamini",   "data/students/S001/sample.png", g1);
        addStudent("S002", "Sipho Nkosi",     "data/students/S002/sample.png", g2);
        addStudent("S003", "Lerato Mokoena",  "data/students/S003/sample.png", g3);
        addStudent("S004", "Thabo Mahlangu",  "data/students/S004/sample.png", g4);
        addStudent("S005", "Nomsa Zulu",      "data/students/S005/sample.png", g5);
        addStudent("S006", "Zanele Khumalo",  "data/students/S006/sample.png", g6);
        addStudent("S007", "Mandla Sithole",  "data/students/S007/sample.png", g7);

        System.out.println("Database preloaded with " + studentCount + " students.");
    }

    /**
     * Adds weighted edges between feature-node pairs.
     * Weight = normalised similarity between the two feature values (0.0 – 1.0).
     */
    private void addDefaultEdges(HandwritingGraph graph) {
        FeatureNode slant = null, spacing = null, density = null,
                    loop  = null, pressure = null;

        for (FeatureNode n : graph.getAllNodes()) {
            switch (n.getFeatureName()) {
                case "SlantAngle":    slant    = n; break;
                case "LetterSpacing": spacing  = n; break;
                case "StrokeDensity": density  = n; break;
                case "LoopFrequency": loop     = n; break;
                case "PressureZone":  pressure = n; break;
            }
        }

        if (slant   != null && density  != null)
            graph.addEdge(new FeatureEdge("E1", slant,   density,  edgeWeight(slant.getValue(),   density.getValue(),  110.0), "spatial"));
        if (spacing != null && loop     != null)
            graph.addEdge(new FeatureEdge("E2", spacing, loop,     edgeWeight(spacing.getValue(), loop.getValue(),     28.0),  "structural"));
        if (density != null && pressure != null)
            graph.addEdge(new FeatureEdge("E3", density, pressure, edgeWeight(density.getValue(), pressure.getValue(), 70.0),  "pressure"));
        if (slant   != null && spacing  != null)
            graph.addEdge(new FeatureEdge("E4", slant,   spacing,  edgeWeight(slant.getValue(),   spacing.getValue(),  115.0), "spatial"));
    }

    /** Closer values -> weight closer to 1.0. */
    private double edgeWeight(double a, double b, double maxDiff) {
        return Math.round((1.0 - Math.min(Math.abs(a - b) / maxDiff, 1.0)) * 100.0) / 100.0;
    }

    private int findStudentIndex(String studentId) {
        for (int i = 0; i < studentCount; i++)
            if (studentIds[i].equals(studentId)) return i;
        return -1;
    }
    
 // In StudentDatabase.java
    public String getStudentImagePath(String studentId) {
        int i = findStudentIndex(studentId);
        return i == -1 ? null : studentImagePaths[i];
    }

    @Override
    public String toString() {
        return "StudentDatabase{studentCount=" + studentCount + "}";
    }
}