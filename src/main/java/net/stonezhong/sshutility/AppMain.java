package net.stonezhong.sshutility;

/**
 * Created by stonezhong on 5/15/14.
 */
public class AppMain {
    public static void main(String[] args) throws Exception {
        // login to host "www.myhost.com" as root, password is 1234, 22 is the default ssh port
        SSHUtility sshUtility = new SSHUtility("www.myhost.com", 22, "root", "1234");

        sshUtility.connect();

        // execute a command
        sshUtility.exec("/bin/ls");

        // copy a file
        sshUtility.uploadFile("/tmp/foo.txt", "/root/foo.txt");

        // disconnect
        sshUtility.dispose();
    }
}
