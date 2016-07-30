********************************************************************************
Contact Angle Measurement Plugin

This is an ImageJ plugin for calculating contact angles on raw images.  Given a raw image of a drop on a surface, it can automatically detect the drop and the surface via image analysis and then calculate the contact angle.

Authors: Wenting Zhao, Mark Liffiton
Date: 2016/07/30
Requirement: ImageJ 1.48 or later.

Please contact Wenting Zhao (wentingzhao@outlook.com) in case of any errors or
questions.
********************************************************************************

User Instructions:

a) Download the JAR file to the plugins folder, restart ImageJ, then choose “ContactAngle”.

b) Select the drop region: roughly place the drop in the center of the region, such that the vertical extent of the region does not extend much beyond the top or bottom of the drop, and the horizontal extent contains some of the surface on which the drop rests (extending roughly half of the radius of the drop to the right and left of the drop).

c) The contact angle and the approximated circle are drawn on the image.

d) The results table will be displayed automatically after plugin first run. In addition to recording the values of a contact angle, it’ll also calculate mean and median of the data generated over multiple runs of the plugin. Note that mean and median are only valid for the same drop, so please make sure that you close the results table before you analyze another image.

Algorithm Visualization:

We have made a poster demonstrating its internal working.  So for more details and algorithm visualization, please visit http://digitalcommons.iwu.edu/cgi/viewcontent.cgi?article=3313&context=jwprc
