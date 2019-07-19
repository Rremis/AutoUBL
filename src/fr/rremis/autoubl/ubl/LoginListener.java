package fr.rremis.autoubl.ubl;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import fr.rremis.autoubl.AutoUBL;

public class LoginListener implements Listener {
	

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onLogin(AsyncPlayerPreLoginEvent event) {
		if (!AutoUBL.getInstance().isReady()) {
			event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
					"UBL is not ready!");
			return;
		}
		if (AutoUBL.getInstance().isUUIDReady()) {
			try {
				if (AutoUBL.getInstance().isBanned(event.getName(), event.getUniqueId())) {
					event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
							AutoUBL.getInstance().getBanMessage(event.getUniqueId()));
				}
				return;
			} catch (NoSuchMethodError ex) {
				AutoUBL.getInstance().getLogger()
						.warning(
								"This server is outdated so we are forced to fall back on a slow inefficient method fetcher player UUIDs from Mojangs API server. Please consider updating to at least the latest CB 1.7.5-R0.1-SNAPSHOT or later");
				try {
					String ign = event.getName();
					UUID uuid = UUIDFetcher.getUUIDOf(ign);
					if (AutoUBL.getInstance().isBanned(ign, uuid)) {
						event.disallow(
								AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
								AutoUBL.getInstance().getBanMessage(uuid));
					}
				} catch (Exception ex1) {
					AutoUBL.getInstance().getLogger().log(Level.WARNING,
							"Failed to lookup UUID of " + event.getName(), ex1);
					event.disallow(
							AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
							"Authentication failed due to "
									+ ex1.getLocalizedMessage());
				}
				return;
			}
		}
		if (AutoUBL.getInstance().isBanned(event.getName())) {
			event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
					AutoUBL.getInstance().getBanMessage(event.getName()));
		}
	}
}
