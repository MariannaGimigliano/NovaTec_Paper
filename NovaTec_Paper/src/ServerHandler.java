import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/* ServerHandler implementa il gestore della connessione con il server */
public class ServerHandler implements Runnable {
  private Socket s; // Gestisce interamente il proprio socket
  PrintWriter toServer;

  public ServerHandler(Socket s) throws IOException {
    this.s = s;
    this.toServer = new PrintWriter(s.getOutputStream(), true); // Canale di invio verso il client
  }

  @Override
  public void run() {
    try {
      Scanner fromServer = new Scanner(s.getInputStream()); // Canale di ricezione dal client

      // Ciclo di vita del ServerHandler
      while (true) {
        String request = fromServer.nextLine();
        String[] splitRequest = request.split(" ", 2);
        String requestType = splitRequest[0];

        System.out.println(request);

        if (requestType.equals("quit")) { // arresta il client
          break;
        } else if (requestType.equals(" ")) { // gestisce comandi sconosciuti
          System.out.println("Comando sconosciuto");
        }
      }

      // Chiudi la connessione e arresta il ServerHandler
      System.out.println("Server terminato!");
      s.close();
      fromServer.close();
      System.exit(0);
    } catch (IOException e) {
      System.err.println("Errore durante un'operazione di I/O");
    }
  }
}
