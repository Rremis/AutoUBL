package fr.rremis.autoubl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import fr.rremis.autoubl.ubl.BanEntry;
import fr.rremis.autoubl.ubl.BanlistUpdater;
import fr.rremis.autoubl.ubl.CSVReader;
import fr.rremis.autoubl.ubl.LoginListener;
import fr.rremis.autoubl.ubl.StringTemplate;

public class AutoUBL extends JavaPlugin implements CommandExecutor {
	
	private static AutoUBL instance;
	
	@Override
	public void onEnable(){
		instance = this;
		
		System.out.println("[AutoUBL] Loading UBL");
		this.banlistUpdater = new BanlistUpdater();
		reload();
		
		getCommand("ubl").setExecutor(this);
		Bukkit.getServer().getPluginManager().registerEvents(new LoginListener(), this);
		
		System.out.println("[AutoUBL] Plugin activated");
	}
	
	@Override
	public void onDisable(){
		System.out.println("[AutoUBL] Plugin disabled");
	}

	
	public static AutoUBL getInstance(){
		return instance;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String lab, String[] args) {

		if (cmd.getName().equalsIgnoreCase("ubl")) {
			sender.sendMessage(ChatColor.GRAY+"["+ChatColor.YELLOW+"AutoUBL"+ChatColor.GRAY+"] "+ChatColor.GREEN+"UBL is activated");
			return true;
		}
		return true;
	}

	/* UBL SETUP */
	
	private BanlistUpdater banlistUpdater;
	private Map<String, BanEntry> banlistByIGN;
	private Map<UUID, BanEntry> banlistByUUID;
	private Set<String> exempt;
	private StringTemplate banMessageTemplate;
	
	public void reload(){
		this.banMessageTemplate = StringTemplate.getStringTemplate("{Reason} - {Courtroom Post}");
	    this.exempt = new HashSet<>();
	    this.updateBanlist();
	}

	public void updateBanlist() { this.banlistUpdater.load(); }

	public boolean isBanned(String ign){	
	    String lname = ign.toLowerCase();
	    return (this.banlistByIGN.containsKey(lname)) && (!this.exempt.contains(lname));
	}

	public boolean isBanned(String ign, UUID uuid){ return (this.banlistByUUID.containsKey(uuid)) && (!this.exempt.contains(ign.toLowerCase()));}

	public String getBanMessage(String ign){
	    BanEntry banEntry = (BanEntry)this.banlistByIGN.get(ign.toLowerCase());
	    
	    if (banEntry == null) {
	    	return "Not defines";
	    }
	    return this.banMessageTemplate.format(banEntry.getData());
	}

	public String getBanMessage(UUID uuid){
	    BanEntry banEntry = (BanEntry)this.banlistByUUID.get(uuid);
	    if (banEntry == null) {
	    	return "Not defines";
	    }
	    return this.banMessageTemplate.format(banEntry.getData());
	}

	public boolean isReady(){
	    return (this.banlistByIGN != null) && (this.banlistByUUID != null) && ((!this.banlistByIGN.isEmpty()) || (!this.banlistByUUID.isEmpty()));
	}

	public boolean isUUIDReady(){ return (this.banlistByUUID != null) && (!this.banlistByUUID.isEmpty()); }

	public void setBanList(String fieldNamesCSV, List<String> banlist){
	    String[] fieldNames = CSVReader.parseLine(fieldNamesCSV);
	    if (!Arrays.asList(fieldNames).contains(getIGNFieldName())) {
	    	getLogger().warning("There is no matching IGN field (" + getIGNFieldName() + ") in the ban-list data. Please check the UBL spreadsheet and set 'fields.ign' in your config.yml to the correct field name");
	    	getServer().broadcast("[AutoUBL] No IGN field found in the ban-list data. If you also have no UUID field then your server will be locked to non-ops for your protection. Please see your server logs for details in how to fix this issue", "bukkit.op");
	    }
	    if (!Arrays.asList(fieldNames).contains(getUUIDFieldName())) {
	    	getLogger().warning("There is no matching UUID field (" + getUUIDFieldName() + ") in the ban-list data. Please check the UBL spreadsheet and set 'fields.uuid' in your config.yml to the correct field name");
	    	getServer().broadcast("[AutoUBL] No UUID field found in the ban-list data. If Mojang has not yet allowed name-changing, this is not a problem. Otherwise, please check your server logs for details on how to fix this issue", "bukkit.op");
	    }	
	    this.banlistByIGN = new HashMap<>();
	    this.banlistByUUID = new HashMap<>();
	    for (String rawCSV : banlist) {
	    	BanEntry banEntry = new BanEntry(fieldNames, rawCSV);
	    	String ign = banEntry.getData(getIGNFieldName());
	    	if (ign != null) {
	    		this.banlistByIGN.put(ign.toLowerCase(), banEntry);
	    		banEntry.setIgn(ign);
	    	}
	    	String uuidString = banEntry.getData(getUUIDFieldName()).trim();
	    	if (uuidString != null) {
	    		if (uuidString.length() == 32) {
	    			StringBuilder sb = new StringBuilder();
	    			sb.append(uuidString.substring(0, 8)).append('-');
	    			sb.append(uuidString.substring(8, 12)).append('-');
	    			sb.append(uuidString.substring(12, 16)).append('-');
	    			sb.append(uuidString.substring(16, 20)).append('-');
	    			sb.append(uuidString.substring(20, 32));
	    			uuidString = sb.toString();
	    		}
	    		if (uuidString.length() == 36) {
	    			UUID uuid = UUID.fromString(uuidString);
	    			this.banlistByUUID.put(uuid, banEntry);
	    			banEntry.setUUID(uuid);
	    		} else {
	    			getLogger().warning("Invalid UUID in ban-list for " + ign + ": " + uuidString);
	    		}
	    	}
	    }
	}

	public String getIGNFieldName(){
	    String ignFieldName = getConfig().getString("fields.ign", null);
	    if ((ignFieldName == null) || (ignFieldName.isEmpty())) {
	    	ignFieldName = "IGN";
	    	getConfig().set("fields.ign", "IGN");
	    	saveConfig();
	    }
	    return ignFieldName;
	}

	public String getUUIDFieldName(){
	    String uuidFieldName = getConfig().getString("fields.uuid", null);
	    if ((uuidFieldName == null) || (uuidFieldName.isEmpty())) {
	    	uuidFieldName = "UUID";
	    	getConfig().set("fields.uuid", "UUID");
	    	saveConfig();
	    }
	    return uuidFieldName;
	}
	
}
