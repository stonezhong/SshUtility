package net.stonezhong.sshutility;

import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by szhong on 4/27/14.
 */
class MyUserInfo implements UserInfo {

    private String password;

    MyUserInfo(String password) {
        this.password = password;
    }
    MyUserInfo() {
        this(null);
    }


    private static Set<String> positiveAnswer = new HashSet<String>() {{
        add("YES");
        add("TRUE");
    }};

    boolean readBooleanFromConsole()  {
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        try {
            String input = r.readLine();
            if ((input != null) && (positiveAnswer.contains(input.toUpperCase()))) {
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    String readStringFromConsole()  {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            return r.readLine();
        } catch (IOException e) {
            return "";
        }
    }

    @Override
    public boolean promptPassphrase(String message) {
        // we always want to enter pass phrase
        /*
        System.out.printf("[promptPassphrase] %s%n", message);
        return readBooleanFromConsole();
        */
        return true;
    }
    @Override
    public String getPassphrase() {
        System.out.printf("[getPassphrase] please enter pass phrase:");
        return readStringFromConsole();
    }


    @Override
    public boolean promptPassword(String message) {
        // we always want to enter password
        /*
        System.out.printf("[promptPassword] %s%n", message);
        return readBooleanFromConsole();
        */
        return true;
    }
    @Override
    public String getPassword() {
        System.out.printf("[getPassword] please enter password:");
        if (password != null) {
            System.out.printf("****%n");
            return password;
        }
        return readStringFromConsole();
    }


    @Override
    public boolean promptYesNo(String message) {
        // our answer is alway "yes"
        System.out.printf("[promptYesNo] %s%n", message);
        // return readBooleanFromConsole();
        System.out.printf("Yes%n");
        return true;
    }

    @Override
    public void showMessage(String message) {
        System.out.printf("[showMessage] %s%n", message);
    }

}
