### Example with Hela cells

Example with Hela cells stained with PKmito Orange on an
LMD Petri dish.

**First step**: Acquiring an image.

We have obtained an image using EVOS microscope 
(EVOS FL Auto 2 Imaging System). Then processed and loaded a multichannel
image into QuPath:

![Image](./assets/example2/Image.png)

We can see the cells, as well as the marks made on a membrane
which will help us locate the calibration later. 

**Second step**: Segmentation and export preparation in QuPath

The aim here was to discriminate between darker and brigther mitochondria:
![Image](./assets/example2/Cells_zoomed.png)

and we have run a custom cellpose script for that purpose with following result:
![Image](./assets/example2/Cells_zoomed_segmented.png)

In order to cut the cells out, we expanded them by the radius of 20 um.
Just in case we worked on a copy of our image.

Choosing a class priority will result in preserving this class if object
of different class intersects it while expanding. Regarding the option when 
same class intersects, we chose to keep one of the objects because merging them
would sometimes result in large objects of bright class cells encapsulating
a dark cell, which would inevitably fall down to the same collector when cutting 
out. Removing some objects didn't make that much of a difference.

![Gif showing expanding](./assets/example2/Processing.gif)


Then we put 3 calibration points:
![Image](./assets/example2/Calibration_points_after.png)

**Third step**: Export.

![Gif showing exporting](./assets/example2/Exporting.gif)


**Fourth step**: Import to the LMD software.













