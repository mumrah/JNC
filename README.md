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