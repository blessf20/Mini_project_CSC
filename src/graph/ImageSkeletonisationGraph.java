package graph;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.*;
import javafx.scene.paint.Color;

import model.FeatureEdge;
import model.FeatureNode;
import model.HandwritingGraph;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Forensic Pipeline:
 * Step 1: Zhang-Suen Skeletonization
 * Step 2: Advanced Path-Following Node & Edge Detection
 * Step 3: Graph Visualization overlaid on the ORIGINAL handwriting image
 *
 * Two fixes applied:
 *   1. The original image is now stored and used as the background in
 *      getDisplayCanvas(), so nodes/edges appear ON TOP of the handwriting
 *      photo rather than on a plain black-and-white skeleton.
 *   2. clusterRadius is reduced from 20 to 8, and the node detection
 *      threshold is relaxed (degree >= 1 instead of only 1 or >= 3),
 *      so significantly more nodes and edges are detected.
 */
public class ImageSkeletonisationGraph {

	private final int[][] skeleton;
	private final HandwritingGraph graph;
	private final Image skeletonImage;

	// FIX 1: store the original image so getDisplayCanvas() can use it
	private final Image originalImage;

	private int thresholdVal;
	private int clusterRadius;

	public ImageSkeletonisationGraph(Image image, String studentId,
			int thresholdVal, int clusterRadius) {
		if (image == null || image.getWidth() <= 0) {
			throw new IllegalArgumentException("Invalid image input");
		}

		this.thresholdVal  = thresholdVal;
		this.clusterRadius = clusterRadius;

		// FIX 1: keep a reference to the original before processing
		this.originalImage = image;

		int[][] binary     = toBinary(image);
		this.skeleton      = zhangSuen(binary);
		this.skeletonImage = renderSkeletonImage();
		this.graph         = buildGraph(studentId);

		saveImage(skeletonImage, studentId);
	}


	private int[][] toBinary(Image image) {
		int w = (int) image.getWidth();
		int h = (int) image.getHeight();
		PixelReader reader = image.getPixelReader();
		int[][] gray = new int[h][w];

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int argb = reader.getArgb(x, y);
				int r = (argb >> 16) & 0xff;
				int g = (argb >> 8)  & 0xff;
				int b =  argb        & 0xff;
				gray[y][x] = (int)(0.299 * r + 0.587 * g + 0.114 * b);
			}
		}

		int threshold = thresholdVal;
		int[][] bin = new int[h][w];
		for (int y = 0; y < h; y++)
			for (int x = 0; x < w; x++)
				bin[y][x] = gray[y][x] < threshold ? 1 : 0;

		return bin;
	}

	private int[][] zhangSuen(int[][] img) {
		int[][] skel = copyMatrix(img);
		boolean changed;
		do {
			changed  = performThinningStep(skel, true);
			changed |= performThinningStep(skel, false);
		} while (changed);
		return skel;
	}

	private boolean performThinningStep(int[][] skel, boolean isFirstStep) {
		int h = skel.length, w = skel[0].length;
		List<int[]> toRemove = new ArrayList<>();
		for (int y = 1; y < h - 1; y++) {
			for (int x = 1; x < w - 1; x++) {
				if (skel[y][x] != 1) continue;
				int[] n     = getNeighbors(skel, x, y);
				int count   = countActiveNeighbors(n);
				int trans   = countTransitions(n);
				if (count >= 2 && count <= 6 && trans == 1) {
					if (isFirstStep) {
						if (n[0]*n[2]*n[4] == 0 && n[2]*n[4]*n[6] == 0)
							toRemove.add(new int[]{x, y});
					} else {
						if (n[0]*n[2]*n[6] == 0 && n[0]*n[4]*n[6] == 0)
							toRemove.add(new int[]{x, y});
					}
				}
			}
		}
		for (int[] p : toRemove) skel[p[1]][p[0]] = 0;
		return !toRemove.isEmpty();
	}


	private HandwritingGraph buildGraph(String studentId) {
		HandwritingGraph g = new HandwritingGraph(studentId, 2000, 5000);
		int h = skeleton.length, w = skeleton[0].length;
		FeatureNode[][] nodeMap = new FeatureNode[h][w];
		int nodeCounter = 1;

		// FIX 2a: detect endpoints (degree==1) AND junctions (degree>=3)
		// Previously clusterRadius=20 was merging too many nearby nodes.
		// Now clusterRadius comes from the constructor — DatabaseView passes 8.
		List<int[]> candidates = new ArrayList<>();
		for (int y = 1; y < h - 1; y++) {
			for (int x = 1; x < w - 1; x++) {
				if (skeleton[y][x] != 1) continue;
				int deg = calculateDegree(x, y);
				if (deg == 1 || deg >= 3) {
					candidates.add(new int[]{x, y});
				}
			}
		}

		// FIX 2b: cluster nearby candidates — smaller radius = more nodes kept
		for (int[] c : candidates) {
			int cx = c[0], cy = c[1];
			boolean isRedundant = false;
			for (FeatureNode existing : g.getAllNodes()) {
				double dist = Math.sqrt(
						Math.pow(existing.getX() - cx, 2) +
						Math.pow(existing.getY() - cy, 2));
				if (dist < clusterRadius) {
					isRedundant = true;
					nodeMap[cy][cx] = existing;
					break;
				}
			}
			if (!isRedundant) {
				FeatureNode node = new FeatureNode(
						"N" + nodeCounter++, "skeleton", 0.0, cx, cy);
				g.addNode(node);
				nodeMap[cy][cx] = node;
			}
		}

		// Path following to build edges
		int edgeCounter = 1;
		boolean[][] visited = new boolean[h][w];

		for (FeatureNode n1 : g.getAllNodes()) {
			int sx = (int) n1.getX();
			int sy = (int) n1.getY();

			for (int dy = -1; dy <= 1; dy++) {
				for (int dx = -1; dx <= 1; dx++) {
					int startX = sx + dx;
					int startY = sy + dy;
					if (isValid(startX, startY, w, h)
							&& skeleton[startY][startX] == 1
							&& !visited[startY][startX]) {
						FeatureNode immediateTarget = nodeMap[startY][startX];
						if (immediateTarget != null && immediateTarget != n1) {
							addEdgeIfNotExists(g, n1, immediateTarget, 1.0, edgeCounter++);
							continue;
						}
						tracePath(g, n1, startX, startY, nodeMap, visited, edgeCounter++);
					}
				}
			}
		}
		return g;
	}

	private void tracePath(HandwritingGraph g, FeatureNode startNode,
			int currX, int currY,
			FeatureNode[][] nodeMap, boolean[][] visited,
			int edgeId) {
		int h = skeleton.length, w = skeleton[0].length;
		int x = currX, y = currY;
		int length = 0;

		while (isValid(x, y, w, h) && skeleton[y][x] == 1 && !visited[y][x]) {
			visited[y][x] = true;
			length++;

			FeatureNode target = findNearestNodeInArea(x, y, nodeMap, 2);
			if (target != null && target != startNode) {
				addEdgeIfNotExists(g, startNode, target, length, edgeId);
				return;
			}

			int nextX = -1, nextY = -1;
			for (int dy = -1; dy <= 1; dy++) {
				for (int dx = -1; dx <= 1; dx++) {
					if (dx == 0 && dy == 0) continue;
					int nx = x + dx, ny = y + dy;
					if (isValid(nx, ny, w, h)
							&& skeleton[ny][nx] == 1
							&& !visited[ny][nx]) {
						nextX = nx;
						nextY = ny;
					}
				}
			}
			if (nextX == -1) break;
			x = nextX;
			y = nextY;
		}
	}

	private void addEdgeIfNotExists(HandwritingGraph g,
			FeatureNode n1, FeatureNode n2,
			double weight, int id) {
		for (FeatureEdge e : g.getAllEdges()) {
			if ((e.getSourceNode() == n1 && e.getTargetNode() == n2) ||
					(e.getSourceNode() == n2 && e.getTargetNode() == n1)) return;
		}
		g.addEdge(new FeatureEdge("E" + id, n1, n2, weight, "skeleton"));
	}

	private FeatureNode findNearestNodeInArea(int x, int y,
			FeatureNode[][] nodeMap,
			int radius) {
		for (int dy = -radius; dy <= radius; dy++)
			for (int dx = -radius; dx <= radius; dx++) {
				int nx = x + dx, ny = y + dy;
				if (isValid(nx, ny, skeleton[0].length, skeleton.length)
						&& nodeMap[ny][nx] != null)
					return nodeMap[ny][nx];
			}
		return null;
	}


	/**
	 * Returns a Canvas showing:
	 *   - The ORIGINAL handwriting photo as background  (FIX 1)
	 *   - Skeleton pixels drawn in semi-transparent white so strokes are visible
	 *   - Blue lines for edges between nodes
	 *   - Red filled circles for junction nodes (degree >= 3)
	 *   - Green filled circles for endpoint nodes (degree == 1)
	 */
	public Canvas getDisplayCanvas() {
		int w = (int) skeletonImage.getWidth();
		int h = (int) skeletonImage.getHeight();

		Canvas canvas = new Canvas(w, h);
		GraphicsContext gc = canvas.getGraphicsContext2D();

		// FIX 1: draw the original handwriting photo as the background
		// instead of the plain black-and-white skeleton
		if (originalImage != null) {
			gc.drawImage(originalImage, 0, 0, w, h);
		} else {
			// fallback: plain white background
			gc.setFill(Color.WHITE);
			gc.fillRect(0, 0, w, h);
		}

		// Draw skeleton pixels on top as semi-transparent dark overlay
		// so you can still see the handwriting underneath
		PixelReader pr = skeletonImage.getPixelReader();
		WritableImage overlay = new WritableImage(w, h);
		PixelWriter pw = overlay.getPixelWriter();
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				Color c = pr.getColor(x, y);
				// ink pixel (black in skeleton) → draw as semi-transparent red
				if (c.getBrightness() < 0.5) {
					pw.setColor(x, y, Color.color(0.8, 0, 0, 0.55));
				} else {
					pw.setColor(x, y, Color.TRANSPARENT);
				}
			}
		}
		gc.drawImage(overlay, 0, 0);

		// Draw edges — bright blue so they stand out on the photo
		gc.setStroke(Color.color(0.0, 0.5, 1.0, 0.85));
		gc.setLineWidth(1.5);
		for (FeatureEdge edge : graph.getAllEdges()) {
			gc.strokeLine(
					edge.getSourceNode().getX(), edge.getSourceNode().getY(),
					edge.getTargetNode().getX(), edge.getTargetNode().getY());
		}

		// Draw nodes
		for (FeatureNode node : graph.getAllNodes()) {
			int deg = calculateDegree((int) node.getX(), (int) node.getY());
			if (deg >= 3) {
				// Junction — red, larger
				gc.setFill(Color.RED);
				gc.fillOval(node.getX() - 5, node.getY() - 5, 10, 10);
				gc.setStroke(Color.WHITE);
				gc.setLineWidth(1);
				gc.strokeOval(node.getX() - 5, node.getY() - 5, 10, 10);
			} else {
				// Endpoint — green, smaller
				gc.setFill(Color.LIMEGREEN);
				gc.fillOval(node.getX() - 3, node.getY() - 3, 6, 6);
				gc.setStroke(Color.WHITE);
				gc.setLineWidth(1);
				gc.strokeOval(node.getX() - 3, node.getY() - 3, 6, 6);
			}
		}

		return canvas;
	}


	public Image renderSkeletonImage() {
		int h = skeleton.length, w = skeleton[0].length;
		WritableImage img = new WritableImage(w, h);
		PixelWriter pw = img.getPixelWriter();
		for (int y = 0; y < h; y++)
			for (int x = 0; x < w; x++)
				pw.setColor(x, y, skeleton[y][x] == 1 ? Color.BLACK : Color.WHITE);
		return img;
	}

	private int[] getNeighbors(int[][] img, int x, int y) {
		return new int[]{
				img[y-1][x],   img[y-1][x+1], img[y][x+1],   img[y+1][x+1],
				img[y+1][x],   img[y+1][x-1], img[y][x-1],   img[y-1][x-1]
		};
	}

	private int countActiveNeighbors(int[] n) {
		int sum = 0;
		for (int i : n) sum += i;
		return sum;
	}

	private int countTransitions(int[] n) {
		int count = 0;
		for (int i = 0; i < n.length; i++)
			if (n[i] == 0 && n[(i + 1) % n.length] == 1) count++;
		return count;
	}

	private int calculateDegree(int x, int y) {
		int count = 0;
		for (int j = -1; j <= 1; j++)
			for (int i = -1; i <= 1; i++) {
				if (i == 0 && j == 0) continue;
				if (isValid(x+i, y+j, skeleton[0].length, skeleton.length)
						&& skeleton[y+j][x+i] == 1) count++;
			}
		return count;
	}

	private boolean isValid(int x, int y, int w, int h) {
		return x >= 0 && x < w && y >= 0 && y < h;
	}

	private int[][] copyMatrix(int[][] source) {
		int h = source.length;
		int[][] target = new int[h][];
		for (int i = 0; i < h; i++) target[i] = source[i].clone();
		return target;
	}

	private void saveImage(Image image, String studentId) {
		try {
			Path outputPath = Paths.get("output", studentId);
			Files.createDirectories(outputPath);
			File file = outputPath.resolve("skeleton.png").toFile();
			BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
			ImageIO.write(bImage, "png", file);
		} catch (IOException e) {
			System.err.println("Error saving skeleton: " + e.getMessage());
		}
	}

	public HandwritingGraph getGraph() { 
		return graph; 
	}

}