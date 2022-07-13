import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Server {
  static ArrayList<ClientHandler> clients = new ArrayList<>();
  PrintWriter toClient;

  public static void main(String args[]) {
    // Accetta come argomenti il percorso della cartella della macchina host
    // in cui verranno custoditi i file e la porta su cui il server resta in
    // ascolto di richieste di connessione da parte dei client
    if (args.length < 1) {
      System.err.println("Digitare: java Server <path/> <port>");
      return;
    }
    String path = args[0];
    int port = Integer.parseInt(args[1]);
    Files input = new Files(path);

    try {
      ServerSocket listener = new ServerSocket(port);
      Scanner serverInput = new Scanner(System.in);

      String request; // Leggi la richiesta dell'utente...
      // operazioni possibili sul server
      System.out.println(
        "Digita: \n" +
        "[Ascolta], per metterti in ascolto \n" +
        "[quit], per chiudere la connessione \n" +
        "[info], per ricevere informazioni riguardanti i client."
      );
      do {
        request = serverInput.nextLine();

        // rimane in attesa di nuove connessioni
        if (request.equals("Ascolta")) {
          System.out.println("In ascolto...");
          Socket s = listener.accept();
          System.out.println("Client connesso!");

          // mantiene una lista di tutti i ClientHandler connessi
          ClientHandler c = new ClientHandler(s, input, path);
          clients.add(c);

          // delega la gestione della nuova connessione a un thread ClientHandler dedicato
          Thread clientHandlerThread = new Thread(
            new ClientHandler(s, input, path)
          );
          clientHandlerThread.start();
          // disconnette eventuali client ancora connessi e chiude il server
        } else if (request.equals("quit")) {
          toAll(request);
          break;
          // mostra a schermo alcune informazioni sul server
        } else if (request.equals("info")) {
          System.out.println("Numero lettori connessi: " + input.getReader());
          System.out.println("Numero scrittori connessi: " + input.getWriter());
          System.out.println(
            "Numero di file gestiti: " + input.getNumberFiles()
          );
        }
      } while (!request.equals("quit"));

      // chiude la connessione
      System.out.println("Connessione chiusa!");
      listener.close();
      serverInput.close();
      System.exit(0);
    } catch (IOException e) {
      System.err.println("Errore durante un'operazione di I/O");
    }
  }

  // permette di inviare messaggi dal server a tutti i client connessi
  public static void toAll(String request) {
    for (ClientHandler c : clients) {
      c.toClient.println(request);
    }
  }
}
