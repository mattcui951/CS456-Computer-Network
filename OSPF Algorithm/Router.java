

import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.lang.*;
// learned from https://stackoverflow.com/questions/3736058/java-object-to-byte-and-byte-to-object-converter-for-tokyo-cabinet

public class Router {

    public final int NBR_ROUTER = 5;
    //static int current_connected_router = 0;

    public class pkt_HELLO
    {
        public int router_id; /* id of the router who sends the HELLO PDU */
        public int link_id; /* id of the link through which it is sent */

//        public void setRouter_id(int id) {router_id = id;}
//        public void setLink_id(int id) {link_id = id;}
    }

    public class pkt_LSPDU
    {
        public int sender; /* sender of the LS PDU */
        public int router_id; /* router id */
        public int link_id; /* link id */
        public int cost; /* cost of the link */
        public int via; /* id of the link through which the LS PDU is sent */

//        public void setSender(int i) {sender = i;}
//        public void setRouter_id(int i) {router_id = i;}
//        public void setLink_id(int i) {link_id = i;}
//        public void setCost(int i) {cost = i;}
//        public void setVia(int i) {via = i;}

    }

    public class pkt_INIT {
        public int router_id; /* id of the link through which the LS PDU is sent */

//        public void setRouter_id(int id) {router_id = id;}
    }

    public class link_cost {
        public int link; /* link id */
        public int cost; /* associated cost */
//        public void setLink(int a) {link = a;}
//        public void setCost(int b) {cost = b;}
    }
    public class circuit_DB implements Serializable
    {
        public int nbr_link; /* number of links attached to a router */
        public link_cost[] linkcost;
        /* we assume that at most NBR_ROUTER links are attached to each router */

//        public void setNbr_link(int nbr) {nbr_link = nbr;}
//        public void setLinkcost(link_cost[] a) {linkcost = a;}
    }
    //inputs
    static int router_id;
    static String nse_host;
    static int nse_port;
    static int router_port;

    static InetAddress nse_host_addr;

    // udp sockets
    static DatagramSocket send_socket;
    static DatagramSocket receive_socket;

    //udp packets
    static DatagramPacket send_packet;
    static DatagramPacket receive_packet;

    // a buffer.
    static byte[] temp_byte_arr;

    // a logger
    static PrintWriter router_log;

    // received circuit DB
    static circuit_DB received_DB;

    // adjacency matrix for graph
    public int[][] graph;

    // all router lspdus
    public ArrayList<pkt_LSPDU> r1_lspdu;
    public ArrayList<pkt_LSPDU> r2_lspdu;
    public ArrayList<pkt_LSPDU> r3_lspdu;
    public ArrayList<pkt_LSPDU> r4_lspdu;
    public ArrayList<pkt_LSPDU> r5_lspdu;

    public ArrayList<ArrayList<pkt_LSPDU>> all_lspdus;
    public ArrayList<Integer> helloed_neighbor_through_link_id;

    public void main(String[] args) throws Exception{

        if (args.length != 4) {
            System.err.println("Inputs number incorrect, <router_id> <nse_host> <nse_port> <router_port>");
            System.exit(1);
        } else {
            router_id = Integer.parseInt(args[0]);
            nse_host = args[1];
            nse_port = Integer.parseInt(args[2]);
            router_port = Integer.parseInt(args[3]);
            if ((router_id < 0) || (nse_port < 0) || (router_port < 0))
            {
                System.err.println("the inputs must not contain negative value");
                System.exit(1);
            }
        }

        helloed_neighbor_through_link_id = new ArrayList<>();


        pkt_INIT ini_packet = new pkt_INIT();
        ini_packet.router_id = router_id;

        //init the socket
        send_socket = new DatagramSocket();
        receive_socket = new DatagramSocket();
        nse_host_addr = InetAddress.getByName(nse_host);

        // init the logger
        String temp = "router" + router_id + ".log";
        router_log = new PrintWriter(temp, "UTF-8");

        // send the init packet
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(router_id);
        temp_byte_arr = buffer.array();
        send_packet = new DatagramPacket(temp_byte_arr, temp_byte_arr.length, nse_host_addr,nse_port);
        send_socket.send(send_packet);
        // log the info
        router_log.println("Router id: " + router_id + " is sending pkt_INIT");
        router_log.flush();
//        current_connected_router++;
//        while (current_connected_router < NBR_ROUTER) {
//            Thread.sleep(1000);
//        }
//        current_connected_router = 0;

        // receive circuit_DB and deserialize the packet
        // receive circuit_DB
        byte[] receive_buffer = new byte[256];
        receive_packet = new DatagramPacket(receive_buffer, receive_buffer.length);

        receive_socket.receive(receive_packet);
        receive_buffer = receive_packet.getData();

        // unpack the packet and store the data.
        ByteBuffer bitebuffer = ByteBuffer.wrap(receive_buffer);
        int nbr_link = bitebuffer.getInt();
        link_cost[] tmp = new link_cost[nbr_link];
        router_log.println("Router id: " + router_id + " received circuit_DB, nbr_link: " + nbr_link);
        router_log.flush();

        // create hello packets simultaneously.
        ArrayList<pkt_HELLO> hello_packets = new ArrayList<>();
        // create LSPDU packets of this router
        r1_lspdu = new ArrayList<>();
        r2_lspdu = new ArrayList<>();
        r3_lspdu = new ArrayList<>();
        r4_lspdu = new ArrayList<>();
        r5_lspdu = new ArrayList<>();

        for (int i = 0; i < nbr_link; i++) {
            int id = bitebuffer.getInt();
            int cost = bitebuffer.getInt();
            link_cost current = new link_cost();
            current.cost = cost;
            current.link = id;
            tmp[i] = current;

            router_log.println(i + " link: link_id: " + id + "link_cost: " + cost);
            router_log.flush();

            pkt_HELLO hello = new pkt_HELLO();
            hello.link_id = id;
            hello.router_id = router_id;
            hello_packets.add(hello);

            pkt_LSPDU LSPDU = new pkt_LSPDU();
            LSPDU.sender = router_id;
            LSPDU.link_id = id;
            LSPDU.cost = cost;
            LSPDU.router_id = router_id;
            LSPDU.via = id;
            if (router_id == 1) r1_lspdu.add(LSPDU);
            else if (router_id == 2) r2_lspdu.add(LSPDU);
            else if (router_id == 3) r3_lspdu.add(LSPDU);
            else if (router_id == 4) r4_lspdu.add(LSPDU);
            else if (router_id == 5) r5_lspdu.add(LSPDU);
        }

        print_all_lspdu();
        // start sending hello packets
        for (int i = 0; i < nbr_link; i++) {
            pkt_HELLO current = hello_packets.get(i);

            ByteBuffer bitebuffer2 = ByteBuffer.allocate(8);
            bitebuffer2.putInt(current.router_id);
            bitebuffer2.putInt(current.link_id);
            byte[] temp_byte_arr2 = bitebuffer2.array();
            DatagramPacket send_packet2 = new DatagramPacket(temp_byte_arr2, temp_byte_arr2.length, nse_host_addr, nse_port);
            send_socket.send(send_packet2);
            router_log.println("R"+ current.router_id+" sends a pkt_HELLO, link_id: " + current.link_id);
            router_log.flush();
        }



        // initialize adjacency matrix to -1 to indicate there is no way to get to any nodes at first since no info
        graph = new int[6][6];
        for (int i = 1; i < 6; i++) {
            for (int j = 1; j < 6; j++) {
                if (i == j) graph[i][j] = 0;
                else graph[i][j] = -1;
            }
        }

        // Then staying and waiting for hellos and lspdus to update my own graph
        while(true)
        {
            byte[] receive = new byte[1024];
            DatagramPacket receive_packet = new DatagramPacket(receive, receive.length);
            receive_socket.receive(receive_packet);
            if (receive_packet.getLength() <= 10) {
                //it is a hello packet.
                pkt_HELLO neighbors_hello = new pkt_HELLO();

                receive = receive_packet.getData();
                ByteBuffer what_name = ByteBuffer.wrap(receive);
                int helloed_router_id = what_name.getInt();
                int helloed_link_id = what_name.getInt();

                neighbors_hello.router_id = helloed_router_id;
                neighbors_hello.link_id = helloed_link_id;
                helloed_neighbor_through_link_id.add(helloed_link_id);
                // first update a lspdu's router_id and via field by searching through all lspdus.
//                if (router_id == 1){
//                    for (int i = 0; i < r1_lspdu.size(); i++) {
//                        pkt_LSPDU cur = r1_lspdu.get(i);
//                        if ((cur.sender == router_id) && (cur.link_id == helloed_link_id) && (cur.router_id == 0)){
//                            r1_lspdu.get(i).router_id = helloed_router_id;
//                            r1_lspdu.get(i).via = helloed_link_id;
//                        }
//                    }
//                } else if (router_id == 2) {
//                    for (int i = 0; i < r2_lspdu.size(); i++) {
//                        pkt_LSPDU cur = r2_lspdu.get(i);
//                        if ((cur.sender == router_id) && (cur.link_id == helloed_link_id) && (cur.router_id == 0)){
//                            r2_lspdu.get(i).router_id = helloed_router_id;
//                            r2_lspdu.get(i).via = helloed_link_id;
//                        }
//                    }
//                } else if (router_id == 3) {
//                    for (int i = 0; i < r3_lspdu.size(); i++) {
//                        pkt_LSPDU cur = r3_lspdu.get(i);
//                        if ((cur.sender == router_id) && (cur.link_id == helloed_link_id) && (cur.router_id == 0)){
//                            r3_lspdu.get(i).router_id = helloed_router_id;
//                            r3_lspdu.get(i).via = helloed_link_id;
//                        }
//                    }
//                } else if (router_id == 4) {
//                    for (int i = 0; i < r4_lspdu.size(); i++) {
//                        pkt_LSPDU cur = r4_lspdu.get(i);
//                        if ((cur.sender == router_id) && (cur.link_id == helloed_link_id) && (cur.router_id == 0)){
//                            r4_lspdu.get(i).router_id = helloed_router_id;
//                            r4_lspdu.get(i).via = helloed_link_id;
//                        }
//                    }
//                } else if (router_id == 5) {
//                    for (int i = 0; i < r5_lspdu.size(); i++) {
//                        pkt_LSPDU cur = r5_lspdu.get(i);
//                        if ((cur.sender == router_id) && (cur.link_id == helloed_link_id) && (cur.router_id == 0)){
//                            r5_lspdu.get(i).router_id = helloed_router_id;
//                            r5_lspdu.get(i).via = helloed_link_id;
//                        }
//                    }
//                }



                // send lspdu we have, to this neighbor

                for (int i = 0; i < r1_lspdu.size(); i++) {
                    // start sending only have 4*5 = 20 bytes
                    ByteBuffer lspdu_send_buffer = ByteBuffer.allocate(20);
                    lspdu_send_buffer.putInt(r1_lspdu.get(i).sender);
                    lspdu_send_buffer.putInt(r1_lspdu.get(i).router_id);
                    lspdu_send_buffer.putInt(r1_lspdu.get(i).link_id);
                    lspdu_send_buffer.putInt(r1_lspdu.get(i).cost);
                    lspdu_send_buffer.putInt(r1_lspdu.get(i).via);
                    byte[] lspdu_send_byte_arr = lspdu_send_buffer.array();
                    DatagramPacket packet1 = new DatagramPacket(lspdu_send_byte_arr, 20, nse_host_addr, nse_port);
                    send_socket.send(packet1);
                    router_log.println("R" + router_id + " sends an LS PDU: sender: " + r1_lspdu.get(i).sender + " router_id: " + r1_lspdu.get(i).router_id + " link_id: " + r1_lspdu.get(i).link_id + " cost: " + r1_lspdu.get(i).cost + " via: " + r1_lspdu.get(i).via);
                    router_log.flush();
                }
                for (int i = 0; i < r2_lspdu.size(); i++) {
                    // start sending only have 4*5 = 20 bytes
                    ByteBuffer lspdu_send_buffer = ByteBuffer.allocate(20);
                    lspdu_send_buffer.putInt(r2_lspdu.get(i).sender);
                    lspdu_send_buffer.putInt(r2_lspdu.get(i).router_id);
                    lspdu_send_buffer.putInt(r2_lspdu.get(i).link_id);
                    lspdu_send_buffer.putInt(r2_lspdu.get(i).cost);
                    lspdu_send_buffer.putInt(r2_lspdu.get(i).via);
                    byte[] lspdu_send_byte_arr = lspdu_send_buffer.array();
                    DatagramPacket packet1 = new DatagramPacket(lspdu_send_byte_arr, 20, nse_host_addr, nse_port);
                    send_socket.send(packet1);
                    router_log.println("R" + router_id + " sends an LS PDU: sender: " + r1_lspdu.get(i).sender + " router_id: " + r1_lspdu.get(i).router_id + " link_id: " + r1_lspdu.get(i).link_id + " cost: " + r1_lspdu.get(i).cost + " via: " + r1_lspdu.get(i).via);
                    router_log.flush();
                }
                for (int i = 0; i < r3_lspdu.size(); i++) {
                    // start sending only have 4*5 = 20 bytes
                    ByteBuffer lspdu_send_buffer = ByteBuffer.allocate(20);
                    lspdu_send_buffer.putInt(r3_lspdu.get(i).sender);
                    lspdu_send_buffer.putInt(r3_lspdu.get(i).router_id);
                    lspdu_send_buffer.putInt(r3_lspdu.get(i).link_id);
                    lspdu_send_buffer.putInt(r3_lspdu.get(i).cost);
                    lspdu_send_buffer.putInt(r3_lspdu.get(i).via);
                    byte[] lspdu_send_byte_arr = lspdu_send_buffer.array();
                    DatagramPacket packet1 = new DatagramPacket(lspdu_send_byte_arr, 20, nse_host_addr, nse_port);
                    send_socket.send(packet1);
                    router_log.println("R" + router_id + " sends an LS PDU: sender: " + r1_lspdu.get(i).sender + " router_id: " + r1_lspdu.get(i).router_id + " link_id: " + r1_lspdu.get(i).link_id + " cost: " + r1_lspdu.get(i).cost + " via: " + r1_lspdu.get(i).via);
                    router_log.flush();
                }
                for (int i = 0; i < r4_lspdu.size(); i++) {
                    // start sending only have 4*5 = 20 bytes
                    ByteBuffer lspdu_send_buffer = ByteBuffer.allocate(20);
                    lspdu_send_buffer.putInt(r4_lspdu.get(i).sender);
                    lspdu_send_buffer.putInt(r4_lspdu.get(i).router_id);
                    lspdu_send_buffer.putInt(r4_lspdu.get(i).link_id);
                    lspdu_send_buffer.putInt(r4_lspdu.get(i).cost);
                    lspdu_send_buffer.putInt(r4_lspdu.get(i).via);
                    byte[] lspdu_send_byte_arr = lspdu_send_buffer.array();
                    DatagramPacket packet1 = new DatagramPacket(lspdu_send_byte_arr, 20, nse_host_addr, nse_port);
                    send_socket.send(packet1);
                    router_log.println("R" + router_id + " sends an LS PDU: sender: " + r1_lspdu.get(i).sender + " router_id: " + r1_lspdu.get(i).router_id + " link_id: " + r1_lspdu.get(i).link_id + " cost: " + r1_lspdu.get(i).cost + " via: " + r1_lspdu.get(i).via);
                    router_log.flush();
                }
                for (int i = 0; i < r5_lspdu.size(); i++) {
                    // start sending only have 4*5 = 20 bytes
                    ByteBuffer lspdu_send_buffer = ByteBuffer.allocate(20);
                    lspdu_send_buffer.putInt(r5_lspdu.get(i).sender);
                    lspdu_send_buffer.putInt(r5_lspdu.get(i).router_id);
                    lspdu_send_buffer.putInt(r5_lspdu.get(i).link_id);
                    lspdu_send_buffer.putInt(r5_lspdu.get(i).cost);
                    lspdu_send_buffer.putInt(r5_lspdu.get(i).via);
                    byte[] lspdu_send_byte_arr = lspdu_send_buffer.array();
                    DatagramPacket packet1 = new DatagramPacket(lspdu_send_byte_arr, 20, nse_host_addr, nse_port);
                    send_socket.send(packet1);
                    router_log.println("R" + router_id + " sends an LS PDU: sender: " + r1_lspdu.get(i).sender + " router_id: " + r1_lspdu.get(i).router_id + " link_id: " + r1_lspdu.get(i).link_id + " cost: " + r1_lspdu.get(i).cost + " via: " + r1_lspdu.get(i).via);
                    router_log.flush();
                }
                //print_all_lspdu();



            } else {
                // it is a lspdu packet
                receive = receive_packet.getData();
                ByteBuffer what_name2 = ByteBuffer.wrap(receive);
                pkt_LSPDU tmp_for_rcvd_lspdu = new pkt_LSPDU();
                int received_lspdu_sender = what_name2.getInt();
                int received_lspdu_router_id = what_name2.getInt();
                int received_lspdu_link_id = what_name2.getInt();
                int received_lspdu_cost = what_name2.getInt();
                int received_lspdu_via = what_name2.getInt();
                tmp_for_rcvd_lspdu.cost = received_lspdu_cost;
                tmp_for_rcvd_lspdu.link_id = received_lspdu_link_id;
                tmp_for_rcvd_lspdu.router_id = received_lspdu_router_id;
                tmp_for_rcvd_lspdu.via = received_lspdu_via;
                tmp_for_rcvd_lspdu.sender = received_lspdu_sender;
                //see if we already have this lspdu or not.
                boolean already_have = false;
                if (received_lspdu_router_id == 1)
                {
                    for (int i = 0; i < r1_lspdu.size(); i++)
                    {
                        if ((r1_lspdu.get(i).link_id == received_lspdu_link_id) && (r1_lspdu.get(i).router_id == received_lspdu_router_id))
                        {
                            already_have = true;
                            break;
                        }
                    }
                } else if (received_lspdu_router_id == 2)
                {
                    for (int i = 0; i < r2_lspdu.size(); i++) {
                        if ((r2_lspdu.get(i).link_id == received_lspdu_link_id) && (r2_lspdu.get(i).router_id == received_lspdu_router_id))
                        {
                            already_have = true;
                            break;
                        }
                    }
                } else if (received_lspdu_router_id == 3)
                {
                    for (int i = 0; i < r3_lspdu.size(); i++) {
                        if ((r3_lspdu.get(i).link_id == received_lspdu_link_id) && (r3_lspdu.get(i).router_id == received_lspdu_router_id))
                        {
                            already_have = true;
                            break;
                        }
                    }
                } else if (received_lspdu_router_id == 4)
                {
                    for (int i = 0; i < r4_lspdu.size(); i++) {
                        if ((r4_lspdu.get(i).link_id == received_lspdu_link_id) && (r4_lspdu.get(i).router_id == received_lspdu_router_id))
                        {
                            already_have = true;
                            break;
                        }
                    }
                } else if (received_lspdu_router_id == 5)
                {
                    for (int i = 0; i < r5_lspdu.size(); i++) {
                        if ((r5_lspdu.get(i).link_id == received_lspdu_link_id) && (r5_lspdu.get(i).router_id == received_lspdu_router_id))
                        {
                            already_have = true;
                            break;
                        }
                    }
                }
                router_log.println("R" + router_id + " receives an LS PDU: sender " + received_lspdu_sender + ", router_id " + received_lspdu_router_id + ", link_id " + received_lspdu_link_id + ", cost " + received_lspdu_cost + ", via " + received_lspdu_via);
                router_log.flush();
                // if this router does not have the lspdu, add it.
                if (!already_have)
                {
                    if (received_lspdu_router_id == 1) r1_lspdu.add(tmp_for_rcvd_lspdu);
                    else if (received_lspdu_router_id == 2) r2_lspdu.add(tmp_for_rcvd_lspdu);
                    else if (received_lspdu_router_id == 3) r3_lspdu.add(tmp_for_rcvd_lspdu);
                    else if (received_lspdu_router_id == 4) r4_lspdu.add(tmp_for_rcvd_lspdu);
                    else if (received_lspdu_router_id == 5) r5_lspdu.add(tmp_for_rcvd_lspdu);

                }
                print_all_lspdu();
                int sender_is = received_lspdu_sender;
                for (int i = 0; i < tmp.length; i++) {
                    // only send to helloed neighbors except for who send you this
                    for (int j = 0; j < helloed_neighbor_through_link_id.size(); j++) {
                        if ((tmp[i].link == helloed_neighbor_through_link_id.get(j)) && (tmp[i].link != tmp_for_rcvd_lspdu.via))
                        {
                            // set sender and via first
                            pkt_LSPDU new_lspdu_to_send = new pkt_LSPDU();
                            new_lspdu_to_send.sender = router_id;
                            new_lspdu_to_send.router_id = received_lspdu_router_id;
                            new_lspdu_to_send.via = tmp[i].link;
                            new_lspdu_to_send.link_id = received_lspdu_link_id;
                            new_lspdu_to_send.cost = received_lspdu_cost;

                            ByteBuffer new_buffer = ByteBuffer.allocate(20);
                            new_buffer.putInt(new_lspdu_to_send.sender);
                            new_buffer.putInt(new_lspdu_to_send.router_id);
                            new_buffer.putInt(new_lspdu_to_send.link_id);
                            new_buffer.putInt(new_lspdu_to_send.cost);
                            new_buffer.putInt(new_lspdu_to_send.via);

                            DatagramPacket sendPacket = new DatagramPacket(new_buffer.array(), 20, nse_host_addr, nse_port);
                            send_socket.send(sendPacket);
                            router_log.println("R" + router_id + " sends an LS PDU: sender " + new_lspdu_to_send.sender + ", router_id " + new_lspdu_to_send.router_id + ", link_id " + new_lspdu_to_send.router_id + ", cost " + new_lspdu_to_send.cost + ", via " + new_lspdu_to_send.via);
                            router_log.flush();
                        }
                    }

                }


            }

        }




    }

    public void print_all_lspdu(){
        router_log.println("R1 -> R1 nbr link " + r1_lspdu.size());
        for (int i = 0; i < r1_lspdu.size(); i++) {
            router_log.println("R" + router_id + "->R1, link " + r1_lspdu.get(i).link_id + " cost " + r1_lspdu.get(i).cost);
        }
        router_log.println("R2 -> R2 nbr link " + r2_lspdu.size());
        for (int i = 0; i < r2_lspdu.size(); i++) {
            router_log.println("R" + router_id + "->R2, link " + r2_lspdu.get(i).link_id + " cost " + r2_lspdu.get(i).cost);
        }
        router_log.println("R3 -> R3 nbr link " + r3_lspdu.size());
        for (int i = 0; i < r3_lspdu.size(); i++) {
            router_log.println("R" + router_id + "->R3, link " + r3_lspdu.get(i).link_id + " cost " + r3_lspdu.get(i).cost);
        }
        router_log.println("R4 -> R4 nbr link " + r4_lspdu.size());
        for (int i = 0; i < r4_lspdu.size(); i++) {
            router_log.println("R" + router_id + "->R4, link " + r4_lspdu.get(i).link_id + " cost " + r4_lspdu.get(i).cost);
        }
        router_log.println("R5 -> R5 nbr link " + r5_lspdu.size());
        for (int i = 0; i < r5_lspdu.size(); i++) {
            router_log.println("R" + router_id + "->R5, link " + r5_lspdu.get(i).link_id + " cost " + r5_lspdu.get(i).cost);
        }
        router_log.flush();
    }
}
