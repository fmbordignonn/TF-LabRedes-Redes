package com.labredes.tf.redes;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.stream.Collectors;
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

                    sendPacket(info);

                    DatagramPacketResponse response = receivePacket();

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

        DatagramPacketInfo packetInfo = null;

        List<String> acksReceived = new ArrayList<String>();

        //trocar por slow start max packages?
        int quantPacketSend = SLOW_START_MAX_DATA_PACKAGES + 1;

        try {
            while (packets.size() != listIterator) {

                for (int i = 0; i < quantPacketSend; i++) {

                    try {
                        packetInfo = packets.get(listIterator);
                    } catch (Exception ex) {
                        //acabou de iterar, enviou tudo
                        break;
                    }

                    sendPacket(packetInfo);
                    DatagramPacketResponse response = receivePacket();

                    checkReplicateAck(response, packetInfo.getSeq());

                    acksReceived.add("recebe response: " + response.getMessage() + ":" + response.getSeq());

                    listIterator++;
                }

                for (int i = 0; i < acksReceived.size(); i++) {
                    System.out.println(acksReceived.get(i));
                }

                acksReceived = new ArrayList<String>();

                quantPacketSend++;
            }

            String finalServerResponse = "";

            if (packetInfo.isFinalPacket()) {
                while (!finalServerResponse.equals("FINISHED")) {
                    System.out.println("FALTOU ALGUM PACOTE NO CAMINHO, CONVERSANDO COM SERVER PRA VER QUAL");

                    finalServerResponse = sendLastMissingPackets();
                }
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

    public static void checkReplicateAck(DatagramPacketResponse response, int seqSent) throws Exception {
        //ACK duplicado, deu pau..
        if (seqSent != response.getSeq() - 1) {
            //System.out.println("recebeu um ack replicado");

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

            if (!packetsLostSeqNumber.isEmpty()) {
                //value aqui é o seq do pacote perdido
                for (int seq : packetsLostSeqNumber) {
                    DatagramPacketInfo packet = packets
                            .stream()
                            .filter(x -> x.getSeq() == seq)
                            .findFirst()
                            //TEMPORARIO
                            .orElse(null);
                    //tecnicamente, o programa NUNCA vai cair nesse orElseThrow,pq o sequenciamento de pacotes vai estar correto,
                    //ele tá aqui só pq enquanto estamos testando com pacotes mockados, eles nao sao perdidos na rede, mas sim deletados
                    //no lado do client
                    //.orElseThrow(() -> new Exception("Não foi encontrado o pacote que falhou no envio"));

                    //TEMPORARIO TBM
                    if (packet == null) {
                        if (seq == 4) {
                            packet = new DatagramPacketInfo(new byte[]{4, 4, 4, 4}, 123453252, seq);
                        }

                        if (seq == 5) {
                            packet = new DatagramPacketInfo(new byte[]{5, 5, 5, 5}, 123453252, seq);
                        }

                        if (seq == 6) {
                            packet = new DatagramPacketInfo(new byte[]{6, 6, 6, 6}, 123453252, seq);
                        }

                        if (seq == 7) {
                            packet = new DatagramPacketInfo(new byte[]{7, 7, 7, 7}, 123453252, seq);
                        }

                        if (seq == 11) {
                            packet = new DatagramPacketInfo(new byte[]{11, 11, 11, 11}, 123453252, seq);
                        }

                        if (seq == 12) {
                            packet = new DatagramPacketInfo(new byte[]{12, 12, 12, 12}, 123453252, seq);
                        }
                    }


                    System.out.println("REENVIANDO PACOTE QUE FOI PERDIDO - SEQ[" + replicado + "]");

                    sendPacket(packet);

                    DatagramPacketResponse newResponse = receivePacket();

                    System.out.println("PACOTE QUE HAVIA FALHADO RECEBIDO COM SUCESSO!");

                    //ver oq fazer com o response aqui

                    //removendo que este pacote foi perdido
                    acksReplicados.remove(seq);
                }
            }
        }
    }

    public static String sendLastMissingPackets() throws Exception{
        DatagramPacketResponse newResponse = null;

        List<Integer> packetsLostSeqNumber = acksReplicados.entrySet().stream()
                //se ja tiver 3 ou mais acks na lista...
                .filter(x -> x.getValue() >= 1)
                //pega a key (seq do pacote perdido)...
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (!packetsLostSeqNumber.isEmpty()) {
            //value aqui é o seq do pacote perdido

            for (int seq : packetsLostSeqNumber) {
                DatagramPacketInfo packet = packets
                        .stream()
                        .filter(x -> x.getSeq() == seq)
                        .findFirst()
                        //TEMPORARIO
                        .orElse(null);
                //tecnicamente, o programa NUNCA vai cair nesse orElseThrow,pq o sequenciamento de pacotes vai estar correto,
                //ele tá aqui só pq enquanto estamos testando com pacotes mockados, eles nao sao perdidos na rede, mas sim deletados
                //no lado do client
                //.orElseThrow(() -> new Exception("Não foi encontrado o pacote que falhou no envio"));

                //TEMPORARIO TBM
                if (packet == null) {
                    if (seq == 1) {
                        packet = new DatagramPacketInfo(new byte[]{1,1,1,1}, 123453252, seq);
                    }

                    if (seq == 2) {
                        packet = new DatagramPacketInfo(new byte[]{2,2,2,2}, 123453252, seq);
                    }

                    if (seq == 3) {
                        packet = new DatagramPacketInfo(new byte[]{3,3,3,3}, 123453252, seq);
                    }

                    if (seq == 4) {
                        packet = new DatagramPacketInfo(new byte[]{4, 4, 4, 4}, 123453252, seq);
                    }

                    if (seq == 5) {
                        packet = new DatagramPacketInfo(new byte[]{5, 5, 5, 5}, 123453252, seq);
                    }

                    if (seq == 6) {
                        packet = new DatagramPacketInfo(new byte[]{6, 6, 6, 6}, 123453252, seq);
                    }

                    if (seq == 7) {
                        packet = new DatagramPacketInfo(new byte[]{7, 7, 7, 7}, 123453252, seq);
                    }

                    if (seq == 8) {
                        packet = new DatagramPacketInfo(new byte[]{8,8,8,8}, 123453252, seq);
                    }

                    if (seq == 9) {
                        packet = new DatagramPacketInfo(new byte[]{9,9,9,9}, 123453252, seq);
                    }

                    if (seq == 10) {
                        packet = new DatagramPacketInfo(new byte[]{10,10,10,10}, 123453252, seq);
                    }

                    if (seq == 11) {
                        packet = new DatagramPacketInfo(new byte[]{11, 11, 11, 11}, 123453252, seq);
                    }

                    if (seq == 12) {
                        packet = new DatagramPacketInfo(new byte[]{12, 12, 12, 12}, 123453252, seq);
                    }
                }


                System.out.println("REENVIANDO PACOTE QUE FOI PERDIDO - SEQ[" + seq + "]");

                sendPacket(packet);

                newResponse = receivePacket();

                if(!newResponse.getMessage().trim().equals("FINISHED")){
                    acksReplicados.remove(seq);
                    acksReplicados.put(newResponse.getSeq(), 3);

                    sendLastMissingPackets();
                }

                System.out.println("PACOTE QUE HAVIA FALHADO RECEBIDO COM SUCESSO!");

                //ver oq fazer com o response aquinewResponse = receivePacket();

                //removendo que este pacote foi perdido
                acksReplicados.remove(seq);

                return newResponse.getMessage();
            }
        }

        return "FINISHED";
    }

    public static DatagramPacketResponse parseResponseMessage(DatagramPacket message) {
        String[] split = new String(message.getData()).split("-");

        if (split[0].trim().equals("FINISHED")) {
            //nao importa o seq aqui
            return new DatagramPacketResponse(split[0], 1);
        }

        return new DatagramPacketResponse(split[0], Integer.parseInt(split[1].trim()));
    }

    public static void sendPacket(DatagramPacketInfo packet) throws Exception {
        String message = "";

        if (packet.isFinalPacket()) {
            message = Arrays.toString(packet.getFileData()) + "-" + packet.getCRC() + "-" + packet.getSeq() + "-" + packet.isFinalPacket();
        } else {
            message = Arrays.toString(packet.getFileData()) + "-" + packet.getCRC() + "-" + packet.getSeq();
        }

        System.out.println("enviando mensagem: " + message);

        byte[] packetData = message.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(packetData, packetData.length, ipAddress, RECEIVER_PORT);

        clientSocket.send(sendPacket);
    }

    public static DatagramPacketResponse receivePacket() throws Exception {
        byte[] responseData = new byte[1024];

        DatagramPacket receivePacket = new DatagramPacket(responseData, responseData.length, ipAddress, RECEIVER_PORT);

        clientSocket.receive(receivePacket);

        DatagramPacketResponse response = parseResponseMessage(receivePacket);

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

        //12 pacotes
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 1));
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 2));
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 3));
//        //packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 4));
//        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 5));
//        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 6));
//        //packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 7));
//        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 8));
//        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 9));
//        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 10));
        //packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 11));
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 12));
        packets.add(new DatagramPacketInfo("mock".getBytes(), valor, 13, true));


//        Path path = Paths.get(filepath);
//
//        List<String> data = Files.readAllLines(path);
//
//        //MOCK, ALTERAR PRA 0 QUANDO FOR VERSAO FINAL
//        int numeroSequencia = 13;
//
//        //coloca na lista de dados de cada packet o que deve ser enviado, em ordem
//        for (int i = 0; i < data.size(); i++) {
//
//            String content = data.get(i);
//            final int MAX_BYTES = 300;
//
//            if (content.toCharArray().length < MAX_BYTES) {
//                char[] contentBytes = new char[MAX_BYTES];
//                char[] contentChars = content.toCharArray();
//
//                for (int j = 0; j < contentChars.length; j++) {
//                    contentBytes[j] = contentChars[j];
//                }
//
//                for (int j = contentChars.length; j < MAX_BYTES; j++) {
//                    contentBytes[j] = FILE_END_DELIMITER_CHAR;
//                }
//
//                content = new String(contentBytes);
//
//            }
//
//            byte[] arrayBytes = content.getBytes();
//
//            long crc = calculaCRC(arrayBytes);
//
//            packets.add(new DatagramPacketInfo(arrayBytes, crc, numeroSequencia));
//
//            numeroSequencia++;
//
//        }
//
//        in.close();
    }
}
