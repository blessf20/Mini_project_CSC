package classification;
/**
 * The 3 main categories that the handwritings will be classified into 
 * 1.Print - a non-connected style that focuses on precise ,careful strokes
 * 2.Cursive - has excessive loops and heavily connected letters 
 * 3.Mixed Print-Cursive - combination of the print and cursive characters 
 */
public enum HandwritingStyle {

	/**
	 * The types of handwritings
	 */
	PRINT_BLOCK("Non-connected Writing"), 
	CURSIVE("Connected Writing"),
	MIXED("Mixed Print-Cursive");
	
	//the type of the handwriting
	private final String type ;

	//Constructor for the class 
	HandwritingStyle(String type) {
		
		this.type = type;
	}
	
	/**
	 * Gets the type of handwriting style
	 * @return type Ctaegory the handwriting falls into
	 */
	public String getType() {
		return type;
	}
	
}
