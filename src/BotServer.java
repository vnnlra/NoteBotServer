import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BotServer {

    private final String token;
    private final File offsetFile;  //viene usato per tenere memoria dell'ultimo update
                                    //ci salva il parametro update_id dell'ultimo messaggio ricevuto
                                    //vedi file esempio_messaggio_ricevuto.json

    public BotServer(String token) {
        this.token = token;
        this.offsetFile = new File("offset.txt");
    }

    public void processOneBatch() {
        try {
            long lastUpdate = readOffset(); //legge nel file qual è id dell'ultimo messaggio inviato
            //String url = "https://api.telegram.org/bot" + token + "/getUpdates?timeout=25&offset=" + (lastUpdate + 1);
            String url = "https://api.telegram.org/bot" + token + "/getUpdates?offset=" + (lastUpdate + 1);

            String json = httpGet(url);

            List<TelegramMessage> messages = TelegramJsonParser.parseMessages(json);

            long maxSeen = lastUpdate;
            for (TelegramMessage up : messages) {
                if (up.updateId > maxSeen) maxSeen = up.updateId;

                if (up.chatId == null || up.text == null) continue;
                handleCommand(up.chatId, up.text.trim());
            }
            if (maxSeen > lastUpdate) writeOffset(maxSeen);

        } catch (Exception e) {
            System.err.println("Errore processOneBatch: " + e.getMessage());
        }
    }
    // -------- HTTP GET minimale --------
    private String httpGet(String urlStr) throws IOException {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        StringBuilder response = new StringBuilder();

        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET"); // esplicito, anche se è il default
            conn.setConnectTimeout(10000); // 10 secondi
            conn.setReadTimeout(10000);

            int status = conn.getResponseCode();

            // Sceglie lo stream giusto: input se 200 OK, error stream altrimenti
            InputStream stream = (status >= 200 && status < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

        } finally {
            // Chiusura sicura degli stream e della connessione
            if (reader != null) try { reader.close(); } catch (IOException ignored) {}
            if (conn != null) conn.disconnect();
        }

        return response.toString();
    }

    private void handleCommand(String chatId, String text) {
        try {
            if (text.startsWith("/nota ")) {
                String nota = text.substring(6).trim();
                if (nota.isEmpty()) { send(chatId, "❗ Usa: /nota <testo>"); return; }
                appendNote(chatId, nota);
                send(chatId, "📝 Nota salvata:\n" + nota);
            }
            else if (text.startsWith("/leggi")) {
                int n = 5;
                String[] parts = text.split("\\s+");
                if (parts.length > 1 && parts[1].matches("\\d+")) n = Integer.parseInt(parts[1]);

                List<String> lines = readNotes(chatId);
                if (lines.isEmpty()) { send(chatId, "📭 Nessuna nota salvata."); return; }

                List<String> last = lines.subList(Math.max(0, lines.size() - n), lines.size());
                StringBuilder sb = new StringBuilder();
                sb.append("🗒️ Ultime ").append(last.size()).append(" note:\n");
                int idx = 1;
                for (String line : last) {
                    String[] kv = line.split("\t", 2);
                    String ts = kv.length > 0 ? kv[0] : "";
                    String note = kv.length > 1 ? kv[1] : line;
                    sb.append(idx++).append(") [").append(ts).append("] ").append(note).append("\n");
                }
                send(chatId, sb.toString());
            }

            else if (text.equals("/help")) {
                send(chatId,
                        "👋 Ciao! Comandi:\n" +
                                "/nota <testo>  → salva una nota\n" +
                                "/leggi [N]     → mostra le ultime N note (default 5)");
            }
        } catch (Exception e) {
            System.err.println("Errore handleCommand: " + e.getMessage());
        }
    }

    // -------- Storage file per chat --------

    private File storeFile(String chatId) {
        return new File("notes_" + chatId + ".txt");
    }

    private void appendNote(String chatId, String nota) throws IOException {
        try (FileWriter w = new FileWriter(storeFile(chatId), true)) {
            w.write(now() + "\t" + nota + System.lineSeparator());
        }
    }

    private List<String> readNotes(String chatId) throws IOException {
        File f = storeFile(chatId);
        if (!f.isFile()) return Collections.emptyList();
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            for (String ln; (ln = br.readLine()) != null; ) {
                ln = ln.trim();
                if (!ln.isEmpty()) lines.add(ln);
            }
        }
        return lines;
    }

    private String now() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date());
    }



    // -------- Persistenza offset --------

    private long readOffset() {
        try {
            File f = offsetFile;
            if (!f.isFile()) return 0;
            String s = new String(java.nio.file.Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
            if (s.isEmpty()) return 0;
            return Long.parseLong(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private void writeOffset(long v) {
        try {
            java.nio.file.Files.write(offsetFile.toPath(), Long.toString(v).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {}
    }

    // -------- Invio messaggi (riusa la tua NoteBot) --------

    private void send(String chatId, String text) {
        try {
            NoteBot sender = new NoteBot(token, chatId);
            sender.sendMessage(text);
        } catch (Exception e) {
            System.err.println("Errore send: " + e.getMessage());
        }
    }
}

