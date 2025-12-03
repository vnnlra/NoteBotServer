import java.net.*;
import java.io.*;

public class NoteBot {
    private final String token;
    private final String chatId;

    public NoteBot(String token, String chatId) {
        this.token = token;
        this.chatId = chatId;
    }

    //Invia un messaggio al Bot Telegram
    public String sendMessage(String msg) throws Exception {
        String urlStr = "https://api.telegram.org/bot" + token + "/sendMessage"
                + "?chat_id=" + chatId
                + "&text=" + URLEncoder.encode(msg, "UTF-8");
        System.out.println("Url "+urlStr);
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line,jsonResponse = "";
        while ((line = in.readLine()) != null) {
            jsonResponse+=line;
        }
        in.close();
        return jsonResponse;
    }
    // Invia un messaggio usando HTTP GET
    public String sendMessageGET(String msg) throws Exception {

        // 1. Parametri nella URL (tipico del GET)
        //  in questo caso invochiamo la api telegram getMessaage
        String urlStr = "https://api.telegram.org/bot" + token + "/sendMessage"
                + "?chat_id=" + chatId
                + "&text=" + URLEncoder.encode(msg, "UTF-8");

        URL url = new URL(urlStr);

        // 2. Creo l'oggetto connessione (NON viene aperta la connessione)
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // 3. Imposto metodo prima dell'apertura
        conn.setRequestMethod("GET");

        // ---------- DIDATTICA ----------
        // La connessione HTTP NON è ancora avvenuta.
        // -------------------------------------

        // 4. Apertura effettiva della connessione
        // Avviene ora → getInputStream()
        BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8")
        );

        // 5. Leggo la risposta JSON
        StringBuilder resp = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            resp.append(line);
        }
        in.close();

        return resp.toString();
    }

    // Invia un messaggio usando HTTP POST
    public String sendMessagePOST(String msg) throws Exception {

        // 1. URL senza parametri (tipico del POST)
        String urlStr = "https://api.telegram.org/bot" + token + "/sendMessage";
        URL url = new URL(urlStr);

        // 2. Parametri del body
        String body = "chat_id=" + chatId +
                "&text=" + URLEncoder.encode(msg, "UTF-8");

        // 3. Creo l'oggetto connessione (non apre la connessione)
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // 4. Imposto metodo e uso del body
        conn.setRequestMethod("POST");
        conn.setDoOutput(true); // obbligatorio true per i POST, serve se voglio inviare un body


        // ---------- NOTA DIDATTICA ----------
        // Ancora nessuna connessione aperta!
        // Si aprirà appena accediamo allo stream.
        // -------------------------------------

        // 5. Apertura effettiva → getOutputStream()

        try (OutputStream os = conn.getOutputStream()) {
            // la request viene inviata
            os.write(body.getBytes("UTF-8"));
        }

        // 6. Leggo la risposta (connessione già aperta)
        BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8")
        );

        StringBuilder resp = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            resp.append(line);
        }
        in.close();

        return resp.toString();
    }
}