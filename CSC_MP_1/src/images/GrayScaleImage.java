package images;
public class GrayScaleImage extends AbstractImage<Double> {
	public GrayScaleImage(int width, int height, String name) {
		super(width, height, name);
		for(int y = 0; y < super.getHeight(); y++) {
			for(int x = 0; x < super.getWidth(); x++) {
				super.setPixel(x, y, 255.0);
			}
		}
	}
	

	/**
	 * 
	 */
	@Override
	public String toPPM() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("P2\n");
		sb.append(String.format("%s %s\n", super.getWidth(), super.getHeight()));
		sb.append("255\n");
		sb.append(toString());
		
		return sb.toString();
	}
}
