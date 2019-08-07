import java.net.*;
import java.util.*;
// random number generator learned from https://stackoverflow.com/questions/5887709/getting-random-numbers-in-java
// used https://docs.oracle.com/javase/7/docs/api/java/net/Socket.html
// used https://docs.oracle.com/javase/7/docs/api/java/net/DatagramSocket.html
// used https://www.baeldung.com/udp-in-java
public class Server {
    public static void main(String[] args) {
        int my_server_req_code = 1209;
        int n_port = 2026;
        int low_bound = 1025;
        int high_bound = 2025;
        System.out.println("This server's Port number is: " + n_port);

        // stage 1: use UDP to create server socket and verifies client's requirements code
        byte[] buffer = new byte[1024]; // buffer created
        DatagramSocket serversocket = null;
        try {
            serversocket = new DatagramSocket(n_port); // server socket created
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
            System.exit(1);
        }


        // a loop to keep multiple clients to connect
        while (true) {
            // create the server packet to receive from one client using the buffer created
            DatagramPacket server_packet = new DatagramPacket(buffer, buffer.length);

            // receive the client packet in server_packet
            try {
                serversocket.receive(server_packet);
            } catch (Exception e) {
                System.err.println("Exception: " + e.getMessage());
                System.exit(1);
            }


            // get client address, port number, req_code since we have to send the r_port back
            InetAddress client_address = server_packet.getAddress();
            int client_port_number = server_packet.getPort();
            String req_code_msg = new String(server_packet.getData());
            int client_req_code = Integer.parseInt(req_code_msg);


            //see if client's required code is correct. If so, send back r_port

            if (client_req_code == my_server_req_code) {

                // generate random port number for client
                Random rand = new Random();
                int r_port = rand.nextInt(1000) + 1025;
                System.out.println("The Random Port number is: " + r_port);

                //embed it into packet and send it back
                String r_port_str = Integer.toString(r_port);
                buffer = r_port_str.getBytes();
                DatagramPacket send_port_packet = new DatagramPacket(buffer, buffer.length, client_address, client_port_number);
                try {
                    serversocket.send(send_port_packet);
                } catch (Exception e) {
                    System.err.println("Exception: " + e.getMessage());
                    System.exit(1);
                }


            } else {
                System.out.println("A client tried to connect with incorrect required code, access denied.");
            }
        }





    }
}
