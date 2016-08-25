# Smart Compass

This app allows you to select a location from Google maps, and then an arrow guides you in the direction you need to go, much like a compass points north.

The idea is that it will help you orient yourself when walking to a location, since the top down view of a map doesn't show your current orientation very well.
It is particularly useful if the destination is not actually an address on a road. For example, on a college campus you can simply select a building, and the arrow will guide you in that direction.


### Choosing a Location
Select destination on the map to navigate to.
<br><img src="/screenshots/Map_screenshot.png" height="40%" width="40%" />


### Arrow Navigation
The arrow points in the direction you need to go, and tells you how far away you are.
<br><img src="/screenshots/Navigation_screenshot.png" height="40%" width="40%" />


### Current Work
I'm currently working to import a 3D model of an arrow I designed in Blender. The arrow will tilt in all directions of the phones rotation, rather than just laterally like the current image does.
<br><img src="/screenshots/3DArrow_screenshot.png" />


# To Do
* Import 3D arrow model and animate with OpenGL.
* Allow a user to request location of another user, and be guided to each other.
* Add settings and info screens.
* Properly manage different device screen sizes.
