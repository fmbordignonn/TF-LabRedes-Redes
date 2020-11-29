package com.labredes.tf.redes;


import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.CRC32;

public class ClientEnvia {

    static List<DatagramPacketInfo> packets = new ArrayList<>();

    static final int SLOW_START_MAX_DATA_PACKAGES = 2;
    static final char FILE_END_DELIMITER_CHAR = '|';
    static InetAddress ipAddress = null;
    static final int RECEIVER_PORT = 9876;

    static Map<Integer, Integer> acksReplicados = new HashMap<>();

    static DatagramSocket clientSocket;

    public static void main(String args[]) throws Exception {
        System.out.println("\n---- Terminal ----\n");
        System.out.println("Aperte 1 para estabelecer conexão");
        System.out.println("Aperte 0 para sair");

        Scanner in = new Scanner(System.in);

        int input = in.nextInt();

        switch (input) {
            case 1:
                startConection();
                break;
            case 0:
                System.exit(0);
            default:
                break;
        }
    }


    public static void startConection() throws Exception {
        //estabelecendo que esse socket roda na porta 6789
        clientSocket = new DatagramSocket(6789);

        ipAddress = InetAddress.getByName("localhost");

        System.out.println("\nConexão estabelecida!");

        createPackets();

        int listIterator = initializeSlowStart(SLOW_START_MAX_DATA_PACKAGES);

        if (listIterator >= packets.size()) {
            System.out.println("ja enviou tudo, nao precisa do avoidance");
        } else {
            congestionAvoidance(listIterator);
            System.out.println("\nConexão encerrada!");
        }
    }


    public static int initializeSlowStart(int packageLimit) throws Exception {
        int pacotesParaEnviar = 1;

        int listIterator = 0;

        int actualPackageLimit = 1;
        int packetCalculo = 1;
        while (packetCalculo != packageLimit) {
            packetCalculo *= 2;
            actualPackageLimit = actualPackageLimit * 2 + 1;
        }

        List<String> acksReceived = new ArrayList<String>();

        DatagramPacketInfo info;

        try {
            while (pacotesParaEnviar <= actualPackageLimit) {
                for (listIterator = listIterator; listIterator < pacotesParaEnviar; listIterator++) {
                    try {
                        info = packets.get(listIterator);
                    } catch (Exception ex) {
                        //acabou de iterar, enviou tudo
                        break;
                    }

                    DatagramPacketResponse response = sendPacket(info);

                    acksReceived.add("recebe response: " + response.getMessage() + ":" + response.getSeq());

                    if (response.getSeq() != info.getSeq() + 1) {
                        System.out.println("ack duplicado recebido");
                        System.out.println("DEU ACK DUPLICADO NO SLOW START, ENCERRANDO APP (TEMPORARIO)");
                        System.exit(0);
                    }
                }

                for (int i = 0; i < acksReceived.size(); i++) {
                    System.out.println(acksReceived.get(i));
                }

                acksReceived = new ArrayList<String>();

                pacotesParaEnviar = pacotesParaEnviar * 2 + 1;
            }
        } catch (SocketTimeoutException ex) {
            for (int i = 0; i < acksReceived.size(); i++) {
                System.out.println(acksReceived.get(i));
            }

            acksReceived = new ArrayList<String>();

            System.out.println("Timeout");
            System.out.println("Reenviando pacote...");

            //listIterator = 0;
            initializeSlowStart(SLOW_START_MAX_DATA_PACKAGES);

        }
        return listIterator;
    }

    public static void congestionAvoidance(int listIterator) throws Exception {
        System.out.println("Cheguei no congestionAvoidance");

        DatagramPacketInfo info;

        List<String> acksReceived = new ArrayList<String>();

        //trocar por slow start max packages?
        int quantPacketSend = SLOW_START_MAX_DATA_PACKAGES + 1;

        try {
            while (packets.size() != listIterator) {

                for (int i = 0; i < quantPacketSend; i++) {

                    try {
                        info = packets.get(listIterator);
                    } catch (Exception ex) {
                        //acabou de iterar, enviou tudo
                        break;
                    }

                    DatagramPacketResponse response = sendPacket(info);

                    checkReplicateAck(response, info.getSeq());

                    acksReceived.add("recebe response: " + response.getMessage() + ":" + response.getSeq());

                    listIterator++;
                }

                for (int i = 0; i < acksReceived.size(); i++) {
                    System.out.println(acksReceived.get(i));
                }

                acksReceived = new ArrayList<String>();

                quantPacketSend++;
            }

        } catch (SocketTimeoutException ex) {
            congestionAvoidance(listIterator);

            for (int i = 0; i < acksReceived.size(); i++) {
                System.out.println(acksReceived.get(i));
            }

            acksReceived = new ArrayList<String>();

            System.out.println("Timeout");
            System.out.println("Reenviando pacote...");

            listIterator = 0;
            //ta dando pau
            initializeSlowStart(listIterator);

        }
    }

    public static void checkReplicateAck(DatagramPacketResponse response, int seqSent) throws Exception{
        //ACK duplicado, deu pau..
        if (seqSent != response.getSeq() - 1 ) {
            System.out.println("recebeu um ack replicado");

            int replicado = response.getSeq();

            if (!acksReplicados.containsKey(replicado)) {
                acksReplicados.put(replicado, 1);
            } else {
                acksReplicados.put(replicado, acksReplicados.get(replicado) + 1);
            }

            List<Integer> packetsLostSeqNumber = acksReplicados.entrySet().stream()
                    //se ja tiver 3 ou mais acks na lista...
                    .filter(x -> x.getValue() >= 3)
                    //pega a key (seq do pacote perdido)...
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            //value aqui é o seq do pacote perdido
            for (int seq : packetsLostSeqNumber) {
                DatagramPacketInfo packet = packets.get(seq);
                System.out.println("REENVIANDO PACOTE QUE FOI PERDIDO - SEQ[" + packet.getSeq() + "]");
                DatagramPacketResponse newResponse = sendPacket(packet);

                //ver oq fazer com o response aqui

                //removendo que este pacote foi perdido
                acksReplicados.remove(seq);
                //TRATAR OQ ACONTECE SE DER PAU DNV
            }
        }
    }

    public static DatagramPacketResponse parseMessage(DatagramPacket message) {
        String[] split = new String(message.getData()).split("-");

        return new DatagramPacketResponse(split[0], Integer.parseInt(split[1].trim()));
    }

    public static DatagramPacketResponse sendPacket(DatagramPacketInfo packet) throws Exception {
        byte[] responseData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(responseData, responseData.length, ipAddress, RECEIVER_PORT);

        String message = Arrays.toString(packet.getFileData()) + "-" + packet.getCRC() + "-" + packet.getSeq();

        System.out.println("enviando mensagem: " + message);

        byte[] packetData = message.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(packetData, packetData.length, ipAddress, RECEIVER_PORT);

        clientSocket.send(sendPacket);

        clientSocket.receive(receivePacket);

        DatagramPacketResponse response = parseMessage(receivePacket);

        //System.out.println("recebe response: " + response.getMessage() + ":" + response.getSeq());

        return response;
    }

    public static long calculaCRC(byte[] array) {
        CRC32 crc = new CRC32();

        crc.update(array);

        long valor = crc.getValue();

        //System.out.println("Valor crc: " + valor);

        return valor;
    }

    public static void createPackets() throws Exception {

        Scanner in = new Scanner(System.in);

        System.out.println("\n---- Escolha um arquivo ----\n");
        System.out.println("1 - case 1, 300 bytes");
        System.out.println("2 - case 1, ? bytes");

        int input = in.nextInt();

        String filepath = "C:\\Users\\Felipe\\Desktop\\Trabalho final redes\\TF-LabRedes-Redes\\tf-redes-envia\\input\\case1.txt";

        long valor = 1215645;

        //16 pacotes
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 1));
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 2));
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 3));
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 4));
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 5));
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 6));
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 7));
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 8));
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 9));
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 10));
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 11));
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 12));

        Path path = Paths.get(filepath);

        List<String> data = Files.readAllLines(path);

        //MOCK, ALTERAR PRA 0 QUANDO FOR VERSAO FINAL
        int numeroSequencia = 13;

        //coloca na lista de dados de cada packet o que deve ser enviado, em ordem
        for (int i = 0; i < data.size(); i++) {

            String content = data.get(i);
            final int MAX_BYTES = 300;

            if (content.toCharArray().length < MAX_BYTES) {
                char[] contentBytes = new char[MAX_BYTES];
                char[] contentChars = content.toCharArray();

                for (int j = 0; j < contentChars.length; j++) {
                    contentBytes[j] = contentChars[j];
                }

                for (int j = contentChars.length; j < MAX_BYTES; j++) {
                    contentBytes[j] = FILE_END_DELIMITER_CHAR;
                }

                content = new String(contentBytes);

            }

            byte[] arrayBytes = content.getBytes();

            long crc = calculaCRC(arrayBytes);

            packets.add(new DatagramPacketInfo(arrayBytes, crc, numeroSequencia));

            numeroSequencia++;

        }

        in.close();
    }
}
