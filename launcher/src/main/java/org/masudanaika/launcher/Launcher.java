package org.masudanaika.launcher;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

/**
 * Launcher
 *
 * @author masuda, Masuda Naika
 */
public class Launcher {

    private static final String SERVER_IP = "server.ip";
    private static final String JAR_NAME = "jar.name";
    private static final String ALGORITHM = "MD5";
    private static final String JAR_DIR = "client";
    private static final String FILES_PATH = "/dolphin/files/";
    private static final int CONNECTION_TIMEOUT = 5000;
    private static TrustManager[] tm ={ new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            @Override
            public void checkClientTrusted(X509Certificate[] chain,
                    String authType) throws CertificateException {
            }
            @Override
            public void checkServerTrusted(X509Certificate[] chain,
                    String authType) throws CertificateException {
            }
        } };
    private String serverAddr;
    private String jarName;

    public static void main(String[] args) {
        //ホスト名の検証ルール　何が来てもtrueを返す
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname,
                    SSLSession session) {
                return true;
            }
        });

        new Launcher().start();
    }

    private void start() {

        try {
            setServerAddr();
            load();
            launch();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "プログラムをロードできません", "Dolphin Launcher", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace(System.err);
            System.exit(1);
        }
        System.exit(0);
    }

    private void launch() throws IOException {

        Path path = Paths.get(JAR_DIR, jarName);
        ProcessBuilder pb = new ProcessBuilder();
        if (isWin()) {
            pb.command("javaw", "-jar", path.toString());
        } else {
            pb.command("java", "-jar", path.toString());
        }
        pb.start();
    }

    private boolean isWin() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    private void load() throws Exception {

        Path jarPath = Paths.get(JAR_DIR, jarName);

        String hash = Files.exists(jarPath)
                ? getFileHash(jarPath, ALGORITHM) : "";

        StringBuilder sb = new StringBuilder();
        sb.append("https://").append(serverAddr).append(":8443").append(FILES_PATH).append("index.html");
        URL url = new URL(sb.toString());

        HttpsURLConnection con = null;
        String remoteHash = null;
        List<String> pathsStr = new ArrayList<>();
        try {
            //参考：http://symfoware.blog68.fc2.com/blog-entry-1165.html
            SSLContext sslcontext = SSLContext.getInstance("SSL");
            sslcontext.init(null, tm, null);

            con = (HttpsURLConnection) url.openConnection();
            con.setConnectTimeout(CONNECTION_TIMEOUT);
            con.setRequestMethod("GET");
            con.setSSLSocketFactory(sslcontext.getSocketFactory());
            con.setInstanceFollowRedirects(false);
            con.connect();

            try (InputStream is = con.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    //タグ除去
                    line = line.trim().replaceAll("<.+?>", "");
                    if (line.startsWith("HASH:")) {
                        remoteHash = line.substring("HASH:".length());
                    } else if (line.startsWith("FILE:")) {
                        String fileName = line.substring("FILE:".length());
                        pathsStr.add(fileName);
                    }
                    sb.append(line);
                }
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        
        if (!hash.equals(remoteHash)) {
            eraseDir(Paths.get(JAR_DIR));
            loadFile(pathsStr);
        }
    }

    // clientフォルダ内を消去する
    private void eraseDir(Path root) {

        if (!Files.exists(root)) {
            return;
        }

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(root)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    eraseDir(path);
                }
                Files.delete(path);
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }

    // サーバーからファイルをダウンロードする
    private void loadFile(List<String> pathsStr) throws Exception {

        int len = pathsStr.size();
        ProgressMonitor pm = new ProgressMonitor((JFrame) null, "ダウンロード中です", "", 0, len);

        for (int i = 0; i < len; ++i) {
            if (pm.isCanceled()) {
                pm.close();
                throw new InterruptedException();
            }
            String pathStr = pathsStr.get(i);
            pm.setProgress(i);
            pm.setNote(pathStr);

            StringBuilder sb = new StringBuilder();
            sb.append("https://").append(serverAddr).append(":8443").append(FILES_PATH).append(pathStr);
            URL url = new URL(sb.toString());
            HttpsURLConnection con = null;
            try {
                SSLContext sslcontext = SSLContext.getInstance("SSL");
                sslcontext.init(null, tm, null);

                con = (HttpsURLConnection) url.openConnection();
                con.setConnectTimeout(CONNECTION_TIMEOUT);
                con.setRequestMethod("GET");
                con.setInstanceFollowRedirects(false);
                con.setSSLSocketFactory(sslcontext.getSocketFactory());
                con.connect();
                Path path = Paths.get(JAR_DIR, pathStr);
                Path parent = path.getParent();
                if (parent != null && !Files.exists(path)) {
                    Files.createDirectories(parent);
                }
                Files.copy(con.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        
        pm.close();
    }

    private void setServerAddr() throws Exception {
        Properties prop = new Properties();
        Path propPath = Paths.get("launcher.properties");
        URL config = propPath.toUri().toURL();
        prop.load(config.openStream());
        serverAddr = prop.getProperty(SERVER_IP);
        jarName = prop.getProperty(JAR_NAME);
    }

    // byte[]を２桁のHex文字列に変換
    private String bytesToHex(byte[] bytes) {

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(Integer.toHexString((b & 0xF0) >> 4));
            sb.append(Integer.toHexString(b & 0xF));

        }
        return sb.toString();
    }

    // ファイルのHashを計算する
    private String getFileHash(Path path, String algorithm) throws Exception {

        MessageDigest md = MessageDigest.getInstance(algorithm);
        try (FileInputStream fis = new FileInputStream(path.toFile());
                BufferedInputStream bis = new BufferedInputStream(fis);
                DigestInputStream dis = new DigestInputStream(bis, md)) {
            while (dis.read() != -1) {
            }
        }

        byte[] digest = md.digest();
        return bytesToHex(digest);
    }
}
