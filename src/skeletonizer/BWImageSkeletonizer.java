package skeletonizer;

import java.util.ArrayList;

import images.BWImage;
/**
 * Applies the Zhang Suen thinning to a black and white image
 * @author Leon Nkadimeng
 */
public class BWImageSkeletonizer {
	
	/**
	 * Sums the pixel values of pixels around the 3x3 neighborhood excluding the center pixel
	 * @param image black and white image with pixels
	 * @param x x-coordinate of center pixel
	 * @param y y-coordinate of center pixel
	 * @return the sum of surrounding pixels
	 */
	private static int sumPixels(BWImage image, int x, int y) {
		int sum = 0;
		sum += image.getPixel(x, y - 1);
		sum += image.getPixel(x + 1, y - 1);
		sum += image.getPixel(x + 1, y);
		sum += image.getPixel(x + 1, y + 1);
		sum += image.getPixel(x, y + 1);
		sum += image.getPixel(x - 1, y + 1);
		sum += image.getPixel(x - 1, y);
		sum += image.getPixel(x - 1, y - 1);
		return sum;
	}
	/**
	 * Calculate the number of 0 to 1 transitions around a given pixel in clockwise direction
	 * @param image black and white image containing pixels
	 * @param x x-coordinate of pixel
	 * @param y y-coordinate of pixel
	 * @return the number of 0 to 1 transitions around a the pixel
	 */
	private static int num0To1Transitions(BWImage image, int x, int y) {
		int numTransitions = 0;
		
		if(image.getPixel(x, y - 1) == 0 && image.getPixel(x + 1, y - 1) == 1)
			numTransitions++;
		
		if(image.getPixel(x + 1, y - 1) == 0 && image.getPixel(x + 1, y) == 1)
			numTransitions++;
		
		if(image.getPixel(x + 1, y) ==  0 &&  image.getPixel(x + 1, y + 1) == 1)
			numTransitions++;
		
		if(image.getPixel(x + 1, y + 1) == 0 && image.getPixel(x, y + 1) == 1)
			numTransitions++;
		
		if(image.getPixel(x, y+1) == 0 && image.getPixel(x - 1, y + 1) == 1)
			numTransitions++;
		
		if(image.getPixel(x - 1, y + 1) == 0 && image.getPixel(x - 1, y) == 1)
			numTransitions++;
		
		if(image.getPixel(x - 1, y) == 0 && image.getPixel(x - 1, y - 1) == 1)
			numTransitions++;
		
		if(image.getPixel(x - 1, y - 1) == 0 && image.getPixel(x, y - 1) == 1)
			numTransitions++;
		
		return numTransitions;
	}
	
	/**
	 * Class representing the position of an image pixel
	 */
	private static class Pixel {
		private int x, y;
		public Pixel(int x, int y) {
			this.x = x;
			this.y = y;
			//this.value = value;
		}
		
		public int getX() { return x;}
		public int getY() {return y;}
		public String toString() {return String.format("%s %s", x, y);}
		//public int getValue() {return value;}
	}
	/**
	 * Skeletonize the image by applying the Zhang Suen algorithm until there are no more pixels changed.
	 * @param image the black and white imaeg we want to skeletonize
	 * @return skeletonized image
	 */
	public static BWImage skeletonize(BWImage image) {
		// number of pixels set from 1 to 0 after each iteration
		int numChanged = 0;
		do {
			// reset the number of changed pixels so we can calculate for this iteration
			numChanged = 0;
			
			// list of marked pixels
			ArrayList<Pixel> markedPixels = new ArrayList<Pixel>();
			
			// pass 1
			for(int y = 1; y < image.getHeight() - 1; y++) {
				for(int x = 1; x < image.getWidth() - 1; x++) {
					// if black pixel, continue
					if(image.getPixel(x, y) == 0)
						continue;
					// get the number of 0 to 1 transitions arount the neighbourhood in a clockwise direction
					int numTransitions = num0To1Transitions(image, x, y);
					// calculate the number of white pixels around the neighbourhood
					int sum = sumPixels(image, x, y);
					
					// create condition for removal of pixel
					boolean condition = (sum >= 2) && (sum <= 6);
					condition = condition && (numTransitions == 1);
					condition = condition && (image.getPixel(x, y - 1) * image.getPixel(x + 1, y) * image.getPixel(x, y + 1) == 0);
					condition = condition && (image.getPixel(x + 1, y) * image.getPixel(x, y + 1) * image.getPixel(x - 1, y) == 0);
					if(condition) {
						markedPixels.add(new Pixel(x, y));
					}
				}
			}
			
			// set all marked pixels to 0
			for(Pixel p : markedPixels) {
				image.setPixel(p.getX(), p.getY(), 0);
			}
			// compute the number of changed pixels
			numChanged = markedPixels.size();
						
			// clear the marked pixels list
			markedPixels.clear();
			
			// pass 2
			for(int y = 1; y < image.getHeight() - 1; y++) {
				for(int x = 1; x < image.getWidth() - 1; x++) {
					// if black pixel, continue
					if(image.getPixel(x, y) == 0)
						continue;
					// get the number of 0 to 1 transitions arount the neighbourhood in a clockwise direction
					int numTransitions = num0To1Transitions(image, x, y);
					// calculate the number of white pixels around the neighbourhood
					int sum = sumPixels(image, x, y);
					// create condition for removal of pixel
					boolean condition = (sum >= 2) && (sum <= 6);
					condition = condition && (numTransitions == 1);
					condition = condition && (image.getPixel(x, y - 1) * image.getPixel(x + 1, y) * image.getPixel(x - 1, y) == 0);
					condition = condition && (image.getPixel(x, y - 1) * image.getPixel(x, y + 1) * image.getPixel(x - 1, y) == 0);
					if(condition) {
						markedPixels.add(new Pixel(x, y));
					}
				}
			}
			// set all marked pixels to background
			for(Pixel p : markedPixels) {
				image.setPixel(p.getX(), p.getY(), 0);
			}
			// compute the number of changed pixels
			numChanged += markedPixels.size();
		} while(numChanged != 0);
		
		return image;
	}
	
	
}
