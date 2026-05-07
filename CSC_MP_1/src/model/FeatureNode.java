package model;

public class FeatureNode {

    private String id;
    private String featureName;
    private double value;

    // Pixel coordinates — used by ImageSkeletonisationGraph to position
    // skeleton nodes directly on the handwriting image.
    // Default to -1 when not a spatial node (e.g. feature-vector nodes).
    private double x;
    private double y;

    /**
     * Standard constructor — used by FeatureExtractor for the 5 handwriting
     * feature nodes (SlantAngle, LetterSpacing, etc.).
     * x and y default to -1 (no screen position).
     */
    public FeatureNode(String id, String featureName, double value) {
        this.id          = id;
        this.featureName = featureName;
        this.value       = value;
        this.x           = -1;
        this.y           = -1;
    }

    /**
     * Spatial constructor — used by ImageSkeletonisationGraph to place nodes
     * at specific pixel coordinates on the skeleton image.
     */
    public FeatureNode(String id, String featureName, double value, double x, double y) {
        this.id          = id;
        this.featureName = featureName;
        this.value       = value;
        this.x           = x;
        this.y           = y;
    }

    public String getId()                        { return id; }
    public void   setId(String id)               { this.id = id; }

    public String getFeatureName()               { return featureName; }
    public void   setFeatureName(String name)    { this.featureName = name; }

    public double getValue()                     { return value; }
    public void   setValue(double value)         { this.value = value; }

    public double getX()                         { return x; }
    public double getY()                         { return y; }

    @Override
    public String toString() {
        return "FeatureNode{id='" + id + "', feature='" + featureName +
               "', value=" + value + "}";
    }
}