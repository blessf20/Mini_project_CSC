package classification;

import java.util.List;
import model.HandwritingGraph;
import model.FeatureNode;

/**
 * Classifies handwriting graphs into PRINT_BLOCK, CURSIVE, or MIXED.
 *Trained GNN (GNNClassifier) using W1.csv and W2.csv weights.
 *Rule-based classifier triggered automatically if GNN fails.
 */
public class Classifier {

    // Rule-based fallback constants
    private static final double maxCursive      = 9.5;
    private static final double minCursive      = 7.5;
    private static final double minPrint        = 5.0;
    private static final double maxPrint        = 5.5;
    private static final double slantMaxCursive = 108.0;
    private static final double slantPrintMin   = 75.0;
    private static final double slantPrintMax   = 110.0;
    private static final double highPressure    = 65.0;
    private static final double tightSpacing    = 9.0;
    private static final double wideSpacing     = 22.0;

    /**
     * Classifies the students using the Trained GNN USING W1.CSV and W2.csv/ rule based classification and refines the scores by use of a GNN
     * The Gnn adjusts the results based on hpw similar the neighbouring student features are 
     * @param studentIds - list of the student IDs
     * @param names - student names 
     * @param graphs -graphs of each student
     * @param edges -list to adjust the scores 
     * @return = styles with GNN adjustments 
     */
    public static StyleOutput[] classifyBatchGNN(List<String> studentIds,List<String> names,List<HandwritingGraph> graphs,List<int[]> edges) {
    	// classify each student individually using ONNX/rule-based
    	StyleOutput[] results = new StyleOutput[graphs.size()];
        // GNN classification 
        try {
            GNNClassifier gnn = new GNNClassifier();
            HandwritingStyle[] gnnStyles = gnn.classifyBatch(graphs, edges);
            float[][] embeddings = gnn.getLastEmbeddings();

            for (int i = 0; i < graphs.size(); i++) {
                HandwritingStyle style = gnnStyles[i];
                float[] emb = embeddings[i]; // [PRINT, CURSIVE, MIXED]

                // GNN confidence for the predicted class
                float gnnConfidence = switch (style) {
                case CURSIVE     -> emb[0];
                case MIXED       -> emb[1];
                default          -> emb[2]; // PRINT_BLOCK
                };

                int confidence = Math.round(gnnConfidence * 100);
                int cursiveScore = Math.round(emb[0] * 100);
                int mixedScore   = Math.round(emb[1] * 100);
                int printScore   = Math.round(emb[2] * 100);

                StyleOutput result = new StyleOutput(
                    studentIds.get(i), names.get(i),
                    style, printScore, cursiveScore
                );
                result.setSimilarityScore(confidence);

                System.out.println("[GNN] " + names.get(i)
                    + " → " + style + " | confidence: " + confidence + "%");

                results[i] = result;
            }

            return results; 

        } catch (Exception e) {
            System.err.println("[Classifier] GNN failed — falling back to rule-based. "
                + e.getMessage());
        }

        //Fallback: rule-based per student 
        for (int i = 0; i < graphs.size(); i++) {
            results[i] = buildResultRuleBased(
                studentIds.get(i), names.get(i), graphs.get(i));
        }

    	return results;
}

    /**
     * Rule based classification to fall back on oncve the model fails 
     * @param studentId unique identifier
     * @param name - name of students
     * @param graph - graph with feature nodes
     * @return StyleOutput with the handwriting style and the scores 
     */
    private static StyleOutput buildResultRuleBased(String studentId,String name,HandwritingGraph graph) {
        
    	HandwritingStyle style = classify(graph);
        int[]            sc    = calculateScores(graph, style);
        int printScore         = sc[0];
        int cursiveScore       = sc[1];

        StyleOutput result = new StyleOutput(studentId, name, style, printScore, cursiveScore);

        //similarity for rule based 
        double similarityScore;
        switch (style) {
            case PRINT_BLOCK:
                similarityScore = printScore;
                break;
            case CURSIVE:
                similarityScore = cursiveScore;
                break;
            default:
                similarityScore = (printScore + cursiveScore) / 2.0;
                break;
        }

        result.setSimilarityScore(similarityScore);
        System.out.println("[Classifier] Rule-based predicted: " + style
                         + " | similarity: " + similarityScore + "%");
        return result;
    }

 /**
  * Determines the handwriting style based on the scores of each priority be ut cursive , print  or mixed 
  * @param graph HandwritingGrapg  containing the feature nodes 
  * @return HandwritingStyle  enum values 
  */
    public static HandwritingStyle classify(HandwritingGraph graph) {
        try {
            double slant    = getFeature(graph, "SlantAngle");
            double spacing  = getFeature(graph, "LetterSpacing");
            double loops    = getFeature(graph, "LoopFrequency");
            double pressure = getFeature(graph, "PressureZone");

            if (loops > maxCursive && slant > slantMaxCursive)
                return HandwritingStyle.CURSIVE;
            if (loops < maxPrint && slant >= 78 && slant <= 105)
                return HandwritingStyle.PRINT_BLOCK;
            if (loops >= minPrint && loops <= maxCursive
                    && slant >= 80 && slant <= 115)
                return HandwritingStyle.MIXED;
            if (loops > 9.0 && slant >= slantPrintMin
                    && slant <= slantMaxCursive)
                return HandwritingStyle.MIXED;
            if (loops < 6.0 && slant > 115)
                return HandwritingStyle.MIXED;
            if ((spacing < tightSpacing || spacing > wideSpacing)
                    && loops >= 5.0 && loops <= 10.0)
                return HandwritingStyle.MIXED;
            if (pressure > highPressure && loops >= 5.0 && loops <= 9.0)
                return HandwritingStyle.MIXED;
            if (loops > minCursive)
                return HandwritingStyle.CURSIVE;
            if (loops < minPrint
                    && slant >= slantPrintMin && slant <= slantPrintMax)
                return HandwritingStyle.PRINT_BLOCK;

            return HandwritingStyle.MIXED;

        } catch (IllegalArgumentException e) {
        	// if a feature is missing from the graph 
            System.err.println("Missing features: " + e.getMessage());
            return HandwritingStyle.MIXED;
        }
    }

   /**
    * Calculates the scores based on how strongly rge features align with each handwriting style
    * Print signals:   few loops, wide spacing, upright slant, light pressure, low density
    * Cursive signals: many loops, tight spacing, forward slant, heavy pressure, high density
    * @param graph HandwritingGraph containing the 5 feature nodes
    * @param style the styles of the handwriting 
    * @return the scores of each handwriting feature
    */
    private static int[] calculateScores(HandwritingGraph graph,HandwritingStyle style) {
        try {
        	
            double loops    = getFeature(graph, "LoopFrequency");
            double slant    = getFeature(graph, "SlantAngle");
            double spacing  = getFeature(graph, "LetterSpacing");
            double pressure = getFeature(graph, "PressureZone");
            double density  = getFeature(graph, "StrokeDensity");

            //features that influence the PRINT score
            int printSignals = 0;
            if (loops < 4.0)                  printSignals++;
            if (spacing > 18.0)               printSignals++;
            if (slant >= 75 && slant <= 105)  printSignals++;
            if (pressure < 40.0)              printSignals++;
            if (density < 2.5)                printSignals++;

            //features that influence the cursive score 
            int cursiveSignals = 0;
            if (loops > 8.0)                  cursiveSignals++;
            if (slant > 105.0)                cursiveSignals++;
            if (spacing < 12.0)               cursiveSignals++;
            if (pressure > 55.0)              cursiveSignals++;
            if (density > 4.0)                cursiveSignals++;

            int printScore, cursiveScore;
            if (style == HandwritingStyle.PRINT_BLOCK) {
                printScore   = Math.min(100, 60 + (printSignals * 10));
                cursiveScore = Math.max(0, 100 - printScore);
            } else if (style == HandwritingStyle.CURSIVE) {
                cursiveScore = Math.min(100, 60 + (cursiveSignals * 10));
                printScore   = Math.max(0, 100 - cursiveScore);
            } else {
                printScore   = 40 + (printSignals * 5);
                cursiveScore = 40 + (cursiveSignals * 5);
            }

            printScore   = Math.max(0, Math.min(100, printScore));
            cursiveScore = Math.max(0, Math.min(100, cursiveScore));
            return new int[]{printScore, cursiveScore};

        } catch (IllegalArgumentException e) {
            System.err.println("Missing feature in scoring: " + e.getMessage());
            return new int[]{50, 50};
        }
    }


    /**
     * Extracts a feature value by name. Throws if not found.
     * @param graph the HandwritingGraph to search
     * @param featureName exact feature to match
     * @return value of the faeture node
     */
    private static double getFeature(HandwritingGraph graph,String featureName) {
        FeatureNode node = findNode(graph, featureName);
        if (node == null) throw new IllegalArgumentException(featureName);
        return node.getValue();
    }

    /**
     * Searches the graph for a node matching featureName.
     * @param graph        the HandwritingGraph to search
     * @param featureName  the feature name to match
     * @return             the matching FeatureNode, or null if not found
     */
    private static FeatureNode findNode(HandwritingGraph graph,String featureName) {
        for (int i = 0; i < graph.getNodeCount(); i++) {
            String nodeId = "N" + (i + 1);
            FeatureNode node = graph.getNode(nodeId);
            if (node != null
                    && node.getFeatureName().equals(featureName)) {
                return node;
            }
        }
        return null;
    }
}