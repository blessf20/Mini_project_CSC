package images;

public class BWImage extends AbstractImage<Integer> {

	/** Number of ink (value=1) pixels */
	private int numWhitePixels;

	/**
	 * Constructor
	 * @param width  width of the image
	 * @param height height of the image
	 * @param name   file name of the image
	 */
	public BWImage(int width, int height, String name) {
		super(width, height, name);
	}

	/**
	 * Updates pixel at coordinates (x, y), keeping the ink-pixel count accurate.
	 */
	@Override
	public void setPixel(int x, int y, Integer value) {
		if (super.getPixel(x, y) != null && super.getPixel(x, y) == 1 && value == 0)
			numWhitePixels--;
		if (super.getPixel(x, y) == null || (super.getPixel(x, y) == 0 && value == 1))
			numWhitePixels++;
		super.setPixel(x, y, value);
	}

	/**
	 * Returns the number of ink pixels (value == 1).
	 */
	public int getNumWhitePixels() {
		return numWhitePixels;
	}

	/**
	 * Returns a new BWImage with all pixel values flipped (0→1, 1→0).
	 * Useful for correcting images where ink and background are swapped.
	 */
	public BWImage invertPixels() {
		BWImage inverted = new BWImage(super.getWidth(), super.getHeight(), super.getName());
		for (int y = 0; y < super.getHeight(); y++)
			for (int x = 0; x < super.getWidth(); x++)
				inverted.setPixel(x, y, super.getPixel(x, y) ^ 1);
		return inverted;
	}

	/**
	 * Returns a PPM (P2 greyscale) representation of this image.
	 */
	@Override
	public String toPPM() {
		StringBuilder sb = new StringBuilder();
		sb.append("P2\n");
		sb.append(String.format("%s %s\n", super.getWidth(), super.getHeight()));
		sb.append("1\n");
		sb.append(toString());
		return sb.toString();
	}
}