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




 
 
On RPI:

    socat -d -d pty,raw,echo=0,link=/tmp/vmodem0 tcp-l:54321,reuseaddr,fork
 
Then configure linbpq to use /tmp/vmodem0 serial device (it's a pty, not a real device). 
 
Then on another machine, run:
 
    socat -d -d tcp:david-packet.local:54321 pty,raw,echo=0,link=/tmp/vmodem0
   
Which makes another pty that you can connect to. We can create many of these types of bridges
to emulate multiple ports connected to linbpq. Well, at least two if we're using `tarpn config`


On the Java side, we can run multiple instances of JNC connected in this way
for testing.


## G8BPQ behavior:

When we send a new NODES message from a new call/alias, it will send a SABM message.
This changes the ROUTES output to include ">" which I think means you're connected.
Does it always try to keep an active connection to every known node? This might make
sense actually since it seems to use I frames (not UI) for the Net/Rom packets (I frames
require a connection).


Need to set QUALITY=0 in bpq32.cfg for ports?



3/31/2019

DataLinkManager exposes all packets for a given port, need the ability to filter this by "session" (which local callsign
the ax.25 session is connected to). Maybe rename DataLinkManager to DataPortSomething and reserve the Link name for
the thing that interacts with a single ax.25 session (link).

Command ideas:

`LINKS`: list all links and their state

> link 1: k4dbz-2 to kn4orb-2 is UP
> link 2: k4dbz-5 to kn4orb-5 is DOWN

`CONNECT [n] [call]`