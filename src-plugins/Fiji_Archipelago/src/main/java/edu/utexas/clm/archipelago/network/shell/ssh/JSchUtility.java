package edu.utexas.clm.archipelago.network.shell.ssh;

import com.jcraft.jsch.*;
import edu.utexas.clm.archipelago.exception.ShellExecutionException;
import edu.utexas.clm.archipelago.listen.NodeShellListener;
import edu.utexas.clm.archipelago.network.node.NodeManager;
import edu.utexas.clm.archipelago.network.shell.NodeShellParameters;

import java.io.File;


public class JSchUtility
{

    final JSch jsch;
    final UserInfo ui;
    final Session session;
    final JSchExecThread jset;
    final int port;
    final String keyfile;
    final String executablePath;
    final String arguments;
    final NodeShellListener listener;
    
    
    public JSchUtility(NodeManager.NodeParameters param, NodeShellListener listener, String args)
            throws ShellExecutionException
    {
        try
        {
            final String eroot = param.getExecRoot();
            this.listener = listener;
            arguments = args;
            jsch = new JSch();
            ui = new NodeShellUserInfo();

            port = param.getShellParams().getInteger("ssh-port");
            keyfile = param.getShellParams().getString("keyfile");
            executablePath = eroot + "/" + param.getShellParams().getString("executable");


            jsch.addIdentity(new File(keyfile).getAbsolutePath());

            session = jsch.getSession(param.getUser(), param.getHost(), port);

            session.setUserInfo(ui);
            session.connect();

            jset = new JSchExecThread(param.getID(), session);
        }
        catch (Exception e)
        {
            throw new ShellExecutionException(e);
        }
    }
    
    public Session getSession()
    {
        return session;
    }
    
    public JSch getJSch()
    {
        return jsch;
    }

    public boolean fileExists() throws JSchException
    {
        return jset.fileExists(ui, executablePath);
    }

    public Channel exec() throws JSchException
    {
        return jset.exec(ui, executablePath + " " + arguments, listener);
    }



}
