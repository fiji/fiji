package edu.utexas.clm.archipelago.network.shell.ssh;

import com.jcraft.jsch.UserInfo;
import ij.IJ;
import ij.gui.GenericDialog;


public class NodeShellUserInfo implements UserInfo
{

    private String passPhrase = "";
    private boolean displayEnabled = true;
    private boolean passSet = false;

    public String getPassphrase()
    {
        return passPhrase;
    }

    public String getPassword()
    {
        return null;
    }

    public boolean promptPassword(String s)
    {
        return true;
    }

    public boolean promptPassphrase(String s)
    {
        if (displayEnabled && !passSet)
        {
            GenericDialog gd = new GenericDialog("Enter Passphrase");

            gd.addStringField("Please enter the public key passphrase", "");

            gd.showDialog();

            passPhrase = gd.getNextString();

            passSet = true;
        }
        return true;
    }

    public boolean promptYesNo(String s)
    {
        return true;
    }

    public void showMessage(String s)
    {
        if (displayEnabled)
        {
            IJ.showMessage(s);
        }
    }

    public void enableDisplay()
    {
        displayEnabled = true;
    }

    public void disableDisplay()
    {
        displayEnabled = false;
    }

    public void unsetPass()
    {
        passPhrase = "";
        passSet = false;
    }

}

