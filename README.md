Data Port provides access to the Data Link Layer (Layer 2)

These packets have:

* Source Link address
* Destination Link address
* Message
* Frame Check Sequence

In TARPN, these packets are AX.25 packets with KISS framing. Only UI
frames are used from the AX.25 spec.

Layer 2 packets received on the data port _should_ always be destined
for this Node, but possibly this could not be the case. These packets
should be dropped.







```
> Connect: SABM C P
< Ack: UA R F
> Welcome: I C P R0 S0 Pid=0xf0 Len=50
> Ack: RR R F R1
< More welcome: I C P R1 S2 Pid=0xf0 Len=32
> Ack RR R F R3
<More welcome: I C P R1 S3 Pid=0xf0 Len=149
> Ack RR R F R4

...

> Bye: I C P R2 S7 Pid=0xf0 Len=2
< Ack: RR R F R0
< Disconnect: DISC C P
> Ack: UA R F
```

