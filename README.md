BitCounter in Chisel
=======================

BitCounter addressed to multi cycle bit count in Chisel.

## How to use

Use constructor to instantiate BitCounter.

```
Module(new BitCounter(width = 32, stepsPerCycle = 2))
```

`width` means that bit width of counting, and `stepsPerCycle` means that how many steps are passed at each cycle.

To be more clear about step, below is a example figure.

```
  inputs (5bit)
 |  |     | | |     --------------------
+----+  +-------+     ↑
| HA |  |  CSA  |     |
+----+  +-------+     |      one step
 |  |     |   |       |
 |  +-------+ |       ↓
 |        | | |     --------------------
 | +------+ | |       ↑
 | |        | |       |
+----+    +-----+     |      one step
| HA |    | HA  |     |
+----+    +-----+     ↓
 | |        | |     --------------------
```

If you specify `stepsPerCycle` as `1`, registers are inserted between each steps.  
If you also specify `stepsPerCycle` as `0`, this module is elaborated as combinational circuit.

## Interfaces

There are two interfaces. one is input and another is output.  
input is `io.in` that is `DecoupledIO[Vec[Bool]]` type.  
output is `io.out` that is `DecoupledIO[UInt]` type.

When `io.in.valid` and `io.in.ready` are asserted, `io.in.bits` is loaded as operand.  
When `io.out.valid` is asserted, `io.out.bits` is valid data.  
If you assert `io.out.ready`, module decide to send next result in next cycle.  

