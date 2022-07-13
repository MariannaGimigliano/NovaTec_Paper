import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

public class Files {
  private int readerCount; // numero lettori
  private int writerCount; // numero scrittori
  private int waitCount; // numero client in attesa

  private Hashtable<String, Integer> readTable = new Hashtable<>(); // hashtable file - num lettori
  private Hashtable<String, Integer> writeTable = new Hashtable<>(); // hashtable file - num scrittori
  private Hashtable<String, Integer> waitingTable = new Hashtable<>(); // hashtable file - num client in attesa

  private Hashtable<String, Boolean> dbReadingTable = new Hashtable<>(); // hashtable file - booleano di lettura
  private Hashtable<String, Boolean> dbWritingTable = new Hashtable<>(); // hashtable file - booleano di scrittura
  private Hashtable<String, Boolean> dbWaitingTable = new Hashtable<>(); // hashtable file - booleano di client in attesa

  private File folder;
  private Vector<File> list;
  private String path;

  public Files(String path) {
    this.path = path;
    this.folder = new File(path);
    this.list = new Vector<File>(Arrays.asList(folder.listFiles()));
    readerCount = 0;
    writerCount = 0;
    inizialize();
  }

  // riempie le table con tutti i file presenti sul Server,
  // imposta a 0 il conteggio di lettori e scrittori,
  // imposta a false i booleani di lettura e scrittura
  public void inizialize() {
    for (int i = 0; i < list.size(); i++) {
      readTable.put(list.get(i).getName(), 0);
      writeTable.put(list.get(i).getName(), 0);
      waitingTable.put(list.get(i).getName(), 0);

      dbReadingTable.put(list.get(i).getName(), false);
      dbWritingTable.put(list.get(i).getName(), false);
      dbWaitingTable.put(list.get(i).getName(), false);
    }
  }

  // inserisce nelle table un nuovo file a seguito di un comando "create"
  public void insert(String fileName) {
    readTable.put(fileName, 0);
    writeTable.put(fileName, 0);
    waitingTable.put(fileName, 0);

    dbReadingTable.put(fileName, false);
    dbWritingTable.put(fileName, false);
    dbWaitingTable.put(fileName, false);

    // aggiungo il file all'arrayList
    list.add(new File(path + fileName));
  }

  // rimuove dalle table il file con il vecchio nome e inserisce
  // il file con il nuovo nome a seguito di un comando "rename"
  public void update(String oldFileName, String newFileName) {
    readTable.remove(oldFileName);
    writeTable.remove(oldFileName);
    waitingTable.remove(oldFileName);

    dbReadingTable.remove(oldFileName);
    dbWritingTable.remove(oldFileName);
    dbWaitingTable.remove(oldFileName);

    readTable.put(newFileName, 0);
    writeTable.put(newFileName, 0);
    waitingTable.put(newFileName, 0);

    dbReadingTable.put(newFileName, false);
    dbWritingTable.put(newFileName, false);
    dbWaitingTable.put(newFileName, false);

    // aggiorno il file nell'arrayList
    int index = list.indexOf(new File(path + oldFileName));
    list.set(index, new File(path + newFileName));
  }

  // rimuove un file dalle table a seguito di un comando "delete"
  public void delete(String fileName) {
    readTable.remove(fileName);
    writeTable.remove(fileName);
    waitingTable.remove(fileName);

    dbReadingTable.remove(fileName);
    dbWritingTable.remove(fileName);
    dbWaitingTable.remove(fileName);

    // rimuovo il file dall'arrayList
    list.remove(new File(path + fileName));
  }

  // gestisce gli accessi in lettura
  public synchronized int startRead(String fileName)
    throws IOException, InterruptedException {
    while (dbWritingTable.get(fileName) == true) {
      wait(); // dbReading non gestito perchè più lettori sono ammessi
    }
    ++readerCount;

    int count = readTable.get(fileName);
    count++;
    readTable.replace(fileName, count); // aggiorno la table dei lettori

    if (count == 1) {
      dbReadingTable.replace(fileName, true); // aggiorno la table
    }

    System.out.println(
      "Numero di lettori nel file '" +
      fileName +
      "': " +
      readTable.get(fileName)
    );
    return readerCount;
  }

  // gestisce la fine di una sessione di lettura
  public synchronized int endRead(String fileName)
    throws IOException, InterruptedException {
    // notifica di aver terminato le operazioni sul db e readerCount decrementa
    --readerCount;

    int count = readTable.get(fileName);
    count--;
    readTable.replace(fileName, count); // aggiorno la table dei lettori

    if (count == 0) {
      dbReadingTable.replace(fileName, false); // aggiorno la table
      notifyAll();
    }

    System.out.println(
      "Numero di lettori nel file '" +
      fileName +
      "': " +
      readTable.get(fileName)
    );
    return readerCount;
  }

  // gestisce gli accessi in scrittura
  public synchronized int startEdit(String fileName)
    throws IOException, InterruptedException {
    // se un client sta editando non è possibile né leggere né scrivere
    while (
      dbWritingTable.get(fileName) == true ||
      dbReadingTable.get(fileName) == true
    ) {
      wait();
    }
    ++writerCount;

    int count = writeTable.get(fileName);
    count++;
    writeTable.replace(fileName, count); // aggiorno la table degli scrittori

    if (count == 1) {
      dbWritingTable.replace(fileName, true); // aggiorno la table
    }

    System.out.println(
      "Numero di scrittori nel file '" +
      fileName +
      "': " +
      writeTable.get(fileName)
    );
    return writerCount;
  }

  // gestisce la fine di una sessione di scrittura
  public synchronized int endEdit(String fileName)
    throws IOException, InterruptedException {
    // notifica di aver terminato le operazioni sul file e dbWriting può tornare a
    // false
    --writerCount;

    int count = writeTable.get(fileName);
    count--;
    writeTable.replace(fileName, count); // aggiorno la table degli scrittori

    dbWritingTable.replace(fileName, false); // aggiorno la table
    notifyAll();

    System.out.println(
      "Numero di scrittori nel file '" +
      fileName +
      "': " +
      writeTable.get(fileName)
    );
    return writerCount;
  }

  // gestisce gli accessi concorrenti per i comandi "rename" e "delete"
  public synchronized void startDeleteRename(String fileName)
    throws IOException, InterruptedException {
    // se sono presenti lettori o scrittori all'interno del file non posso eliminare
    // o rinominare il file
    while (
      dbWritingTable.get(fileName) == true ||
      dbReadingTable.get(fileName) == true ||
      dbWaitingTable.get(fileName) == true
    ) {
      wait();
    }
    waitCount++;

    int count = waitingTable.get(fileName);
    count++;
    waitingTable.replace(fileName, count); // aggiorno la table dei client in attesa

    if (count == 1) {
      dbWaitingTable.replace(fileName, true); // aggiorno la table
    }

    if (readTable.get(fileName) == 1) {
      dbReadingTable.replace(fileName, true); // aggiorno la table
    }

    if (writeTable.get(fileName) == 1) {
      dbWritingTable.replace(fileName, true); // aggiorno la table
    }
  }

  // gestisce il termine dei comandi "rename" e "delete"
  public synchronized void endDeleteRename(String fileName)
    throws IOException, InterruptedException {
    // notifica di aver terminato le operazioni sul file
    waitCount--;

    if (waitCount == 0) {
      dbWaitingTable.replace(fileName, false); // aggiorno la table
      notifyAll();
    }
  }

  public synchronized int getReader() {
    return readerCount;
  }

  public synchronized int getWriter() {
    return writerCount;
  }

  public synchronized int getNumberFiles() {
    return list.size();
  }

  public synchronized Map<String, Integer> getReadMap() {
    return readTable;
  }

  public synchronized Map<String, Integer> getWriteMap() {
    return writeTable;
  }
}
