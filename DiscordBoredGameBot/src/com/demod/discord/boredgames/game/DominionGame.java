package com.demod.discord.boredgames.game;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.demod.discord.boredgames.Display;
import com.demod.discord.boredgames.Emojis;
import com.demod.discord.boredgames.Game;
import com.demod.discord.boredgames.game.DominionCard.CardType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;

public class DominionGame extends Game {

	public class Player {
		private final Member member;

		private final List<DominionCard> deck = new ArrayList<>();
		private final List<DominionCard> discard = new ArrayList<>();
		private final List<DominionCard> hand = new ArrayList<>();
		private final List<DominionCard> play = new ArrayList<>();

		private final List<Entry<Predicate<DominionCard>, Runnable>> firstPlayConditions = new ArrayList<>();

		private int actionsAvailable;
		private int buysAvailable;
		private int coinsAvailable;

		public boolean skipPhase;

		public Player(Member member) {
			this.member = member;
		}

		public void addActions(int count) {
			actionsAvailable += count;
		}

		public void addBuys(int count) {
			buysAvailable += count;
		}

		public void addCoins(int count) {
			coinsAvailable += count;
		}

		public void addFirstPlayCondition(Predicate<DominionCard> condition, Runnable action) {
			firstPlayConditions.add(new SimpleImmutableEntry<>(condition, action));
		}

		private boolean canBuyCards() {
			return buysAvailable > 0 && !getSupplyCardsAtMostCost(coinsAvailable).isEmpty();
		}

		private boolean canPlayActions() {
			return actionsAvailable > 0 && hand.stream().anyMatch(c -> c.getType() == CardType.Action);
		}

		private boolean canPlayTreasures() {
			return hand.stream().anyMatch(c -> c.getType() == CardType.Treasure);
		}

		public boolean chooseActionOrSkip(String actionEmoji, String actionMessage, String message) {
			return requestAction(this, ImmutableMap.of(actionEmoji, actionMessage), message, true, false).isPresent();
		}

		public DominionCard chooseCard(List<DominionCard> choices, String message) {
			return requestChooseCard(this, choices, message, false, false).get();
		}

		public boolean chooseCardOrSkip(DominionCard card, String message) {
			return requestChooseCard(this, Arrays.asList(card), message, true, false).isPresent();
		}

		public Optional<DominionCard> chooseCardOrSkip(List<DominionCard> choices, String message) {
			return requestChooseCard(this, choices, message, true, false);
		}

		public void cleanup() {
			discard.addAll(hand);
			hand.clear();

			discard.addAll(play);
			play.clear();

			firstPlayConditions.clear();

			for (int i = 0; i < 5; i++) {
				Optional<DominionCard> card = takeFromDeck();
				if (!card.isPresent()) {
					break;
				}
				hand.add(card.get());
			}
		}

		public void discard(DominionCard card) {
			discard.add(card);
		}

		public void drawCards(int count) {
			for (int i = 0; i < count; i++) {
				Optional<DominionCard> card = takeFromDeck();
				if (!card.isPresent()) {
					break;
				}
				hand.add(card.get());
			}
		}

		public void gain(DominionCard card) {
			discard.add(card);
		}

		public void gameOver() {
			deck.addAll(play);
			play.clear();

			deck.addAll(discard);
			discard.clear();

			deck.addAll(hand);
			hand.clear();
		}

		public List<Player> getAllPlayers() {
			List<Player> playerOrder = new ArrayList<>(getPlayerCount());
			playerOrder.add(this);
			playerOrder.addAll(getOtherPlayers());
			return playerOrder;
		}

		public List<DominionCard> getDeck() {
			return deck;
		}

		public List<DominionCard> getDiscard() {
			return discard;
		}

		public int getEmptySupplyPileCount() {
			return initialSupplyCount - supply.entrySet().size();
		}

		public List<DominionCard> getHand() {
			return hand;
		}

		public Member getMember() {
			return member;
		}

		public String getName() {
			return member.getEffectiveName();
		}

		public List<Player> getOtherPlayers() {
			int index = players.indexOf(this);
			List<Player> otherPlayers = new ArrayList<>(players.size() - 1);
			for (int i = 0; i < players.size() - 1; i++) {
				otherPlayers.add(players.get((index + i + 1) % players.size()));
			}
			return otherPlayers;
		}

		public int getPlayerCount() {
			return players.size();
		}

		public int getScore() {
			return deck.stream().filter(c -> c.getType() == CardType.Treasure)
					.mapToInt(c -> CardType.Treasure.getInteraction(c).getCoinValue(this)).sum();
		}

		public List<DominionCard> getSupplyCardsAtMostCost(int coins) {
			return DominionGame.this.getSupplyCardsAtMostCost(coins);
		}

		public int getTotalCardCount() {
			return deck.size() + discard.size() + hand.size() + play.size();
		}

		public DominionCard hiddenChooseCard(List<DominionCard> choices, String message) {
			return requestChooseCard(this, choices, message, false, true).get();
		}

		public Optional<DominionCard> hiddenChooseCardOrSkip(List<DominionCard> choices, String message) {
			return requestChooseCard(this, choices, message, true, true);
		}

		public void initializeGame() {
			deck.clear();
			discard.clear();
			hand.clear();
			play.clear();

			for (int i = 0; i < 7; i++) {
				deck.add(DominionCard.Copper);
			}
			for (int i = 0; i < 3; i++) {
				deck.add(DominionCard.Estate);
			}

			Collections.shuffle(deck);

			drawCards(5);
		}

		public void play(DominionCard card) {
			play(card, true);
		}

		public void play(DominionCard card, boolean putInPlay) {
			if (putInPlay) {
				play.add(card);
			}

			if (card.getType() == CardType.Action) {
				CardType.Action.getInteraction(card).onPlay(this);
			}

			if (card.getType() == CardType.Treasure) {
				int coins = CardType.Treasure.getInteraction(card).getCoinValue(this);
				addCoins(coins);
			}

			firstPlayConditions.removeIf(e -> {
				if (e.getKey().test(card)) {
					e.getValue().run();
					return true;
				} else {
					return false;
				}
			});
		}

		public void putInHand(DominionCard card) {
			hand.add(card);
		}

		public void putOnDeck(DominionCard card) {
			deck.add(card);
		}

		public boolean removeFromDiscard(DominionCard card) {
			return discard.remove(card);
		}

		public boolean removeFromHand(DominionCard card) {
			return hand.remove(card);
		}

		public boolean removeFromPlay(DominionCard card) {
			int lastIndexOf = play.lastIndexOf(card);
			if (lastIndexOf != -1) {
				play.remove(lastIndexOf);
				return true;
			} else {
				return false;
			}
		}

		public void reveal(DominionCard card, String message) {
			requestReveal(this, Arrays.asList(card), message);
		}

		public void reveal(List<DominionCard> cards, String message) {
			requestReveal(this, cards, message);
		}

		public Optional<DominionCard> takeFromDeck() {
			if (deck.isEmpty()) {
				if (discard.isEmpty()) {
					return Optional.empty();
				}

				deck.addAll(discard);
				discard.clear();

				Collections.shuffle(deck);
			}

			return Optional.of(deck.remove(deck.size() - 1));
		}

		public boolean takeFromSupply(DominionCard card) {
			return supply.remove(card);
		}

		public void trash(DominionCard card) {
			trash.add(card);
		}

	}

	private boolean hotseat;
	private final List<Player> players = new ArrayList<>();
	private final Set<DominionCard> kingdoms = new LinkedHashSet<>();

	private Optional<Player> winner = Optional.empty();

	private int turn;

	private final Multiset<DominionCard> supply = LinkedHashMultiset.create();
	private int initialSupplyCount;
	private final Deque<DominionCard> trash = new ArrayDeque<>();

	public DominionGame() {
		pickKingdoms();
	}

	private void autoPlayTreasures(Player player) {
		List<DominionCard> autoTreasures = player.hand.stream().filter(c -> {
			switch (c) {
			case Copper:
			case Silver:
			case Gold:
				return true;
			default:
				return false;
			}
		}).collect(Collectors.toList());
		for (DominionCard card : autoTreasures) {
			player.removeFromHand(card);
			player.play(card);
		}
	}

	private void buyChooseCard(Player player, DominionCard card) {
		player.buysAvailable--;
		player.coinsAvailable -= card.getCost();
		supply.remove(card);
		player.discard(card);
	}

	private void checkGameOver() {
		if (supply.count(DominionCard.Province) == 0 || supply.entrySet().size() <= initialSupplyCount - 3) {

			for (Player player : players) {
				player.gameOver();
			}

			int bestScore = 0;
			Player bestPlayer = null;
			for (int i = 0; i < players.size(); i++) {
				Player player = players.get((turn + players.size() - i) % players.size());
				int score = player.getScore();
				if (score >= bestScore) {
					bestScore = score;
					bestPlayer = player;
				}
			}
			winner = Optional.of(bestPlayer);
		}
	}

	private Player currentPlayer() {
		return players.get(turn);
	}

	private void endTurn(Player player) {
		player.cleanup();
	}

	private String generateDisplayCardList(Collection<DominionCard> cards) {
		return cards.stream().map(c -> c.getEmoji() + c.getTitle()).collect(Collectors.joining("\n"));
	}

	private String generateDisplayDeckBreakdown(Player player) {
		List<String> lines = new ArrayList<>();
		Multiset<DominionCard> cardCounts = LinkedHashMultiset.create(player.deck);
		for (Multiset.Entry<DominionCard> entry : cardCounts.entrySet().stream()
				.sorted((e1, e2) -> Integer.compare(e2.getCount(), e1.getCount())).collect(Collectors.toList())) {
			DominionCard card = entry.getElement();
			lines.add("**" + entry.getCount() + "** " + card.getEmoji() + " " + card.getTitle());
		}
		return lines.stream().collect(Collectors.joining("\n"));
	}

	private String generatePlayerStats(Player player, boolean showActionsRemaining) {
		List<String> lines = new ArrayList<>();

		lines.add("Player: **" + player.getMember().getEffectiveName() + "**");

		if (showActionsRemaining) {
			lines.add("Actions: **" + player.actionsAvailable + "**");
		}
		lines.add("Buys: **" + player.buysAvailable + "**");
		lines.add("Coins: **" + player.coinsAvailable + "**");
		lines.add("Deck: **" + player.deck.size() + "**");
		lines.add("Discarded: **" + player.discard.size() + "**");

		return lines.stream().collect(Collectors.joining("\n"));
	}

	private LinkedHashSet<DominionCard> getBuyChoices(Player player) {
		return new LinkedHashSet<>(getSupplyCardsAtMostCost(player.coinsAvailable));
	}

	private LinkedHashSet<DominionCard> getPlayActionChoices(Player player) {
		return player.hand.stream().filter(c -> c.getType() == CardType.Action)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private LinkedHashSet<DominionCard> getPlayTreasureChoices(Player player) {
		return player.hand.stream().filter(c -> c.getType() == CardType.Treasure)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public List<DominionCard> getSupplyCardsAtMostCost(int coins) {
		return supply.entrySet().stream().map(Multiset.Entry::getElement).filter(c -> c.getCost() <= coins)
				.sorted((c1, c2) -> Integer.compare(c1.getCost(), c2.getCost())).collect(Collectors.toList());
	}

	@Override
	protected String getTitle() {
		return "Dominion Card Game";
	}

	private void initializeGame() {
		supply.clear();
		trash.clear();

		int victorySize = new int[] { 0, 8, 12, 12 }[players.size() - 1];
		int curseSize = new int[] { 0, 10, 20, 30 }[players.size() - 1];
		int copperSize = new int[] { 0, 46, 39, 32 }[players.size() - 1];

		supply.add(DominionCard.Estate, victorySize);
		supply.add(DominionCard.Duchy, victorySize);
		supply.add(DominionCard.Province, victorySize);
		supply.add(DominionCard.Curse, curseSize);
		supply.add(DominionCard.Copper, copperSize);
		supply.add(DominionCard.Silver, 40);
		supply.add(DominionCard.Gold, 30);

		for (DominionCard card : kingdoms) {
			if (card.getType() == CardType.Victory) {
				supply.add(card, victorySize);
			} else {
				supply.add(card, 10);
			}
		}

		initialSupplyCount = supply.entrySet().size();

		for (Player player : players) {
			player.initializeGame();
		}
	}

	public boolean isGameOver() {
		return winner.isPresent();
	}

	private void pickKingdoms() {
		kingdoms.clear();
		Random rand = new Random();
		while (kingdoms.size() < 10) {
			kingdoms.add(DominionCard.kingdoms[rand.nextInt(DominionCard.kingdoms.length)]);
		}
	}

	private void playActionsChooseCard(Player player, DominionCard card) {
		player.actionsAvailable--;
		player.removeFromHand(card);
		player.play(card);
	}

	private void playTreasuresChooseCard(Player player, DominionCard card) {
		player.removeFromHand(card);
		player.play(card);
	}

	private void registerCardChoices(Display<?> display, Player player, LinkedHashSet<DominionCard> playActionChoices,
			Consumer<DominionCard> action) {
		for (DominionCard card : playActionChoices) {
			display.addExclusiveAction(player.getMember(), card.getEmoji(), p -> action.accept(card));
		}
	}

	private Optional<String> requestAction(Player player, Map<String, String> actionEmojiMessages, String message,
			boolean skippable, boolean hidden) {
		if (skippable) {
			if (actionEmojiMessages.isEmpty()) {
				return Optional.empty();
			}
		} else {
			if (actionEmojiMessages.size() == 1) {
				return actionEmojiMessages.keySet().stream().findFirst();
			}
		}

		Display<Optional<String>> display = hidden ? displayPrivate(player.getMember()) : displayChannel();
		EmbedBuilder embed = display.getBuilder();
		embed.addField("Choosing Player", generatePlayerStats(player, false), true);
		embed.setDescription(message);

		for (Entry<String, String> entry : actionEmojiMessages.entrySet()) {
			String actionEmoji = entry.getKey();
			String actionMessage = entry.getValue();
			embed.addField(actionEmoji + " Action Choice", actionMessage, true);
			display.addExclusiveAction(player.getMember(), actionEmoji, Optional.of(actionEmoji));
		}

		if (skippable) {
			display.addExclusiveAction(player.getMember(), Emojis.TRACK_NEXT, Optional.empty());
		}

		return display.send();
	}

	private Optional<DominionCard> requestChooseCard(Player player, List<DominionCard> choices, String message,
			boolean skippable, boolean hidden) {
		List<DominionCard> uniqueChoices = choices.stream().distinct().collect(Collectors.toList());
		if (skippable) {
			if (choices.isEmpty()) {
				return Optional.empty();
			}
		} else {
			if (uniqueChoices.size() == 1) {
				return Optional.of(choices.get(0));
			}
		}

		if (hidden) {
			displayChannel(embed -> {
				embed.setDescription("Sent **" + player.getMember().getEffectiveName()
						+ "** a private message.  Waiting for a response...");
			}).send();
		}

		Display<Optional<DominionCard>> display = hidden ? displayPrivate(player.getMember()) : displayChannel();
		EmbedBuilder embed = display.getBuilder();
		if (!hidden) {
			embed.addField("Choosing Player", generatePlayerStats(player, false), true);
		}
		embed.addField("Card Choices", generateDisplayCardList(choices), true);
		embed.setDescription(message);

		for (DominionCard card : uniqueChoices) {
			embed.addField(card.getEmoji() + " Choose " + card.getTitle(), card.getText(), true);
			if (hidden) {
				display.addAction(card.getEmoji(), Optional.of(card));
			} else {
				display.addExclusiveAction(player.getMember(), card.getEmoji(), Optional.of(card));
			}
		}

		if (skippable) {
			if (hidden) {
				display.addAction(Emojis.TRACK_NEXT, Optional.empty());
			} else {
				display.addExclusiveAction(player.getMember(), Emojis.TRACK_NEXT, Optional.empty());
			}
		}

		Optional<DominionCard> result = display.send();

		if (hidden) {
			displayPrivate(player.getMember(), e -> {
				e.setDescription("Your response has been submitted.");
			}).send();
		}

		return result;
	}

	private void requestReveal(Player player, List<DominionCard> cards, String message) {
		displayChannel(embed -> {
			embed.addField("Current Player", generatePlayerStats(player, false), true);
			embed.setDescription(message);
			embed.addField("Revealed Cards", generateDisplayCardList(cards), true);
		}).addExclusiveAction(player.getMember(), Emojis.BLOCK_OK, p -> {
		}).send();
	}

	@Override
	public void run() {
		runHotseatPhase();
		while (!isGameOver()) {
			Player player = currentPlayer();
			startTurn(player);
			runPlayActionsPhase(player);
			runPlayTreasuresPhase(player);
			runBuyPhase(player);
			endTurn(player);
			checkGameOver();
			turn = (turn + 1) % players.size();
		}
		runGameOverPhase();
	}

	private void runBuyPhase(Player player) {
		player.skipPhase = false;

		while (player.canBuyCards() && !player.skipPhase) {
			Display<?> display = displayChannel(embed -> {
				embed.setTitle("Buy Phase - Buy any Supply Cards");
				embed.setDescription(
						"Buy cards from the supply if you choose to do so.\n\nPress the corresponding reaction to play that card, or skip the buy phase by pressing "
								+ Emojis.TRACK_NEXT + ".");

				embed.addField("In Hand", generateDisplayCardList(player.hand), true);
				embed.addField("In Play", generateDisplayCardList(player.play), true);
				embed.addField("Current Player", generatePlayerStats(player, false), false);

				for (DominionCard card : getBuyChoices(player)) {
					embed.addField(
							card.getEmoji() + " Buy " + card.getTitle()
									+ (card.getCost() == 0 ? " (Free)" : (" (Cost " + card.getCost() + ")")),
							card.getText(), true);
				}
			});

			registerCardChoices(display, player, getBuyChoices(player), c -> buyChooseCard(player, c));
			display.addExclusiveAction(player.getMember(), Emojis.TRACK_NEXT, p -> {
				player.skipPhase = true;
			});

			display.send();
		}
	}

	private void runGameOverPhase() {
		displayChannel(embed -> {
			embed.setDescription("Game Over! The winner is **" + winner.get().getName() + "**!");

			for (Player player : players) {
				embed.addField(player.getName() + "\n(**" + player.getScore() + "** Victory Points)",
						generateDisplayDeckBreakdown(player), true);
			}
		}).send();
	}

	private void runHotseatPhase() {
		hotseat = true;
		while (hotseat) {
			List<Member> playerMembers = players.stream().map(Player::getMember).collect(Collectors.toList());

			Display<?> display = displayChannel(embed -> {
				embed.setDescription("This is a 2-4 player game, and can take some time to play.\n\n"
						+ "Try to build a deck with the most victory points when the game ends.  "
						+ "You play action cards that help you (or hurt others) and buy more cards to build the best deck to win.\n"
						+ "The game is over when any 3 supplies of cards run out, or when the highest victory card (Province) supply runs out.\n\n"
						+ "Press the " + Emojis.HAND_SPLAYED + " to join, press the " + Emojis.SHUFFLE
						+ " to generate a new set of kingdoms, press the " + Emojis.QUESTION
						+ " for details, and press the " + Emojis.GAME_DIE + " to begin the game!");

				if (!players.isEmpty()) {
					embed.addField("Players", players.stream().map(Player::getName).collect(Collectors.joining("\n")),
							true);
				}

				embed.addField("Kingdoms", generateDisplayCardList(kingdoms), false);
			});

			if (players.size() < 4) {
				display.addAction(Emojis.HAND_SPLAYED, p -> {
					if (players.size() < 4 && !players.stream().anyMatch(p2 -> p2.getMember().equals(p))) {
						players.add(new Player(p));
					}
				});
			}
			display.addExclusiveAction(playerMembers, Emojis.SHUFFLE, p -> {
				pickKingdoms();
			});
			if (players.size() >= 2) {
				display.addExclusiveAction(playerMembers, Emojis.GAME_DIE, p -> {
					hotseat = false;
				});
			}

			display.send();
		}

		initializeGame();
		turn = 0;
	}

	private void runPlayActionsPhase(Player player) {
		player.skipPhase = false;

		while (player.canPlayActions() && !player.skipPhase) {
			Display<?> display = displayChannel(embed -> {
				embed.setTitle("Action Phase - Play any Action Cards");
				embed.setDescription(
						"Play action cards if you choose to do so.\n\nPress the corresponding reaction to play that card, or skip the action phase by pressing "
								+ Emojis.TRACK_NEXT + ".");

				embed.addField("In Hand", generateDisplayCardList(player.hand), true);
				embed.addField("In Play", generateDisplayCardList(player.play), true);
				embed.addField("Current Player", generatePlayerStats(player, true), false);

				for (DominionCard card : getPlayActionChoices(player)) {
					embed.addField(card.getEmoji() + " Play " + card.getTitle(), card.getText(), true);
				}
			});

			registerCardChoices(display, player, getPlayActionChoices(player), c -> playActionsChooseCard(player, c));
			display.addExclusiveAction(player.getMember(), Emojis.TRACK_NEXT, p -> {
				player.skipPhase = true;
			});

			display.send();
		}
	}

	private void runPlayTreasuresPhase(Player player) {
		player.skipPhase = false;
		autoPlayTreasures(player);

		while (player.canPlayTreasures() && !player.skipPhase) {
			Display<?> display = displayChannel(embed -> {
				embed.setTitle("Treasure Phase - Play any Treasure Cards");
				embed.setDescription(
						"Play treasure cards if you choose to do so.\n\nPress the corresponding reaction to play that card, or skip the treasure phase by pressing "
								+ Emojis.TRACK_NEXT + ".");

				embed.addField("In Hand", generateDisplayCardList(player.hand), true);
				embed.addField("In Play", generateDisplayCardList(player.play), true);
				embed.addField("Current Player", generatePlayerStats(player, false), false);

				for (DominionCard card : getPlayTreasureChoices(player)) {
					embed.addField(card.getEmoji() + " Play " + card.getTitle(), card.getText(), true);
				}
			});

			registerCardChoices(display, player, getPlayTreasureChoices(player),
					c -> playTreasuresChooseCard(player, c));
			display.addExclusiveAction(player.getMember(), Emojis.TRACK_NEXT, p -> {
				player.skipPhase = true;
			});

			display.send();
		}
	}

	private void startTurn(Player player) {
		player.actionsAvailable = 1;
		player.buysAvailable = 1;
		player.coinsAvailable = 0;
	}

}
