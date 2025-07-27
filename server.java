import javax.crypto.Cipher; 
import javax.crypto.KeyGenerator; 
import javax.crypto.SecretKey; 
import javax.crypto.spec.SecretKeySpec; 
import java.security.NoSuchAlgorithmException; 
import java.io.*; 
import java.net.*; 
import java.util.concurrent.atomic.AtomicInteger; 
import java.util.concurrent.locks.ReentrantLock; 
import javax.print.*; 
import javax.print.attribute.*; 
import javax.print.attribute.standard.Copies; 
38 
 
 
public class Server { 
    private static final int PORT = 8087; 
    private static final int BUF_SIZE = 1024; 
    private static AtomicInteger nextJobId = new AtomicInteger(1); // Global 
job ID counter 
    private static AtomicInteger currentJobInQueue = new AtomicInteger(1); // 
Track the current job position in queue 
    private static final ReentrantLock lock = new ReentrantLock(); // Lock for 
thread safety 
    private static final String AES_KEY = "1234567890123456"; // 16-byte key 
for AES-128 
public static void main(String[] args) { 
        try (ServerSocket serverSocket = new ServerSocket(PORT)) { 
            System.out.println("Server listening for clients on port " + PORT + "..."); 
  while (true) { 
                Socket clientSocket = serverSocket.accept(); 
                int clientJobId = nextJobId.getAndIncrement(); // Unique job ID for 
each client 
39 
 
                System.out.println("Client connected: " + 
clientSocket.getInetAddress() + " (Job " + clientJobId + ")"); 
               // Handle each client in a new thread 
                new Thread(new ClientHandler(clientSocket, clientJobId)).start(); 
            } 
        } catch (IOException e) { 
            e.printStackTrace(); 
        } 
    } 
static class ClientHandler implements Runnable { 
        private Socket clientSocket; 
        private int clientJobId; 
 public ClientHandler(Socket socket, int jobId) { 
            this.clientSocket = socket; 
            this.clientJobId = jobId; 
        } 
     @Override 
        public void run() { 
40 
 
            try (BufferedReader in = new BufferedReader(new 
InputStreamReader(clientSocket.getInputStream())); 
                 OutputStream out = clientSocket.getOutputStream()) { 
 // Get the client's position in the queue and increment the queue position 
for the next client 
                int positionInQueue = currentJobInQueue.getAndIncrement(); 
                int waitTimeEstimate = (positionInQueue - 1) * 2; // Assume each job 
takes 2 seconds 
// Send queue message to the client 
                String queueMessage = "You are job " + positionInQueue + " in the 
print queue. Estimated wait time: " + waitTimeEstimate + " seconds.\n"; 
                out.write(queueMessage.getBytes()); 
                out.flush(); 
String startTime = getTimestamp(); 
                System.out.println("Job " + clientJobId + " started at " + startTime); 
  // Read encrypted data from client, decrypt it, and store in a buffer 
                char[] buffer = new char[BUF_SIZE]; 
                int bytesRead; 
41 
 
                StringBuilder decryptedData = new StringBuilder(); 
  while ((bytesRead = in.read(buffer)) != -1) { 
                    // Decrypt the received data using AES 
                    decryptedData.append(aesDecrypt(new String(buffer, 0, 
bytesRead))); 
                } 
System.out.println("Sending data for Job " + clientJobId + " to printer..."); 
                boolean printerAvailable = 
sendToPrinter(decryptedData.toString()); // Check printer availability 
 // Send job status based on printer availability 
                String jobStatusMessage; 
                if (printerAvailable) { 
                    System.out.println("Data for Job " + clientJobId + " sent to 
printer."); 
                    jobStatusMessage = "Print job " + clientJobId + " completed 
successfully.\n"; 
                } else { 
                    System.out.println("Job " + clientJobId + " failed: Printer not 
connected."); 
42 
 
                    jobStatusMessage = "Print job " + clientJobId + " failed: Printer not 
connected.\n" } 
                out.write(jobStatusMessage.getBytes()); 
                out.flush(); 
String endTime = getTimestamp(); 
                System.out.println("Job " + clientJobId + " ended at " + endTime); 
 lock.lock(); 
                try { 
                    currentJobInQueue.decrementAndGet(); // Decrement queue 
counter once job is completed 
                } finally { 
                    lock.unlock(); 
                } 
 } catch (IOException e) { 
                e.printStackTrace(); 
                 try { 
                clientSocket.close(); 
                } catch (IOException e) { 
43 
 
                    e.printStackTrace(); 
                } 
            } 
        } 
    } 
 // AES decryption method 
    private static String aesDecrypt(String encryptedText) { 
        try { 
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(), 
"AES"); 
            Cipher cipher = Cipher.getInstance("AES"); 
            cipher.init(Cipher.DECRYPT_MODE, keySpec); 
           byte[] decodedText = encryptedText.getBytes("UTF-8"); 
            byte[] decryptedText = cipher.doFinal(decodedText); 
 
            return new String(decryptedText, "UTF-8"); 
        } catch (Exception e) { 
            e.printStackTrace(); 
44 
 
            return null; 
        } 
    } 
 // Method to open print dialog and send the decrypted text to the printer 
    private static boolean sendToPrinter(String decryptedData) { 
        // Save the decrypted data to a temporary file 
        File tempFile = new File("decrypted_document.txt"); 
        try (FileWriter writer = new FileWriter(tempFile)) { 
            writer.write(decryptedData); 
        } catch (IOException e) { 
            e.printStackTrace(); 
            return false; // Failed to save decrypted data 
        } 
// Open print dialog and send file to printer 
        try (FileInputStream fis = new FileInputStream(tempFile)) { 
            DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE; 
            PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet(); 
45 
 
            attrs.add(new Copies(1));  // Specify number of copies 
 PrintService[] printServices = PrintServiceLookup.lookupPrintServices(flavor, 
attrs); 
            if (printServices.length == 0) { 
                System.out.println("No printers are available."); 
                return false; 
            } 
 PrintService selectedService = ServiceUI.printDialog(null, 200, 200, 
printServices, null, flavor, attrs); 
            if (selectedService == null) { 
                System.out.println("Print job cancelled by the user."); 
                return false; // User cancelled the print dialog 
            } 
          DocPrintJob job = selectedService.createPrintJob(); 
            Doc doc = new SimpleDoc(fis, flavor, null);   job.print(doc, attrs); 
    System.out.println("Print job successfully sent to: " + 
selectedService.getName()); 
 return true; 
46 
 
        } catch (IOException | PrintException e) { 
            e.printStackTrace(); 
            return false; // Print job failed due to an error 
        } finally { 
            // Optionally, delete the temporary file after printing 
            tempFile.delete(); 
        } 
    } 
    // Method to get current timestamp 
    private static String getTimestamp() { 
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd 
HH:mm:ss"); 
        return dtf.format(LocalDateTime.now()); 
    }}