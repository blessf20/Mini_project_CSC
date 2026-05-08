package images;

import javafx.scene.image.Image;

public class RGBImage extends AbstractImage<RGBPixel> {

	/**
	 * Constructs an RGBImage from a JavaFX Image.
	 * @param image     JavaFX source image
	 * @param imageName file name of the image
	 */
	public RGBImage(Image image, String imageName) {
		super((int) image.getWidth(), (int) image.getHeight(), imageName);
		for (int y = 0; y < super.getHeight(); y++)
			for (int x = 0; x < super.getWidth(); x++) {
				double red   = image.getPixelReader().getColor(x, y).getRed()   * 255;
				double green = image.getPixelReader().getColor(x, y).getGreen() * 255;
				double blue  = image.getPixelReader().getColor(x, y).getBlue()  * 255;
				super.setPixel(x, y, new RGBPixel(red, green, blue));
			}
	}

	/**
	 * Constructs a blank RGBImage filled with white pixels.
	 * Used when building output images programmatically (e.g. coloured skeleton overlays).
	 * @param width     image width in pixels
	 * @param height    image height in pixels
	 * @param imageName file name / label for this image
	 */
	public RGBImage(int width, int height, String imageName) {
		super(width, height, imageName);
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				super.setPixel(x, y, new RGBPixel(255, 255, 255));
	}

	/**
	 * Returns a PPM (P3 colour) representation of this image.
	 */
	@Override
	public String toPPM() {
		StringBuilder sb = new StringBuilder();
		sb.append("P3\n");
		sb.append(String.format("%s %s\n", super.getWidth(), super.getHeight()));
		sb.append("255\n");
		sb.append(toString());
		return sb.toString();
	}
}