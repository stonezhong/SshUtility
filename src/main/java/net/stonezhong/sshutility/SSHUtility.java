package net.stonezhong.sshutility;

import com.jcraft.jsch.*;
import org.apache.commons.io.IOUtils;

import java.io.*;

/**
 * Created by szhong on 4/27/14.
 */
public class SSHUtility {

    private static int checkAck(InputStream in) throws IOException{
        int b=in.read();
        // b may be 0 for success,
        // 1 for error,
        // 2 for fatal error,
        // -1
        if(b==0) return b;
        if(b==-1) return b;

        if(b==1 || b==2){
            StringBuffer sb=new StringBuffer();
            int c;
            do {
                c=in.read();
                sb.append((char)c);
            }
            while(c!='\n');
            if(b==1){ // error
                System.out.print(sb.toString());
            }
            if(b==2){ // fatal error
                System.out.print(sb.toString());
            }
        }
        throw new RuntimeException("Remote SSH Server failed the operation");
        // return b;
    }

    public void uploadFile(String localFilename, String remoteFilename) throws JSchException, IOException {

        ChannelExec channel = null;
        OutputStream remoteOutput = null;
        InputStream remoteInput = null;
        FileInputStream localFileInput = null; // maybe better if buffered
        BufferedInputStream bufferedLocalFileInput = null;

        System.out.printf("copying file [%s] ==> [%s]: ", localFilename, remoteFilename);
        try {

            String command = String.format("scp -t %s", remoteFilename);
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            remoteOutput = channel.getOutputStream();
            remoteInput = channel.getInputStream();

            channel.connect();
            checkAck(remoteInput);

            File localFile = new File(localFilename);
            localFileInput = new FileInputStream(localFilename);
            bufferedLocalFileInput = new BufferedInputStream(localFileInput);


            // send "C0644 filesize filename", where filename should not include '/'
            command = String.format("C0644 %d %s\n", localFile.length(), localFile.getName());
            remoteOutput.write(command.getBytes());
            remoteOutput.flush();
            checkAck(remoteInput);

            // send content of the file
            IOUtils.copy(bufferedLocalFileInput, remoteOutput);

            // write '\0'
            remoteOutput.write(0);
            remoteOutput.flush();
            checkAck(remoteInput);
            System.out.printf("ok%n");
        } finally {
            IOUtils.closeQuietly(bufferedLocalFileInput);
            IOUtils.closeQuietly(localFileInput);
            IOUtils.closeQuietly(remoteOutput);
            IOUtils.closeQuietly(remoteInput);

            try {
                channel.disconnect();
            } catch (Exception e) {
            }
        }
    }

    public int exec(String command) throws JSchException, IOException {
        return exec(command, -1, null);
    }

    static class ByteBuffer {
        byte[] buffer;
        byte[] target;

        int beginIdx;
        int endIdx;
        boolean isEmpty;

        ByteBuffer(byte[] target) {
            this.target = target;
            this.buffer = new byte[target.length];
            beginIdx = 0;
            endIdx = 0;
            isEmpty = true;
        }

        int getLength() {
            if (isEmpty) {
                return 0;
            }
            if (endIdx >= beginIdx) {
                return (endIdx - beginIdx + 1);
            }
            return (endIdx + buffer.length - beginIdx + 1);
        }

        int nextPosition(int oldPosition) {
            int ret = oldPosition + 1;
            if (ret == buffer.length) {
                return 0;
            }
            return ret;
        }

        boolean matchTarget() {
            if (getLength() != target.length) {
                return false;
            }

            for (int i = 0, j = beginIdx; i < buffer.length; i ++) {
                if (target[i] != buffer[j]) {
                    return false;
                }
                j = nextPosition(j);
            }

            return true;
        }

        void add(byte b) {
            if (isEmpty) {
                beginIdx = 0;
                endIdx = 0;
                buffer[0] = b;
                isEmpty = false;
                return ;
            }

            endIdx = nextPosition(endIdx);
            buffer[endIdx] = b;
            if (endIdx == beginIdx) {
                beginIdx = nextPosition(beginIdx);
            }
        }
    }



    public int exec(String command, long timeout, String endPrompt) throws JSchException, IOException {

        ChannelExec channel = (ChannelExec)session.openChannel("exec");
        channel.setCommand(command);
        channel.setErrStream(null);
        channel.setInputStream(null);
        channel.setOutputStream(null);

        channel.connect();

        InputStream  in = channel.getInputStream();
        InputStream  err = channel.getErrStream();
        OutputStream out = channel.getOutputStream();

        ByteBuffer byteBuffer = null;
        if (endPrompt != null) {
            byteBuffer = new ByteBuffer(endPrompt.getBytes());
        }


        long begin = System.currentTimeMillis();
        try {
            int retCode = 0;
            for (; ; ) {
                if (err.available() > 0) {
                    System.err.write(err.read());
                    System.err.flush();
                    continue;
                }
                if (in.available() > 0) {
                    int ch = in.read();
                    if (byteBuffer != null) {
                        byteBuffer.add((byte)ch);
                    }
                    System.out.write(ch);
                    System.out.flush();
                    if (byteBuffer != null && byteBuffer.matchTarget()) {
                        break;
                    }
                    continue;
                }
                if (System.in.available() > 0) {
                    out.write(System.in.read());
                    out.flush();
                    continue;
                }
                if (channel.isClosed()) {
                    retCode = channel.getExitStatus();
                    break;
                }
                try {
                    if (timeout != -1) {
                        long current = System.currentTimeMillis();
                        if ((current - begin) >= timeout) {
                            break;
                        }
                    }
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
            }
            channel.disconnect();
            return retCode;
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(err);
        }
    }

    final private String host;
    final private int port;
    final private String username;
    final private String password;
    final Session session;

    public SSHUtility(String host, int port, String username, String password) throws JSchException {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;

        JSch jsch=new JSch();
        session = jsch.getSession(username, host, port);
    }

    public void connect() throws JSchException {
        MyUserInfo ui = new MyUserInfo(password);
        session.setUserInfo(ui);
        session.connect();
    }

    public void dispose() {
        if (session != null) {
            session.disconnect();
        }
    }


}
