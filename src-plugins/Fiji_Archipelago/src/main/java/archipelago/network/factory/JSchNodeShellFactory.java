package archipelago.network.factory;


import archipelago.network.ClusterNode;
import archipelago.network.NodeShell;
import com.jcraft.jsch.*;
import ij.IJ;
import ij.gui.GenericDialog;

import java.io.*;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

public class JSchNodeShellFactory implements NodeShellFactory
{

    private class JSNUserInfo implements UserInfo
    {
        
        private String passphrase = "";
        private boolean displayEnabled = true;
        private boolean passSet = false;
        
        public String getPassphrase() {
            return passphrase;
        }

        public String getPassword() {
            return null;
        }

        public boolean promptPassword(String s) {
            return true;
        }

        public boolean promptPassphrase(String s) {
            if (displayEnabled && !passSet)
            {
                GenericDialog gd = new GenericDialog("Enter Passphrase");

                gd.addStringField("Please enter the public key passphrase", "");

                gd.showDialog();

                passphrase = gd.getNextString();

                passSet = true;
            }
            return true;
        }

        public boolean promptYesNo(String s) {
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
            passSet = false;
        }

    }
    
    private class JSchNodeShell implements NodeShell
    {
        private final ClusterNode node;
        private String lastResponse;
        private Thread t;
        private final AtomicBoolean isRunning;
        private final AtomicBoolean success;
        private final Vector<String> quickSyncString;
        
        
        public JSchNodeShell(ClusterNode nodeIn, int port) throws JSchException
        {
            node = nodeIn;
            lastResponse = "";
            isRunning = new AtomicBoolean(false);
            success = new AtomicBoolean(false);
            quickSyncString = new Vector<String>();
            quickSyncString.set(0, "");
        }
        
        public ClusterNode getNode() {
            return node;
        }

        private void setSuccess(boolean b)
        {
            success.set(b);
            isRunning.set(false);
        }
        
        private synchronized void setMessage(String s)
        {
            quickSyncString.set(0, s == null ? "" : s);
        }
        
        
        public boolean isActive()
        {
            return isRunning.get();
        }
        
        public boolean lastSuccess()
        {
            return success.get();
        }
        
        public String lastMessage()
        {
            return quickSyncString.get(0);
        }
        
        public boolean exec(String command) {
            Session session;            

            if (isRunning.get())
            {
                return false;
            }
            
            try
            {
                //Channel channel
                ChannelExec channelExec;

                session = jsch.getSession(node.getUser(), node.getHost(), port);
                session.setUserInfo(ui);
                session.connect();

                final Channel channel = session.openChannel("exec");
                channelExec = (ChannelExec)channel;

                channel.setInputStream(null);
                channelExec.setErrStream(System.err);

                t = new Thread()
                {
                    public void run()
                    {
                        BufferedReader in;
                        try
                        {
                            in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
                            channel.connect();
                            setSuccess(true);
                            
                        }
                        catch (JSchException jse)
                        {
                            setSuccess(false);
                            return;
                        }
                        catch (IOException ioe)
                        {
                            setSuccess(false);
                            return;
                        }
                        
                        try
                        {
                            setMessage(in.readLine());
                        }
                        catch (IOException ioe)
                        {
                            //Nothing
                        }
                    }
                };
            }
            catch (JSchException jse)
            {
                return false;
            }

            isRunning.set(true);
            success.set(false);
            setMessage("");
            
            t.start();

            return true;
        }

        public void join()
        {
            try
            {
                t.join();
            }
            catch (InterruptedException ie)
            {
                //nope
            }
        }

        public String getExecResponse()
        {
            return lastResponse;
        }
        
        public boolean verify()
        {
            if (isActive())
            {
                return true;
            }
            else
            {
                exec("echo");
                join();
                return lastSuccess();
            }
        }
    }

    
    public static final int DEFAULT_PORT = 22; 
    
    private final JSch jsch;
    private final UserInfo ui;
    private boolean ready;
    private int port;
    
    public JSchNodeShellFactory(File authkey)
    {
        ready = false;
        jsch = new JSch();
        addPubKey(authkey);
        ui = new JSNUserInfo();
        port = DEFAULT_PORT;
    }
    
    public boolean addPubKey(File authkey)
    {
        try
        {
            jsch.addIdentity(authkey.getAbsolutePath());
            ready = true;
            return true;
        }
        catch (JSchException jse)
        {
            return false;
        }
    }
    
    public void setPort(int portIn)
    {
        port = portIn;
    }
    
    public NodeShell getShell(final ClusterNode node)
    {
        if (ready)
        {
            NodeShell shell;

            try
            {
                shell = new JSchNodeShell(node, port);
            }
            catch (JSchException jse)
            {
                shell = null;
            }

            return shell;
        }
        else
        {
            return null;
        }
    }
    

}
