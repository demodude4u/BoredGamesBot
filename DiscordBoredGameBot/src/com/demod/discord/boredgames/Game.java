package com.demod.discord.boredgames;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.json.JSONObject;

import com.demod.dcba.GuildSettings;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;

public abstract class Game {

	private static final String GROUP_GLOBAL = "__GLOBAL__";

	private static final String JSONKEY_GUILD_GAMES = "games";
	private static final String JSONKEY_GAME_PLAYERSAVES = "player-saves";

	private int id = -1;
	private TextChannel channel;
	private DiscordBoredGameBot bot;
	private Thread thread;

	private final Map<String, Message> messages = new ConcurrentHashMap<>();

	private <T> Display<T> display(MessageChannel channel) {
		Display<T> display = new Display<T>(d -> {
			String messageKey = channel.getId();
			Message message = messages.get(messageKey);
			Entry<Message, T> entry = bot.waitForDisplay(d, channel, Optional.ofNullable(message));
			messages.put(messageKey, entry.getKey());
			return entry.getValue();
		});
		display.getBuilder().setTitle(getTitle());
		return display;
	}

	public <T> Display<T> displayChannel() {
		return display(channel);
	}

	public <T> Display<T> displayChannel(Consumer<EmbedBuilder> builder) {
		Display<T> display = displayChannel();
		builder.accept(display.getBuilder());
		return display;
	}

	public <T> Display<T> displayPrivate(Member player) {
		return display(player.getUser().openPrivateChannel().complete());
	}

	public <T> Display<T> displayPrivate(Member player, Consumer<EmbedBuilder> builder) {
		Display<T> display = displayPrivate(player);
		builder.accept(display.getBuilder());
		return display;
	}

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
						Member player = channel.getGuild().getMemberById(userId);
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

	public Optional<JSONObject> getGuildSave(Member player) {
		return getGroupSave(channel.getGuild().getId(), player);
	}

	public Map<Member, JSONObject> getGuildSaves() {
		return getGroupSaves(channel.getGuild().getId());
	}

	int getId() {
		return id;
	}

	Thread getThread() {
		return thread;
	}

	protected abstract String getTitle();

	public abstract void run();

	public void setGlobalSave(Member player, Optional<JSONObject> playerSave) {
		setGroupSave(GROUP_GLOBAL, player, playerSave);
	}

	public void setGlobalSaves(Map<Member, JSONObject> playerSaves) {
		setGroupSaves(channel.getGuild().getId(), playerSaves);
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

	public void setGuildSave(Member player, Optional<JSONObject> playerSave) {
		setGroupSave(channel.getGuild().getId(), player, playerSave);
	}

	public void setGuildSaves(Map<Member, JSONObject> playerSaves) {
		setGroupSaves(channel.getGuild().getId(), playerSaves);
	}

	void setId(int id) {
		this.id = id;
	}

	void setInternalInfo(DiscordBoredGameBot bot, TextChannel channel, int id, Thread thread) {
		this.bot = bot;
		this.channel = channel;
		this.id = id;
		this.thread = thread;
	}

}
