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
import com.demod.dcba.SlashCommandEvent;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public abstract class Game {

	private static final String JSONKEY_GUILD_GAMES = "games";
	private static final String JSONKEY_GAME_PLAYERSAVES = "player-saves";

	private int id = -1;
	private MessageChannel channel;
	private DiscordBoredGameBot bot;
	private Thread thread;

	private final Map<String, Message> messages = new ConcurrentHashMap<>();

	private String saveKey;

	private <T> Display<T> display(MessageChannel channel) {
		Display<T> display = new Display<T>(d -> {
			String messageKey = channel.getId();
			Message message = messages.get(messageKey);
			if (d.getNotify().size() == 1 && channel.getType() == ChannelType.TEXT) {
				bot.notifyForAction(d.getNotify().iterator().next(), (TextChannel) channel);
			}
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

	public <T> Display<T> displayPrivate(User player) {
		return display(player.openPrivateChannel().complete());
	}

	public <T> Display<T> displayPrivate(User player, Consumer<EmbedBuilder> builder) {
		Display<T> display = displayPrivate(player);
		builder.accept(display.getBuilder());
		return display;
	}

	int getId() {
		return id;
	}

	public Optional<JSONObject> getSave(User player) {
		return Optional.ofNullable(getSaves().get(player));
	}

	public Map<User, JSONObject> getSaves() {
		JSONObject guildJson = GuildSettings.get(saveKey);
		if (guildJson.has(JSONKEY_GUILD_GAMES)) {
			JSONObject gamesJson = guildJson.getJSONObject(JSONKEY_GUILD_GAMES);
			String gameId = getClass().getSimpleName();
			if (gamesJson.has(gameId)) {
				JSONObject gameJson = gamesJson.getJSONObject(gameId);
				if (gameJson.has(JSONKEY_GAME_PLAYERSAVES)) {
					Map<User, JSONObject> ret = new LinkedHashMap<>();
					JSONObject playerSavesJson = gameJson.getJSONObject(JSONKEY_GAME_PLAYERSAVES);
					for (String userId : playerSavesJson.keySet()) {
						User player = channel.getJDA().getUserById(userId);
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

	Thread getThread() {
		return thread;
	}

	protected abstract String getTitle();

	public abstract void run();

	void setId(int id) {
		this.id = id;
	}

	void setInternalInfo(DiscordBoredGameBot bot, SlashCommandEvent e, int id, Thread thread) {
		this.bot = bot;
		if (e.getChannelType() == ChannelType.PRIVATE) {
			this.saveKey = "USER-" + e.getUser().getId();
		} else {
			this.saveKey = e.getGuild().getId();
		}
		this.channel = e.getMessageChannel();
		this.id = id;
		this.thread = thread;
	}

	public void setSave(User player, Optional<JSONObject> playerSave) {
		Map<User, JSONObject> playerSaves = getSaves();
		if (playerSave.isPresent()) {
			playerSaves.put(player, playerSave.get());
		} else {
			playerSaves.remove(player);
		}
		setSaves(playerSaves);
	}

	public void setSaves(Map<User, JSONObject> playerSaves) {
		JSONObject guildJson = GuildSettings.get(saveKey);

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

		for (Entry<User, JSONObject> entry : playerSaves.entrySet()) {
			User player = entry.getKey();
			JSONObject playerSave = entry.getValue();
			playerSavesJson.put(player.getId(), playerSave);
		}

		GuildSettings.save(saveKey, guildJson);
	}

}
