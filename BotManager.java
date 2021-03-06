/*
f * Copyright 2012 Andrew Bashore
 * This file is part of GeoBot.
 * 
 * GeoBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * GeoBot is distributed in the hope that it will be useful
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with GeoBot.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.bashtech.geobot;

import net.bashtech.geobot.gui.BotGUI;
import net.bashtech.geobot.modules.BotModule;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

public class BotManager {

	static BotManager instance;
	// API KEYS
	public String bitlyAPIKey;
	public String bitlyLogin;
	public String LastFMAPIKey;
	public String SteamAPIKey;
	public String krakenOAuthToken;
	public String krakenClientID;
	public String YoutubeAPIKey;
	public String CoeBotTVAPIKey;
	public String pusherAppKey;
	String nick;
	String server;
	int port;
	String password;
	String localAddress;
	boolean publicJoin;
	boolean monitorPings;
	int pingInterval;
	boolean useGUI;
	BotGUI gui;
	Map<String, Channel> channelList;
	Set<String> blockedChannelList;
	Set<String> admins;
	List<String> emoteSet;
	List<Pattern> globalBannedWords;
	boolean verboseLogging;
	ReceiverBot receiverBot;
	String bothelpMessage;
	boolean ignoreHistory;

	boolean wsEnabled;
	int wsPort;
	String wsAdminPassword;
	boolean twitchChannels;
	private PropertiesFile config;
	private Set<BotModule> modules;
	private Set<String> tagAdmins;
	private Set<String> tagStaff;
	int multipleTimeout;
	boolean randomNickColor;
	int randomNickColorDiff;
	boolean useEventFeed;
	String eventFeedURL;
	String defaultBullet;

	Map<Integer, List<Pattern>> banPhraseLists;
	// ********
	private String _propertiesFile;

	Map<String, Set<String>> emoteSetMapping;

	public BotManager(String propertiesFile) {
		BotManager.setInstance(this);
		_propertiesFile = propertiesFile;
		channelList = new HashMap<String, Channel>();
		blockedChannelList = new HashSet<String>();
		admins = new HashSet<String>();
		modules = new HashSet<BotModule>();
		tagAdmins = new HashSet<String>();
		tagStaff = new HashSet<String>();
		emoteSet = new LinkedList<String>();
		globalBannedWords = new LinkedList<Pattern>();
		emoteSetMapping = new HashMap<String, Set<String>>();
		banPhraseLists = new HashMap<Integer, List<Pattern>>();

		loadGlobalProfile();

		if (useGUI) {
			gui = new BotGUI();
		}

		receiverBot = new ReceiverBot(server, port);
		List<String> outdatedChannels = new LinkedList<String>();
		for (Map.Entry<String, Channel> entry : channelList.entrySet()) {
			String channel = entry.getValue().getChannel();
			if (!JSONUtil.krakenOutdatedChannel(channel.substring(1))
					|| receiverBot.getNick().equalsIgnoreCase(
							channel.substring(1))
					|| entry.getValue().staticChannel) {
				log("BM: Joining channel " + channel);
				receiverBot.joinChannel(channel.toLowerCase(),true);
				try {
					Thread.currentThread().sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				outdatedChannels.add(channel);
			}

		}
		receiverBot.startPusher();
		receiverBot.startJoinCheck();
		log("BM: Done Joining Channels");

		// Start EventFeedReader
		if (useEventFeed) {
			Runnable task = new EventFeedReader();
			Thread worker = new Thread(task);
			worker.setName("Reader");
			worker.start();
		}

		// Remove outdatedChannels
		for (String channel : outdatedChannels) {
			log("BM: Removing channel: " + channel);
			this.removeChannel(channel);
		}

		// Start timer to check for bot disconnects
		Timer reconnectTimer = new Timer();
		reconnectTimer.scheduleAtFixedRate(new ReconnectTimer(channelList),
				30 * 1000, 30 * 1000);

	}

	public static BotManager getInstance() {
		return instance;
	}

	public static void setInstance(BotManager bm) {
		if (instance == null) {
			instance = bm;
		}
	}

	public static String getRemoteContent(String urlString) {

		String dataIn = "";
		try {
			URL url = new URL(urlString);
			// System.out.println("DEBUG: Getting data from " + url.toString());
			URLConnection conn = url.openConnection();
			conn.setRequestProperty("User-Agent", "CoeBot");
			// conn.setConnectTimeout(5 * 1000);
			// conn.setReadTimeout(5 * 1000);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null)
				dataIn += inputLine;
			in.close();

		} catch (Exception ex) {
			if (ex instanceof SocketTimeoutException) {
				return "API took too long to respond.";
			}
			ex.printStackTrace();
		}

		return dataIn;
	}

	public static String getRemoteContentTwitch(String urlString,
			int krakenVersion) {

		String dataIn = "";
		try {
			URL url = new URL(urlString);
			// System.out.println("DEBUG: Getting data from " + url.toString());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setUseCaches(false);

			// conn.setConnectTimeout(5 * 1000);
			// conn.setReadTimeout(5 * 1000);

			if (BotManager.getInstance().krakenClientID.length() > 0)
				conn.setRequestProperty("Client-ID",
						BotManager.getInstance().krakenClientID);

			conn.setRequestProperty("Accept", "application/vnd.twitchtv.v"
					+ krakenVersion + "+json");

			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						conn.getInputStream()));
				String inputLine;
				while ((inputLine = in.readLine()) != null)
					dataIn += inputLine;
				in.close();
			} catch (IOException exerr) {
				String inputLine;

				InputStream errorStream = conn.getErrorStream();
				if (errorStream != null) {
					BufferedReader inE = new BufferedReader(
							new InputStreamReader(errorStream));
					while ((inputLine = inE.readLine()) != null)
						dataIn += inputLine;
					inE.close();
				}
			}
			conn.disconnect();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return dataIn;
	}

	public static String postRemoteDataStrawpoll(String urlString) {

		String line = "";
		try {
			HttpURLConnection c = (HttpURLConnection) (new URL(
					"http://strawpoll.me/api/v2/polls").openConnection());

			c.setRequestMethod("POST");
			c.setRequestProperty("Content-Type", "application/json");
			c.setRequestProperty("User-Agent", "CB2");

			c.setDoOutput(true);
			c.setDoInput(true);
			c.setUseCaches(false);

			String queryString = urlString;

			c.setRequestProperty("Content-Length",
					Integer.toString(queryString.length()));

			DataOutputStream wr = new DataOutputStream(c.getOutputStream());
			wr.writeBytes(queryString);

			wr.flush();
			wr.close();

			Scanner inStream = new Scanner(c.getInputStream());

			while (inStream.hasNextLine())
				line += (inStream.nextLine());

			inStream.close();
			System.out.println(line);
			try {
				JSONParser parser = new JSONParser();
				Object obj = parser.parse(line);
				JSONObject jsonObject = (JSONObject) obj;
				line = (Long) jsonObject.get("id") + "";

			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (MalformedURLException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return line;
	}

	public static String postRemoteDataTwitch(String urlString,
			String postData, int krakenVersion) {
		URL url;
		HttpURLConnection conn;

		try {
			url = new URL(urlString);

			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");

			//conn.setFixedLengthStreamingMode(postData.getBytes().length);
			//conn.setRequestProperty("Content-Type",
			//		"application/x-www-form-urlencoded");
			conn.setRequestProperty("Accept", "application/vnd.twitchtv.v"
					+ krakenVersion + "+json");
			conn.setRequestProperty("Authorization",
					"OAuth " + BotManager.getInstance().krakenOAuthToken);
			conn.setRequestProperty("Client-ID",
					BotManager.getInstance().krakenClientID);
			// conn.setConnectTimeout(5 * 1000);
			// conn.setReadTimeout(5 * 1000);

			PrintWriter out = new PrintWriter(conn.getOutputStream());
			out.print(postData);
			out.close();

			String response = "";

			Scanner inStream = new Scanner(conn.getInputStream());

			while (inStream.hasNextLine())
				response += (inStream.nextLine());

			inStream.close();
			return response;

		} catch (MalformedURLException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return "";
	}

	public static String postDataLinkShortener(String postData) {
		URL url;
		HttpURLConnection conn;
		postData = "{\"longUrl\": \"" + postData + "\"}";

		try {
			url = new URL("https://www.googleapis.com/urlshortener/v1/url");

			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("User-Agent", "CoeBot");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Content-Length",
					"" + Integer.toString(postData.getBytes().length));

			PrintWriter out = new PrintWriter(conn.getOutputStream());
			System.out.println(postData);
			out.print(postData);
			out.close();

			String response = "";

			Scanner inStream = new Scanner(conn.getInputStream());

			while (inStream.hasNextLine())
				response += (inStream.nextLine());

			inStream.close();
			return response;

		} catch (MalformedURLException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return "";

	}

	public static String postRemoteDataSongRequest(String urlString,
			String channel, String requester) {
		if (BotManager.getInstance().CoeBotTVAPIKey.length() > 4) {
			URL url;
			HttpURLConnection conn;

			try {
				url = new URL("http://coebot.tv/api/v1/reqsongs/add/"
						+ channel.toLowerCase() + "$"
						+ BotManager.getInstance().CoeBotTVAPIKey + "$"
						+ BotManager.getInstance().nick);

				String postData = "url="
						+ URLEncoder.encode(urlString, "UTF-8")
						+ "&requestedBy="
						+ URLEncoder.encode(requester, "UTF-8");
				conn = (HttpURLConnection) url.openConnection();
				System.out.println(postData);
				conn.setDoOutput(true);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("User-Agent", "CoeBot");
				conn.setRequestProperty("Content-Type",
						"application/x-www-form-urlencoded");
				conn.setRequestProperty("Content-Length",
						"" + Integer.toString(postData.getBytes().length));

				// conn.setConnectTimeout(5 * 1000);
				// conn.setReadTimeout(5 * 1000);

				PrintWriter out = new PrintWriter(conn.getOutputStream());
				out.print(postData);
				out.close();
				String response = "";
				if (conn.getResponseCode() < 400) {

					Scanner inStream = new Scanner(conn.getInputStream());

					while (inStream.hasNextLine()){
						response += (inStream.nextLine());
					}
					inStream.close();
				} else {
					Scanner inStream = new Scanner(conn.getErrorStream());

					while (inStream.hasNextLine()){
						response += (inStream.nextLine());
					}
					inStream.close();
				}
				System.out.println(response);

				return response;

			} catch (MalformedURLException ex) {
				ex.printStackTrace();
			} catch (IOException ex) {
				ex.printStackTrace();

			}

			return null;
		} else {
			return null;
		}

	}

	public static String postCoebotConfig(String postData, String channel) {
		if (BotManager.getInstance().CoeBotTVAPIKey.length() > 4) {
			URL url;
			HttpURLConnection conn;

			try {
				url = new URL("http://coebot.tv/api/v1/channel/update/config/"
						+ channel.toLowerCase() + "$"
						+ BotManager.getInstance().CoeBotTVAPIKey + "$"
						+ BotManager.getInstance().nick);

				conn = (HttpURLConnection) url.openConnection();
				conn.setDoOutput(true);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("User-Agent", "CoeBot");
				conn.setRequestProperty("Content-Type", "application/json");
				conn.setRequestProperty("Content-Length",
						"" + Integer.toString(postData.getBytes().length));

				// conn.setConnectTimeout(5 * 1000);
				// conn.setReadTimeout(5 * 1000);

				PrintWriter out = new PrintWriter(conn.getOutputStream());
				out.print(postData);
				out.close();

				String response = "";

				Scanner inStream = new Scanner(conn.getInputStream());

				while (inStream.hasNextLine())
					response += (inStream.nextLine());

				inStream.close();
				return response;

			} catch (MalformedURLException ex) {
				ex.printStackTrace();
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			return "";
		} else
			return "";
	}

	public static String postCoebotVars(String postData, String requestURL) {
		if (BotManager.getInstance().CoeBotTVAPIKey.length() > 4) {
			URL url;
			HttpURLConnection conn;

			try {
				url = new URL(requestURL + "$"
						+ BotManager.getInstance().CoeBotTVAPIKey + "$"
						+ BotManager.getInstance().nick);

				conn = (HttpURLConnection) url.openConnection();
				conn.setDoOutput(true);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("User-Agent", "CoeBot");
				conn.setRequestProperty("Content-Type", "application/json");
				conn.setRequestProperty("Content-Length",
						"" + Integer.toString(postData.getBytes().length));

				// conn.setConnectTimeout(5 * 1000);
				// conn.setReadTimeout(5 * 1000);

				PrintWriter out = new PrintWriter(conn.getOutputStream());
				out.print(postData);
				out.close();

				String response = "";

				Scanner inStream = new Scanner(conn.getInputStream());

				while (inStream.hasNextLine())
					response += (inStream.nextLine());

				inStream.close();
				return response;

			} catch (MalformedURLException ex) {
				ex.printStackTrace();
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			return "";
		} else
			return "";
	}

	public String coebotJoinChannel(String channel, String botName) {
		if (BotManager.getInstance().CoeBotTVAPIKey.length() > 4) {

			try {
				JSONParser parser = new JSONParser();
				Object obj = parser
						.parse(BotManager
								.getRemoteContent("http://coebot.tv/api/v1/channel/join/"
										+ channel.toLowerCase()
										+ "$"
										+ BotManager.getInstance().CoeBotTVAPIKey
										+ "$" + BotManager.getInstance().nick));

				JSONObject response = (JSONObject) obj;
				String resp = (String) response.get("status");
				return resp;

			} catch (Exception e) {

				return "Error";
			}
		} else
			return "ok";
	}

	public String coebotPartChannel(String channel, String botName) {
		if (BotManager.getInstance().CoeBotTVAPIKey.length() > 4) {
			try {
				JSONParser parser = new JSONParser();
				Object obj = parser
						.parse(BotManager
								.getRemoteContent("http://coebot.tv/api/v1/channel/part/"
										+ channel.toLowerCase()
										+ "$"
										+ BotManager.getInstance().CoeBotTVAPIKey
										+ "$" + botName));

				JSONObject response = (JSONObject) obj;
				String resp = (String) response.get("status");
				return resp;

			} catch (Exception e) {
				e.printStackTrace();
				return "Error";
			}
		} else
			return "Error";
	}

	public static String putRemoteData(String urlString, String postData) throws IOException
			 {

		URL url;
		HttpURLConnection conn;

		try {
			url = new URL(urlString);

			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("PUT");

			//conn.setFixedLengthStreamingMode(postData.getBytes().length);
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Accept",
					"application/vnd.twitchtv.v2+json");
			conn.setRequestProperty("Authorization",
					"OAuth " + BotManager.getInstance().krakenOAuthToken);
			conn.setRequestProperty("Client-ID",
					BotManager.getInstance().krakenClientID);
			// conn.setConnectTimeout(5 * 1000);
			// conn.setReadTimeout(5 * 1000);
			

			PrintWriter out = new PrintWriter(conn.getOutputStream());
			out.print(postData);
			out.close();

			String response = "";
			
			Scanner inStream = new Scanner(conn.getInputStream());
			System.out.println("Sent, waiting for response...");
			while (inStream.hasNextLine())
				response += (inStream.nextLine());

			return response;

		} catch (MalformedURLException ex) {
			ex.printStackTrace();
		}

		return "";
	}

	private void loadGlobalProfile() {
		config = new PropertiesFile(_propertiesFile);

		log("BM: Reading global file > " + _propertiesFile);

		if (!config.keyExists("nick")) {
			config.setString("nick", "");
		}
		if (!config.keyExists("server")) {
			config.setString("server", "");
		}

		if (!config.keyExists("password")) {
			config.setString("password", "");
		}

		if (!config.keyExists("port")) {
			config.setInt("port", 6667);
		}

		if (!config.keyExists("channelList")) {
			config.setString("channelList", "");
		}
		if (!config.keyExists("blockedChannelList")) {
			config.setString("blockedChannelList", "");
		}

		if (!config.keyExists("adminList")) {
			config.setString("adminList", "");
		}

		if (!config.keyExists("publicJoin")) {
			config.setBoolean("publicJoin", false);
		}

		if (!config.keyExists("monitorPings")) {
			config.setBoolean("monitorPings", false);
		}

		if (!config.keyExists("pingInterval")) {
			config.setInt("pingInterval", 350);
		}

		if (!config.keyExists("useGUI")) {
			config.setBoolean("useGUI", false);
		}

		if (!config.keyExists("localAddress")) {
			config.setString("localAddress", "");
		}

		if (!config.keyExists("verboseLogging")) {
			config.setBoolean("verboseLogging", false);
		}

		if (!config.keyExists("bothelpMessage")) {
			config.setString(
					"bothelpMessage",
					"You can find info about CoeBot's default commands at: http://coebot.tv/commands");
		}

		// API KEYS

		if (!config.keyExists("LastFMAPIKey")) {
			config.setString("LastFMAPIKey", "");
		}

		if (!config.keyExists("SteamAPIKey")) {
			config.setString("SteamAPIKey", "");
		}

		if (!config.keyExists("krakenOAuthToken")) {
			config.setString("krakenOAuthToken", "");
		}

		if (!config.keyExists("krakenClientID")) {
			config.setString("krakenClientID", "");
		}

		if (!config.keyExists("wsPort")) {
			config.setInt("wsPort", 8887);
		}

		if (!config.keyExists("wsEnabled")) {
			config.setBoolean("wsEnabled", false);
		}

		if (!config.keyExists("wsAdminPassword")) {
			config.setString("wsAdminPassword", "");
		}

		if (!config.keyExists("twitchChannels")) {
			config.setBoolean("twitchChannels", true);
		}

		if (!config.keyExists("ignoreHistory")) {
			config.setBoolean("ignoreHistory", true);
		}

		if (!config.keyExists("multipleTimeout")) {
			config.setInt("multipleTimeout", 3);
		}

		if (!config.keyExists("randomNickColor")) {
			config.setBoolean("randomNickColor", false);
		}

		if (!config.keyExists("randomNickColorDiff")) {
			config.setInt("randomNickColorDiff", 5);
		}

		if (!config.keyExists("useEventFeed")) {
			config.setBoolean("useEventFeed", false);
		}

		if (!config.keyExists("eventFeedURL")) {
			config.setString("eventFeedURL", "");
		}
		if (!config.keyExists("youtubeAPIKey")) {
			config.setString("youtubeAPIKey", "");
		}
		if (!config.keyExists("CoeBotTVAPIKey")) {
			config.setString("CoeBotTVAPIKey", "");
		}
		if (!config.keyExists("pusherAppKey")) {
			config.setString("pusherAppKey", "");
		}
		if (!config.keyExists("defaultbullet")) {
			config.setString("defaultbullet", "#!");
		}

		// ********
		defaultBullet = config.getString("defaultbullet");
		nick = config.getString("nick");
		server = config.getString("server");
		port = Integer.parseInt(config.getString("port"));
		localAddress = config.getString("localAddress");
		password = config.getString("password");
		useGUI = config.getBoolean("useGUI");
		monitorPings = config.getBoolean("monitorPings");
		pingInterval = config.getInt("pingInterval");
		publicJoin = config.getBoolean("publicJoin");
		verboseLogging = config.getBoolean("verboseLogging");
		bothelpMessage = config.getString("bothelpMessage");

		wsEnabled = config.getBoolean("wsEnabled");
		wsPort = config.getInt("wsPort");
		wsAdminPassword = config.getString("wsAdminPassword");

		twitchChannels = config.getBoolean("twitchChannels");
		ignoreHistory = config.getBoolean("ignoreHistory");
		multipleTimeout = config.getInt("multipleTimeout");

		randomNickColor = config.getBoolean("randomNickColor");
		randomNickColorDiff = config.getInt("randomNickColorDiff");

		useEventFeed = config.getBoolean("useEventFeed");
		eventFeedURL = config.getString("eventFeedURL");

		// API KEYS

		LastFMAPIKey = config.getString("LastFMAPIKey");
		SteamAPIKey = config.getString("SteamAPIKey");
		krakenOAuthToken = config.getString("krakenOAuthToken");
		krakenClientID = config.getString("krakenClientID");
		YoutubeAPIKey = config.getString("youtubeAPIKey");
		CoeBotTVAPIKey = config.getString("CoeBotTVAPIKey");
		pusherAppKey = config.getString("pusherAppKey");
		// ********

		for (String s : config.getString("channelList").split(",")) {
			System.out.println("DEBUG: Adding channel " + s);
			if (s.length() > 1) {
				channelList.put(s.toLowerCase(), new Channel(s));
			}
		}
		for (String s : config.getString("blockedChannelList").trim()
				.split(",")) {
			System.out.println("DEBUG: Adding channel " + s
					+ " to blocked list.");
			if (s.length() > 1 && s.startsWith("#")) {
				blockedChannelList.add(s.toLowerCase());
			}
		}

		for (String s : config.getString("adminList").split(",")) {
			if (s.length() > 1) {
				admins.add(s.toLowerCase());
			}
		}

		loadEmotes();
		loadGlobalBannedWords();
		loadBanPhraseList();

		if (server.length() < 1) {
			System.exit(1);
		}
	}

	public synchronized Channel getChannel(String channel) {
		if (channelList.containsKey(channel.toLowerCase())) {
			return channelList.get(channel.toLowerCase());
		} else {
			return null;
		}
	}

	public synchronized void addBlockedChannel(String channel) {
		blockedChannelList.add(channel.toLowerCase());
		writeBlockedChannelList();
	}

	public synchronized void removeBlockedChannel(String channel) {

		blockedChannelList.remove(channel.toLowerCase());
		writeBlockedChannelList();
	}

	public synchronized boolean checkChannel(String channel) {
		System.out.println("Checking channel " + channel);
		boolean alreadyIn = channelList.containsKey(channel.toLowerCase());
		System.out.println("Already in: " + alreadyIn);
		boolean blocked = blockedChannelList.contains(channel.toLowerCase());
		System.out.println("Blocked: " + blocked);
		if (blocked || alreadyIn) {
			return true;
		} else {
			return false;
		}

	}

	public synchronized boolean addChannel(String name, int mode) {
		if (channelList.containsKey(name.toLowerCase())) {
			System.out.println("INFO: Already in channel " + name);
			return false;
		}
		if (blockedChannelList.contains(name.toLowerCase())) {
			System.out.println("INFO: Channel is blocked from joining.");
			return false;
		}
		Channel tempChan = new Channel(name.toLowerCase(), mode);

		channelList.put(name.toLowerCase(), tempChan);

		log("BM: Joining channel " + tempChan.getChannel());
		receiverBot.joinChannel(tempChan.getChannel());
		try {
			Thread.sleep(400);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log("BM: Joined channel " + tempChan.getChannel());

		writeChannelList();

		return true;
	}

	public synchronized void removeChannel(String name) {
		if (!channelList.containsKey(name.toLowerCase())) {
			log("BM: Not in channel " + name);
			return;
		}

		Channel tempChan = channelList.get(name.toLowerCase());

		// Stop timers
		System.out.println("Stopping timers");
		Iterator itr = tempChan.commandsRepeat.entrySet().iterator();
		while (itr.hasNext()) {
			Map.Entry pairs = (Map.Entry) itr.next();
			((RepeatCommand) pairs.getValue()).setStatus(false);
		}
		Iterator itr2 = tempChan.commandsSchedule.entrySet().iterator();
		while (itr2.hasNext()) {
			Map.Entry pairs = (Map.Entry) itr2.next();
			((ScheduledCommand) pairs.getValue()).setStatus(false);
		}

		receiverBot.partChannel(name.toLowerCase());
		channelList.remove(name.toLowerCase());

		writeChannelList();
	}

	public synchronized void reloadChannel(String name) {
		if (!channelList.containsKey(name.toLowerCase())) {
			log("BM: Not in channel " + name);
			return;
		}

		channelList.get(name.toLowerCase()).reload();
	}

	public boolean rejoinChannel(String name) {
		if (!channelList.containsKey(name.toLowerCase())) {
			log("BM: Not in channel " + name);
			return false;
		}

		Channel tempChan = channelList.get(name.toLowerCase());
		receiverBot.partChannel(tempChan.getChannel());
		receiverBot.joinChannel(tempChan.getChannel());

		return true;
	}

	public synchronized void reconnectAllBotsSoft() {
		receiverBot.disconnect();
	}

	private synchronized void writeChannelList() {
		String channelString = "";
		for (Map.Entry<String, Channel> entry : channelList.entrySet()) {
			channelString += entry.getKey() + ",";
		}

		config.setString("channelList", channelString);
	}

	private synchronized void writeBlockedChannelList() {
		String blockedChannelString = "";
		for (String s : blockedChannelList) {
			blockedChannelString += s + ",";
		}

		config.setString("blockedChannelList", blockedChannelString);
	}

	public void registerModule(BotModule module) {
		modules.add(module);
	}

	public Set<BotModule> getModules() {
		return modules;
	}

	public BotGUI getGUI() {
		return gui;
	}

	public String getLocalAddress() {
		return localAddress;
	}

	public void sendGlobal(String message, String sender) {
		for (Map.Entry<String, Channel> entry : channelList.entrySet()) {
			Channel tempChannel = (Channel) entry.getValue();
			if (tempChannel.getMode() == 0)
				continue;

			String globalMsg = "> Global: " + message + " (from " + sender
					+ " to " + tempChannel.getChannel() + ")";
			ReceiverBot.getInstance().sendMessage(tempChannel.getChannel(),
					globalMsg);
		}
	}

	public boolean isAdmin(String nick) {
		if (admins.contains(nick.toLowerCase()))
			return true;
		else
			return false;
	}

	public boolean isTagAdmin(String name) {
		return tagAdmins.contains(name.toLowerCase());
	}

	public boolean isTagStaff(String name) {
		return tagStaff.contains(name.toLowerCase());
	}

	public void addTagAdmin(String name) {
		tagAdmins.add(name.toLowerCase());
	}

	public void addTagStaff(String name) {
		tagStaff.add(name.toLowerCase());
	}

	private void loadEmotes() {
		emoteSet.clear();
		emoteSet = JSONUtil.getEmotes();

		System.out.println("Loaded " + emoteSet.size() + " emotes.");
	}

	public void loadGlobalBannedWords() {
		globalBannedWords.clear();
		File f = new File("globalbannedwords.cfg");
		if (!f.exists())
			try {
				f.createNewFile();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		try {
			Scanner in = new Scanner(f, "UTF-8");

			while (in.hasNextLine()) {
				String line = in.nextLine().replace("\uFEFF", "");
				if (line.length() > 0) {

					if (line.startsWith("REGEX:"))
						line = line.replaceAll("REGEX:", "");
					else
						line = ".*" + Pattern.quote(line) + ".*";

					System.out.println(line);
					Pattern tempP = Pattern.compile(line,
							Pattern.CASE_INSENSITIVE);
					globalBannedWords.add(tempP);
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void loadBanPhraseList() {
		banPhraseLists = new HashMap<Integer, List<Pattern>>();

		File f = new File("bannedphrases.cfg");
		if (!f.exists())
			try {
				f.createNewFile();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		try {
			Scanner in = new Scanner(f, "UTF-8");

			while (in.hasNextLine()) {
				String line = in.nextLine().replace("\uFEFF", "");

				if (line.length() > 0) {

					if (line.startsWith("#"))
						continue;

					String[] parts = line.split("\\|", 2);
					int severity = Integer.parseInt(parts[0]);
					line = parts[1];

					if (line.startsWith("REGEX:"))
						line = line.replaceAll("REGEX:", "");
					else
						line = ".*\\b" + Pattern.quote(line) + "\\b.*";

					System.out.println(line);
					Pattern tempP = Pattern.compile(line,
							Pattern.CASE_INSENSITIVE);

					for (int c = severity; c >= 0; c--) {
						if (!banPhraseLists.containsKey(c))
							banPhraseLists.put(c, new LinkedList<Pattern>());
						banPhraseLists.get(c).add(tempP);
						// System.out.println("Adding " + tempP.toString()
						// + " to s=" + c);
					}
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void followChannel(String channel) {
		try {
			System.out.println(BotManager.putRemoteData(
					"https://api.twitch.tv/kraken/users/" + this.nick
							+ "/follows/channels/" + channel, ""));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void addSubBySet(String username, String setID) {
		if (emoteSetMapping.containsKey(setID)) {
			emoteSetMapping.get(setID).add(username);
		} else {
			emoteSetMapping.put(setID, new HashSet<String>());
		}
	}

	public boolean checkEmoteSetMapping(String username, String setID) {
		if (emoteSetMapping.containsKey(setID)) {
			if (emoteSetMapping.get(setID).contains(username))
				return true;
		}

		return false;
	}

	public void log(String line) {
		System.out.println(line);

		if (useGUI) {
			getGUI().log(line);
		}
	}
}
