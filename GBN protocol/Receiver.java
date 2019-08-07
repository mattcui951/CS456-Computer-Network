import java.io.*;
import java.net.*;
import java.util.*;
// learned printerwriter from https://docs.oracle.com/javase/7/docs/api/java/io/PrintWriter.html
// learned file reader from https://www.geeksforgeeks.org/different-ways-reading-text-file-java/ and https://docs.oracle.com/javase/8/docs/api/java/io/BufferedReader.html
// learned long to int cast from https://stackoverflow.com/questions/4355303/how-can-i-convert-a-long-to-int-in-java/4355475
// learned arraylist methods from https://docs.oracle.com/javase/8/docs/api/java/util/ArrayList.html
// learned charset from https://docs.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html
// learned timer set for socket operation from https://docs.oracle.com/javase/7/docs/api/java/net/Socket.html
public class Receiver {

    static final int max_size = 512;
    static final int Seq_num_modulo = 32;

    //input
    static String emulator_addr;
    static String emulator_port_num;
    static String receiver_port_num;
    static String output_filename;

    // 2 udp sockets to receive and send ack
    static DatagramSocket receiver_socket;
    static DatagramSocket send_ack_socket;

    // 2 printwriter to files
    static PrintWriter receive_seq_num_writer;
    static PrintWriter output_writer;

    static int current_ack_packet;


    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("wrong format of command-line input, should be: <hostname for the network emulator> <UDP port number used by\n" +
                    "the link emulator to receive ACKs from the receiver> <UDP port number used by the\n" +
                    " receiver to receive data from the emulator> <name of the file into which the\n" +
                    "received data is written> in the exact order");
            System.exit(1);
        }

        // get inputs
        emulator_addr = args[0];
        emulator_port_num = args[1];
        receiver_port_num = args[2];
        output_filename = args[3];

        //initialization of socket and printwriter.
        receiver_socket = new DatagramSocket(Integer.parseInt(receiver_port_num));
        send_ack_socket = new DatagramSocket();

        receive_seq_num_writer = new PrintWriter("arrival.log", "UTF-8");
        output_writer = new PrintWriter(output_filename, "UTF-8");
        // record current acked packet, start with -1 means we have no acked packets at start
        current_ack_packet = -1;
//      start receiving
        while (true) {
            // receive packet and parse it into the packet we defined. retrieve the data and seq_num
            byte[] temp = new byte[max_size];
            DatagramPacket receive_packet = new DatagramPacket(temp, max_size);
            receiver_socket.receive(receive_packet);
            byte[] received_data = receive_packet.getData();
            packet current_packet = packet.parseUDPdata(received_data);
            //get seq number and log it
            int seq_num = current_packet.getSeqNum();
            receive_seq_num_writer.println(seq_num);
            //get packet's data
            byte[] data = current_packet.getData();
            //output_writer.print(Arrays.toString(data));
            int packet_type = current_packet.getType();
//            if (current_ack_packet == -1) {
//                if (seq_num == 0)
//                {
//                    current_ack_packet = 0;
//                    if (packet_type == 1) {
//                        output_writer.print(Arrays.toString(data));
//                        packet ack_packet = packet.createACK(seq_num);
//                        byte[] ack_data = ack_packet.getData();
//                        DatagramPacket send_packet = new DatagramPacket(ack_data, ack_data.length, InetAddress.getByName(emulator_addr), Integer.parseInt(emulator_port_num));
//                        send_ack_socket.send(send_packet);
//                    }
//                    else if (packet_type == 0)
//                    {
//                        System.err.println("Ack pakcet received, which is not expected in receiver");
//                        System.exit(1);
//                    }
//                }
//
//            }

            // see if it is the exact packet with the seq_num we are expecting
            if ((current_ack_packet + 1) % Seq_num_modulo == seq_num) {
                // if it is a data packet, put the data in the output file and send the ack packet back to the sender.
                if (packet_type == 1) {
                    output_writer.print(Arrays.toString(data));

                    packet ack_packet = packet.createACK(seq_num);
                    byte[] ack_data = ack_packet.getData();
                    DatagramPacket send_packet = new DatagramPacket(ack_data, ack_data.length, InetAddress.getByName(emulator_addr), Integer.parseInt(emulator_port_num));
                    send_ack_socket.send(send_packet);
                    // update acked packet number
                    current_ack_packet = seq_num;
                }
                // if it is a ack packet, it is obviously something wrong because we are not expecting ack packets from sender.
                else if (packet_type == 0) {
                    System.err.println("Ack pakcet received, which is not expected in receiver");
                    System.exit(1);
                }
                // if it is a EOT packet, we then send a EOT packet
                else if (packet_type == 2) {
                    packet eot_ack_packet = packet.createEOT((1+seq_num)&Seq_num_modulo);
                    byte[] eot_data = eot_ack_packet.getData();
                    DatagramPacket send_packet = new DatagramPacket(eot_data, eot_data.length, InetAddress.getByName(emulator_addr), Integer.parseInt(emulator_port_num));
                    send_ack_socket.send(send_packet);
                    //close 2 printwriters
                    receive_seq_num_writer.close();
                    output_writer.close();
                    break;
                }
            }
            // if we received the packets that we are not expecting for, send an acked packet with last acked packet.
            else {
                packet ack_packet = packet.createACK(current_ack_packet);
                byte[] ack_data = ack_packet.getData();
                DatagramPacket send_packet = new DatagramPacket(ack_data, ack_data.length, InetAddress.getByName(emulator_addr), Integer.parseInt(emulator_port_num));
                send_ack_socket.send(send_packet);
            }
        }
        send_ack_socket.close();
        receiver_socket.close();
    }
}
