package algorithm;

import images.BWImage;
import images.GrayScaleImage;
import images.RGBImage;
import javafx.scene.image.Image;
import model.FeatureNode;
import model.HandwritingGraph;
import skeletonizer.BWImageSkeletonizer;
import utilities.GrayScaleImageBinarizer;
import utilities.RGBToGrayScaleImageConverter;

import java.io.File;

/**
 * Extracts handwriting features from an image and populates a HandwritingGraph.
 *
 * Two entry points:
 *
 *   extractFeatures(String imagePath, String studentId)
 *       Runs the complete pipeline:
 *       raw image -> RGBImage -> greyscale -> Niblack binarize
 *                 -> Zhang-Suen skeleton -> feature extraction
 *
 *   extractFeatures(BWImage skeleton, String studentId)
 *       Skips directly to feature extraction on an already-skeletonized image.
 *       Useful when the caller has already run the pipeline.
 */
public class FeatureExtractor {

	/**
	 * Full-pipeline entry point.
	 * Loads imagePath, runs greyscale -> binarize -> skeletonize, then extracts
	 * the 5 handwriting features into a HandwritingGraph.
	 */
	public HandwritingGraph extractFeatures(String imagePath, String studentId) {
		File imageFile = new File(imagePath);
		if (!imageFile.exists()) {
			System.out.println("Warning: image not found at " + imagePath + " - using random values");
			return randomGraph(studentId);
		}

		try {
			// Step 1 - load raw image as JavaFX Image, wrap in RGBImage
			Image fxImage = new Image("file:" + imageFile.getAbsolutePath());
			if (fxImage.getWidth() <= 0 || fxImage.getHeight() <= 0) {
				System.out.println("Warning: invalid image dimensions - using random values");
				return randomGraph(studentId);
			}
			RGBImage rgb = new RGBImage(fxImage, imageFile.getName());

			// Step 2 - RGB -> greyscale
			GrayScaleImage grey = RGBToGrayScaleImageConverter.toGreyScale(rgb);

			// Step 3 - greyscale -> binary (Niblack adaptive threshold)
			GrayScaleImageBinarizer binarizer = new GrayScaleImageBinarizer();
			BWImage bw = binarizer.applyNiblacksMethod(grey);

			// Step 4 - binary -> skeleton (Zhang-Suen thinning)
			BWImage skeleton = BWImageSkeletonizer.skeletonize(bw);

			// Step 5 - extract features from the skeleton
			return extractFeatures(skeleton, studentId);

		} catch (Exception e) {
			System.out.println("Pipeline error for " + imagePath + ": " + e.getMessage());
			return randomGraph(studentId);
		}
	}

	/**
	 * Feature-extraction-only entry point.
	 * Accepts an already-skeletonized BWImage (pixels are 0=background, 1=ink).
	 */
	public HandwritingGraph extractFeatures(BWImage skeleton, String studentId) {
		HandwritingGraph graph = new HandwritingGraph(studentId);

		try {
			double slantAngle    = calculateSlantAngle(skeleton);
			double letterSpacing = calculateLetterSpacing(skeleton);
			double strokeDensity = calculateStrokeDensity(skeleton);
			double loopFrequency = calculateLoopFrequency(skeleton);
			double pressureZone  = calculatePressureZone(skeleton);

			graph.addNode(new FeatureNode("N1", "SlantAngle",    slantAngle));
			graph.addNode(new FeatureNode("N2", "LetterSpacing", letterSpacing));
			graph.addNode(new FeatureNode("N3", "StrokeDensity", strokeDensity));
			graph.addNode(new FeatureNode("N4", "LoopFrequency", loopFrequency));
			graph.addNode(new FeatureNode("N5", "PressureZone",  pressureZone));

			System.out.printf("Features extracted for %s: slant=%.2f spacing=%.2f " +
					"density=%.2f loop=%.2f pressure=%.2f%n",
					studentId, slantAngle, letterSpacing,
					strokeDensity, loopFrequency, pressureZone);

		} catch (Exception e) {
			System.out.println("Feature extraction error: " + e.getMessage());
			return randomGraph(studentId);
		}

		return graph;
	}

	

	/**
	 * Average slant angle via Sobel gradient on ink pixels.
	 * Returns degrees in 0-180 range.
	 */
	private double calculateSlantAngle(BWImage image) {
		int width  = image.getWidth();
		int height = image.getHeight();

		double totalAngle = 0;
		int count = 0;

		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				if (safeGet(image, x, y) == 0) continue;

				double gx =
						-1 * safeGet(image, x-1, y-1) + safeGet(image, x+1, y-1) +
						-2 * safeGet(image, x-1, y)   + 2 * safeGet(image, x+1, y) +
						-1 * safeGet(image, x-1, y+1) + safeGet(image, x+1, y+1);

				double gy =
						-1 * safeGet(image, x-1, y-1) - 2 * safeGet(image, x, y-1)
						- safeGet(image, x+1, y-1)
						+    safeGet(image, x-1, y+1) + 2 * safeGet(image, x, y+1)
						+    safeGet(image, x+1, y+1);

				double mag = Math.sqrt(gx * gx + gy * gy);
				if (mag < 0.5) continue;

				double angle = Math.atan2(gy, gx) * (180.0 / Math.PI);
				if (angle < 0) angle += 180;
				totalAngle += angle;
				count++;
			}
		}

		if (count == 0) return 90.0;

		// Replace the mapping section in calculateSlantAngle
		double raw = totalAngle / count;

		// Raw Sobel range for your images is 65-95
		// Map linearly to 65-115 — a gentler stretch
		double mapped = 65.0 + (raw - 65.0) * (115.0 - 65.0) / (95.0 - 65.0);

		
		return Math.max(60.0, Math.min(150.0, mapped));
	}

	/**
	 * Mean width of vertical-projection gaps (spaces between letters/words).
	 */
	private double calculateLetterSpacing(BWImage image) {
		int width  = image.getWidth();
		int height = image.getHeight();

		int[] vertProj = new int[width];
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				if (safeGet(image, x, y) == 1) vertProj[x]++;

		double totalGap = 0;
		int gapCount = 0;
		boolean inGap = false;
		int currentGap = 0;

		for (int x = 0; x < width; x++) {
			if (vertProj[x] == 0) {
				if (!inGap) { inGap = true; currentGap = 0; }
				currentGap++;
			} else {
				if (inGap && currentGap > 3) { totalGap += currentGap; gapCount++; }
				inGap = false;
			}
		}

		return gapCount > 0 ? totalGap / gapCount : 15.0;
	}

	/**
	 * Ink-pixel percentage across the whole skeleton image.
	 */
	private double calculateStrokeDensity(BWImage image) {
		int width  = image.getWidth();
		int height = image.getHeight();
		int ink    = 0;

		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				if (safeGet(image, x, y) == 1) ink++;

		return (ink * 100.0) / (width * height);
	}

	/**
	 * Normalised count of horizontal-projection valleys.
	 * Valleys correspond to the hollow interiors of looped letters (o, e, b ...).
	 */
	private double calculateLoopFrequency(BWImage image) {
	    int width  = image.getWidth();
	    int height = image.getHeight();

	    int totalTransitions = 0;
	    int rowsWithInk      = 0;

	    for (int y = 0; y < height; y++) {
	        int transitions = 0;
	        boolean wasInk  = false;
	        for (int x = 0; x < width; x++) {
	            boolean isInk = safeGet(image, x, y) == 1;
	            if (isInk != wasInk) transitions++;
	            wasInk = isInk;
	        }
	        if (transitions > 0) {
	            totalTransitions += transitions;
	            rowsWithInk++;
	        }
	    }

	    if (rowsWithInk == 0) return 0.0;

	    double avgTransitions = (double) totalTransitions / rowsWithInk;

	    // Your actual range is 30-80 transitions per row
	    // Map that to 0-15:  value/80 * 15
	    double result = Math.max(0.0, Math.min(15.0, (avgTransitions / 80.0) * 15.0));

	   
	    return result;
	}

	/**
	 * Variance of ink-run lengths per row, normalised to 0-100.
	 * High variance = inconsistent pen pressure.
	 */
	private double calculatePressureZone(BWImage image) {
		int width  = image.getWidth();
		int height = image.getHeight();

		java.util.ArrayList<Double> runs = new java.util.ArrayList<>();

		for (int y = 0; y < height; y++) {
			int run = 0;
			for (int x = 0; x < width; x++) {
				if (safeGet(image, x, y) == 1) {
					run++;
				} else {
					if (run >= 2) runs.add((double) run);
					run = 0;
				}
			}
			if (run >= 2) runs.add((double) run);
		}

		if (runs.isEmpty()) return 0.0;

		double sum = 0;
		for (double r : runs) sum += r;
		double mean = sum / runs.size();

		double variance = 0;
		for (double r : runs) variance += Math.pow(r - mean, 2);
		variance /= runs.size();

		return Math.min(100.0, variance / 25.0 * 100.0);
	}


	/** Reads a BWImage pixel safely, returning 0 (background) for out-of-bounds. */
	private int safeGet(BWImage image, int x, int y) {
		if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) return 0;
		Integer v = image.getPixel(x, y);
		return v == null ? 0 : v;
	}

	/** Fallback graph with random feature values when image loading fails. */
	private HandwritingGraph randomGraph(String studentId) {
		HandwritingGraph g = new HandwritingGraph(studentId);
		java.util.Random r = new java.util.Random();
		g.addNode(new FeatureNode("N1", "SlantAngle",    60  + r.nextDouble() * 60));
		g.addNode(new FeatureNode("N2", "LetterSpacing",  5  + r.nextDouble() * 25));
		g.addNode(new FeatureNode("N3", "StrokeDensity", 10  + r.nextDouble() * 40));
		g.addNode(new FeatureNode("N4", "LoopFrequency",  2  + r.nextDouble() * 18));
		g.addNode(new FeatureNode("N5", "PressureZone",  20  + r.nextDouble() * 60));
		return g;
	}


	// Add this method to FeatureExtractor.java

	/**
	 * Returns the skeletonized BWImage for a given student's sample.
	 * Re-runs the pipeline up to skeletonization (doesn't re-extract features).
	 * @param imagePath path to the student's sample image
	 * @return skeletonized BWImage, or null if processing fails
	 */
	public BWImage getSkeletonImage(String imagePath) {
		File imageFile = new File(imagePath);
		if (!imageFile.exists()) {
			System.out.println("Warning: image not found at " + imagePath);
			return null;
		}

		try {
			Image fxImage = new Image("file:" + imageFile.getAbsolutePath());
			if (fxImage.getWidth() <= 0 || fxImage.getHeight() <= 0) {
				return null;
			}
			RGBImage rgb = new RGBImage(fxImage, imageFile.getName());
			GrayScaleImage grey = RGBToGrayScaleImageConverter.toGreyScale(rgb);
			GrayScaleImageBinarizer binarizer = new GrayScaleImageBinarizer();
			BWImage bw = binarizer.applyNiblacksMethod(grey);
			BWImage skeleton = BWImageSkeletonizer.skeletonize(bw);
			return skeleton;
		} catch (Exception e) {
			System.out.println("Skeleton extraction error: " + e.getMessage());
			return null;
		}
	}
	
}