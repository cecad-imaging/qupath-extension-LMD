## QuCut - Leica LMD support extension for QuPath
This extension aims to provide an open-source solution for automating 
the segmentation process within Leica's laser microdissection software. It exports 
detections from [QuPath](https://qupath.github.io/) to an XML file, which can be imported into 
[LMD7](https://www.leica-microsystems.com/products/light-microscopes/p/leica-lmd7/).

### Installation
Download a JAR file from releases and drag it into QuPath's main 
window as described [here](https://qupath.readthedocs.io/en/0.4/docs/intro/extensions.html#installing-extensions).

### Workflow overview
1. Acquire an image of your slide.
2. Add calibration points to your image in QuPath.
3. Segment cells or perform other processing in QuPath. 
4. Expand segmentations (make them artificially bigger so that the laser won't cut through the cell itself).
5. Choose a collector, assign objects to the collector's wells labels.
6. Export segmented cells (as detection objects in QuPath).
7. Import the XML into Leica's software.
8. Perform further processing in the LMD.

### Usage Details
 - **Calibration Points**: In order to add calibration points, create 3 separate 
annotations with a single point each and name the first one "calibration1" 
and the rest accordingly. Note that although the points' locations technically 
can be random, you will need to find the exact same 3 points in Leica's LMD 
software viewer. The easiest way to do this is to put them around default locations,
to which the stage moves for each point during calibration process.
These locations are: top left area for the first one, top right for the second 
and bottom right for the third one. Note that these are corners of the slide as seen 
in the microscope's viewer when the slide is flipped and ready for the regions to be cut out, 
whether they match your view of the slide in QuPath depends on your scan.


- **Detections Expanding**: You will most likely need to expand your segmentations 
having the laser's aperture in mind. To do so, select objects to expand 
(Ctrl+Alt+D for all detections) and head to 'More Options' in LMD Support window, then:

    Set the radius and the desired behaviour when two objects intersect. 
    
    If the intersecting object belong to different classes, 
    you can either 
    exclude these objects or set an order of priority for 
    objects of different classes. An object with higher priority will be preserved.
    Setting a priority is probably almost always a desired choice.

    Objects of the same class can be either merged or one of them can be removed. 
    In case of the latter option - the object to be removed is chosen randomly. 
    Generally merging 
    may result in less accurate distinction between the classes, 
    so the second option is the recommended one.
    Note that the expanding won't 
    process annotations and the new, enlarged objects will be generic detections 
    regardless of their previous type. The processing may take some time in case 
    of many objects (>5000). The smaller the number of
    processed objects at once, the better. 


- **Laser aperture visualization**: It is possible to visualize the laser cutting area 
by setting the shapes boundaries thickness to be equal the laser's selected aperture 
in the Leica's LMD software.


- **Exporting**: Each object which is not an 'annotation' will be counted
as shape to be cut out by the LMD and exported to the output XML, the calibration 
points are always included in the export.



- **Shapes simplification**: The shapes of the detections can be 
simplified in order to optimize the laser cutting. 


- **96-Well Plate**: Objects assignment process for 96-Well Plate differs from 
other collector options. Since the number of labels is way bigger, 
instead of assigning objects to the particular well, it is possible to assign them
to a number of wells, and the objects will be distributed across the specified 
number of wells randomly. Additional file containing assignment details is 
generated in the same location that the main XML output.
Note that each well within the single assignment will 
contain the same number of objects, thus the specified number 
of the objects should be divisible by the number of wells.

### Examples

- [Basic example with LMD slide](./examples/Example_Basic.md)
- [Example with fluorescent probe on LMD plate](./examples/Example_Hela_PKmO.md)

NOTE:
As for now the examples are outdated and present older version
of the extension. In case of any doubts regarding the workflow though, they
still can provide a valuable insight.

If you have further questions or want to report 
a problem, please [open an issue](https://github.com/cecad-imaging/qupath-extension-LMD/issues).




