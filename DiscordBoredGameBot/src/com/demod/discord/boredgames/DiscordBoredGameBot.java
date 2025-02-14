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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.demod.dcba.CommandReporting;
import com.demod.dcba.DCBA;
import com.demod.dcba.DiscordBot;
import com.demod.dcba.SlashCommandEvent;
import com.demod.discord.boredgames.Display.ActionButton;
import com.demod.discord.boredgames.Display.ResultAction;
import com.demod.discord.boredgames.game.Connect4Game;
import com.demod.discord.boredgames.game.DominionGame;
import com.demod.discord.boredgames.game.YahtzeeGame;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.AbstractScheduledService;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;

public class DiscordBoredGameBot extends AbstractScheduledService {
	private static final int NOTIFY_MINUTES = 5;

	private static ExecutorService executor = Executors.newCachedThreadPool();

	public static void main(String[] args) {
		new DiscordBoredGameBot().startAsync().awaitTerminated();
	}

	private final DiscordBot bot;

	// Key = Game ID
	private final LinkedHashMap<Integer, Game> games = new LinkedHashMap<>();
	private final AtomicInteger nextGameId = new AtomicInteger(0);

	// Key = Message ID
	private final Map<String, Consumer<ButtonInteractionEvent>> awaitingActions = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<User, Entry<TextChannel, Long>> notifyMemberMillis = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<User, Long> memberLastActionMillis = new ConcurrentHashMap<>();

	public DiscordBoredGameBot() {
		bot = DCBA.builder()//
				.setInfo("Bored Game Bot")//
				.withSupport("Message Demod for help or any found bugs!")//
				//
				.addSlashCommand("connect4", "Start a game of Connect Four. (2-4 Players)",
						e -> startGame(e, new Connect4Game()))//
				.addSlashCommand("yahtzee", "Start a game of Yahtzee! (1 Player)", e -> startGame(e, new YahtzeeGame()))//
				.addSlashCommand("dominion", "Start a game of Dominion! (2-4 Players)",
						e -> startGame(e, new DominionGame()))//
				//
				.addButtonHandler(this::onAction)//
				//
				.create();
	}

	private void checkAndNotifyMembers() {
		long nowMillis = System.currentTimeMillis();
		Iterables.removeIf(notifyMemberMillis.entrySet(), entry -> {
			User player = entry.getKey();
			TextChannel channel = entry.getValue().getKey();
			long notifyMillis = entry.getValue().getValue();
			if (nowMillis > notifyMillis) {
				try {
					player.openPrivateChannel().complete()
							.sendMessage("Hi! It is your turn! ==> " + channel.getAsMention()).complete();
					System.out.println("Notified " + player.getEffectiveName() + " to take their turn.");
				} catch (Exception e) {
					e.printStackTrace();
				}
				return true;
			}
			return false;
		});
	}

	public void notifyForAction(User player, TextChannel channel) {
		Optional<Long> lastActionMillis = Optional.ofNullable(memberLastActionMillis.get(player));
		notifyMemberMillis.put(player, new SimpleImmutableEntry<>(channel,
				lastActionMillis.orElse(System.currentTimeMillis()) + TimeUnit.MINUTES.toMillis(NOTIFY_MINUTES)));
		checkAndNotifyMembers();
	}

	private synchronized void onAction(ButtonInteractionEvent e, CommandReporting reporting) {
		String id = e.getMessage().getId();
		if (awaitingActions.containsKey(id)) {
			Consumer<ButtonInteractionEvent> consumer = awaitingActions.get(id);
			if (e.getChannelType() == ChannelType.TEXT) {
				notifyMemberMillis.remove(e.getUser());
				memberLastActionMillis.put(e.getUser(), System.currentTimeMillis());
			}
			executor.submit(() -> {
				consumer.accept(e);
			});
		}
		e.deferEdit().complete();
	}

	private Optional<Message> reloadMessage(Message message) {
		try {
			return Optional.ofNullable(message.getChannel().retrieveMessageById(message.getId()).complete());
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	@Override
	protected void runOneIteration() throws Exception {
		checkAndNotifyMembers();
	}

	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedDelaySchedule(1, 1, TimeUnit.MINUTES);
	}

	@Override
	protected void shutDown() {
		bot.stopAsync();
		games.values().forEach(g -> g.getThread().interrupt());
	}

	private synchronized void startGame(SlashCommandEvent e, Game game) {
		int gameId = nextGameId.getAndIncrement();
		Thread thread = new Thread(() -> {
			try {
				games.put(gameId, game);
				game.setInternalInfo(this, e, gameId, Thread.currentThread());
				game.run();
			} catch (Throwable ex) {
				ex.printStackTrace();
				System.err.println("GAME IS KILL :(");
				throw ex;
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
	}

	public <T> Entry<Message, T> waitForDisplay(Display<T> display, MessageChannel channel, Optional<Message> message) {
		display.getBuilder().setTimestamp(Instant.now());

		MessageEmbed messageEmbed = display.getBuilder().build();

		if (message.isPresent()) {
			message = reloadMessage(message.get());
		}

		if (message.isPresent() && (!message.get().getChannel().getId().equals(channel.getId())
				|| channel.getType() == ChannelType.PRIVATE)) {
			message.get().delete().complete();
			message = Optional.empty();
		}

		List<LayoutComponent> actionRows = display.getActionRows().stream()
				.map(l -> ActionRow.of(l.stream().map(b -> b.button).collect(Collectors.toList())))
				.filter(l -> !l.isEmpty()).collect(Collectors.toList());

		if (message.isPresent()) {
			MessageEditAction editAction = message.get().getChannel().editMessageEmbedsById(message.get().getId(),
					messageEmbed);
			if (!actionRows.isEmpty()) {
				editAction = editAction.setComponents(actionRows);
			}
			message = Optional.ofNullable(editAction.complete());
		} else {
			MessageCreateAction createAction = channel.sendMessageEmbeds(messageEmbed);
			if (!actionRows.isEmpty()) {
				createAction = createAction.setComponents(actionRows);
			}
			message = Optional.of(createAction.complete());
		}

		CompletableFuture<T> awaitor = new CompletableFuture<>();

		LinkedHashMap<String, ActionButton<T>> actions = display.getActions();
		if (!actions.isEmpty()) {
			awaitingActions.put(message.get().getId(), e -> {
				if (!actions.containsKey(e.getComponentId())) {
					return;
				}

				ResultAction<T> action = actions.get(e.getComponentId()).action;
				if (!action.accept(e.getUser())) {
					return;
				}

				try {
					T result = action.call(e.getUser());
					awaitor.complete(result);
				} catch (Exception e1) {
					awaitor.completeExceptionally(e1);
					System.out.println("ACTION EXCEPTIONED - " + e1.getMessage());
				}
			});
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
