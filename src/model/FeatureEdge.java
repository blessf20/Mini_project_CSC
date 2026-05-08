package model;

/**
 * Represents a relationship between two FeatureNodes in the handwriting graph.
 * Each edge has a weight that indicates the strength of the relationship
 * and a type that describes the nature of the relationship.
 */
public class FeatureEdge {

    /** Unique identifier for this edge */
    private String id;

    /** The node where this edge starts */
    private FeatureNode sourceNode;

    /** The node where this edge ends */
    private FeatureNode targetNode;

    /** The strength of the relationship between the two nodes */
    private double weight;

    /** The type of relationship between the two nodes (e.g. spatial, structural, pressure) */
    private String relationshipType;

    /**
     * Constructor for FeatureEdge
     * @param id the unique identifier for this edge
     * @param sourceNode the node where this edge starts
     * @param targetNode the node where this edge ends
     * @param weight the strength of the relationship between the two nodes
     * @param relationshipType the type of relationship between the two nodes
     */
    public FeatureEdge(String id, FeatureNode sourceNode, FeatureNode targetNode,
                       double weight, String relationshipType) {
        this.id = id;
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
        this.weight = weight;
        this.relationshipType = relationshipType;
    }

    /**
     * Gets the id of this edge
     * @return the edge id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id of this edge
     * @param id the new edge id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the source node of this edge
     * @return the source node
     */
    public FeatureNode getSourceNode() {
        return sourceNode;
    }

    /**
     * Sets the source node of this edge
     * @param sourceNode the new source node
     */
    public void setSourceNode(FeatureNode sourceNode) {
        this.sourceNode = sourceNode;
    }

    /**
     * Gets the target node of this edge
     * @return the target node
     */
    public FeatureNode getTargetNode() {
        return targetNode;
    }

    /**
     * Sets the target node of this edge
     * @param targetNode the new target node
     */
    public void setTargetNode(FeatureNode targetNode) {
        this.targetNode = targetNode;
    }

    /**
     * Gets the weight of this edge
     * @return the edge weight
     */
    public double getWeight() {
        return weight;
    }

    /**
     * Sets the weight of this edge
     * @param weight the new edge weight
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * Gets the relationship type of this edge
     * @return the relationship type
     */
    public String getRelationshipType() {
        return relationshipType;
    }

    /**
     * Sets the relationship type of this edge
     * @param relationshipType the new relationship type
     */
    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    /**
     * Returns a string representation of this edge
     * @return a string describing the edge
     */
    @Override
    public String toString() {
        return "FeatureEdge{id='" + id + "', " +
               sourceNode.getFeatureName() + " --[" + weight + "]--> " +
               targetNode.getFeatureName() + ", type='" + relationshipType + "'}";
    }
}