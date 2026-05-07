package classification;
/**
 * 
 */
public class StyleOutput {
	//unique identifier for the student
    private final String studentId;
    //student's full name
    private final String studentname;
    //the type of handwriting style
    private final HandwritingStyle style;  
    //score representing how strongly the handwriting is Print
    private final int print_blockScore;
    //score representing how strongly the handwriting is Cursive 
    private final int cursiveScore;
    //stores the similarity
    private double similarityScore ;

    // Constructor
    public StyleOutput(String studentId, String studentname,HandwritingStyle style, int print_blockScore, int cursiveScore) {
        this.studentId = studentId;
        this.studentname = studentname;
        this.style = style;
        this.print_blockScore = print_blockScore;
        this.cursiveScore = cursiveScore;
    }


    // Getters

    /**
     * Returns the student's full name 
     * @return studentname - name of student
     */
    public String getStudentname() {
        return studentname;
    }

    /**
     * Returns the type of handwriting style
     * @return style  Print_BLOCK ,CURSIVE or MIXED 
     */
    public HandwritingStyle getStyle() {
        return style;
    }

    /**
     * Short uppercase tag for table cells and badge labels
     */
    public String getTag() {
        switch (style) {
            case PRINT_BLOCK: return "PRINT";
            case CURSIVE:     return "CURSIVE";
            default:          return "MIXED";
        }
    }

    
    /**
     * Returns the similarity score calculated during classification
     * Value is between 0.0 (no match) and 100.0 (perfect match)
     * @return similarity percentage 0.0 - 100.0
     */
    public double getSimilarityScore() {
        return this.similarityScore;
    }

    /**
     * Sets the similarity score after classification is complete
     * @param similarityScore value between 0.0 and 100.0
     */
    public void setSimilarityScore(double similarityScore) {
     
        this.similarityScore = Math.max(0.0, Math.min(100.0, similarityScore));
    }
    
    @Override
    public String toString() {
        return String.format(
            "ClassificationResult{id=%s, style=%s, printScore=%d, cursiveScore=%d}",
            studentId, style, print_blockScore, cursiveScore);
    }
}