package utilities;
import images.BWImage;
import images.GrayScaleImage;

public class GrayScaleImageBinarizer {

	/**
	 * Computes mean of surrounding pixels within the window, including the center pixel (pixel @ (x, y))
	 * @param image gray scale image containing the pixels
	 * @param x x-coordinate of center pixel
	 * @param y y-coordinate of center pixel
	 * @param paddingAmount amount of padding we added to the image which is half the size of our window
	 * @return mean of surrounding pixels within the window
	 */
	private double computeMean(GrayScaleImage image, int x, int y, int paddingAmount) {
		double mean = 0d;
		int outerStart = y - paddingAmount;
		int outerEnd   = y + paddingAmount;
		int innerStart = x - paddingAmount;
		int innerEnd   = x + paddingAmount;

		for (int yCoord = outerStart; yCoord <= outerEnd; yCoord++)
			for (int xCoord = innerStart; xCoord <= innerEnd; xCoord++)
				mean += image.getPixel(xCoord, yCoord);

		final int n = (innerEnd - innerStart + 1) * (outerEnd - outerStart + 1);
		return mean / n;
	}

	/**
	 * Computes standard deviation of surrounding pixels within the window
	 */
	private double computeStandardDeviation(GrayScaleImage image, int x, int y,
	                                         double mean, int paddingAmount) {
		int outerStart = y - paddingAmount;
		int outerEnd   = y + paddingAmount;
		int innerStart = x - paddingAmount;
		int innerEnd   = x + paddingAmount;

		double sum = 0d;
		for (int yCoord = outerStart; yCoord <= outerEnd; yCoord++)
			for (int xCoord = innerStart; xCoord <= innerEnd; xCoord++)
				sum += Math.pow(image.getPixel(xCoord, yCoord) - mean, 2);

		final int n = (innerEnd - innerStart + 1) * (outerEnd - outerStart + 1);
		return Math.sqrt(sum / (n - 1));
	}

	/**
	 * Calculates Niblack's threshold for a pixel
	 */
	private double computeNiblacksThreshold(double mean, double k,
	                                          double standardDev, double R) {
		return mean * (1 + k * ((standardDev / R) - 1));
	}

	/**
	 * Applies Niblack's adaptive thresholding to a greyscale image.
	 *
	 * Convention: 1 = ink (dark pixel), 0 = background (light pixel).
	 * A pixel whose greyscale value is BELOW the local threshold is ink.
	 * This is the correct direction for handwriting on a white background.
	 *
	 * The old version had this inverted (> threshold → 1), which caused
	 * every feature extractor to see a mostly-white skeleton and produce
	 * LoopFrequency=0 and PressureZone=100 for all students.
	 */
	public BWImage applyNiblacksMethod(GrayScaleImage image) {
		BWImage newImage = new BWImage(image.getWidth(), image.getHeight(), image.getName());
		final int paddingAmount = 7;
		GrayScaleImage paddedImage = padImage(image, paddingAmount);

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				final double mean  = computeMean(paddedImage,
				                        x + paddingAmount, y + paddingAmount, paddingAmount);
				final double stdDev = computeStandardDeviation(paddedImage,
				                        x + paddingAmount, y + paddingAmount, mean, paddingAmount);
				final double threshold = computeNiblacksThreshold(mean, 0.2, stdDev, 128);

				// pixel BELOW threshold = dark = ink = 1
				// pixel ABOVE threshold = light = background = 0
				if (image.getPixel(x, y) > threshold) {
					newImage.setPixel(x, y, 0);   // background
				} else {
					newImage.setPixel(x, y, 1);   // ink
				}
			}
		}

		return newImage;
	}

	/**
	 * Pads the image by replicating border pixels outward.
	 */
	private static GrayScaleImage padImage(GrayScaleImage image, int amount) {
		GrayScaleImage padded = new GrayScaleImage(
			image.getWidth()  + amount * 2,
			image.getHeight() + amount * 2,
			image.getName());

		for (int y = 0; y < padded.getHeight(); y++) {
			for (int x = 0; x < padded.getWidth(); x++) {
				int srcX = Math.min(Math.max(x - amount, 0), image.getWidth()  - 1);
				int srcY = Math.min(Math.max(y - amount, 0), image.getHeight() - 1);
				padded.setPixel(x, y, image.getPixel(srcX, srcY));
			}
		}
		return padded;
	}
}