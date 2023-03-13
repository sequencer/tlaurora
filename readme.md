# tlaurora

## Introduction
This is a project that encode TileLink message to Xilinx Aurora link-layer protocol for Chip To Chip interconnect.

## Pending PRs
This repository always tracks remote developing branches, it may need some patches to work, `make patch` will append below in sequence:
<!-- BEGIN-PATCH -->
chisel https://github.com/chipsalliance/chisel/pull/3045.diff  
rocket-chip https://github.com/chipsalliance/rocket-chip/pull/3291.diff  
berkeley-hardfloat https://github.com/ucb-bar/berkeley-hardfloat/pull/73.diff  
berkeley-hardfloat https://github.com/ucb-bar/berkeley-hardfloat/pull/72.diff  
berkeley-hardfloat https://github.com/ucb-bar/berkeley-hardfloat/pull/71.diff  
<!-- END-PATCH -->
