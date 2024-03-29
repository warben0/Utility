package main.java.utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;

import com.palmergames.bukkit.TownyChat.config.ChatSettings;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.chat.Chat;

@SuppressWarnings("deprecation")
public class EventSetTab implements Listener {

	private Main plugin;

	public EventSetTab(Main plugin) {
		this.plugin = plugin;
	}

	String header, footer, player;

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		if (plugin.getConfig().getBoolean("tab.organize")
				|| plugin.getConfig().getBoolean("gamer-tag-add-prefix.enabled")) {
			setWeight(p);
		}
		if (plugin.getConfig().getBoolean("tab.enabled")) {
			for (Player subP : Bukkit.getOnlinePlayers()) {
				updateTab(subP);
			}
			new BukkitRunnable() {
				@Override
				public void run() {
					if (p.isOnline())
						updateTab(p);
					else
						cancel();
				}
			}.runTaskTimer(plugin, 0L, 200L);
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (plugin.getConfig().getBoolean("tab.enabled")) {
			for (Player subP : Bukkit.getOnlinePlayers()) {
				updateTab(subP);
			}
			Player p = event.getPlayer();
			UUID pu = p.getUniqueId();
			Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
			for (Team t : scoreboard.getTeams()) {
				if (t.getName().contains(pu.toString().substring(0, 8))) {
					t.unregister();
				}
			}
		}
	}

	public void updateTab(Player p) {
		header = replaceVariables(plugin.getConfig().getString("tab.header"), p);
		footer = replaceVariables(plugin.getConfig().getString("tab.footer"), p);
		player = replaceVariables(plugin.getConfig().getString("tab.username"), p);
		p.setPlayerListHeader(header);
		p.setPlayerListName(player);
		p.setPlayerListFooter(footer);
	}

	public String replaceVariables(String replaceString, Player p) {
		Chat c = Main.getChat();
		String[] pGroups = c.getPlayerGroups(p);
		try {
			int pCount = 0;
			for (Player plr : Bukkit.getOnlinePlayers()) {
				if (!Main.getEss().getUser(plr).isVanished()) {
					pCount++;
				}
			}
			replaceString = replaceString.replace("{PLAYERS}", pCount + "");
		} catch (Exception notUsingEss) {
		}
		replaceString = replaceString.replace("{MAXPLAYERS}", plugin.getServer().getMaxPlayers() + "")
				.replace("{PLAYERS}", plugin.getServer().getOnlinePlayers().size() + "")
				.replace("{USERNAME}", p.getName()).replace("{DISPLAYNAME}", p.getDisplayName());
		try {
			if (Main.usingTowny) {
				Resident r = TownyUniverse.getDataSource().getResident(p.getName());
				replaceString = replaceString.replace("{TOWNYCOLOUR}", r.isKing() ? ChatSettings.getKingColour()
						: (r.isMayor() ? ChatSettings.getMayorColour() : ChatSettings.getResidentColour()));
			}
		} catch (Exception noTowny) {
			replaceString = replaceString.replace("{TOWNYCOLOUR}", "");
		}
		replaceString = (c.getPlayerPrefix(p) != null ? replaceString.replace("{PREFIX}", c.getPlayerPrefix(p))
				: replaceString.replace("{PREFIX}", c.getGroupPrefix(p.getWorld(), pGroups[0])));
		replaceString = (c.getPlayerSuffix(p) != null ? replaceString.replace("{SUFFIX}", c.getPlayerSuffix(p))
				: replaceString.replace("{SUFFIX}", c.getGroupSuffix(p.getWorld(), pGroups[0])));
		return ChatColor.translateAlternateColorCodes('&', replaceString);
	}

	public void setWeight(Player p) {
		Team pTeam;
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		String toUse = "_";
		ArrayList<String> pto = new ArrayList<String>();
		Iterator<PermissionAttachmentInfo> iterator = p.getEffectivePermissions().iterator();
		while (iterator.hasNext()) {
			String perm = iterator.next().getPermission();
			if (perm.contains("wmctab")) {
				pto.add(perm.replace("wmctab.", ""));
			}
		}
		Collections.sort(pto);
		toUse = pto.get(0);
		Chat c = Main.getChat();
		String puuidsub = p.getUniqueId().toString().substring(0, 8);
		try {
			pTeam = scoreboard.registerNewTeam(toUse + "-" + puuidsub);
			if (plugin.getConfig().getBoolean("gamer-tag-add-prefix.enabled")) {
				pTeam.setPrefix(ChatColor.translateAlternateColorCodes('&', c.getPlayerPrefix(p)));
				if (c.getPlayerPrefix(p) == "") {
					pTeam.setPrefix(c.getGroupPrefix(p.getWorld(), c.getPlayerGroups(p)[0]));
				}
			}
			
			pTeam.setOption(Option.COLLISION_RULE,
					OptionStatus.valueOf(plugin.getConfig().getString("teams.collisionRule")));
			pTeam.setOption(Option.DEATH_MESSAGE_VISIBILITY,
					OptionStatus.valueOf(plugin.getConfig().getString("teams.deathMessageVisibility")));
			pTeam.setOption(Option.NAME_TAG_VISIBILITY,
					OptionStatus.valueOf(plugin.getConfig().getString("teams.nametagVisibility")));

			pTeam.addEntry(p.getName());
		} catch (Exception ignore) {
		}
	}
}
