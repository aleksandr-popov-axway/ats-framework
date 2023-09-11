/*
 * Copyright 2017 Axway Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.axway.ats.action.ftp;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.filetransfer.FileTransferException;
import com.axway.ats.common.filetransfer.TransferMode;
import com.axway.ats.core.CoreLibraryConfigurator;
import com.axway.ats.core.filetransfer.AbstractFileTransferClient;
import com.axway.ats.core.filetransfer.model.TransferListener;
import com.axway.ats.core.filetransfer.model.ftp.FtpListener;
import com.axway.ats.core.filetransfer.model.ftp.FtpResponseListener;
import com.axway.ats.core.filetransfer.model.ftp.SynchronizationFtpTransferListener;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The {@link FileTransferFtpClient} uses the Apache Commons Net component suite for Java
 * ( https://commons.apache.org/proper/commons-net/ ) to initiate and execute FTP
 * connections to a remote server.
 */
@PublicAtsApi
public class FileTransferFtpClient extends AbstractFileTransferClient implements IFtpClient {
    private org.apache.commons.net.ftp.FTPClient client = null;
    private static final Logger log           = Logger.getLogger(FileTransferFtpClient.class);

    /**
     * Constructor
     *
     */
    @PublicAtsApi
    public FileTransferFtpClient() {

        super();
    }

    /**
     * Set the {@link TransferMode} that this protocol should use
     *
     * @param mode
     * @throws FtpException
     */
    @PublicAtsApi
    @Override
    public void setTransferMode(
            TransferMode mode ) throws FtpException {

        if (this.client != null && this.client.isConnected() && this.transferMode != mode) {
            try {
                log.info("Set file transfer mode to " + mode);
                if (mode == TransferMode.ASCII) {
                    if (!this.client.setFileType(org.apache.commons.net.ftp.FTPClient.ASCII_FILE_TYPE)) {
                        throw new Exception("Unable to set transfer mode to ASCII");
                    }
                } else {
                    if (!this.client.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)) {
                        throw new Exception("Unable to set transfer mode to BINARY");
                    }
                }
            } catch (Exception e) {
                throw new FtpException("Error setting file transfer mode to " + mode, e);
            }
        }

        super.setTransferMode(mode);
    }

    /**
     * Connect to a remote host using basic authentication
     *
     * @param hostname
     *            the host to connect to
     * @param userName
     *            the user name
     * @param password
     *            the password for the provided user name
     * @throws FileTransferException
     */
    @PublicAtsApi
    @Override
    public void connect(
            String hostname,
            String userName,
            String password ) throws FileTransferException {

        log.info("Connecting to " + hostname + " on port " + this.port + " using username " + userName
                + " and password " + password);
        // make new FTP object for every new connection
        disconnect();
        this.client = new org.apache.commons.net.ftp.FTPClient();

        /*
         * When uploading/downloading file with encoding that the server does not, the server will return 452.
         * So we have to either set the control encoding to UTF-8 (or the one that is desired), or if the needed encoding is UTF-8,
         * set the UTF-8 autodetect to true
         */
        String controlEncoding = CoreLibraryConfigurator.getInstance().getFtpControlEncoding();
        boolean autodetectUTF8 = CoreLibraryConfigurator.getInstance().getFtpAutodetectUTF8();

        if (!StringUtils.isNullOrEmpty(controlEncoding)) {
            this.client.setControlEncoding(controlEncoding);
            if (log.isDebugEnabled()) {
                log.debug("Control encoding is set to " + controlEncoding);
            }

        }

        this.client.setAutodetectUTF8(autodetectUTF8);
        if (log.isDebugEnabled()) {
            log.debug("Autodetect for UTF-8 is " + ( (autodetectUTF8)
                    ? "enabled"
                    : "disabled"));
        }

        if (!"UTF-8".equalsIgnoreCase(controlEncoding) && autodetectUTF8) {
            log.warn("Autodetecting UTF-8 is enabled, but additionaly, the control encoding is set to '"
                    + controlEncoding + "'. UTF-8 will be used.");
        }

        if (this.listener != null) {
            this.listener.setResponses(new ArrayList<>());
            this.client.addProtocolCommandListener( ((FtpResponseListener) listener));
        }
        /* if debug mode is true, we log messages from all levels */
        if (isDebugMode()) {
            this.client.addProtocolCommandListener(new FtpListener());
        }
        try {
            this.client.setConnectTimeout(this.timeout);
            // connect to the host
            this.client.connect(hostname, this.port);
            // login to the host
            if (!this.client.login(userName, password)) {
                throw new Exception("Invallid username and/or password");
            }
            // enter passive mode
            this.client.enterLocalPassiveMode();
            // set transfer mode
            if (this.transferMode == TransferMode.ASCII) {
                if (!this.client.setFileType(org.apache.commons.net.ftp.FTPClient.ASCII_FILE_TYPE)) {
                    throw new Exception("Unable to set transfer mode to ASCII");
                }
            } else {
                if (!this.client.setFileType(org.apache.commons.net.ftp.FTPClient.BINARY_FILE_TYPE)) {
                    throw new Exception("Unable to set transfer mode to BINARY");
                }
            }

        } catch (Exception e) {
            String errMessage = "Unable to connect to  " + hostname + " on port " + this.port
                    + " using username " + userName + " and password " + password;
            log.error(errMessage, e);
            throw new FileTransferException(e);
        }

        log.info("Successfully connected to " + hostname + " on port " + this.port + " using username "
                + userName + " and password " + password);

    }

    @Override
    public void connect(
            String hostname,
            String keystoreFile,
            String keystorePassword,
            String publicKeyAlias ) throws FileTransferException {

        throw new FileTransferException("Not Supported");

    }

    /**
     * Disconnect from the remote host
     *
     * @throws FileTransferException
     */
    @PublicAtsApi
    @Override
    public void disconnect() throws FileTransferException {

        if (this.client != null && this.client.isConnected()) {
            try {
                this.client.disconnect();
                this.client = null;
            } catch (IOException e) {
                throw new FileTransferException(e);
            }
        }

    }

    @PublicAtsApi
    @Override
    protected void performDownloadFile(
            String localFile,
            String remoteDir,
            String remoteFile ) throws FileTransferException {

        FileOutputStream fos = null;
        try {
            String remoteFileAbsPath = null;
            remoteDir = remoteDir.replace("\\", "/");
            remoteFile = remoteFile.replace("\\", "/");

            if (remoteDir.endsWith("/") && remoteFile.endsWith("/")) {
                remoteFileAbsPath = remoteDir.substring(0, remoteDir.length() - 2) + remoteFile;
            } else if (!remoteDir.endsWith("/") && !remoteFile.endsWith("/")) {
                remoteFileAbsPath = remoteDir + "/" + remoteFile;
            } else {
                remoteFileAbsPath = remoteDir + remoteFile;
            }

            // download the file
            fos = new FileOutputStream(new File(localFile));
            if (!this.client.retrieveFile(remoteFileAbsPath, fos)) {
                throw new FtpException("Unable to retrieve "
                        + (remoteDir.endsWith("/")
                        ? remoteDir
                        : remoteDir + "/")
                        + remoteFile + " from "
                        + this.client.getPassiveHost() + " as a " + localFile);
            }
        } catch (Exception e) {
            log.error("Unable to download file " + localFile, e);
            throw new FileTransferException(e);
        } finally {
            // close the file output stream
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    log.error("Unable to close the file stream after successful download!", e);
                }
            }
        }

        if (remoteDir != null && !remoteDir.endsWith("/")) {
            remoteDir += "/";
        }
        log.info("Successfully downloaded '" + localFile + "' from '" + remoteDir + remoteFile + "', host "
                + client.getPassiveHost());
    }

    @PublicAtsApi
    @Override
    protected void performUploadFile(
            String localFile,
            String remoteDir,
            String remoteFile ) throws FileTransferException {

        FileInputStream fis = null;

        try {
            String remoteFileAbsPath = null;
            remoteDir = remoteDir.replace("\\", "/");
            remoteFile = remoteFile.replace("\\", "/");

            if (remoteDir.endsWith("/") && remoteFile.endsWith("/")) {
                remoteFileAbsPath = remoteDir.substring(0, remoteDir.length() - 2) + remoteFile;
            } else if (!remoteDir.endsWith("/") && !remoteFile.endsWith("/")) {
                remoteFileAbsPath = remoteDir + "/" + remoteFile;
            } else {
                remoteFileAbsPath = remoteDir + remoteFile;
            }

            // upload the file
            fis = new FileInputStream(new File(localFile));
            if (!this.client.storeFile(remoteFileAbsPath, fis)) {
                throw new FtpException("Unable to store " + localFile + " to "
                        + this.client.getPassiveHost() + " as a "
                        + (remoteDir.endsWith("/")
                        ? remoteDir
                        : remoteDir + "/")
                        + remoteFile);
            }
        } catch (Exception e) {
            log.error("Unable to upload file!", e);
            throw new FileTransferException(e);
        } finally {
            // close the file input stream
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    log.error("Unable to close the file stream after successful upload!", e);
                }
            }
        }

        if (remoteDir != null && !remoteDir.endsWith("/")) {
            remoteDir += "/";
        }
        log.info("Successfully uploaded '" + localFile + "' to '" + remoteDir + remoteFile + "', host "
                + client.getPassiveHost());
    }

    @Override
    public String[] getAllReplyLines() {

        return this.client.getReplyStrings();

    }

    @Override
    public void logAllReplyLines() {

        log.info("REPLY: " + getAllReplyLinesAsString());

    }

    @Override
    public String getAllReplyLinesAsString() {

        StringBuilder sb = new StringBuilder();

        for (String line : getAllReplyLines()) {
            sb.append(line);
        }

        return sb.toString();

    }

    @PublicAtsApi
    @Override
    public int pasv() {

        if (this.passivePort != -1) {
            log.warn("Already in passive mode");
            return this.passivePort;
        }
        try {
            int reply = this.client.pasv();
            if (reply >= 400) {
                throw new RuntimeException(constructExecutionErrorMessage("PASV", null));
            }
            this.passivePort = extractPassivePort(getAllReplyLinesAsString());
            return this.passivePort;
        } catch (Exception e) {
            throw new RuntimeException(constructExceptionMessage("PASV", null), e);
        }

    }

    /**
     * Currently not supporting commands requiring opening of data connection
     * @param command the command to run
     * @return String representing the return code
     * @throws FtpException
     */
    @PublicAtsApi
    @Override
    public String executeCommand(String command) throws FtpException {
        return this.executeCommand(command, (InputStream) null);

    }

    @Override
    public Object executeCommand(String command, Object[] arguments) throws FtpException {

        throw new FtpException("Not implemented. Use " + this.getClass().getName()
                + ".executeCommand(" + String.class.getName() + ", " + InputStream.class
                + ") instead");
    }

    @PublicAtsApi
    @Override
    public String executeCommand(String command, InputStream localData) throws FtpException {

        String result=null;
        try {
            if(this.passivePort==-1){
                this.pasv();
            }
            int replyCode=this.client.sendCommand(command);
            if (replyCode == 150) { // data connection opened
                Socket dataSocket = null;
                InputStream dataInputStream = null;
                OutputStream dataOutputStream = null;
                try {
                    dataSocket = new Socket(this.client.getRemoteAddress().getHostAddress(), this.passivePort);
                    if (localData != null) {
                        dataOutputStream = dataSocket.getOutputStream();
                        IoUtils.copyStream(localData, dataOutputStream);
                        dataOutputStream.flush();
                    } else {
                        dataInputStream = dataSocket.getInputStream();
                        StringBuilder sb = new StringBuilder();
                        int i;
                        while ( (i = dataInputStream.read()) != -1) {
                            sb.append((char) i);
                        }
                        result = sb.toString();
                    }
                } finally {
                    if (dataSocket != null) {
                        dataSocket.close();
                    }
                    this.passivePort = -1;
                    replyCode = this.client.getReply();
                }
            } else if (replyCode >= 300 && replyCode < 400) { // command sequence started, server waiting for further FTP commands
                return getAllReplyLinesAsString();
            } else if (replyCode >= 400) {
                throw new FtpException(constructExecutionErrorMessage(command, null));
            } else if (replyCode >= 200 && replyCode < 300) {
                this.passivePort = -1;
                return getAllReplyLinesAsString();
            }
        }catch (Exception e){
            this.passivePort=-1;
            throw new FtpException(constructExceptionMessage(command, null),
                    e);
        }

        return result;
    }

    @PublicAtsApi
    @Override
    public String help() {

        try {
            this.client.help();
        } catch (IOException e) {
            throw new FtpException(constructExceptionMessage("HELP", null), e);
        }

        return getAllReplyLinesAsString();

    }

    @PublicAtsApi
    @Override
    public String pwd() {

        try {
            this.client.pwd();
        } catch (IOException e) {
            throw new FtpException(constructExceptionMessage("PWD", null), e);
        }
        String result = getAllReplyLinesAsString();
        String[] tokens = result.split(" ");
        return tokens[1].replace("\"","");

    }

    @PublicAtsApi
    @Override
    public void cwd( String directory ) {

        try {
            this.client.cwd(directory);
        } catch (IOException e) {
            throw new FtpException(constructExceptionMessage("CWD", null), e);
        }

    }

    @PublicAtsApi
    @Override
    public String cdup() {

        try {
            this.client.cdup();
        } catch (IOException e) {
            throw new FtpException(constructExceptionMessage("CDUP", null), e);
        }
        String result = getAllReplyLinesAsString();
        String[] tokens = result.split(" ");
        return tokens[1];

    }

    @PublicAtsApi
    @Override
    public void mkd( String directory ) {

        try {
            this.client.mkd(directory);
        } catch (IOException e) {
            throw new FtpException(constructExceptionMessage("MKD", null), e);
        }

    }

    @PublicAtsApi
    @Override
    public void rmd( String pathName ) {

        try {
            this.client.rmd(pathName);
        } catch (IOException e) {
            throw new FtpException(constructExceptionMessage("RMD", null), e);
        }

    }

    @PublicAtsApi
    @Override
    public long size(String file) {

        try {
            return Long.parseLong(this.client.getSize(file));
        } catch (IOException e) {
            throw new FtpException(constructExceptionMessage("SIZE", null), e);
        }

    }

    @PublicAtsApi
    @Override
    public List<String> list( String directory ) {

        List<String> fileNames = new ArrayList<>();
        String result = executeCommand("LIST " + directory);
        if (StringUtils.isNullOrEmpty(result)) {
            return fileNames;
        }
        String[] tokens = result.split("\n");
        for (String token : tokens) {
            fileNames.add(token.substring(0, token.length() - 1));
        }
        return fileNames;
    }

    @PublicAtsApi
    @Override
    public List<String>listFileNames(String directory){
        try {
            return Arrays.stream(this.client.listNames(directory)).collect(Collectors.toList());
        } catch (IOException e) {
            throw new FtpException(constructExceptionMessage("LIST", null), e);
        }
    }

    @PublicAtsApi
    @Override
    public String mlst(String path) {

        try {
            if (StringUtils.isNullOrEmpty(path)) {
                this.client.mlst();
            } else {
                this.client.mlst(path);
            }
        } catch (Exception e) {
            throw new FtpException(constructExceptionMessage("MLST", null), e);
        }
        return getAllReplyLines()[1];
    }

    @PublicAtsApi
    @Override
    public int getLastReplyCode(){
        return this.client.getReplyCode();
    }

    @PublicAtsApi
    @Override
    public List<String> mlsd(String directory) {

        List<String> fileNames = new ArrayList<>();
        String result = executeCommand("MLSD " + directory);
        if (StringUtils.isNullOrEmpty(result)) {
            return fileNames;
        }
        String[] tokens = result.split("\n");
        for (String token : tokens) {
            fileNames.add(token.substring(0, token.length() - 1));
        }
        return fileNames;

    }

    @PublicAtsApi
    @Override
    public List<String> nlst( String directory ) {

        List<String> fileNames = new ArrayList<>();
        String result = executeCommand("NLST " + directory);
        if (StringUtils.isNullOrEmpty(result)) {
            return fileNames;
        }
        String[] tokens = result.split("\n");
        for (String token : tokens) {
            fileNames.add(token.substring(0, token.length() - 1));
        }
        return fileNames;

    }

    @PublicAtsApi
    @Override
    public void appe( String file, String content ) {

        ByteArrayInputStream bais = null;
        try {
            bais = new ByteArrayInputStream(content.getBytes());
            executeCommand("APPE " + file, bais);
        } catch (Exception e) {
            throw new RuntimeException(constructExceptionMessage("APPE ", new String[]{ file }), e);
        } finally {
            IoUtils.closeStream(bais);
        }

    }

    @PublicAtsApi
    @Override
    public void dele( String file ) {

        try {
            this.client.deleteFile(file);
        } catch (IOException e) {
            throw new FtpException(constructExceptionMessage("DELE", null), e);
        }

    }

    @PublicAtsApi
    @Override
    public void rename(String from, String to) {

        try {
            this.client.rename(from, to);
        } catch (IOException e) {
            throw new FtpException(constructExceptionMessage("RENAME", null), e);
        }

    }

    @PublicAtsApi
    @Override
    protected TransferListener addListener(
            int progressEventNumber ) {

        SynchronizationFtpTransferListener listener = new SynchronizationFtpTransferListener(this,
                progressEventNumber);
        this.client.addProtocolCommandListener(listener);

        return listener;
    }

    @PublicAtsApi
    @Override
    protected void removeListener(
            TransferListener listener ) {

        this.client.removeProtocolCommandListener((ProtocolCommandListener) listener);

    }

    @Override
    protected void finalize() throws Throwable {

        // ensure the connection is terminated
        this.disconnect();

        this.listener = null;

        super.finalize();
    }

    @Override
    public void enableResponseCollection(
            boolean enable ) {

        if (enable) {
            this.listener = new FtpResponseListener();
            // If it's connected add the listener to gather the responses
            if (this.client != null) {
                this.client.addProtocolCommandListener((FtpResponseListener) listener);
            }
        } else {
            // If it's connected remove the listener
            if (this.client != null) {
                this.client.removeProtocolCommandListener((FtpResponseListener) listener);
            }
            this.listener = null;
        }
    }

    @PublicAtsApi
    @Override
    public String[] getResponses() {

        if (this.listener == null) {
            return new String[]{};
        }

        List<String> responses = this.listener.getResponses();

        return responses.toArray(new String[responses.size()]);
    }

    @Override
    public void setKeystore( String keystoreFile, String keystorePassword, String alias ) {

        throw new UnsupportedOperationException("FTP Connections over SSL are not supported.");

    }

    @Override
    public void setTrustStore( String truststoreFile, String truststorePassword ) {

        throw new UnsupportedOperationException("FTP Connections over SSL are not supported.");

    }

    @Override
    public void setTrustedServerSSLCertificate( String certificateFile ) {

        throw new UnsupportedOperationException("FTP Connections over SSL are not supported.");

    }

    private String constructExceptionMessage( String command, String[] arguments ) {

        StringBuilder sb = new StringBuilder();

        sb.append("Unable to execute '" + command + " ");
        if (arguments != null) {
            for (String argument : arguments) {
                sb.append(argument + " ");
            }
        }
        sb.setLength(sb.toString().length() - 1);
        sb.append("' command");

        return sb.toString();

    }

    private String constructExecutionErrorMessage( String command, String[] arguments ) {

        StringBuilder sb = new StringBuilder();

        sb.append("Error occured while executing '" + command + " ");
        if (arguments != null) {
            for (String argument : arguments) {
                sb.append(argument + " ");
            }
        }
        sb.setLength(sb.toString().length() - 1);
        sb.append("' command. Server's response was: " + getAllReplyLinesAsString());

        return sb.toString();
    }

    private int extractPassivePort( String reply ) {

        String[] tokens = reply.split("\\(");
        if (tokens.length == 2) {
            String[] addressTokens = tokens[1].split(",");
            if (addressTokens.length == 6) {
                int p1 = Integer.parseInt(addressTokens[4]);
                int p2 = Integer.parseInt(addressTokens[5].split("\\)")[0]);
                int port = (p1 * 256) + p2;
                return port;
            }
        }
        throw new RuntimeException("Could not obtain passive port from reply '" + reply + "'");
    }
}
