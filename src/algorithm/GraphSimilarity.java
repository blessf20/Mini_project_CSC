package algorithm;

import model.HandwritingGraph;

/**
 * Compares handwriting graphs using cosine similarity on feature vectors.
 */
public class GraphSimilarity {

    /**
     * Calculates similarity between two graphs using both node values and edge weights.
     * The contribution of nodes vs edges is determined dynamically based on how many
     * of each exist in the graph — no hardcoded split ratio.
     *
     * finalScore = nodeRatio * nodeSimilarity + edgeRatio * edgeSimilarity
     * where nodeRatio = nodeCount / (nodeCount + edgeCount)
     *
     * @param graph1 first handwriting graph
     * @param graph2 second handwriting graph
     * @return similarity score between 0 and 1 (1 = identical)
     */
    public double calculateSimilarity(HandwritingGraph graph1, HandwritingGraph graph2) {
        double[] nodeVector1 = graph1.toFeatureVector();
        double[] nodeVector2 = graph2.toFeatureVector();
        double nodeSimilarity = cosineSimilarity(nodeVector1, nodeVector2);

        double[] edgeVector1 = graph1.toEdgeWeightVector();
        double[] edgeVector2 = graph2.toEdgeWeightVector();

        // If either graph has no edges, fall back to node similarity only
        if (edgeVector1.length == 0 || edgeVector2.length == 0) {
            return nodeSimilarity;
        }

        double edgeSimilarity = cosineSimilarity(edgeVector1, edgeVector2);

        // Dynamic split: more edges = edges contribute more to the final score
        int nodeCount = graph1.getNodeCount();
        int edgeCount = graph1.getEdgeCount();
        double total = nodeCount + edgeCount;
        double nodeRatio = nodeCount / total;
        double edgeRatio = edgeCount / total;

        return (nodeRatio * nodeSimilarity) + (edgeRatio * edgeSimilarity);
    }
    
    /**
     * Finds the best matching student for a suspect graph
     * @param suspectGraph the graph to match
     * @param studentGraphs array of known student graphs
     * @param studentNames array of corresponding student names
     * @return the ID of the best matching student
     */
    public MatchResult findBestMatch(HandwritingGraph suspectGraph, 
                                      HandwritingGraph[] studentGraphs,
                                      String[] studentNames,
                                      String[] studentIds) {
        double bestScore = -1;
        int bestIndex = -1;
        
        for (int i = 0; i < studentGraphs.length; i++) {
            double score = calculateSimilarity(suspectGraph, studentGraphs[i]);
            System.out.printf("Similarity with %s: %.2f%%\n", studentNames[i], score * 100);
            
            if (score > bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }
        
        if (bestIndex != -1) {
            return new MatchResult(studentIds[bestIndex], studentNames[bestIndex], bestScore);
        }
        
        return null;
    }
    
    /**
     * Calculates cosine similarity between two vectors
     */
    private double cosineSimilarity(double[] vectorA, double[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        
        if (normA == 0 || normB == 0) {
            return 0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    /**
     * Inner class to hold match results
     */
    public static class MatchResult {
        private String studentId;
        private String studentName;
        private double similarityScore;
        
        public MatchResult(String studentId, String studentName, double similarityScore) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.similarityScore = similarityScore;
        }
        
        public String getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public double getSimilarityScore() { return similarityScore; }
        
        @Override
        public String toString() {
            return String.format("Match: %s (%s) - %.1f%% confidence", 
                               studentName, studentId, similarityScore * 100);
        }
    }
}