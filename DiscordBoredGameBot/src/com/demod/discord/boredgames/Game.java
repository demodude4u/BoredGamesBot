package com.demod.discord.boredgames;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.json.JSONObject;

import com.demod.dcba.GuildSettings;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;

public abstract class Game {
	@FunctionalInterface
	public interface Action {
		void apply(Member player);
	}

	@FunctionalInterface
	public interface ActionRegistry {
		void addAction(String emoji, Action action);

		default void addAction(String emoji, PlayerlessAction action) {
			addAction(emoji, (Action) action);
		}

		default void addExclusiveAction(Member player, String emoji, Action action) {
			addAction(emoji, p -> {
				if (p.equals(player)) {
					action.apply(p);
				}
			});
		}
	}

	@FunctionalInterface
	public interface PlayerlessAction extends Action {
		void apply();

		@Override
		default void apply(Member player) {
			apply();
		}
	}

	private static final String GROUP_GLOBAL = "__GLOBAL__";

	private static final String JSONKEY_GUILD_GAMES = "games";
	private static final String JSONKEY_GAME_PLAYERSAVES = "player-saves";

	private Guild guild;

	public abstract boolean allowUndo();

	public abstract void buildDisplay(EmbedBuilder embed);

	public abstract Game copy();

	public Optional<JSONObject> getGlobalSave(Member player) {
		return getGroupSave(GROUP_GLOBAL, player);
	}

	public Map<Member, JSONObject> getGlobalSaves() {
		return getGroupSaves(GROUP_GLOBAL);
	}

	public Optional<JSONObject> getGroupSave(String group, Member player) {
		return Optional.ofNullable(getGroupSaves(group).get(player));
	}

	public Map<Member, JSONObject> getGroupSaves(String group) {
		JSONObject guildJson = GuildSettings.get(group);
		if (guildJson.has(JSONKEY_GUILD_GAMES)) {
			JSONObject gamesJson = guildJson.getJSONObject(JSONKEY_GUILD_GAMES);
			String gameId = getClass().getSimpleName();
			if (gamesJson.has(gameId)) {
				JSONObject gameJson = gamesJson.getJSONObject(gameId);
				if (gameJson.has(JSONKEY_GAME_PLAYERSAVES)) {
					Map<Member, JSONObject> ret = new LinkedHashMap<>();
					JSONObject playerSavesJson = gameJson.getJSONObject(JSONKEY_GAME_PLAYERSAVES);
					for (String userId : playerSavesJson.keySet()) {
						Member player = guild.getMemberById(userId);
						JSONObject playerSave = playerSavesJson.getJSONObject(userId);
						if (player != null) {
							ret.put(player, playerSave);
						}
					}
					return ret;
				}
			}
		}
		return new HashMap<>();
	}

	public Guild getGuild() {
		return guild;
	}

	public Optional<JSONObject> getGuildSave(Member player) {
		return getGroupSave(guild.getId(), player);
	}

	public Map<Member, JSONObject> getGuildSaves() {
		return getGroupSaves(guild.getId());
	}

	public abstract boolean isGameOver();

	public abstract void registerActions(ActionRegistry registry);

	public void setGlobalSave(Member player, Optional<JSONObject> playerSave) {
		setGroupSave(GROUP_GLOBAL, player, playerSave);
	}

	public void setGlobalSaves(Map<Member, JSONObject> playerSaves) {
		setGroupSaves(guild.getId(), playerSaves);
	}

	public void setGroupSave(String group, Member player, Optional<JSONObject> playerSave) {
		Map<Member, JSONObject> playerSaves = getGroupSaves(group);
		if (playerSave.isPresent()) {
			playerSaves.put(player, playerSave.get());
		} else {
			playerSaves.remove(player);
		}
		setGroupSaves(group, playerSaves);
	}

	public void setGroupSaves(String group, Map<Member, JSONObject> playerSaves) {
		JSONObject guildJson = GuildSettings.get(group);

		JSONObject gamesJson;
		if (guildJson.has(JSONKEY_GUILD_GAMES)) {
			gamesJson = guildJson.getJSONObject(JSONKEY_GUILD_GAMES);
		} else {
			guildJson.put(JSONKEY_GUILD_GAMES, gamesJson = new JSONObject());
		}

		JSONObject gameJson;
		String gameId = getClass().getSimpleName();
		if (gamesJson.has(gameId)) {
			gameJson = gamesJson.getJSONObject(gameId);
		} else {
			gamesJson.put(gameId, gameJson = new JSONObject());
		}

		JSONObject playerSavesJson;
		if (gameJson.has(JSONKEY_GAME_PLAYERSAVES)) {
			playerSavesJson = gameJson.getJSONObject(JSONKEY_GAME_PLAYERSAVES);
		} else {
			gameJson.put(JSONKEY_GAME_PLAYERSAVES, playerSavesJson = new JSONObject());
		}

		for (Entry<Member, JSONObject> entry : playerSaves.entrySet()) {
			Member player = entry.getKey();
			JSONObject playerSave = entry.getValue();
			playerSavesJson.put(player.getUser().getId(), playerSave);
		}

		GuildSettings.save(group, guildJson);
	}

	void setGuild(Guild guild) {
		this.guild = guild;
	}

	public void setGuildSave(Member player, Optional<JSONObject> playerSave) {
		setGroupSave(guild.getId(), player, playerSave);
	}

	public void setGuildSaves(Map<Member, JSONObject> playerSaves) {
		setGroupSaves(guild.getId(), playerSaves);
	}
}
