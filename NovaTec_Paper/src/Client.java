import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

  public static void main(String args[]) {
    // Prende in input l'indirizzo e la porta a cui connettersi
    if (args.length < 2) {
      System.err.println("Digitare: java Client <host> <port>");
      return;
    }
    String host = args[0];
    int port = Integer.parseInt(args[1]);

    try {
      // Connettiti a host:port
      Socket s = new Socket(host, port);
      System.out.println("Connesso!");

      PrintWriter output = new PrintWriter(s.getOutputStream(), true);
      Scanner scanner = new Scanner(System.in);

      // delega la gestione della nuova connessione a un thread ServerHandler dedicato
      Thread serverHandlerThread = new Thread(new ServerHandler(s));
      serverHandlerThread.start();

      String userInput;
      do {
        userInput = scanner.nextLine();
        output.println(userInput);

        // arresta il client
        if (userInput.equals("quit")) {
          break;
        }
      } while (!userInput.equals("quit"));

      // chiude la connessione
      System.out.println("Connessione chiusa!");
      s.close();
      output.close();
      scanner.close();
      System.exit(0);
      // Se il server non è raggiungibile all’indirizzo e alla porta specificati,
      // viene restituito un messaggio di errore
    } catch (IOException e) {
      System.err.println("Server non raggiungibile!");
    }
  }
}
