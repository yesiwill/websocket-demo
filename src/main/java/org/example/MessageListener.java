package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Component
public class MessageListener {

    private static final String ENCRYPTION_ALGORITHM = "PBEWithECDSAandSHA1";
    private static final String ENCRYPTION_KEY = "CAgECwIPCwwBAwknDAMHCgQPBg8eCQUBGgoBCQYHCQ0=";
    private static final String FILE_PATH = "message.txt";

    private WebSocketSession webSocketSession;
    private final ObjectMapper objectMapper;
    private final StandardWebSocketClient webSocketClient;

    public MessageListener(ObjectMapper objectMapper) throws ExecutionException, InterruptedException, IOException {
        this.objectMapper = objectMapper;
        this.webSocketClient = new StandardWebSocketClient();
        this.webSocketSession = connectToServer();
    }

    private WebSocketSession connectToServer() throws ExecutionException, InterruptedException, IOException {
        String url = "wss://nws.taoguba.com.cn/vipws?source=ANDROID&tokenId=android_8568144%2604d8e8a4-1c95-4ae1-86f9-e37d2938da5f&version=1";
        WebSocketSession session = webSocketClient.doHandshake(new MyWebSocketHandler(), url).get();
        System.out.println("Connected to server: " + url);
        return session;
    }

    @Scheduled(fixedRate = 30000) // 每30秒执行一次
    public void sendHeartbeatMessage() throws ExecutionException, InterruptedException, IOException {
        if (webSocketSession.isOpen()) {
            Map<String, Object> message = new HashMap<>();
            message.put("anchor", false);
            message.put("tokenId", "android_2779743\u002689c51142-454b-4f69-ae3e-57cce8ced40f");
            message.put("type", "heart");
            message.put("version", "1");
            message.put("wsType", "vipws");

            String payload = objectMapper.writeValueAsString(message);
            webSocketSession.sendMessage(new TextMessage(payload));
            System.out.println("Sent heartbeat message: " + payload);
        } else {
            System.out.println("WebSocket session is closed.");
            webSocketSession = connectToServer();
        }
    }

    @Scheduled(fixedRate = 40000) // 每40秒执行一次
    public void sendPingMessage() throws IOException, ExecutionException, InterruptedException {
        if (webSocketSession.isOpen()) {
            String pingMessage = "ping";
            webSocketSession.sendMessage(new TextMessage(pingMessage));
            System.out.println("Sent ping message: " + pingMessage);
        } else {
            System.out.println("WebSocket session is closed.");
            webSocketSession = connectToServer();
        }
    }

    private class MyWebSocketHandler extends TextWebSocketHandler {
        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            System.out.println("Received message: " + message.getPayload());
            String decryptMessage = decryptMessage(message.getPayload(), "CAgECwIPCwwBAwknDAMHCgQPBg8eCQUBGgoBCQYHCQ0=");
            System.out.println(decryptMessage);
            if (!"success".equals(decryptMessage)) {
                decryptMessage = LocalDateTime.now().toString() + "\n" + decryptMessage + "\n";
                try {
                    FileWriter writer = new FileWriter("message.txt", true); // 追加模式
                    writer.write(decryptMessage);
                    writer.close();
                    System.out.println("存储消息:" + decryptMessage);
                } catch (IOException e) {
                    System.out.println("发生错误：" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public static String decryptMessage(String encryptedMessage, String key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] encryptedMessageBytes = Base64.getDecoder().decode(encryptedMessage);
            byte[] decryptedMessageBytes = cipher.doFinal(encryptedMessageBytes);
            return new String(decryptedMessageBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}