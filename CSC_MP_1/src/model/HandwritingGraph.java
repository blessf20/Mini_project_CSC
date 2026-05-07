package model;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a graph of handwriting features for a single student.
 *
 * Two constructors:
 *   HandwritingGraph(studentId)
 *       — standard graph for 5 feature nodes. Capacity: 10 nodes, 20 edges.
 *
 *   HandwritingGraph(studentId, maxNodes, maxEdges)
 *       — large graph for skeleton pixel nodes. Pass e.g. 2000, 5000.
 *         Used by ImageSkeletonisationGraph which can detect hundreds of
 *         junction/endpoint nodes across a handwriting image.
 *
 * The static MAX_NODES / MAX_EDGES constants are gone — limits are now
 * per-instance so the two use cases don't interfere with each other.
 */
public class HandwritingGraph {

    private String studentId;
    private FeatureNode[] nodes;
    private FeatureEdge[] edges;
    private int nodeCount;
    private int edgeCount;

    // Per-instance capacity — set by constructor
    private final int maxNodes;
    private final int maxEdges;

    // GNN edge list (used by classifier)
    private List<int[]> gnnEdges = new ArrayList<>();

    /**
     * Standard constructor — for 5 handwriting feature nodes.
     * Capacity: 10 nodes, 20 edges.
     */
    public HandwritingGraph(String studentId) {
        this(studentId, 10, 20);
    }

    /**
     * Large-capacity constructor — for skeleton pixel graphs.
     * ImageSkeletonisationGraph uses this so it can store hundreds of nodes.
     *
     * Example:
     *   new HandwritingGraph(studentId, 2000, 5000)
     */
    public HandwritingGraph(String studentId, int maxNodes, int maxEdges) {
        this.studentId = studentId;
        this.maxNodes  = maxNodes;
        this.maxEdges  = maxEdges;
        this.nodes     = new FeatureNode[maxNodes];
        this.edges     = new FeatureEdge[maxEdges];
        this.nodeCount = 0;
        this.edgeCount = 0;
    }


    public void addGNNEdge(int fromIdx, int toIdx) {
        gnnEdges.add(new int[]{fromIdx, toIdx});
    }

    public List<int[]> getGNNEdges() { return gnnEdges; }


    public boolean addNode(FeatureNode node) {
        if (nodeCount >= maxNodes) {
            System.out.println("Graph is full, cannot add node: " + node.getId());
            return false;
        }
        if (findNodeIndex(node.getId()) != -1) {
            System.out.println("Node " + node.getId() + " already exists.");
            return false;
        }
        nodes[nodeCount++] = node;
        // Only print for small graphs — skeleton graphs add hundreds of nodes
        // and spamming the console makes everything slow
        if (maxNodes <= 10) System.out.println("Node added: " + node);
        return true;
    }

    public FeatureNode getNode(String nodeId) {
        int index = findNodeIndex(nodeId);
        if (index == -1) {
            System.out.println("Node " + nodeId + " not found.");
            return null;
        }
        return nodes[index];
    }

    public FeatureNode[] getAllNodes() {
        FeatureNode[] result = new FeatureNode[nodeCount];
        for (int i = 0; i < nodeCount; i++) result[i] = nodes[i];
        return result;
    }

    public boolean updateNode(String nodeId, String newFeatureName, double newValue) {
        int index = findNodeIndex(nodeId);
        if (index == -1) {
            System.out.println("Node " + nodeId + " not found.");
            return false;
        }
        nodes[index].setFeatureName(newFeatureName);
        nodes[index].setValue(newValue);
        System.out.println("Node updated: " + nodes[index]);
        return true;
    }

    public boolean removeNode(String nodeId) {
        int index = findNodeIndex(nodeId);
        if (index == -1) {
            System.out.println("Node " + nodeId + " not found.");
            return false;
        }
        removeEdgesForNode(nodeId);
        for (int i = index; i < nodeCount - 1; i++) nodes[i] = nodes[i + 1];
        nodes[--nodeCount] = null;
        System.out.println("Node " + nodeId + " removed.");
        return true;
    }


    public boolean addEdge(FeatureEdge edge) {
        if (edgeCount >= maxEdges) {
            System.out.println("Graph is full, cannot add edge: " + edge.getId());
            return false;
        }
        if (findNodeIndex(edge.getSourceNode().getId()) == -1 ||
            findNodeIndex(edge.getTargetNode().getId()) == -1) {
            System.out.println("Both nodes must exist before adding an edge.");
            return false;
        }
        if (findEdgeIndex(edge.getId()) != -1) {
            System.out.println("Edge " + edge.getId() + " already exists.");
            return false;
        }
        edges[edgeCount++] = edge;
        if (maxEdges <= 20) System.out.println("Edge added: " + edge);
        return true;
    }

    public FeatureEdge getEdge(String edgeId) {
        int index = findEdgeIndex(edgeId);
        if (index == -1) {
            System.out.println("Edge " + edgeId + " not found.");
            return null;
        }
        return edges[index];
    }

    public FeatureEdge[] getAllEdges() {
        FeatureEdge[] result = new FeatureEdge[edgeCount];
        for (int i = 0; i < edgeCount; i++) result[i] = edges[i];
        return result;
    }

    public boolean updateEdge(String edgeId, double newWeight, String newRelationshipType) {
        int index = findEdgeIndex(edgeId);
        if (index == -1) {
            System.out.println("Edge " + edgeId + " not found.");
            return false;
        }
        edges[index].setWeight(newWeight);
        edges[index].setRelationshipType(newRelationshipType);
        System.out.println("Edge updated: " + edges[index]);
        return true;
    }

    public boolean removeEdge(String edgeId) {
        int index = findEdgeIndex(edgeId);
        if (index == -1) {
            System.out.println("Edge " + edgeId + " not found.");
            return false;
        }
        for (int i = index; i < edgeCount - 1; i++) edges[i] = edges[i + 1];
        edges[--edgeCount] = null;
        System.out.println("Edge " + edgeId + " removed.");
        return true;
    }

    public String getStudentId()           { return studentId; }
    public void   setStudentId(String id)  { this.studentId = id; }
    public int    getNodeCount()           { return nodeCount; }
    public int    getEdgeCount()           { return edgeCount; }

    public FeatureEdge[] getNeighbourEdges(String nodeId) {
        FeatureEdge[] tmp = new FeatureEdge[edgeCount];
        int count = 0;
        for (int i = 0; i < edgeCount; i++) {
            if (edges[i].getSourceNode().getId().equals(nodeId) ||
                edges[i].getTargetNode().getId().equals(nodeId)) {
                tmp[count++] = edges[i];
            }
        }
        FeatureEdge[] result = new FeatureEdge[count];
        for (int i = 0; i < count; i++) result[i] = tmp[i];
        return result;
    }

    public double[] toFeatureVector() {
        double[] v = new double[nodeCount];
        for (int i = 0; i < nodeCount; i++) v[i] = nodes[i].getValue();
        return v;
    }

    public double[] toEdgeWeightVector() {
        double[] v = new double[edgeCount];
        for (int i = 0; i < edgeCount; i++) v[i] = edges[i].getWeight();
        return v;
    }

    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"studentId\":\"").append(studentId).append("\",");
        sb.append("\"nodes\":[");
        for (int i = 0; i < nodeCount; i++) {
            FeatureNode n = nodes[i];
            sb.append("{")
              .append("\"id\":\"").append(n.getId()).append("\",")
              .append("\"feature\":\"").append(n.getFeatureName()).append("\",")
              .append("\"value\":").append(n.getValue())
              .append("}");
            if (i < nodeCount - 1) sb.append(",");
        }
        sb.append("],\"edges\":[");
        for (int i = 0; i < edgeCount; i++) {
            FeatureEdge e = edges[i];
            sb.append("{")
              .append("\"src\":\"").append(e.getSourceNode().getId()).append("\",")
              .append("\"tgt\":\"").append(e.getTargetNode().getId()).append("\",")
              .append("\"weight\":").append(e.getWeight())
              .append("}");
            if (i < edgeCount - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }


    private int findNodeIndex(String nodeId) {
        for (int i = 0; i < nodeCount; i++)
            if (nodes[i].getId().equals(nodeId)) return i;
        return -1;
    }

    private int findEdgeIndex(String edgeId) {
        for (int i = 0; i < edgeCount; i++)
            if (edges[i].getId().equals(edgeId)) return i;
        return -1;
    }

    private void removeEdgesForNode(String nodeId) {
        for (int i = edgeCount - 1; i >= 0; i--) {
            if (edges[i].getSourceNode().getId().equals(nodeId) ||
                edges[i].getTargetNode().getId().equals(nodeId)) {
                for (int j = i; j < edgeCount - 1; j++) edges[j] = edges[j + 1];
                edges[--edgeCount] = null;
            }
        }
    }

    @Override
    public String toString() {
        return "HandwritingGraph{studentId='" + studentId +
               "', nodes=" + nodeCount + ", edges=" + edgeCount + "}";
    }
}