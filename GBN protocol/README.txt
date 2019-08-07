
Compilation:
To compile, use the makefile by executing the make command.

Execution:
1) Execute the emulator: ./nEmulator-linux386 11000 ubuntu1204-004 15000 12000 ubuntu1204-002 14000 1 0.2 1
2) Execute the receiver: java Receiver ubuntu1204-006 12000 15000 "output.txt"
3) Execute the sender:   java Sender ubuntu1204-006 11000 14000 "README.txt"

command_line inputs:
Sender:
<host address of the network emulator>,
<UDP port number used by the emulator to receive data from the sender>,
<UDP port number used by the sender to receive ACKs from the emulator>,
<name of the file to be transferred>
Receiver:
<hostname for the network emulator>,
<UDP port number used by the link emulator to receive ACKs from the receiver>,
<UDP port number used by the receiver to receive data from the emulator>,
<name of the file into which the received data is written>
Emulator:
<emulator's receiving UDP port number in the forward (sender) direction>,
<receiver’s network address>,
<receiver’s receiving UDP port number>,
<emulator's receiving UDP port number in the backward(receiver)direction>
<sender’s network address>,
<sender’s receiving UDP port number>,
<maximum delay of the link in units of millisecond>,
<packet discard probability>,
<verbose-mode>




