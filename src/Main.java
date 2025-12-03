import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {

        // Legge il token dal file token.txt
        String token;
        try {
            token = new String(
                    Files.readAllBytes(Paths.get("token.txt")),
                    "UTF-8"
            ).trim();
        } catch (Exception e) {
            System.err.println("Errore: impossibile leggere token.txt. Crea il file e inserisci il token.");
            return;
        }


        BotServer server = new BotServer(token);

        System.out.println("Avvio polling...");
        while (true) {
            server.processOneBatch();
            Thread.sleep(2000);
        }
    }
}
