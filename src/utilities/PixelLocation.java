package utilities;
/**
 * Represents the location of a pixel in terms of its x and y coordinates.
 * @author Leon Nkadimeng
 */
public class PixelLocation {
	private int x, y;
	/**
	 * 
	 * @param x
	 * @param y
	 */
	public PixelLocation(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public int getX() {return x;}
	public int getY() {return y;}
	@Override
	public boolean equals(Object other) {
		PixelLocation l = (PixelLocation)other;
		return (x == l.x) && (y == l.y);
	}
}
