### Basic Example 
This is a simple example to showcase the workflow using 
a lung tissue as a sample.

**First step**: Preparation and acquiring an image.

Before scanning our slide, we marked 3 points on it. This can be done
in any way, here we burned 3 'X' marks with an LMD microscope, one of 
which is shown below. The purpose of it is to have some kind of landmark which
is easy to find later in both QuPath and LMD software. 

![Marking down the calibration](./assets/example1/BurnedMark.PNG)

Then we acquired a scan of the slide (we can see the 3 marks we 
created before):

![Acquiring an image](./assets/example1/EntireSlideScan.png)

**Second step**: Segmentation in QuPath and export.

Before segmentation, we prepared the data in advance by
inserting 3 separate annotation points and naming each one accordingly
for the export later (calibration1, calibration2, calibration3). 
These coordinates will be used by Leica's
LMD software to convert the coordinates for our shapes,
you will need to mark the same points again under the Leica's 
viewer in the same order as your annotations names suggest. The names are not
case-sensitive, just make sure you don't have a typo.

*This can of course be done at anytime before exporting the shapes.*

*You can also create a mirrored copy of an image, which
can be helpful for navigating under LMD software later.
See "Mirroring" under "[Usage Details]()".*

![Calibration point zoomed](./assets/example1/single_cal.png)
![Calibration points](./assets/example1/three_cal.png)



Next, for the sake of example,
we simply run cell detection to mark some ROIs:
![Cells detection](./assets/example1/detected_cells.png)

In order to cut them, we need to enlarge these particular objects. Later, you will have 
to adjust laser's aperture and power in a way it is suffcient enough to cut
the shapes out, but not too powerful to burn them. We selected
all detections and expanded them by
the radius of 3 micrometers.
Then, we deleted unnecessary parent annotation.

*How much to expand your shapes dependes on factors,
like magnification, laser settings, kind of tissue you're working with, and will
likely require some testing.*

*If you have only one class, the second option in "Expand selected" dialog below 
doesn't matter. In case of 
intersecting each other, the shapes will be merged. As above, see "[Usage Details]()"
for more information.*

![Expand options](./assets/example1/expand_options.png)


*You can convert in between simple detections and annotations like this e.g. to 
use tools dedicated for annotations from QuPath's "Objects/Annotations..." menu.
Here we converted our shapes to annotations to fill unnecessary holes contained within
single shapes and then converted back them to detections.*

![Expanded cells zoomed](./assets/example1/after_expanding.png)

**Third step**: Export.

Since we didn't have any classes, we could only assign all objects to one of the
collector's caps.

![Cells detection](./assets/example1/exporting.gif)


The directory "LMD data" was created and the XML file saved in it (for reference 
you can see [the XML from this example](./assets/example1/2023-09-01%2017.25.26_2023-09-04_16-24-03.xml) 
in assests folder).

**Fourth step**: Importing to the LMD software.

Head to "File/Import Shapes..."
![Importing menu](./assets/example1/importing_option.PNG)
First it asks if the calibration used last time is the one you want to use now.
![Importing window](./assets/example1/importing_window1.PNG)

Then if you have a file with reference points.

![Importing window2](./assets/example1/importing_window2.PNG)

If this is your first time doing this, you can select 'no' and go to the calibration view.

*The points you are going to select now can be written to a file and loaded next time.*

During calibration, we have to choose as accurately as possible, the same points we marked
in QuPath. Here is an example of the same point from above, this time in the LMD software:

![Calibration](./assets/example1/calibrationpoint3.PNG)

In the next dialog the software asks if you want to use actual magnification:
![Magnification](./assets/example1/magnification.PNG)
Just choose the magnification you want to work on and select "yes".
After this, the shapes should be loaded and visible:
![Magnification](./assets/example1/succesful_loading.PNG)
Depending on accuracy of your calibration points, these shapes will be more or less
shifted, you can move them all at once to adjust. 











