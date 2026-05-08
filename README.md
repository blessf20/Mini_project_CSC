## Mini_project_CSC
## CSC03A3 Mini Project
## Team name: Syntax Labs
## Project name: GraphInk
## Presentation
[Presentation video](https://youtu.be/Fcc75W399pc)
### Steps for executing the project from the jar file.<br>
<b>Step 1</b>: Navigate to the jar file in the dist folder. <br>
<b>Step 2</b>: Open cmd inside the dist folder and run the command 'java --module-path "C:\javafx-sdk-21\lib" --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media -jar StudentDetection.jar'.<br>
*<b>Note</b> the 'javafx-sdk-21' on the command can be replaced with the version of java you have on your computer.*


### Steps for executing the project from eclipse <br>
<b>Step 1</b>: Setup javafx.<br>
<b>Step 2</b>: add the <b>gs-core-2.0.jar</b> and <b>gs-ui-javafx-2.0.jar</b> jar files to build path. These are the jar files used by GraphStream to render graphs in javafx.<br>
<b>Step 3</b>: Add the VM arguments for javafx '--module-path "C:\javafx-sdk-21\lib" --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media'.<br>
<b>Step 3</b>: Run the program.

## Sources
- [Classification](https://www.dgl.ai/dgl_docs/en/2.4.x/tutorials/blitz/5_graph_classification.html)
- [Zhang-Suen](https://agcggs680.pbworks.com/f/Zhan-Suen_algorithm.pdf)
- [Thresholding](https://stackoverflow.com/questions/10206441/niblack-algorithm-for-document-binarization)
- [Classification (video)](https://youtu.be/hB4YQqzzh3I?si=u1tzH8Mbm9vz2lTj)
- [Classification (guide)](https://medium.com/@mouissatrabah/training-and-evaluating-a-graph-neural-network-gnn-model-a-comprehensive-guide-7980f86c3f8d)
- [Sobel Operator](https://en.wikipedia.org/wiki/Sobel_operator)
- [Sobel Angle Resolution](https://stackoverflow.com/questions/41138396/sobel-gradient-angle-resolution)
