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
     */
    public HandwritingGraph extractFeatures(String imagePath, String studentId) {
        File imageFile = new File(imagePath);
                
        if (!imageFile.exists()) {
            System.out.println("Warning: image not found at " + imagePath + " - using random values");
            return randomGraph(studentId);
        }

        try {
            Image fxImage = new Image("file:" + imageFile.getAbsolutePath());
            if (fxImage.getWidth() <= 0 || fxImage.getHeight() <= 0) {
                System.out.println("Warning: invalid image dimensions - using random values");
                return randomGraph(studentId);
            }
            RGBImage rgb = new RGBImage(fxImage, imageFile.getName());
            GrayScaleImage grey = RGBToGrayScaleImageConverter.toGreyScale(rgb);
            GrayScaleImageBinarizer binarizer = new GrayScaleImageBinarizer();
            BWImage bw = binarizer.applyNiblacksMethod(grey);
            BWImage skeleton = BWImageSkeletonizer.skeletonize(bw);
            return extractFeatures(skeleton, studentId);

        } catch (Exception e) {
            System.out.println("Pipeline error for " + imagePath + ": " + e.getMessage());
            return randomGraph(studentId);
        }
    }

    /**
     * Feature-extraction-only entry point.
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
     * Slant angle via Sobel gradient.
     * Only counts near-vertical strokes (30-150 degrees).
     * Steeper mapping to amplify differences between print and cursive.
     *
     * Print writers:   raw ~68-78  → mapped ~75-90
     * Cursive writers: raw ~85-95  → mapped ~105-125
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
                    -    safeGet(image, x+1, y-1)
                    +    safeGet(image, x-1, y+1) + 2 * safeGet(image, x, y+1)
                    +    safeGet(image, x+1, y+1);

                double mag = Math.sqrt(gx * gx + gy * gy);
                if (mag < 0.5) continue;

                double angle = Math.atan2(gy, gx) * (180.0 / Math.PI);
                if (angle < 0) angle += 180;

                // Only count strokes that are more vertical than horizontal
                // This filters out horizontal baselines and noise
                if (angle >= 30 && angle <= 150) {
                    totalAngle += angle;
                    count++;
                }
            }
        }

        if (count == 0) return 90.0;

        double raw = totalAngle / count;

        double mapped;
        if (raw <= 90) {
            // upright/backward slant → print range (75-95)
            mapped = 75.0 + (raw - 60.0) * (20.0 / 30.0);
        } else {
            // forward slant → cursive range (95-150)
            mapped = 95.0 + (raw - 90.0) * (55.0 / 30.0);
        }

        return Math.max(60.0, Math.min(150.0, mapped));
    }

    /**
     * Mean width of inter-letter gaps only (3-20 pixels).
     * Word spaces (>20px) are excluded — they skew the average upward.
     * No gaps found = letters are touching = cursive = return tight value.
     *
     * Print:   wide gaps  → result ~12-20
     * Mixed:   medium gaps → result ~8-12
     * Cursive: tight/none  → result ~3-8
     */
    private double calculateLetterSpacing(BWImage image) {
        int width  = image.getWidth();
        int height = image.getHeight();

        int[] vertProj = new int[width];
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                if (safeGet(image, x, y) == 1) vertProj[x]++;

        double totalGap = 0;
        int gapCount    = 0;
        boolean inGap   = false;
        int currentGap  = 0;

        for (int x = 0; x < width; x++) {
            if (vertProj[x] == 0) {
                if (!inGap) { inGap = true; currentGap = 0; }
                currentGap++;
            } else {
                if (inGap) {
                    // Only inter-letter gaps — not word spaces
                    if (currentGap >= 3 && currentGap <= 20) {
                        totalGap += currentGap;
                        gapCount++;
                    }
                }
                inGap = false;
            }
        }

        // No gaps = letters touching = cursive = tight spacing
        double result = gapCount > 0 ? totalGap / gapCount : 5.0;
        return result;
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
     * Counts small intra-word gaps per row — these correspond to loop interiors.
     * Word boundary gaps (>20px) are excluded.
     *
     * Expected ranges after scaling:
     *   Print:   avg ~0-2  loops/row → result ~0-4
     *   Mixed:   avg ~2-4  loops/row → result ~4-8
     *   Cursive: avg ~4-8  loops/row → result ~8-15
     *
     * Scaling: avgLoops * (15.0 / 8.0) — maps 0-8 avg to 0-15
     * This gives meaningful separation between styles.
     */
    private double calculateLoopFrequency(BWImage image) {
        int width  = image.getWidth();
        int height = image.getHeight();

        int totalLoops  = 0;
        int rowsWithInk = 0;

        for (int y = 0; y < height; y++) {
            int inkRuns    = 0;
            boolean wasInk = false;
            int gapLength  = 0;
            boolean inWord = false;

            for (int x = 0; x < width; x++) {
                boolean isInk = safeGet(image, x, y) == 1;

                if (isInk) {
                    if (!wasInk) {
                        // Re-entering ink after a small gap inside a word = loop
                        if (inWord && gapLength > 2 && gapLength < 8) {
                            inkRuns++;
                        }
                        inWord    = true;
                        gapLength = 0;
                    }
                } else {
                    if (wasInk) gapLength = 0;
                    gapLength++;

                    // Large gap = word boundary — reset word context
                    if (gapLength > 20) {
                        inWord    = false;
                        gapLength = 0;
                    }
                }
                wasInk = isInk;
            }

            if (inkRuns > 0) {
                totalLoops += inkRuns;
                rowsWithInk++;
            }
        }

        if (rowsWithInk == 0) return 0.0;

        double avgLoops = (double) totalLoops / rowsWithInk;

        // Map 0-8 avg loops per row → 0-15
        // Keeps print low, pushes cursive high, mixed in between
        double result = Math.min(15.0, avgLoops * (15.0 / 12.0));
        return result;
    }

    /**
     * Variance of ink-run lengths per row, normalised to 0-100.
     * High variance = inconsistent pen pressure = cursive signal.
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

    /** Reads a BWImage pixel safely, returning 0 for out-of-bounds. */
    private int safeGet(BWImage image, int x, int y) {
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) return 0;
        Integer v = image.getPixel(x, y);
        return v == null ? 0 : v;
    }

    /** Fallback graph with random feature values when image loading fails. */
    private HandwritingGraph randomGraph(String studentId) {
        HandwritingGraph g = new HandwritingGraph(studentId);
        java.util.Random r = new java.util.Random();
        g.addNode(new FeatureNode("N1", "SlantAngle",    75  + r.nextDouble() * 30));
        g.addNode(new FeatureNode("N2", "LetterSpacing",  5  + r.nextDouble() * 15));
        g.addNode(new FeatureNode("N3", "StrokeDensity",  1  + r.nextDouble() * 5));
        g.addNode(new FeatureNode("N4", "LoopFrequency",  2  + r.nextDouble() * 10));
        g.addNode(new FeatureNode("N5", "PressureZone",  15  + r.nextDouble() * 30));
        return g;
    }

    /**
     * Returns the skeletonized BWImage for a given student's sample.
     */
    public BWImage getSkeletonImage(String imagePath) {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            System.out.println("Warning: image not found at " + imagePath);
            return null;
        }

        try {
            Image fxImage = new Image("file:" + imageFile.getAbsolutePath());
            if (fxImage.getWidth() <= 0 || fxImage.getHeight() <= 0) return null;

            RGBImage rgb = new RGBImage(fxImage, imageFile.getName());
            GrayScaleImage grey = RGBToGrayScaleImageConverter.toGreyScale(rgb);
            GrayScaleImageBinarizer binarizer = new GrayScaleImageBinarizer();
            BWImage bw = binarizer.applyNiblacksMethod(grey);
            return BWImageSkeletonizer.skeletonize(bw);

        } catch (Exception e) {
            System.out.println("Skeleton extraction error: " + e.getMessage());
            return null;
        }
    }
}