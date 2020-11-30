package com.labredes.tf.redes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.CRC32;


class ClientRecebe {

    static Map<Integer, byte[]> receivedFileData = new HashMap<>();
    static DatagramSocket serverSocket;
    static InetAddress ipAddress;
    static int port;

    public static void main(String args[]) throws Exception {
        System.out.println("Iniciou");

        //estabelecendo que esse socket roda na porta 9876
        serverSocket = new DatagramSocket(9876);

        int finalPacketSeqNumber;

        while (true) {

            //Thread.sleep(1000);

            DatagramPacketInfo packetInfo = receivePacket();

            if (packetInfo.isFinalPacket()) {

                receivedFileData.put(packetInfo.getSeq(), packetInfo.getFileData());

                finalPacketSeqNumber = packetInfo.getSeq();

                int missingPacket = 0;

                do {
                    //ultimo packet recebido
                    //validar se recebeu o ultimo?

                    missingPacket = checkMissingPackets(finalPacketSeqNumber);

                    if (!(missingPacket == 0)) {
                        System.out.println("TÁ NO PACOTE FINAL, E ALGUM FOI PERDIDO NO MEIO DO CAMINHO, INICIANDO REQUISIÇAO DESSE PACOTE");

                        //missingPacket contém o pacote perdido
                        //server deve requisitar ele novamente
                        sendResponsePacket("ACK-" + missingPacket, ipAddress, port);

                        packetInfo = receivePacket();

                        if (packetInfo.getSeq() == missingPacket) {
                            receivedFileData.put(packetInfo.getSeq(), packetInfo.getFileData());
                            continue;
                        }
                    }

                } while (missingPacket != 0);

                sendResponsePacket("FINISHED", ipAddress, port);

                System.out.println("TERMINOU DE RECEBER TODOS PACOTES! DESCONECTANDO CLIENT....");

                buildAndValidateFile(finalPacketSeqNumber);

                break;
            }

            //insere no dicionario de pacotes recebidos os dados desse arquivo, com chave = seq

            long crc = calculaCRC(packetInfo.getFileData());

            if (crc == packetInfo.getCRC()) {
                System.out.println("CRC correto, pacote chegou integro");
            } else {
                System.out.println("Algo se perdeu no caminho do pacote");
            }

            receivedFileData.put(packetInfo.getSeq(), packetInfo.getFileData());

            int missingPacket = checkMissingPackets(packetInfo.getSeq());

            if (!(missingPacket == 0)) {
                //missingPacket contém o pacote perdido
                //server deve requisitar ele novamente
                sendResponsePacket("ACK-" + missingPacket, ipAddress, port);

                continue;
            }

            packetInfo.setSeq(packetInfo.getSeq() + 1);

            sendResponsePacket("ACK-" + packetInfo.getSeq(), ipAddress, port);

        }
    }

    public static void sendResponsePacket(String message, InetAddress ipAddress, int port) throws Exception {
        byte[] sendData = new byte[1024];

        DatagramPacket response = new DatagramPacket(sendData, sendData.length, ipAddress, port);

        response.setData(message.getBytes());

        serverSocket.send(response);
    }

    public static DatagramPacketInfo receivePacket() throws Exception {
        byte[] receiveData = new byte[10024];

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);

        String receivedMessage = new String(receivePacket.getData());

        ipAddress = receivePacket.getAddress();

        port = receivePacket.getPort();

        System.out.println("Mensagem recebida: " + receivedMessage.replace("-", " - "));

        DatagramPacketInfo packetInfo = parseInputMessage(receivedMessage);

        return packetInfo;

    }

    /**
     * @param seqReceived sequence number recebido no pacote atual
     * @return 0 se nao há pacotes faltando, senao retorna o sequence number do pacote faltando
     */
    public static int checkMissingPackets(int seqReceived) {
        //pega todos os seqs antes do que chegou agora
        List<Integer> lista = receivedFileData.keySet()
                .stream()
                .filter(seq -> seq <= seqReceived)
                .collect(Collectors.toList());

        //verifica se os ultimos pacotes antes do que chegou agora chegaram ok..
        //ver se o size nao vai buga
        for (int seq = 1; seq <= seqReceived; seq++) {

            //se um pacote nao chegou, precisa pedir de novo
            //seq - 1 no lista.get pq os index começam em 0
            if (seq != lista.get(seq - 1)) {
                return seq;//faltou pacote aqui (i tá faltando)

                //se o pacote 1 e 2 tiverem faltando, ele vai pedir o 1 até conseguir receber, pra só depois pedir o 2, e assim em diante
                //como mexe direto no dicionario de dados recebidos, ele vai ficar pedindo o mesmo sempre até receber ele
            }
        }

        return 0;
    }

    public static DatagramPacketInfo parseInputMessage(String message) {
        DatagramPacketInfo packetInfo = new DatagramPacketInfo();

        String[] splitMessage = message.split("-");

        packetInfo.setFileData(formatByteArray(splitMessage[0]));
        packetInfo.setCRC(Long.parseLong(splitMessage[1]));
        packetInfo.setSeq(Integer.parseInt(splitMessage[2].trim()));

        try {
            //Dentro de um try-catch pq só no ultimo pacote vem essa info preenchida como true
            packetInfo.setFinalPacket(Boolean.parseBoolean(splitMessage[3].trim()));
        } catch (IndexOutOfBoundsException ex) {
            //é o ultimo pacote...
        }


        return packetInfo;
    }

    //monta o fileData do DatagramPacketInfo
    public static byte[] formatByteArray(String message) {
        String initial = message
                .replace("[", "")
                .replace("]", "")
                .replace(" ", "");

        String[] size = initial.split(",");

        byte[] auxArray = new byte[size.length];

        for (int i = 0; i < size.length; i++) {
            auxArray[i] = Byte.parseByte(size[i]);
        }

        return auxArray;
    }

    public static long calculaCRC(byte[] array) {
        CRC32 crc = new CRC32();

        crc.update(array);

        long valor = crc.getValue();

        return valor;
    }

    public static void buildAndValidateFile(int finalPacketSeqNumber) throws Exception {
        //remove os delimiters e salva o arquivo no caminho indicado pelo usuario
        byte[] lastPacketReceived = receivedFileData.get(finalPacketSeqNumber);

        List<Byte> auxList = new ArrayList<>();

        for (int i = 0; i < lastPacketReceived.length; i++) {
            if (lastPacketReceived[i] != 124) {
                auxList.add(lastPacketReceived[i]);
            }
        }

        byte[] finalArray = new byte[auxList.size()];

        for (int i = 0; i < auxList.size(); i++) {
            finalArray[i] = auxList.get(i);
        }

        receivedFileData.put(finalPacketSeqNumber, finalArray);

        int totalByteCount = 0;

        for (byte[] fileData : receivedFileData.values()) {
            totalByteCount += fileData.length;
        }

        totalByteCount += (receivedFileData.size() * 2) + 1;

        byte[] allFileBytes = new byte[totalByteCount];

        int copyStopped = 0;
        int indexStart = 0;

        byte[] lineSeparator = System.getProperty("line.separator").getBytes();

        for (byte[] value: receivedFileData.values()) {
            for (copyStopped = copyStopped, indexStart = 0; indexStart < value.length; copyStopped++, indexStart++ ) {
                allFileBytes[copyStopped] = value[indexStart];
            }

            //isso talvez quebre em outros sistemas operacionais nao-Windows, pq cada OS trata isso de forma diferente

            copyStopped++;
            allFileBytes[copyStopped] = lineSeparator[0];

            copyStopped++;
            allFileBytes[copyStopped] = lineSeparator[1];
        }

        Scanner in = new Scanner(System.in);

        System.out.println("Digite a pasta de destino do arquivo: ");

        String fileDirectory = in.nextLine();

        fileDirectory = "C:\\Users\\Felipe\\Desktop";

        in.close();

        FileOutputStream fos = new FileOutputStream(fileDirectory + "\\arquivo recebido.txt");

        fos.write(allFileBytes);

        File file = new File(fileDirectory + "\\arquivo recebido.txt");

        file.hashCode();

        System.out.println("Salvou o arquivo com sucesso");

        System.exit(0);
    }
}
