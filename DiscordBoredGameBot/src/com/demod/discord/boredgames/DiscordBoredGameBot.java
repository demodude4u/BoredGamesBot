package com.demod.discord.boredgames;

import java.time.Instant;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.dcba.DCBA;
import com.demod.dcba.DiscordBot;
import com.demod.dcba.ReactionWatcher;
import com.demod.discord.boredgames.Display.ResultAction;
import com.demod.discord.boredgames.game.Connect4Game;
import com.demod.discord.boredgames.game.DominionGame;
import com.demod.discord.boredgames.game.YahtzeeGame;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.AbstractIdleService;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
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

	// Key = Game ID
	private final LinkedHashMap<Integer, Game> games = new LinkedHashMap<>();

	private final AtomicInteger nextGameId = new AtomicInteger(0);

	// Key = Message ID
	private final Map<String, Consumer<GenericMessageReactionEvent>> awaitingReactions = new ConcurrentHashMap<>();

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
				.withHelp("Start a game of Yahtzee! (1 Player)")//
				.addCommand("dominion", (e) -> startGame(e.getTextChannel(), new DominionGame()))//
				.withHelp("Start a game of Dominion! (2-4 Players)")//
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

	private synchronized void onAction(GenericMessageReactionEvent e) {
		if (e.getUser().isBot()) {
			return;
		}

		Optional.ofNullable(awaitingReactions.get(e.getMessageId())).ifPresent(c -> c.accept(e));
	}

	private Optional<Message> reloadMessage(Message message) {
		try {
			return Optional.ofNullable(message.getChannel().getMessageById(message.getId()).complete());
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	@Override
	protected void shutDown() {
		bot.stopAsync();
		games.values().forEach(g -> g.getThread().interrupt());
	}

	private synchronized void startGame(TextChannel channel, Game game) {
		if (!channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_MANAGE)) {
			channel.sendMessage(
					"Insufficient permissions to start game! I Need `Manage Messages` to delete old reactions.")
					.complete();
			return;
		}
		if (!channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_ADD_REACTION)) {
			channel.sendMessage(
					"Insufficient permissions to start game! I Need `Add Reactions` to create action buttons.")
					.complete();
			return;
		}
		if (!channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)) {
			channel.sendMessage("Insufficient permissions to start game! I Need `Embed Links` to create game displays.")
					.complete();
			return;
		}

		int gameId = nextGameId.getAndIncrement();
		Thread thread = new Thread(() -> {
			try {
				games.put(gameId, game);
				game.setInternalInfo(this, channel, gameId, Thread.currentThread());
				game.run();
			} catch (Throwable e) {
				System.err.println("GAME IS KILL :(");
				e.printStackTrace();
				throw e;
			} finally {
				System.out.println("GAME OVER: " + game.getClass().getSimpleName() + " #" + gameId);
				games.remove(gameId);
			}
		});
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	protected void startUp() {
		bot.startAsync().awaitRunning();
		System.out.println(bot.getJDA().asBot().getInviteUrl());
	}

	public <T> Entry<Message, T> waitForDisplay(Display<T> display, MessageChannel channel, Optional<Message> message) {
		display.getBuilder().setTimestamp(Instant.now());

		MessageEmbed messageEmbed = display.getBuilder().build();

		if (message.isPresent()) {
			message = reloadMessage(message.get());
		}

		if (message.isPresent() && !message.get().getChannel().getId().equals(channel.getId())) {
			message.get().delete().complete();
			message = Optional.empty();
		}

		if (message.isPresent()) {
			message = Optional.ofNullable(
					message.get().getChannel().editMessageById(message.get().getId(), messageEmbed).complete());
		} else {
			message = Optional.of(channel.sendMessage(messageEmbed).complete());
		}

		LinkedHashMap<String, ResultAction<T>> actions = display.getActions();
		List<String> lastActions = message.map(
				m -> m.getReactions().stream().map(r -> r.getReactionEmote().getName()).collect(Collectors.toList()))
				.orElseGet(ImmutableList::of);
		if (!Iterables.elementsEqual(lastActions, actions.keySet())) {
			message.get().clearReactions().complete();
			message = reloadMessage(message.get());
		}

		CompletableFuture<T> awaitor = new CompletableFuture<>();

		if (!actions.isEmpty()) {
			awaitingReactions.put(message.get().getId(), e -> {
				if (!actions.containsKey(e.getReactionEmote().getName())) {
					return;
				}

				ResultAction<T> action = actions.get(e.getReactionEmote().getName());
				if (!action.accept(e.getMember())) {
					return;
				}

				try {
					T result = action.call(e.getMember());
					awaitor.complete(result);
				} catch (Exception e1) {
					awaitor.completeExceptionally(e1);
				}
			});

			for (String emoji : actions.keySet()) {
				message.get().addReaction(emoji).complete();
			}
		} else {
			awaitor.complete(null);
		}

		try {
			T result = awaitor.get(1, TimeUnit.DAYS);
			return new SimpleImmutableEntry<>(message.get(), result);
		} catch (InterruptedException | TimeoutException e) {
			throw new RuntimeException(e);// XXX Probably a better way
		} catch (ExecutionException e) {
			e.getCause().printStackTrace();
		}

		return new SimpleImmutableEntry<>(message.get(), null);
	}

}
