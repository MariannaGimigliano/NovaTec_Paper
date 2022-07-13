import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Vector;

/* ClientHandler implementa il gestore della connessione con un client */
public class ClientHandler implements Runnable {
  private Socket s;
  private Files input;
  private String path;
  PrintWriter toClient;

  public ClientHandler(Socket s, Files input, String p) throws IOException {
    this.s = s;
    this.input = input;
    this.path = p;
    this.toClient = new PrintWriter(s.getOutputStream(), true);
  }

  @Override
  public void run() {
    try {
      BufferedReader fromClient = new BufferedReader(
        new InputStreamReader(s.getInputStream())
      );

      // Ciclo di vita del ClientHandler
      while (true) {
        String request = fromClient.readLine();
        String[] splitRequest = request.split(" ", 2);
        String requestType = splitRequest[0];
        String requestArg = splitRequest.length > 1 ? splitRequest[1] : "";

        System.out.println("Comando ricevuto: " + request);

        // read prende come argomento il nome di un file da leggere
        if (requestType.equals("read")) {
          File file = new File(path + requestArg);

          if (file.exists()) {
            try {
              input.startRead(requestArg);
              toClient.println("Sei in modalità lettura!");
              try {
                // legge e stampa il contenuto del file
                BufferedReader br = new BufferedReader(
                  new FileReader(path + requestArg)
                );
                String line = br.readLine();
                while (line != null) {
                  toClient.println(line);
                  line = br.readLine();
                }
                br.close();
              } catch (Exception e) {
                toClient.println(e.getMessage());
                // :close chiude il file e termina la sessione di lettura
              } finally {
                if (fromClient.readLine().equals(":close")) {
                  input.endRead(requestArg);
                  toClient.println("Non sei più in modalità lettura!");
                } else {
                  toClient.println("error");
                }
              }
            } catch (Exception e) {
              toClient.println(
                "Il file non può essere letto perchè non esiste!"
              );
            }
          } else {
            toClient.println("Il file non può essere letto perchè non esiste!");
          }
          // create prende come argomento il nome di un nuovo file da creare
        } else if (requestType.equals("create")) {
          try {
            File files = new File(path + requestArg);

            if (files.exists()) {
              toClient.println(
                "File: " + " '" + requestArg + "'" + " già esistente!"
              );
            } else if (files.createNewFile()) { // crea il nuovo file
              input.insert(requestArg);
              toClient.println(
                "File: " + " '" + requestArg + " ' " + " creato!"
              );
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
          // rename prende come argomenti il nome di un file esistente e il nuovo nome da
          // assegnare a quel file
        } else if (requestType.equals("rename")) {
          try {
            String nameFiles = requestArg;
            String[] split = nameFiles.split(" ");
            File oldFile = new File(path + split[0]);
            File newFile = new File(path + split[1]);

            if (oldFile.exists() && !newFile.exists()) {
              try {
                // rinomina il file
                input.startDeleteRename(oldFile.getName());
                input.update(oldFile.getName(), newFile.getName());
                if (oldFile.renameTo(newFile)) {
                  input.endDeleteRename(newFile.getName());
                  toClient.println("File rinominato!");
                }
              } catch (Exception e) {
                toClient.println(
                  "Il file non può essere rinominato perchè non esiste!"
                );
              }
            } else {
              toClient.println("Il nome del file è già presente!");
            }
          } catch (Exception e) {
            toClient.println("Input non corretto!");
          }
          // list restituisce una lista di tutti i file presenti sul server
        } else if (requestType.equals("list")) {
          try {
            File folder = new File(path);
            File[] list = folder.listFiles();
            // formatto l'output usando SimpleDateFormat per renderlo leggibile
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            toClient.println("Ecco i file:");

            // stampa le informazioni su ogni file
            for (int i = 0; i < list.length; i++) {
              if (list[i].getName().endsWith(".txt")) toClient.println(
                "File: " +
                list[i].getName() +
                "\n" +
                "Ultima modifica: " +
                sdf.format(list[i].lastModified()) +
                "\n" +
                "Numero di lettori: " +
                input.getReadMap().get(list[i].getName()) +
                "\n" +
                "Numero di scrittori: " +
                input.getWriteMap().get(list[i].getName()) +
                "\n"
              );
            }
          } catch (Exception e) {
            toClient.println(e.getMessage());
          }
          // edit prende come argomento il nome di un file da modificare
        } else if (requestType.equals("edit")) {
          File file = new File(path + requestArg);

          if (file.exists()) {
            try {
              input.startEdit(requestArg);
              // input.checkIfExists(requestArg);
              toClient.println("Sei in modalità scrittura");
              try {
                while (requestType != null) {
                  BufferedWriter bufwriter = new BufferedWriter(
                    new FileWriter(path + requestArg, true)
                  );
                  String command = fromClient.readLine();

                  // :backspace elimina l’ultima riga del file
                  if (command.equals(":backspace")) {
                    BufferedReader br = new BufferedReader(
                      new InputStreamReader(
                        new FileInputStream(path + requestArg)
                      )
                    );
                    Vector<String> v = new Vector<String>(); // contiene tutte le righe del file
                    String linea = ""; // Leggo tutto il file e lo memorizzo nel Vector
                    while ((linea = br.readLine()) != null) {
                      v.add(linea);
                    }
                    br.close();
                    if (v.size() == 0) { // Ora riscrivo tutto, tranne l'ultima riga
                      toClient.println("Il file è vuoto!");
                    } else {
                      PrintStream ps = new PrintStream(
                        new FileOutputStream(path + requestArg)
                      );
                      for (int i = 0; i < v.size() - 1; i++) { // Il -1 indica di tralasciare l'ultima
                        ps.println((String) v.elementAt(i));
                      }
                      toClient.println("Hai eliminato l’ultima riga del file!");
                      ps.close();
                    }
                    // :close chiude il file e termina la sessione di scrittura
                  } else if (command.equals(":close")) {
                    input.endEdit(requestArg);
                    toClient.println("Non sei più in modalità scrittura!");
                    break;
                  } else { // la nuova riga di testo viene aggiunta in coda al file
                    bufwriter.write(command); // aggiungo all'interno del file la nuova riga
                    bufwriter.newLine();
                    bufwriter.close();
                    toClient.println("Riga aggiunta al file! ");
                  }
                }
              } catch (Exception e) {
                toClient.println(e.getMessage());
              }
            } catch (Exception e) {
              toClient.println(
                "Il file non può essere modificato perchè non esiste!"
              );
            }
          } else {
            toClient.println(
              "Il file non può essere modificato perchè non esiste!"
            );
          }
          // delete prende come argomento il nome di un file da eliminare
        } else if (requestType.equals("delete")) {
          File file = new File(path + requestArg);
          if (file.exists()) {
            try {
              // elimina il file
              input.startDeleteRename(requestArg);
              input.delete(requestArg);
              if (file.delete()) {
                input.endDeleteRename(requestArg);
                toClient.println("File eliminato!");
              }
            } catch (Exception e) {
              toClient.println(
                "Il file non può essere eliminato perchè non esiste!"
              );
            }
          } else {
            toClient.println(
              "Il file non può essere eliminato perchè non esiste!"
            );
          }
          // arresta il client
        } else if (requestType.equals("quit")) {
          break;
          // gestisce comandi sconosciuti
        } else {
          toClient.println("Comando sconosciuto!");
        }
      }

      // Chiudi la connessione e arresta il ClientHandler
      s.close();
      fromClient.close();
      toClient.close();
      System.out.println("Client disconnesso!");
    } catch (IOException e) {
      System.err.println("Errore durante un'operazione di I/O");
    }
  }
}
