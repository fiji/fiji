package fiji.updater.logic.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * @author Jarek Sacha
 * @since 4/23/11 12:39 AM
 */
final class SSHSessionCreator {

	private SSHSessionCreator() {
	}

	/**
	 * Creates and connects SSH session.
	 *
	 * @param username SSH user name.
	 * @param sshHost  SSH host to connect to.
	 * @param userInfo authentication data.
	 * @return connected session.
	 * @throws JSchException if authentication or connection fails.
	 */
	public static Session connect(String username, String sshHost, final UserInfo userInfo) throws JSchException {

		int port = 22, colon = sshHost.indexOf(':');
		if (colon > 0) {
			port = Integer.parseInt(sshHost.substring(colon + 1));
			sshHost = sshHost.substring(0, colon);
		}

		final JSch jsch = new JSch();

		// Reuse ~/.ssh/known_hosts file
		final File knownHosts = new File(new File(System.getProperty("user.home"), ".ssh"), "known_hosts");
		jsch.setKnownHosts(knownHosts.getAbsolutePath());

		final ConfigInfo configInfo = getIdentity(username, sshHost);
		if (configInfo != null) {
			if (configInfo.username != null) {
				username = configInfo.username;
			}
			if (configInfo.sshHost != null) {
				sshHost = configInfo.sshHost;
			}
			if (configInfo.identity != null) {
				jsch.addIdentity(configInfo.identity);
			}
		}

		final Session session = jsch.getSession(username, sshHost, port);
		session.setUserInfo(userInfo);
		session.connect();

		return session;
	}

	private static ConfigInfo getIdentity(final String username, final String sshHost) {
		final File config = new File(new File(System.getProperty("user.home"), ".ssh"), "config");
		if (!config.exists()) {
			return null;
		}

		try {
			final ConfigInfo result = new ConfigInfo();
			final BufferedReader reader = new BufferedReader(new FileReader(config));
			boolean hostMatches = false;
			for (; ;) {
				String line = reader.readLine();
				if (line == null)
					break;
				line = line.trim();
				int space = line.indexOf(' ');
				if (space < 0) {
					continue;
				}
				final String key = line.substring(0, space).toLowerCase();
				if (key.equals("host")) {
					hostMatches = line.substring(5).trim().equals(sshHost);
				} else if (hostMatches) {
					if (key.equals("user")) {
						if (username == null || username.equals("")) {
							result.username = line.substring(5).trim();
						}
					} else if (key.equals("hostname")) {
						result.sshHost = line.substring(9).trim();
					} else if (key.equals("identityfile")) {
						result.identity = line.substring(13).trim();
					}
					// TODO what if condition do match any here?
				}
			}
			reader.close();
			return result;
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static class ConfigInfo {

		String username;
		String sshHost;
		String identity;
	}
}