inputs: <router_id> <nse_host> <nse_port> <router_port>

// run network in a student environment as follows:
// ./nselinux386 <a student environment host name that all routers are at.> <a free port number>
// for example:
./nselinux386 ubuntu1604-006 19199

// run all 5 routers as follows:
java <router_id> <nse_host> <nse_port> <router_port>

java router 1 ubuntu1604-004 19199 14438
java router 2 ubuntu1604-004 19199 10445
java router 3 ubuntu1604-004 19199 11272
java router 4 ubuntu1604-004 19199 13047
java router 5 ubuntu1604-004 19199 11750
