package com.pv.model;

import com.pv.exceptions.InvalidHostNameException;
import com.pv.util.MXRecordManager;
import org.apache.commons.lang3.StringUtils;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.naming.NamingException;
import java.io.File;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Created by sanitizer on 6/13/2016.
 *
 */
public class EmailMsg{

    private final String protocol = "smtps";
    private String to;
    private String from;
    private String user;
    private String password;
    private String subject;
    private String host;
    private String msgText;
    private Set<String> fileLocations;
    private Set<String> failedFileLocations;
    private Boolean succeeded;
    private String error;
    private MimeMessage msg;
    private MXRecordManager mxRecordManager;
    private Session session;

    public EmailMsg(String _from, String _to, String _subject, String _msgText, Set<String> _fileLocationPaths, String _user, String _password)
            throws InvalidHostNameException,
                   NamingException,
                   MessagingException{

        mxRecordManager = new MXRecordManager();
        Map<Integer, String> hosts = mxRecordManager.getMXRecords(_from);

        if(hosts.isEmpty()){
            throw new InvalidHostNameException(String.format("%s is an invalid host", _from));
        }

        for(Map.Entry h: hosts.entrySet()){
            host = h.getValue().toString();
            break;
        }

        host = "smtp.gmail.com";
        Properties properties = System.getProperties();
//        properties.setProperty("mail.smtp.host", host);
//        properties.setProperty("mail.smtp.smtp.enable", "true");
//        properties.setProperty("mail.smtp.auth", "true");
//        properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
//        properties.put("mail.smtp.socketFactory.fallback", "false");
//        properties.put("mail.smtp.starttls.enable","true");

        properties.put("mail.smtp.user",_user);
        properties.put("mail.smtp.password", _password);
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", 465);
        properties.put("mail.debug", "true");
        properties.put("mail.smtp.auth", "false");
        properties.put("mail.smtp.starttls.enable","true");
        properties.put("mail.smtp.EnableSSL.enable","true");
        properties.put("mail.smtp.useEhlo.enable","true");

        if(user != null){
            properties.setProperty("mail.user", user);
        }

        if(password != null){
            properties.setProperty("mail.password", password);
        }

        to = _to;
        from = _from;
        subject = _subject;
        msgText = _msgText;
        fileLocations = _fileLocationPaths;
        failedFileLocations = new HashSet<>();
        session = Session.getInstance(properties);
        user = _user;
        password = _password;

        BodyPart msgBodyPart = new MimeBodyPart();
        msgBodyPart.setText(msgText);

        Multipart msgBlocks = new MimeMultipart();
        msgBlocks.addBodyPart(msgBodyPart);

        addAttachments(msgBlocks);

        msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        msg.setSubject(subject);
        msg.setContent(msgBlocks);
    }

    public EmailMsg(String _from, String _to, String _subject, String _msgText, Set<String> _fileLocationPaths)
            throws UnknownHostException,
                   MessagingException,
                   InvalidHostNameException,
                   NamingException{
        this(_from, _to, _subject, _msgText, _fileLocationPaths, null, null);
    }

    public boolean succeeded() {
        return succeeded;
    }

    public void sendMsg() throws NoSuchProviderException {
        Transport transport = session.getTransport(protocol);
        try{
            transport.connect(host, user, password);
            transport.sendMessage(msg, msg.getAllRecipients());
            succeeded = true;
        }catch(MessagingException e){
            error = e.getMessage();
            succeeded = false;
            System.out.println(String.format("Message was not sent. Error: %s", e.getMessage()));
        }finally{
            try{
                transport.close();
            }catch(MessagingException e){}
        }
    }

    private void addAttachments(Multipart msgBlocks) throws MessagingException{
        if(fileLocations == null || fileLocations.isEmpty()){return;}

        for(String path: fileLocations){
            File file = new File(path);
            if(!file.exists()){
                failedFileLocations.add(path);
            }else{
                DataSource source = new FileDataSource(file);
                BodyPart bodyPart = new MimeBodyPart();
                bodyPart.setDataHandler(new DataHandler(source));
                bodyPart.setFileName(file.getName());
                msgBlocks.addBodyPart(bodyPart);
            }
        }
    }

    private String printAttachmentFiles(Set<String> _fileLocations){
        StringBuffer buffer = new StringBuffer();
        for(String path: _fileLocations){
            buffer.append("\t\t" + path + "\n");
        }
        return StringUtils.chop(buffer.toString());
    }

    public String toString(){
        return String.format("Sending message ->" +
                             "\n\tFrom: %s" +
                             "\n\tTo: %s" +
                             "\n\tSubject: %s" +
                             "\n\tAttachment Files with names: \n%s" +
                             "\n\tFailed to retrieve Files with names: \n%s" +
                             "\n\t%s",
                             from,
                             to,
                             subject,
                             fileLocations == null || fileLocations.isEmpty() ? "\t\tnone" : printAttachmentFiles(fileLocations),
                             failedFileLocations.isEmpty() ? "\t\tnone" : printAttachmentFiles(failedFileLocations),
                             succeeded == null ? "Message was not sent yet" : "Message was " + (succeeded ? "successfully sent" : "not sent due to an error: " + error));
    }

}