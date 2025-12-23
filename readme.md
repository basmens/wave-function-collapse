# About
For about, check `readme.md` of main branch.

## Backtracking
This branch adds backtracking. It does so by for each tile checking which tiles have influenced it, so if it finds itself with zero possibilities left, it can roll back using those influences. When working on it 3 years ago I said there was a bug, I don't remember exactly what the bug is, but I do remember that it was not a fun one. I did not manage to reproduce it now, but im trusting past me and keeping the backtracking in its own branch for now.

## Performance gain
This branch is also mildly faster it seems. I do not remember how. I had apparently decided that working on refactoring, some small optimizations, and the backtracking all in one commit was a good idea. I cannot be bothered reverse engineering the diff, so whatever.