package images;

public abstract class AbstractImage<T> {
	protected T[][] pixels;
	private int width;
	private int height;
	private String name;
	
	/**
	 * Constructor
	 * @param width width of the image
	 * @param height height of the image
	 * @param name file name of the image
	 */
	public AbstractImage(int width, int height, String name) {
		this.width = width;
		this.height = height;
		this.name = name;
		pixels = (T[][])new Object[width][height];
	}
	
	/**
	 * @return the filename of the image
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Accessor for the image width
	 * @return width of the image
	 */
	public int getWidth() {
		return width;
	}
	
	/**
	 * Accessor for the image height
	 * @return height of the image
	 */
	public int getHeight() {
		return height;
	}
	
	/**
	 * Accessor for a pixel at a specific location
	 * @param x x coordinate of the pixel
	 * @param y y coordinate of the pixel
 	 * @return the pixel at (x, y)
	 */
	public T getPixel(int x, int y) {
		return pixels[x][y];
	}
	
	/**
	 * Returns a PPM version of the image
	 * @return a string containing a ppm representation of the image.
	 */
	public abstract String toPPM();
	
	
	/**
	 * sets a new pixel value for pixel located at coordinates (x, y)
	 * @param x the x coordinate of the pixel
	 * @param y the y coordinate of the pixel
	 * @param pixel the RGB pixel located at (x, y)
	 */
	public void setPixel(int x, int y, T pixelVal) {
		pixels[x][y] = pixelVal;
	}
	
	
	/**
	 * returns a string containing the pixel values of the image
	 * @return a string representation of the pixels array
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				if(pixels[x][y] != null)
					sb.append(String.format("%s ", pixels[x][y].toString()));
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
