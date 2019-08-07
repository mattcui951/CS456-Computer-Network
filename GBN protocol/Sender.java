import java.io.*;
import java.net.*;
import java.util.*;

// learned printerwriter from https://docs.oracle.com/javase/7/docs/api/java/io/PrintWriter.html
// learned file reader from https://www.geeksforgeeks.org/different-ways-reading-text-file-java/ and https://docs.oracle.com/javase/8/docs/api/java/io/BufferedReader.html
// learned long to int cast from https://stackoverflow.com/questions/4355303/how-can-i-convert-a-long-to-int-in-java/4355475
// learned arraylist methods from https://docs.oracle.com/javase/8/docs/api/java/util/ArrayList.html
// learned charset from https://docs.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html
// learned timeout for socket operation from https://docs.oracle.com/javase/7/docs/api/java/net/Socket.html#setSoTimeout(int)
public class Sender {
    static int string_max_len_per_packet = 512;
    static int SeqNumModulo = 32;
    static int timeout_value = 100;
    // input
    static String emulator_host_address;
    static InetAddress emulator_IP_address;

    static String emulator_receive_port_number;

    static String sender_ack_port_num;

    static String file_name;

    //socket
    static DatagramSocket send_socket;
    static DatagramSocket receive_ack_socket;

    // byte array for data
    static byte[] bytearr1;

    // read file variables
    static BufferedReader file_reader;
    static File file_2b_transferred;

    // variables for storing all packet data
    static int number_of_packets;
    //static char[][] all_packet_data;
    static ArrayList<char[]> all_packet_data;

    static int current_packet;
    static int current_ack_packet;
    //    static int oldest_ack_packet_in_window;
    //static int current_window_start;
    //static int current_window_end;
    static int num_of_unacked_packets;

    // window
    static int window_size = 10;
    static ArrayList<Integer> current_window_packets_number;
    static ArrayList<packet> current_window_packets;

    public static void main(String[] args) throws Exception {
        if (args.length == 4) {
            emulator_host_address = args[0];
            emulator_receive_port_number = args[1];
            sender_ack_port_num = args[2];
            file_name = args[3];
        } else {
            System.err.println("Sender's input format is wrong, it should be: <host address of the network emulator> <UDP port number used by\n" +
                    "            the emulator to receive data from the sender> <UDP port number used by the sender to\n" +
                    "            receive ACKs from the emulator> <name of the file to be transferred>");
            System.exit(1);
        }
        // read file and store it in multiple character arrays stored in an array list
        file_reader = new BufferedReader(new FileReader(file_name));
        file_2b_transferred = new File(file_name);
        if ((file_2b_transferred.length() % string_max_len_per_packet) != 0) {
            number_of_packets = (int) (file_2b_transferred.length() / string_max_len_per_packet) + 1;
        } else {
            number_of_packets = (int) (file_2b_transferred.length() / string_max_len_per_packet);
        }

//        all_packet_data = new char[number_of_packets][];
//        char[] a = new char [string_max_len_per_packet];
//        for (int i = 0; i < number_of_packets; i++)
//        {
//            file_reader.read(a, 0, string_max_len_per_packet);
//            for (int j = 0; j < string_max_len_per_packet; j++) {
//                all_packet_data[i][j] = a[j];
//            }
//            a = new char [string_max_len_per_packet];
//        }

        all_packet_data = new ArrayList<>();
        char[] a = new char[string_max_len_per_packet];
        for (int i = 0; i < number_of_packets; i++) {
            file_reader.read(a, 0, string_max_len_per_packet);
            all_packet_data.add(a);
            a = new char[string_max_len_per_packet];
        }
        send_socket = new DatagramSocket();
        receive_ack_socket = new DatagramSocket(Integer.parseInt(sender_ack_port_num));
        emulator_IP_address = InetAddress.getByName(emulator_host_address);
        current_ack_packet = -1;
        current_packet = 0;
//        oldest_ack_packet_in_window = 0;
        num_of_unacked_packets = 0;
        //DatagramPacket udp_packet;
        packet temp;

        // create log files printerwriter
        PrintWriter seq_log = new PrintWriter("seqnum.log", "UTF-8");
        PrintWriter ack_log = new PrintWriter("ack.log", "UTF-8");

        long timer_set_time = 0;

        //  start sending
        while (true) {
            bytearr1 = new byte[string_max_len_per_packet];
            //if all packets are acked
            if (current_ack_packet >= number_of_packets - 1) {
                // send eot packet to receiver
                packet eotpacket = packet.createEOT((1 + number_of_packets) % SeqNumModulo);
                bytearr1 = eotpacket.getUDPdata();
                DatagramPacket udp_packet = new DatagramPacket(bytearr1, bytearr1.length, emulator_IP_address, Integer.parseInt(emulator_receive_port_number));
                send_socket.send(udp_packet);
                seq_log.println(eotpacket.getSeqNum());
                // close both printwriter and sockets
                seq_log.close();
                ack_log.close();
                receive_ack_socket.close();
                send_socket.close();
                break;
            } else {
                // if we have not sent out all packet, then try to send
                if (current_packet < number_of_packets) {
                    // if our window is not full then send packets
                    if (num_of_unacked_packets < window_size) {
                        for (int i = current_packet + num_of_unacked_packets; i < window_size - num_of_unacked_packets + 1; i++) {
                            // sets the timer for first packet of ack sockets if all sent packets are acked
                            if (i == current_packet + num_of_unacked_packets && num_of_unacked_packets == 0) {
//                                oldest_ack_packet_in_window = i;
                                receive_ack_socket.setSoTimeout(timeout_value);
                            }
                            // start sending packets.
                            packet data_packet = packet.createPacket(i % SeqNumModulo, Arrays.toString(all_packet_data.get(i)));
                            byte[] data = data_packet.getUDPdata();
                            DatagramPacket udp_packet = new DatagramPacket(data, data.length, emulator_IP_address, Integer.parseInt(emulator_receive_port_number));
                            send_socket.send(udp_packet);
                            // log the sequence number
                            seq_log.println(i % SeqNumModulo);
                            // increment variables
                            num_of_unacked_packets++;
                            current_packet++;
                        }
                    }
                }
                // start try to receive ack

                if (num_of_unacked_packets != 0) {
                    byte[] ack_buffer = new byte[string_max_len_per_packet];
                    DatagramPacket receive_buffer = new DatagramPacket(ack_buffer, ack_buffer.length);
                    boolean timeout = false;
                    try {
                        receive_ack_socket.receive(receive_buffer);
                    } catch (SocketTimeoutException e) {
                        timeout = true;
                        receive_ack_socket.close();
                    }
                    // if time out
                    if (timeout) {
                        num_of_unacked_packets = 0;
                        current_packet = current_ack_packet + 1;

                        continue;
                    } else {
                        //if not timeout
                        //retrieve the type and log it. update variables.
                        packet receive_ack_packet = packet.parseUDPdata(ack_buffer);
                        int seq_num = receive_ack_packet.getSeqNum();
                        int type = receive_ack_packet.getType();
                        ack_log.println(seq_num);
                        // if it is a eot packet, send a eot to receiver and close
                        if (type == 2) {
                            packet eotpacket = packet.createEOT((1 + number_of_packets) % SeqNumModulo);
                            bytearr1 = eotpacket.getUDPdata();
                            DatagramPacket udp_packet = new DatagramPacket(bytearr1, bytearr1.length, emulator_IP_address, Integer.parseInt(emulator_receive_port_number));
                            send_socket.send(udp_packet);
                            seq_log.println(eotpacket.getSeqNum());
                            // close both printwriter and sockets
                            seq_log.close();
                            ack_log.close();
                            receive_ack_socket.close();
                            send_socket.close();
                        } else if (type == 0){
                            // if it is a ack packet
                            if (seq_num <= current_ack_packet) {
                                // if it is an ack we received before. then duplicate ack must result from a packet loss
                                current_ack_packet = seq_num;
                                current_packet = seq_num+1;
                                num_of_unacked_packets = 0;
                                continue;
                            } else {
                                // if it is an ack we haven't seen before, then all the packets till that sequence number packets are received.
                                num_of_unacked_packets = num_of_unacked_packets - seq_num + current_ack_packet;
                                current_packet = (seq_num+1) % SeqNumModulo;
                                current_ack_packet = seq_num;
                            }

                        }
                    }
                }


//                int recvACK = receiveACKs(senderRecvPort, ackWriter);
//                int ACKed = 0;
//                do{
//                    ACKed = 0;
//                    //Check for ACKs
//                  byte[] receiveData = new byte[512];
//                  DatagramSocket ackSocket = new DatagramSocket(senderRecvPort);
//                  ackSocket.setSoTimeout(timeoutLength);
//                  DatagramPacket ackPacket = new DatagramPacket(receiveData, receiveData.length);
//
//                  //Receive packet
//                  try{
//                      ackSocket.receive(ackPacket);
//                  }
//                  catch(SocketTimeoutException e){
//                      ackSocket.close();
//                      return -1;
//                   }
//                  finally{
//                     ackSocket.close();
//                  }
//
//                  //Extract packet, get the sequence number and log it
//                  packet recvPacket = packet.parseUDPdata(receiveData);
//                  int seqRecv = recvPacket.getSeqNum();
//                  ackWriter.println(seqRecv);
//                  return seqRecv;
//
//                    if(recvACK == -1){			//Timeout: reset number of sent packets and loop again
//                        packetsOut = 0;
//                    }else{
//                        //Count packets received sicne last ACK
//                        while(((seqMod+initialPacket+ACKed) % seqMod) != recvACK) ACKed++;
//                        if(ACKed <= windowSize){
//                            initialPacket = initialPacket + ACKed;		//Adjust the starting packet for next send
//                            packetsOut = packetsOut - ACKed;		//Adjust the amount of packets outstanding
//                            packetsACKed = packetsACKed + ACKed;		//Update total ACKed packets
//                        }
//                    }
//                } while(ACKed > windowSize);


            }
        }
    }
}
