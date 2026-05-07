package classification;

import model.HandwritingGraph;
import model.FeatureNode;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
/**
 * GNN classifier for handwriting style classification
 * Each student is a node whose handwriting features are similar hence influencing each other's classication
 * Trained weights are loaded from W1.csv [16x5] and W2.csv [3x16],
 * exported from a PyTorch GCN trained to 96.04% accuracy on labelled
 * handwriting data (CURSIVE, MIXED, PRINT_BLOCK).
 */
public class GNNClassifier {

	//Trained weight matrices loaded from CSV
    private float[][] W1; // [16][5]
    private float[][] W2; // [3][16]
    //stored oputputs from last classifyBatch() call
    private float[][] lastEmbeddings = null;  // stores Z after classifyBatch
    private float[][] lastH1         = null;  // stores H1 (hidden layer)
    

    private static final String[] LABELS = {"CURSIVE", "MIXED", "PRINT_BLOCK"};
    
    /**
     * Constructs the GNNClassifier by loading the trained weights from CSV files 
     */
    public GNNClassifier() {
        W1 = loadMatrix("W1.csv");
        W2 = loadMatrix("W2.csv");
    }

    /**
     * Loads a weight matrix from a CSV file.
     * @param filename ath to the CSV file relative to the working directory
     * @return 2D float array representing the weight matrix
     */
    private float[][] loadMatrix(String filename) {
        try {
            List<float[]> rows = new ArrayList<>();
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                float[] row = new float[parts.length];
                for (int j = 0; j < parts.length; j++)
                    row[j] = Float.parseFloat(parts[j].trim());
                rows.add(row);
            }
            br.close();
            return rows.toArray(new float[0][]);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load weights: " + filename, e);
        }
    }

    /**
     * Run 2-layer GCN over a batch of student graphs.
     * graphs[i] = one student, edges connect similar students.
     * @param graphs list of HandwritingGraphs — one per student
     * @param edges pairs defining kNN connections between students
     * @return Returns predicted style index for each student.
     */
    public HandwritingStyle[] classifyBatch(List<HandwritingGraph> graphs,
    		List<int[]> edges) {
    	int N = graphs.size();
    	float[][] H = buildFeatureMatrix(graphs, N);
    	float[][] A = buildAdjacency(N, edges);

    	float[][] AH  = matMul(A, H);
    	float[][] H1  = matMul(AH, transpose(W1));
    	relu(H1);

    	float[][] AH1 = matMul(A, H1);
    	float[][] Z   = matMul(AH1, transpose(W2));
    	softmaxRows(Z);

    	HandwritingStyle[] results = new HandwritingStyle[N];
    	for (int i = 0; i < N; i++) {
    		int best = argmax(Z[i]);
    		results[i] = HandwritingStyle.valueOf(LABELS[best]);
    	}

    	this.lastEmbeddings = Z;
    	this.lastH1         = H1;
    	return results;
    }

    /**
     * Extract feature matrix from list of graphs — [N][5]
     * @param graphs list of student HandwritingGraphs
     * @param N number of students
     * @return normalised feature matrix
     */
    private float[][] buildFeatureMatrix(List<HandwritingGraph> graphs, int N) {
        String[] feats = {"SlantAngle","LoopFrequency","LetterSpacing","PressureZone","StrokeDensity"};
        float[][] H = new float[N][5];
        for (int i = 0; i < N; i++) {
            HandwritingGraph g = graphs.get(i);
            for (int j = 0; j < feats.length; j++) {
                FeatureNode node = findNode(g, feats[j]);
                H[i][j] = node != null ? (float) node.getValue() : 0f;
            }
        }
        return normalize(H); 
    }


    /**
     * Build symmetrically normalised adjacency matrix from edge list
     * @param N     number of student nodes in the graph
     * @param edges list of undirected [i,j] edge pairs from kNN construction
     * @return      symmetrically normalised adjacency matrix [N][N]
     */
    private float[][] buildAdjacency(int N, List<int[]> edges) {
        float[][] A = new float[N][N];
        // Add self-loops (A + I)
        for (int i = 0; i < N; i++) A[i][i] = 1f;
        for (int[] e : edges) {
            A[e[0]][e[1]] = 1f;
            A[e[1]][e[0]] = 1f; // undirected
        }
        // D^-0.5 normalisation
        float[] deg = new float[N];
        for (int i = 0; i < N; i++)
            for (int j = 0; j < N; j++) deg[i] += A[i][j];
        for (int i = 0; i < N; i++)
            for (int j = 0; j < N; j++)
                A[i][j] /= (float)(Math.sqrt(deg[i]) * Math.sqrt(deg[j]));
        return A;
    }

    /**
     * Matrix multiplication
     * @param A matrix of size [m][k]
     * @param B matrix of size [k][n]
     * @return result matrix C of size [m][n]
     */
    private float[][] matMul(float[][] A, float[][] B) {
        int m=A.length, k=A[0].length, n=B[0].length;
        float[][] C = new float[m][n];
        for(int i=0;i<m;i++) for(int j=0;j<n;j++)
            for(int l=0;l<k;l++) C[i][j] += A[i][l]*B[l][j];
        return C;
    }
    /**
     * Transposes a matrix M of size [r][c] to [c][r].
     * @param M weight matrice
     * @return result
     */
    private float[][] transpose(float[][] M) {
        float[][] T = new float[M[0].length][M.length];
        for(int i=0;i<M.length;i++) for(int j=0;j<M[0].length;j++) T[j][i]=M[i][j];
        return T;
    }
    
    
    public float[][] getLastEmbeddings() { return lastEmbeddings; }
    /**
     * Applies ReLU activation in-place: replaces all negative values with 0.
     * @param M  matrix to apply ReLU to 
     */
    private void relu(float[][] M) {
        for(float[] r:M) 
        	for(int j=0;j<r.length;j++) 
        		r[j]=Math.max(0,r[j]);
    }
    
    /**
     * Uses max-subtraction for numerical stability.
     * @param M matrix of logits
     */
    private void softmaxRows(float[][] M) {
        for(float[] r:M) {
            float max=r[0]; for(float v:r) if(v>max) max=v;
            float sum=0; for(int j=0;j<r.length;j++){r[j]=(float)Math.exp(r[j]-max);sum+=r[j];}
            for(int j=0;j<r.length;j++) r[j]/=sum;
        }
    }
    /**
     * Returns the index of the maximum value in a float array.
     * Used to determine the predicted class from softmax output.
     *
     * @param v float array of class probabilities
     * @return index of the highest probability class
     */
    private int argmax(float[] v) {
        int b=0; 
        for(int i=1;i<v.length;i++) 
        	if(v[i]>v[b]) b=i; 
        return b;
    }
    
    /**
     * Z-score normalises each feature column of H in place.
     * Ensures all features contribute equally regardless of raw scale.
     * A small epsilon (1e-8) is added to std to prevent division by zero.
     *
     * @param H feature matrix [N][F] to normalise (modified in place)
     * @return the normalised matrix H
     */
    private float[][] normalize(float[][] H) {
        int N=H.length, F=H[0].length;
        for(int j=0;j<F;j++) {
            float sum=0,sq=0;
            for(float[] r:H){sum+=r[j];sq+=r[j]*r[j];}
            float mean=sum/N, std=(float)Math.sqrt(sq/N-mean*mean+1e-8f);
            for(float[] r:H) r[j]=(r[j]-mean)/std;
        }
        return H;
    }

    /**
     * Searches a HandwritingGraph for a FeatureNode matching the given name.
     *
     * @param g    the HandwritingGraph to search
     * @param name the feature name to match 
     * @return the matching FeatureNode, or null if not found
     */
    private FeatureNode findNode(HandwritingGraph g, String name) {
        for(int i=0;i<g.getNodeCount();i++){
            FeatureNode n=g.getNode("N"+(i+1));
            if(n!=null && n.getFeatureName().equals(name)) return n;
        }
        return null;
    }
}