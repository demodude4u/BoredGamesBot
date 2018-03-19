package com.demod.discord.boredgames.game;

import static com.demod.discord.boredgames.game.DominionCards.CardSubType.ATTACK;
import static com.demod.discord.boredgames.game.DominionCards.CardType.ACTION;
import static com.demod.discord.boredgames.game.DominionCards.CardType.TREASURE;
import static com.demod.discord.boredgames.game.DominionCards.CardType.VICTORY;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import com.demod.discord.boredgames.Emojis;
import com.demod.discord.boredgames.game.DominionGame.Player;

public enum DominionCards {

	// Basic Cards
	Copper(Emojis.DOLLAR, 0, TREASURE, "+1C", p -> 1), //
	Silver(Emojis.MONEYBAG, 3, TREASURE, "+2C", p -> 2), //
	Gold(Emojis.GEM, 6, TREASURE, "+3C", p -> 3), //
	Estate(Emojis.RICE_SCENE, 2, VICTORY, "+1VP", p -> 1), //
	Duchy(Emojis.FOUNTAIN, 5, VICTORY, "+3VP", p -> 3), //
	Province(Emojis.PARK, 8, VICTORY, "+6VP", p -> 6), //
	Curse(Emojis.SKULL, 0, VICTORY, "-1VP", p -> -1), //

	// Base Set (First Round)
	Merchant(Emojis.SCALES, 3, ACTION, "+1D|+1A|The first time you play a Silver this turn, +1C.", p -> {
		p.drawCards(1);
		p.addActions(1);
		p.addFirstPlayCondition(c -> c == Silver, () -> p.addCoins(1));
	}), //

	Village(Emojis.HOMES, 3, ACTION, "+1D|+2A", p -> {
		p.drawCards(1);
		p.addActions(2);
	}), //

	Gardens(Emojis.TULIP, 4, VICTORY, "Worth 1VP per 10 cards you have (round down).", p -> {
		return p.getTotalCardCount() / 10;
	}), //

	Smithy(Emojis.HAMMER_PICK, 3, ACTION, "+3D", p -> {
		p.drawCards(3);
	}), //

	Council_Room(Emojis.OFFICE, 5, ACTION, "+4D|+1B|Each other player draws a card.", p -> {
		p.drawCards(4);
		p.addBuys(1);
		for (Player other : p.getOtherPlayers()) {
			other.drawCards(1);
		}
	}), //

	Festival(Emojis.CIRCUS_TENT, 5, ACTION, "+2A|+1B|+2C", p -> {
		p.addActions(2);
		p.addBuys(1);
		p.addCoins(2);
	}), //

	Laboratory(Emojis.MICROSCOPE, 5, ACTION, "+2D,+1A", p -> {
		p.drawCards(2);
		p.addActions(1);
	}), //

	Market(Emojis.SHOPPING_BAGS, 5, ACTION, "+1D|+1A|+1B|+1C", p -> {
		p.drawCards(1);
		p.addActions(1);
		p.addBuys(1);
		p.addCoins(1);
	}), //

	Witch(Emojis.CRYSTAL_BALL, 5, ACTION, ATTACK, "+2D|Each other player gains a Curse.", p -> {
		p.drawCards(2);
		for (Player other : p.getOtherPlayers()) {
			other.gainCard(DominionCards.Curse);
		}
	}), //

	Woodcutter(Emojis.CONSTRUCTION_WORKER, 3, ACTION, "+1B|+2C", p -> {
		p.addBuys(1);
		p.addCoins(2);
	}), //

	Adventurer(Emojis.CROSSED_SWORDS, 6, ACTION,
			"Reveal cards from your deck until you reveal 2 Treasure cards. Put those Treasure cards into your hand and discard the other revealed cards.",
			p -> {
				List<DominionCards> treasures = new ArrayList<>();
				List<DominionCards> notTreasures = new ArrayList<>();
				while (treasures.size() < 2) {
					Optional<DominionCards> card = p.revealCardFromDeck();
					if (!card.isPresent()) {
						break;
					}
					if (card.get().getType() == TREASURE) {
						treasures.add(card.get());
					} else {
						notTreasures.add(card.get());
					}
				}
				treasures.forEach(p::putInHand);
				notTreasures.forEach(p::discard);
			}),//
	;

	public enum CardSubType {
		NONE, REACTION, ATTACK
	}

	public static class CardType<F> {
		public static final CardType<TypeActionInteraction> ACTION = new CardType<>("Action");
		public static final CardType<TypeTreasureInteraction> TREASURE = new CardType<>("Treasure");
		public static final CardType<TypeVictoryInteraction> VICTORY = new CardType<>("Victory");

		private final String name;

		private CardType(String name) {
			this.name = name;
		}

		@SuppressWarnings("unchecked")
		public F getInteraction(DominionCards card) {
			if (card.getType() != this) {
				throw new InternalError(card + " -> " + name);
			}
			return (F) card.typeFunction;
		}
	}

	public interface TypeActionInteraction {
		public void onPlay(Player p);
	}

	public interface TypeTreasureInteraction {
		public int getCoinValue(Player p);
	}

	public interface TypeVictoryInteraction {
		public int getVictoryPoints(Player p);
	}

	public static final DominionCards[] kingdoms = new DominionCards[values().length - 7];
	static {
		for (int i = 7; i < values().length; i++) {
			kingdoms[i - 7] = values()[i];
		}
	}

	private static final LinkedHashMap<String, String> textReplace = new LinkedHashMap<>();
	static {
		// TODO determine if emojis are doable
		// String actionEmoji = Emojis.ARROW_FORWARD;
		// String coinEmoji = Emojis.DOLLAR;
		// String cardEmoji = Emojis.TICKET;
		// String victoryEmoji = Emojis.SHIELD;

		String[] letter = { "A", "B", "C", "D", "VP" };
		String[] labels = { "Action", "Buy", "Coin", "Card", "Victory Point" };
		for (int i = 0; i < letter.length; i++) {
			for (int n = 1; n <= 9; n++) {
				textReplace.put(n + letter[i], "**" + n + " " + labels[i] + (n > 1 ? "s" : "") + "**");
			}
		}
	}

	private final String title;
	private final String emoji;
	private final int cost;
	private final CardType<?> type;
	private final CardSubType subType;

	private final String text;
	private String formattedText = null;

	private Object typeFunction;

	private <F> DominionCards(String emoji, int cost, CardType<F> type, CardSubType subType, String text,
			F typeFunction) {
		this.typeFunction = typeFunction;
		this.title = name().replace('_', ' ');
		this.emoji = emoji;
		this.cost = cost;
		this.type = type;
		this.subType = subType;
		this.text = text;
	}

	private <F> DominionCards(String emoji, int cost, CardType<F> type, String text, F typeFunction) {
		this(emoji, cost, type, CardSubType.NONE, text, typeFunction);
	}

	public int getCost() {
		return cost;
	}

	public String getEmoji() {
		return emoji;
	}

	public CardSubType getSubType() {
		return subType;
	}

	public String getText() {
		if (formattedText == null) {
			formattedText = text.replace('|', '\n');
			for (Entry<String, String> entry : textReplace.entrySet()) {
				formattedText = formattedText.replace(entry.getKey(), entry.getValue());
			}
		}
		return formattedText;
	}

	public String getTitle() {
		return title;
	}

	public CardType<?> getType() {
		return type;
	}
}
