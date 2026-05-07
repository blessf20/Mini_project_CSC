package utilities;
import images.GrayScaleImage;
import images.RGBImage;

public class RGBToGrayScaleImageConverter {
	/**
	 * Converts the RGBImage into a grayscale by calculating a weighted average of each pixel's values.
	 * @param image rgb image we want to convert
	 * @return the gray scale image
	 */
	public static GrayScaleImage toGreyScale(RGBImage image) {
		GrayScaleImage greyScale = new GrayScaleImage(image.getWidth(), image.getHeight(), image.getName());
		for(int y = 0; y < image.getHeight(); y++) {
			for(int x = 0; x < image.getWidth(); x++) {
				// calculate weighted average of rgb pixel values
				double intensity = 0.299 * image.getPixel(x, y).getRed();
				intensity+= 0.587 * image.getPixel(x, y).getGreen();
				intensity += 0.114 * image.getPixel(x, y).getBlue();
				greyScale.setPixel(x, y, intensity);
			}
		}
		return greyScale;
	}
}
