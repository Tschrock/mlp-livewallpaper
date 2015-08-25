# Introduction #

This port is based one the Java Ponies V1.25.0 port of Desktop Ponies.
I did try to keep most of the part 1:1 but some changes were necessary, this page will go through the most significant changes.

## Missing underlying GUI Framework ##
Java Ponies uses the AWT/Swing-Framework to create windows and simply puts the GIFs into those windows. AWT takes care of animating the GIFs and since they are normal windows they are easy to move and hide/show.

Android does not have a window-like feature for the live wallpaper. So most parts of the Pony\_Form class are moved into the Pony class. Features like a right-click menu and the tooltips for text output were completely removed.

Android doesn't cope with GIFs that well so I've implemented a GIFDecoder class that reads the GIF and lets me access each frame. Together with the delay between the frames I can return the currently needed frame for any given time.

## Update function ##
In Java Ponies the update function is called by a Timer that fires every 30ms. In Android we have to update more often to guarantee a constant framerate. The actual draw function is only called if needed (to match framerate) the calculation on Behaviors, Interactions and position, however is done every 30ms.