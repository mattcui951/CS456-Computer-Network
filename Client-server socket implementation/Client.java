import java.net.*;
import java.util.*;
import java.io.*;
// used https://docs.oracle.com/javase/7/docs/api/java/net/Socket.html
// used https://docs.oracle.com/javase/7/docs/api/java/net/DatagramSocket.html
// used https://www.baeldung.com/udp-in-java
public class Client {
    public void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Please enter inputs as the following format: <server_address> <n_port> <req_code> <msg>");
            System.exit(1);
        }
        //stage 1: create UDP socket and send req_code to the server
        String server_address = args[0];
        String n_port = args[1];
        int port_number = Integer.parseInt(n_port);
        String req_code = args[2];
        String msg = args[3];
        DatagramSocket clientSocket = null;
        try {
            clientSocket = new DatagramSocket();
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
            System.exit(1);
        }
        InetAddress Server_Address = null;
        try {
            Server_Address = InetAddress.getByName(server_address);
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
            System.exit(1);
        }

        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, Server_Address, Integer.parseInt(n_port));

        try {
            clientSocket.send(packet);
        }catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
            System.exit(1);
        }


        // now receive the random port number from the server
        try {
            clientSocket.receive(packet);
        }catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
            System.exit(1);
        }

        String received = new String(packet.getData(),0, packet.getLength());
        int random_port_number = Integer.parseInt(received);
        System.out.println("random_port_number: " + random_port_number);

        // stage 2: use TCP to send message to server.






        System.out.println("Hello World!");
    }
}
