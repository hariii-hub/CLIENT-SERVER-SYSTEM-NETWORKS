import javax.crypto.Cipher; 
import javax.crypto.KeyGenerator; 
import javax.crypto.SecretKey; 
import javax.crypto.spec.SecretKeySpec; 
import java.net.*; 
import javax.swing.*; 
import java.awt.*; 
import java.awt.event.*; 
import java.io.*; 
import java.awt.print.*; 
import java.util.Base64; 
public class Client { 
    private static final int BUF_SIZE = 1024; 
    private static SecretKey secretKey; 
   public static void main(String[] args) { 
32 
 
        try { 
            // Generate AES Key 
            secretKey = generateAESKey(); 
        } catch (Exception e) { 
            e.printStackTrace(); 
            return; 
        } 
       JFrame frame = new JFrame("Print Client"); 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        frame.setSize(400, 300); 
       JButton chooseFileButton = new JButton("Choose File to Print"); 
        JButton printButton = new JButton("Print Decrypted Data"); 
        JTextArea encryptedTextArea = new JTextArea(10, 30); 
        encryptedTextArea.setEditable(false); 
        JScrollPane scrollPane = new JScrollPane(encryptedTextArea); 
        chooseFileButton.addActionListener(new ActionListener() { 
            @Override 
33 
 
            public void actionPerformed(ActionEvent e) { 
                JFileChooser fileChooser = new JFileChooser(); 
                int returnValue = fileChooser.showOpenDialog(null); 
                if (returnValue == JFileChooser.APPROVE_OPTION) { 
                    File selectedFile = fileChooser.getSelectedFile(); 
    
                 sendFileToServer(selectedFile, encryptedTextArea); 
                } 
            } 
        }); 
       printButton.addActionListener(new ActionListener() { 
            @Override 
            public void actionPerformed(ActionEvent e) { 
                printDecryptedText(); 
            } 
        }); 
       frame.getContentPane().setLayout(new FlowLayout()); 
34 
 
        frame.getContentPane().add(chooseFileButton); 
        frame.getContentPane().add(printButton); 
        frame.getContentPane().add(scrollPane); 
        frame.setVisible(true); 
    } private static void sendFileToServer(File file, JTextArea 
encryptedTextArea) { 
        try (Socket socket = new Socket("127.0.0.1", 8087); 
             OutputStream out = socket.getOutputStream(); 
             BufferedInputStream fileIn = new BufferedInputStream(new 
FileInputStream(file)); 
             InputStream in = socket.getInputStream()) { 
        System.out.println("Connected to server"); 
 // Read file and encrypt 
            byte[] buffer = new byte[BUF_SIZE]; 
            int bytesRead; 
            StringBuilder encryptedData = new StringBuilder(); 
while ((bytesRead = fileIn.read(buffer)) != -1) { 
                byte[] encrypted = aesEncrypt(buffer, bytesRead); 
35 
 
          
encryptedData.append(Base64.getEncoder().encodeToString(encrypted)); 
            } 
// Display encrypted data in JTextArea 
            encryptedTextArea.setText(encryptedData.toString( 
// Send encrypted data to the server 
            out.write(encryptedData.toString().getBytes()); 
            out.flush(); 
System.out.println("Encrypted file sent to server."); 
 // Receive response from the server 
            byte[] responseBuffer = new byte[BUF_SIZE]; 
            int responseBytes = in.read(responseBuffer); 
            String responseMessage = new String(responseBuffer, 0, 
responseBytes); 
            System.out.println("Server response: " + responseMessage); 
 
        } catch (IOException e) { 
            e.printStackTrace(); 
36 
 
        } 
    } 
 private static byte[] aesEncrypt(byte[] data, int size) { 
        try { 
            Cipher cipher = Cipher.getInstance("AES"); 
            cipher.init(Cipher.ENCRYPT_MODE, secretKey); 
            return cipher.doFinal(data, 0, size); 
        } catch (Exception e) { 
            e.printStackTrace(); 
        } 
        return new byte[0]; 
    }] 
 private static void printDecryptedText() { 
        // For now, you can print decrypted text. For actual decryption logic, add 
server-side decryption 
        System.out.println("Decrypted text to print."); 
    } 
private static SecretKey generateAESKey() throws Exception { 
37 
 
        KeyGenerator keyGen = KeyGenerator.getInstance("AES"); 
        keyGen.init(128); // Use 128 bit key for AES 
        return keyGen.generateKey(); 
    } 