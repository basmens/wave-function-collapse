# About
This is a simple java implementation for the wave function collapse algorithm (WFC). It uses a square tile grid, and a list of modules, where each module is an image together with rules on how they may connect to neighbouring modules. The algorithm then randomly fills in the tiles of the grid with modules while adhering to the constraints.

## Backtracking
Most WFC algorithms have backtracking. The implementation here on the main branch does not have it, but there is a backtracking branch. I have not merged it with main because my last commit 3 years ago said it had a bug, and I don't remember what it is anymore, so I just left it there.

## How to use
In `Main.java`, there are some settings at the top. Set those to your liking and run it.

To use the algorithm itself you have to create a `WfcFeatures` instance first, and add your modules to it, as well as set looping rules. Each module has keys for each direction, a list of rotations that will be generated, and an object of choice (for me the image). Neighbours will only be allowed to connect to each other if they have matching keys. Finally create a `Wfc` instance and run it.