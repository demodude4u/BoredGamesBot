package com.demod.discord.boredgames;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.demod.dcba.DCBA;
import com.demod.dcba.DiscordBot;
import com.demod.dcba.ReactionWatcher;
import com.demod.discord.boredgames.Game.Action;
import com.demod.discord.boredgames.game.Connect4Game;
import com.demod.discord.boredgames.game.YahtzeeGame;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.AbstractIdleService;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;

public class DiscordBoredGameBot extends AbstractIdleService {

	public static void main(String[] args) {
		new DiscordBoredGameBot().startAsync().awaitTerminated();
	}

	private final DiscordBot bot;

	/* String = Channel ID */
	private final Multimap<String, GameSession> games = ArrayListMultimap.create();

	public DiscordBoredGameBot() {
		bot = DCBA.builder()//
				.ignorePrivateChannels()//
				//
				.setInfo("Bored Game Bot")//
				.withSupport("Message Demod for help or any found bugs!")//
				.withInvite(Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EMBED_LINKS,
						Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_MANAGE)
				//
				.addCommand("connect4", (e) -> startGame(e.getTextChannel(), new Connect4Game()))//
				.withHelp("Start a game of Connect Four. (2-4 Players)")//
				.addCommand("yahtzee", (e) -> startGame(e.getTextChannel(), new YahtzeeGame()))//
				.withHelp("Start a game of Yahtzee! (1 Player)")// TODO multi
				//
				.addReactionWatcher(new ReactionWatcher() {
					@Override
					public void seenReaction(MessageReactionAddEvent event) {
						onAction(event);
					}

					@Override
					public void seenReactionRemoved(MessageReactionRemoveEvent event) {
						onAction(event);
					}
				})//
					//
				.create();
	}

	private synchronized void addActionReactions(Message message, LinkedHashMap<String, Action> gameActions) {
		for (String emoji : gameActions.keySet()) {
			message.addReaction(emoji).complete();
		}
	}

	private synchronized void onAction(GenericMessageReactionEvent e) {
		if (e.getUser().isBot()) {
			return;
		}

		for (GameSession session : games.get(e.getChannel().getId())) {
			if (!session.getDisplay().isPresent() || !e.getMessageId().equals(session.getDisplay().get().getId())
					|| !session.getActions().isPresent()) {
				continue;
			}

			Map<String, Action> gameActions = session.getActions().get();

			if (gameActions.containsKey(e.getReactionEmote().getName())) {
				Action action = gameActions.get(e.getReactionEmote().getName());
				action.apply(e.getMember());

				updateGameMessage(e.getTextChannel(), session);
			}
		}

		games.get(e.getChannel().getId()).removeIf(s -> !s.getActions().isPresent()
				|| (System.currentTimeMillis() - s.getLastUpdateMillis() > TimeUnit.DAYS.toMillis(1)));
	}

	@Override
	protected void shutDown() {
		bot.stopAsync();
	}

	private void startGame(TextChannel channel, Game game) {
		game.setGuild(channel.getGuild());
		GameSession session = new GameSession(game);
		games.put(channel.getId(), session);
		updateGameMessage(channel, session);
	}

	@Override
	protected void startUp() {
		bot.startAsync().awaitRunning();
		System.out.println(bot.getJDA().asBot().getInviteUrl());
	}

	private synchronized void updateGameMessage(TextChannel channel, GameSession session) {
		EmbedBuilder embedBuilder = new EmbedBuilder();
		embedBuilder.setTimestamp(Instant.now());
		session.setLastUpdateMillis();

		session.getGame().buildDisplay(embedBuilder);

		MessageEmbed messageEmbed = embedBuilder.build();

		LinkedHashMap<String, Action> lastGameActions = session.getActions().orElseGet(LinkedHashMap::new);
		LinkedHashMap<String, Action> gameActions = new LinkedHashMap<>();
		if (!session.getGame().isGameOver()) {
			session.getGame().registerActions((emoji, action) -> {
				gameActions.put(emoji, action);
			});
			session.setActions(Optional.of(gameActions));
		} else {
			session.setActions(Optional.empty());
		}

		if (session.getDisplay().isPresent()
				&& Iterables.elementsEqual(lastGameActions.keySet(), gameActions.keySet())) {
			channel.editMessageById(session.getDisplay().get().getId(), messageEmbed).complete();

		} else if (session.getDisplay().isPresent()
				&& channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_MANAGE)) {
			Message message = session.getDisplay().get();
			message.clearReactions().complete();
			channel.editMessageById(message.getId(), messageEmbed).complete();
			if (!session.getGame().isGameOver()) {
				addActionReactions(message, session.getActions().get());
			}

		} else {
			if (session.getDisplay().isPresent()) {
				session.getDisplay().get().delete().complete();
			}
			Message message = channel.sendMessage(messageEmbed).complete();
			session.setDisplay(Optional.of(message));
			if (!session.getGame().isGameOver()) {
				addActionReactions(message, session.getActions().get());
			}
		}
	}

}
