package com.demod.discord.boredgames.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.demod.discord.boredgames.Emojis;
import com.demod.discord.boredgames.Game;
import com.demod.discord.boredgames.game.DominionCards.CardType;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;

public class DominionGame extends Game {

	private enum ActionPhase {
		HOTSEAT, PLAY_ACTIONS, PLAY_TREASURES, BUY, GAME_OVER
	}

	public class Player {
		private final Member member;

		private final List<DominionCards> deck = new ArrayList<>();
		private final List<DominionCards> discard = new ArrayList<>();
		private final List<DominionCards> hand = new ArrayList<>();
		private final List<DominionCards> play = new ArrayList<>();

		private int actionsAvailable;
		private int buysAvailable;
		private int coinsAvailable;

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

		public void addFirstPlayCondition(Predicate<DominionCards> condition, Runnable action) {
			// TODO Auto-generated method stub

		}

		private boolean canBuyCards() {
			return buysAvailable > 0 && !getSupplyCardsAtMostCost(coinsAvailable).isEmpty();
		}

		private boolean canPlayActions() {
			return actionsAvailable > 0 && hand.stream().anyMatch(c -> c.getType() == CardType.ACTION);
		}

		private boolean canPlayTreasures() {
			return hand.stream().anyMatch(c -> c.getType() == CardType.TREASURE);
		}

		public void cleanup() {
			discard.addAll(hand);
			hand.clear();

			discard.addAll(play);
			play.clear();

			for (int i = 0; i < 5; i++) {
				Optional<DominionCards> card = takeCardFromDeck();
				if (!card.isPresent()) {
					break;
				}
				hand.add(card.get());
			}
		}

		public void discard(DominionCards card) {
			discard.add(card);
		}

		public void drawCards(int count) {
			for (int i = 0; i < count; i++) {
				Optional<DominionCards> card = takeCardFromDeck();
				if (!card.isPresent()) {
					break;
				}
				hand.add(card.get());
			}
		}

		public void gainCard(DominionCards card) {
			discard.add(card);
		}

		public Member getMember() {
			return member;
		}

		public List<Player> getOtherPlayers() {
			int index = players.indexOf(this);
			List<Player> otherPlayers = new ArrayList<>(players.size() - 1);
			for (int i = 0; i < players.size() - 1; i++) {
				otherPlayers.add(players.get((index + i + 1) % players.size()));
			}
			return otherPlayers;
		}

		public int getTotalCardCount() {
			return deck.size() + discard.size() + hand.size() + play.size();
		}

		public void initializeGame() {
			deck.clear();
			discard.clear();
			hand.clear();
			play.clear();

			for (int i = 0; i < 7; i++) {
				deck.add(DominionCards.Copper);
			}
			for (int i = 0; i < 3; i++) {
				deck.add(DominionCards.Estate);
			}

			Collections.shuffle(deck);

			drawCards(5);
		}

		public void playCard(DominionCards card) {
			play.add(card);

			if (card.getType() == CardType.ACTION) {
				CardType.ACTION.getInteraction(card).onPlay(this);
			}

			if (card.getType() == CardType.TREASURE) {
				int coins = CardType.TREASURE.getInteraction(card).getCoinValue(this);
				addCoins(coins);
			}
		}

		public void putInHand(DominionCards card) {
			hand.add(card);
		}

		public boolean removeFromHand(DominionCards card) {
			return hand.remove(card);
		}

		public Optional<DominionCards> revealCardFromDeck() {
			return takeCardFromDeck();
		}

		private Optional<DominionCards> takeCardFromDeck() {
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

	}

	private final List<Player> players = new ArrayList<>();
	private final Set<DominionCards> kingdoms = new LinkedHashSet<>();

	private ActionPhase phase = ActionPhase.HOTSEAT;
	private final Optional<Member> winner = Optional.empty();

	private int turn;

	private final Multiset<DominionCards> supply = LinkedHashMultiset.create();
	private final Deque<DominionCards> trash = new ArrayDeque<>();

	public DominionGame() {
		pickKingdoms();
	}

	private void autoPlayTreasures() {
		Player player = currentPlayer();
		List<DominionCards> autoTreasures = player.hand.stream().filter(c -> {
			switch (c) {
			case Copper:
			case Silver:
			case Gold:
				return true;
			default:
				return false;
			}
		}).collect(Collectors.toList());
		for (DominionCards card : autoTreasures) {
			player.removeFromHand(card);
			player.playCard(card);
		}
	}

	@Override
	public void buildDisplay(EmbedBuilder embed) {
		switch (phase) {
		case HOTSEAT:
			embed.setDescription("This is a 2-4 player game, and can take some time to play.\n\n"
					+ "Try to build a deck with the most victory points when the game ends.  "
					+ "You play action cards that help you (or hurt others) and buy more cards to build the best deck to win.\n"
					+ "The game is over when any 3 supplies of cards run out, or when the highest victory card (Province) supply runs out.\n\n"
					+ "Press the " + Emojis.HAND_SPLAYED + " to join, press the " + Emojis.SHUFFLE
					+ " to generate a new set of kindoms, and press the " + Emojis.GAME_DIE + " to begin the game!");

			if (!players.isEmpty()) {
				embed.addField("Players", players.stream().map(Player::getMember).map(Member::getEffectiveName)
						.collect(Collectors.joining("\n")), true);
			}

			// TODO Display picked kingdoms
			break;

		case PLAY_ACTIONS:
			// TODO
			break;

		case PLAY_TREASURES:
			// TODO
			break;

		case BUY:
			// TODO
			break;

		case GAME_OVER:
			// TODO
			break;
		}
	}

	private Player currentPlayer() {
		return players.get(turn);
	}

	private void endTurn() {
		currentPlayer().cleanup();
		turn = (turn + 1) % players.size();
		startPlayActionsPhase();
	}

	public List<DominionCards> getSupplyCardsAtMostCost(int coins) {
		// TODO
		return null;
	}

	public void hotseatGameStart(Member player) {
		if (!players.stream().anyMatch(p -> p.getMember().equals(player))) {
			return;
		}
		initializeGame();
		turn = 0;
		startPlayActionsPhase();
	}

	public void hotseatNewPlayer(Member player) {
		if (players.size() < 4 && !players.stream().anyMatch(p -> p.getMember().equals(player))) {
			players.add(new Player(player));
		}
	}

	public void hotseatShuffle() {
		pickKingdoms();
	}

	private void initializeGame() {
		kingdoms.clear();
		supply.clear();
		trash.clear();

		int victorySize = new int[] { 0, 8, 12, 12 }[players.size()];
		int curseSize = new int[] { 0, 10, 20, 30 }[players.size()];
		int copperSize = new int[] { 0, 46, 39, 32 }[players.size()];

		supply.add(DominionCards.Estate, victorySize);
		supply.add(DominionCards.Duchy, victorySize);
		supply.add(DominionCards.Province, victorySize);
		supply.add(DominionCards.Curse, curseSize);
		supply.add(DominionCards.Copper, copperSize);
		supply.add(DominionCards.Silver, 40);
		supply.add(DominionCards.Gold, 30);

		for (DominionCards card : kingdoms) {
			if (card.getType() == CardType.VICTORY) {
				supply.add(card, victorySize);
			} else {
				supply.add(card, 10);
			}
		}

		for (Player player : players) {
			player.initializeGame();
		}
	}

	@Override
	public boolean isGameOver() {
		return winner.isPresent();
	}

	private void pickKingdoms() {
		kingdoms.clear();
		Random rand = new Random();
		while (kingdoms.size() < 10) {
			kingdoms.add(DominionCards.kingdoms[rand.nextInt(DominionCards.kingdoms.length)]);
		}
	}

	@Override
	public void registerActions(ActionRegistry registry) {
		switch (phase) {
		case HOTSEAT:
			if (players.size() < 4) {
				registry.addAction(Emojis.HAND_SPLAYED, this::hotseatNewPlayer);
			}
			registry.addAction(Emojis.SHUFFLE, this::hotseatShuffle);
			if (players.size() >= 2) {
				registry.addAction(Emojis.GAME_DIE, this::hotseatGameStart);
			}
			break;

		case PLAY_ACTIONS:
			// TODO
			break;

		case PLAY_TREASURES:
			// TODO
			break;

		case BUY:
			// TODO
			break;

		case GAME_OVER:
			// TODO
			break;
		}
	}

	private void startBuyPhase() {
		if (currentPlayer().canBuyCards()) {
			phase = ActionPhase.BUY;
		} else {
			endTurn();
		}
	}

	private void startPlayActionsPhase() {
		Player player = currentPlayer();
		player.actionsAvailable = 1;
		player.buysAvailable = 1;
		player.coinsAvailable = 0;
		if (currentPlayer().canPlayActions()) {
			phase = ActionPhase.PLAY_ACTIONS;
		} else {
			startPlayTreasuresPhase();
		}
	}

	private void startPlayTreasuresPhase() {
		autoPlayTreasures();
		if (currentPlayer().canPlayTreasures()) {
			phase = ActionPhase.PLAY_TREASURES;
		} else {
			startBuyPhase();
		}
	}

}
