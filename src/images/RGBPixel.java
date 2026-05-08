package images;

public class RGBPixel {
	private double red;
	private double green;
	private double blue;
	
	public RGBPixel(double red, double green, double blue) {
		this.red = red;
		this.green = green;
		this.blue = blue;
	}
	
	
	
	
	/**
	 * @return the red
	 */
	public double getRed() {
		return red;
	}




	/**
	 * @param red the red to set
	 */
	public void setRed(double red) {
		this.red = red;
	}




	/**
	 * @return the green
	 */
	public double getGreen() {
		return green;
	}




	/**
	 * @param green the green to set
	 */
	public void setGreen(double green) {
		this.green = green;
	}




	/**
	 * @return the blue
	 */
	public double getBlue() {
		return blue;
	}




	/**
	 * @param blue the blue to set
	 */
	public void setBlue(double blue) {
		this.blue = blue;
	}

	public double getAverage() {
		return (red + green + blue) / 3;
	}


	@Override
	public String toString() {
		return String.format("%s %s %s ", red, green, blue);
	}
	

}
