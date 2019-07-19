package fr.rremis.autoubl.ubl;

import org.bukkit.command.CommandExecutor;

public abstract interface IUBLCommand extends CommandExecutor {
	public abstract String getUsage();

	public abstract String getPermission();
}
