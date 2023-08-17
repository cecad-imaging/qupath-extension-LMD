## LMD Export Extension for QuPath
This extension aims to provide a way of exporting segmentations 
created in QuPath to Leica's laser microdissection software.

### Installation
Download a JAR file from releases and drag it into QuPath's main 
window as described [here](https://qupath.readthedocs.io/en/0.4/docs/intro/extensions.html#installing-extensions). 
### Workflow overview
1. Segment cells.
2. Expand segmentations so that the laser will cut and not burn your ROIs.
3. Add calibration points to your QuPath image.
4. Export the objects, optionally assign a collector to the objects of specified classification.
5. Import the XML into Leica's software and follow the instructions.

### Detailed Usage
#### In QuPath
1. Perform cell segmentation in QuPath.
2. In order to add calibration points, create either a single annotation
with 3 points and name it "calibration" or 3 separate annotations with a single 
point each and name the first one "calibration1" and the rest accordingly. 
Note that although the points' locations technically can be random, you will need to find the exact same 3 points in Leica's LMD software viewer (you can for
example mark them with a permanent marker on the slide before scanning to make it
easier). It is also advisable to put the first point around top left, second - top right and third - bottom right corner
as this is the microscope's default configuration. These are corners of the slide as seen in LMD viewer, not QuPath
(_normal_ scan would have these at bottom right, top right and top left corner in QuPath).
3. You will most likely need to expand your segmentations having the laser's aperture
in mind. To do so, select objects to expand and choose the option from the extension's menu.
Set the radius and the desired behaviour when two objects of different classes 
intersect - you can either exclude these objects or set an order of priority for 
objects of diffferent classes. An object with higher priority will be preserved.
Objects of the same class will be merged if intersecting. Note that this allows 
for expanding all kinds of objects but the new, enlarged objects will be generic
detections regardless of their previous type.
4. You can now export objects by choosing the right option in the menu. 
Everything which is not an 'annotation' will be counted
as shape to be cut out by the LMD, the calibration points are always saved. You
can assign objects of different classifications to a specific collector's cap. Choose
a desired collector and after confirming, you'll be prompted to assign the classes.
#### In the LMD software
1. Place your slide in the slide holder and your collector in the collector holder.
2. Import shapes from an XML.
3. Choose your exported XML file.
4. You will be prompted to choose calibration points. If it is the first time, choose
to set them manually, and navigate to each point's location. You can save these points to another XML
file for later reuse if needed.
5. When asked if you want to use actual magnification, 
set your desired magnification (40 should usually work well)
and choose yes.
6. You should now see the imported shapes. If you don't see them, make sure 
the collectors are assigned to the shapes, then they should appear with different collors
for each collector symbol.
7. No matter how hard you've tried to find exactly the same spots you chose previously
in QuPath as calibration points, you will probably need to adjust your shapes 
now by simply dragging them a little bit.
8. Adjust the laser's settings and cut out the cells.  

