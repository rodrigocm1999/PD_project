package pt.Common;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static String hashStringBase36(String str) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(str.getBytes());
        return new BigInteger(1, hash).toString(36);
    }

    public static boolean checkUserPasswordFollowsRules(String password) {
        Pattern pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$#!?/|&\\\\%()]).{8,25}$");
        Matcher matcher = pattern.matcher(password);
        return matcher.find();
    }

    public static boolean checkChannelFollowsRules(String string) {
        return !(string.length() < 3 || string.length() > 25);
    }

    public static boolean checkNameUser(String name) {
        return name.length() >= 3 && name.length() <= 50;
    }

    public static boolean checkUsername(String username) {
        Pattern pattern = Pattern.compile("^[a-zA-Z\\d_]{3,25}$");
        Matcher matcher = pattern.matcher(username);
        return matcher.find();
    }

    public static BufferedImage getCompressedImage(BufferedImage img, int newW, int newH) {
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage bufferedImage = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return bufferedImage;
    }

    public static byte[] getImageBytes(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam jpgWriteParam = writer.getDefaultWriteParam();
        jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpgWriteParam.setCompressionQuality(0.90f);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(baos));
        IIOImage outputImage = new IIOImage(image, null, null);
        writer.write(null, outputImage, jpgWriteParam);
        return baos.toByteArray();
    }

    public static void printList(ArrayList list) {
        printList(list, "");
    }

    public static void printList(ArrayList list, String prefix) {
        System.out.println(prefix + "----------------------------");
        for (Object asd : list) System.out.println("\t" + asd);
        System.out.println("----------------------------");
    }

    public static InetAddress getPublicIp() {
        HttpURLConnection connection = null;
        BufferedReader in = null;
        try {
            URL url = new URL(Constants.PUBLIC_IP_ADDRESS_API);
            connection = (HttpURLConnection) url.openConnection();
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String publicIp = in.readLine();
                in.close();
                return InetAddress.getByName(publicIp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) connection.disconnect();
        }
        return null;
    }

    public static void createDirectories(File file) {
        createDirectories(file, true);
    }

    private static void createDirectories(File directory, boolean bottom) {
        if (directory.exists()) return;
        File parent = directory.getParentFile();
        if (parent != null)
            createDirectories(parent, false);
        if (!bottom)
            directory.mkdir();
    }
}
