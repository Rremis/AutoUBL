package fr.rremis.autoubl.ubl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import fr.rremis.autoubl.AutoUBL;

public class BanlistUpdater {

	private String downloadBanlist(BufferedReader in, int bufferSize,
			int timeout) throws IOException, InterruptedException {
		
		final Thread iothread = Thread.currentThread();
		BukkitTask timer = new BukkitRunnable() {
			public void run() {
				iothread.interrupt();
			}
		}.runTaskLaterAsynchronously(AutoUBL.getInstance(), timeout);
		try {
			char[] buffer = new char[bufferSize];
			StringBuilder sb = new StringBuilder();
			while (true) {
				int bytesRead = in.read(buffer);

				if (bytesRead == -1) {
					return sb.toString();
				}

				sb.append(buffer, 0, bytesRead);

				Thread.sleep(50L);
			}
		} finally {
			timer.cancel();
		}
	}

	public void load() {
		String banlistURL = "https://docs.google.com/spreadsheet/ccc?key=0AjACyg1Jc3_GdEhqWU5PTEVHZDVLYWphd2JfaEZXd2c&output=csv";
		int retries = 3;
		int maxBandwidth = 64;
		int bufferSize = maxBandwidth * 1024 / 20;
		int timeout = 5;
		String data;
		try {
			URL url = new URL(banlistURL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setInstanceFollowRedirects(false);
			conn.setConnectTimeout(timeout * 1000);
			conn.setReadTimeout(timeout * 1000);
			conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
			conn.addRequestProperty("User-Agent", "Mozilla");
			conn.addRequestProperty("Referer", "google.com");
			boolean found = false;
			int tries = 0;
			StringBuilder cookies = new StringBuilder();
			while (!found) {
				int status = conn.getResponseCode();
				if ((status == 302) || (status == 301) || (status == 303)) {
					String newUrl = conn.getHeaderField("Location");
					String headerName;
					for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
						if (headerName.equals("Set-Cookie")) {
							String newCookie = conn.getHeaderField(i);
							newCookie = newCookie.substring(0,
									newCookie.indexOf(";"));
							String cookieName = newCookie.substring(0,
									newCookie.indexOf("="));
							String cookieValue = newCookie.substring(
									newCookie.indexOf("=") + 1,
									newCookie.length());
							if (cookies.length() != 0) {
								cookies.append("; ");
							}
							cookies.append(cookieName).append("=")
									.append(cookieValue);
						}

					}

					conn = (HttpURLConnection) new URL(newUrl).openConnection();
					conn.setInstanceFollowRedirects(false);
					conn.setRequestProperty("Cookie", cookies.toString());
					conn.setConnectTimeout(timeout * 1000);
					conn.setReadTimeout(timeout * 1000);
					conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
					conn.addRequestProperty("User-Agent", "Mozilla");
					conn.addRequestProperty("Referer", "google.com");
				} else if (status == 200) {
					found = true;
				} else {
					tries++;
					if (tries >= retries) {
						throw new IOException(new StringBuilder()
								.append("Failed to reach ")
								.append(url.getHost()).append(" after ")
								.append(retries).append(" attempts").toString());
					}
				}
			}

			BufferedReader in = new BufferedReader(new InputStreamReader(
					conn.getInputStream()), bufferSize);
			try {
				data = downloadBanlist(in, bufferSize, timeout * 20);
				AutoUBL.getInstance().getLogger().info(
						"UBL successfully updated from banlist server");
			} catch (IOException ex) {
				AutoUBL.getInstance().getLogger().severe(new StringBuilder()
								.append("Connection was interrupted while downloading banlist from ")
								.append(banlistURL).toString());
				data = loadFromBackup();
			} catch (InterruptedException ex) {
				AutoUBL.getInstance().getLogger().log(Level.SEVERE,
								"Timed out while waiting for banlist server to send data",
								ex);
				data = loadFromBackup();
			}

			saveToBackup(data);
		} catch (MalformedURLException ex) {
			AutoUBL.getInstance().getLogger()
				.severe("banlist-url in the config.yml is invalid or corrupt. This must be corrected and the config reloaded before the UBL can be updated");
			data = loadFromBackup();
		} catch (IOException ex) {
			AutoUBL.getInstance().getLogger()
					.log(Level.WARNING,
							new StringBuilder().append("Banlist server ")
									.append(banlistURL)
									.append(" is currently unreachable")
									.toString(), ex);
			data = loadFromBackup();
		}

		parseData(data);
	}

	private void parseData(final String data) {
		new BukkitRunnable() {
			public void run() {
				String[] lines = data.split("\\r?\\n");
				if (lines.length < 2) {
					AutoUBL.getInstance().getLogger().warning(
							"Banlist is empty!");
					return;
				}
				AutoUBL.getInstance().setBanList(lines[0], Arrays
						.asList(Arrays.copyOfRange(lines, 1, lines.length)));
			}
		}.runTask(AutoUBL.getInstance());
	}

	public String loadFromBackup() {
		File file = new File(AutoUBL.getInstance().getDataFolder(), "ubl.backup");
		if (!file.exists()) {
			AutoUBL.getInstance()
					.getLogger()
					.severe("The backup file could not be located. You are running without UBL protection!");
			return "";
		}
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			Throwable localThrowable2 = null;
			try {
				StringBuilder sb = new StringBuilder();
				char[] buffer = new char[8192];
				int bytesRead;
				while (true) {
					bytesRead = in.read(buffer);
					if (bytesRead == -1) {
						break;
					}
					sb.append(buffer, 0, bytesRead);
				}

				AutoUBL.getInstance().getLogger().info("UBL loaded from local backup");
				return sb.toString();
			} catch (Throwable localThrowable1) {
				localThrowable2 = localThrowable1;
				throw localThrowable1;
			} finally {
				if (in != null)
					if (localThrowable2 != null)
						try {
							in.close();
						} catch (Throwable x2) {
							localThrowable2.addSuppressed(x2);
						}
					else
						in.close();
			}
		} catch (IOException ex) {
			AutoUBL.getInstance().getLogger().log(Level.SEVERE,
							"Could not load UBL backup. You are running without UBL protection!",
							ex);
		}
		return "";
	}

	public void saveToBackup(String data) {
		File file = new File(AutoUBL.getInstance().getDataFolder(), "ubl.backup");
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			Throwable localThrowable2 = null;
			try {
				out.write(data);
			} catch (Throwable localThrowable1) {
				localThrowable2 = localThrowable1;
				throw localThrowable1;
			} finally {
				if (out != null)
					if (localThrowable2 != null)
						try {
							out.close();
						} catch (Throwable x2) {
							localThrowable2.addSuppressed(x2);
						}
					else
						out.close();
			}
		} catch (IOException ex) {
			AutoUBL.getInstance().getLogger().log(Level.SEVERE,
					"Failed to save UBL backup", ex);
		}

	}
}
