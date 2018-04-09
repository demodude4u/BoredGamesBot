package com.demod.discord.boredgames.game;

import static com.demod.discord.boredgames.game.DominionCard.CardSubType.Attack;
import static com.demod.discord.boredgames.game.DominionCard.CardType.Action;
import static com.demod.discord.boredgames.game.DominionCard.CardType.Treasure;
import static com.demod.discord.boredgames.game.DominionCard.CardType.Victory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import com.demod.discord.boredgames.Emojis;
import com.demod.discord.boredgames.game.DominionGame.Player;

public enum DominionCard {

	// Basic Cards
	Copper(Emojis.DOLLAR, 0, Treasure, "+1C", p -> 1), //
	Silver(Emojis.MONEYBAG, 3, Treasure, "+2C", p -> 2), //
	Gold(Emojis.GEM, 6, Treasure, "+3C", p -> 3), //
	Estate(Emojis.RICE_SCENE, 2, Victory, "+1VP", p -> 1), //
	Duchy(Emojis.FOUNTAIN, 5, Victory, "+3VP", p -> 3), //
	Province(Emojis.PARK, 8, Victory, "+6VP", p -> 6), //
	Curse(Emojis.SKULL, 0, Victory, "-1VP", p -> -1), //

	// Base Set (Second Round)
	Merchant(Emojis.SCALES, 3, Action, "+1D|+1A|The first time you play a Silver this turn, +1C.", p -> {
		p.drawCards(1);
		p.addActions(1);
		p.addFirstPlayCondition(c -> c == Silver, () -> p.addCoins(1));
	}), //

	Village(Emojis.HOMES, 3, Action, "+1D|+2A", p -> {
		p.drawCards(1);
		p.addActions(2);
	}), //

	Gardens(Emojis.TULIP, 4, Victory, "Worth 1VP per 10 cards you have (round down).", p -> {
		return p.getTotalCardCount() / 10;
	}), //

	Smithy(Emojis.HAMMER_PICK, 3, Action, "+3D", p -> {
		p.drawCards(3);
	}), //

	Council_Room(Emojis.OFFICE, 5, Action, "+4D|+1B|Each other player draws a card.", p -> {
		p.drawCards(4);
		p.addBuys(1);
		for (Player other : p.getOtherPlayers()) {
			other.drawCards(1);
		}
	}), //

	Festival(Emojis.CIRCUS_TENT, 5, Action, "+2A|+1B|+2C", p -> {
		p.addActions(2);
		p.addBuys(1);
		p.addCoins(2);
	}), //

	Laboratory(Emojis.MICROSCOPE, 5, Action, "+2D,+1A", p -> {
		p.drawCards(2);
		p.addActions(1);
	}), //

	Market(Emojis.SHOPPING_BAGS, 5, Action, "+1D|+1A|+1B|+1C", p -> {
		p.drawCards(1);
		p.addActions(1);
		p.addBuys(1);
		p.addCoins(1);
	}), //

	Witch(Emojis.CRYSTAL_BALL, 5, Action, Attack, "+2D|Each other player gains a Curse.", p -> {
		p.drawCards(2);
		for (Player other : p.getOtherPlayers()) {
			if (other.takeFromSupply(Curse)) {
				other.gain(Curse);
			}
		}
	}), //

	Woodcutter(Emojis.CONSTRUCTION_WORKER, 3, Action, "+1B|+2C", p -> {
		p.addBuys(1);
		p.addCoins(2);
	}), //

	Adventurer(Emojis.CROSSED_SWORDS, 6, Action,
			"Reveal cards from your deck until you reveal 2 Treasure cards. Put those Treasure cards into your hand and discard the other revealed cards.",
			p -> {
				List<DominionCard> treasures = new ArrayList<>();
				List<DominionCard> notTreasures = new ArrayList<>();
				while (treasures.size() < 2) {
					Optional<DominionCard> card = p.takeFromDeck();
					if (!card.isPresent()) {
						break;
					}
					if (card.get().getType() == Treasure) {
						treasures.add(card.get());
					} else {
						notTreasures.add(card.get());
					}
				}
				treasures.forEach(p::putInHand);
				notTreasures.forEach(p::discard);
			}), //

	Cellar(Emojis.PACKAGE, 2, Action, "+1A|Discard any number of cards, then draw that many.", p -> {
		p.addActions(1);
		int discardCount = 0;
		List<DominionCard> choices;
		while (!(choices = p.getHand()).isEmpty()) {
			Optional<DominionCard> card = p.chooseCardOrSkip(choices,
					"Discard a card, or skip. **(Discard Count: " + discardCount + ")**");
			if (card.isPresent()) {
				p.removeFromHand(card.get());
				p.discard(card.get());
				discardCount++;
			} else {
				break;
			}
		}
		p.drawCards(discardCount);
	}), //

	Chapel(Emojis.CHURCH, 2, Action, "Trash up to 4 cards from your hand.", p -> {
		for (int i = 0; i < 4; i++) {
			Optional<DominionCard> card = p.chooseCardOrSkip(p.getHand(),
					"Trash a card, or skip. **(" + (i + 1) + "/4)**");
			if (card.isPresent()) {
				p.removeFromHand(card.get());
				p.trash(card.get());
			} else {
				break;
			}
		}
	}), //

	Harbinger(Emojis.SANTA, 3, Action,
			"+1D|+1A|Look through your discard pile. You may put a card from it onto your deck.", p -> {
				p.drawCards(1);
				p.addActions(1);
				Optional<DominionCard> card = p.chooseCardOrSkip(p.getDiscard(),
						"Choose a card from your discard pile.");
				if (card.isPresent()) {
					p.removeFromDiscard(card.get());
					p.putOnDeck(card.get());
				}
			}), //

	Vassal(Emojis.STATUE_OF_LIBERTY, 3, Action,
			"+2C|Discard the top card of your deck. If it's an Action card, you may play it.", p -> {
				p.addCoins(2);
				Optional<DominionCard> card = p.takeFromDeck();
				if (card.isPresent()) {
					if (card.get().getType() == Action
							&& p.chooseCardOrSkip(card.get(), "Play the action card, or skip.")) {
						p.play(card.get());
					} else {
						p.discard(card.get());
					}
				}
			}), //

	Workshop(Emojis.TOOLS, 3, Action, "Gain a card costing up to 4C.", p -> {
		List<DominionCard> choices = p.getSupplyCardsAtMostCost(4);
		if (!choices.isEmpty()) {
			DominionCard card = p.chooseCard(choices, "Gain a card.");
			p.takeFromSupply(card);
			p.gain(card);
		}
	}), //

	MoneyLender(Emojis.BANK, 4, Action, "You may trash a Copper from your hand for +3C.", p -> {
		if (p.getHand().contains(Copper) && p.chooseCardOrSkip(Copper, "Trash a copper, or skip.")) {
			p.removeFromHand(Copper);
			p.trash(Copper);
			p.addCoins(3);
		}
	}), //

	Poacher(Emojis.BOW_AND_ARROW, 4, Action, "+1D|+1A|+1C|Discard a card per empty Supply pile.", p -> {
		p.drawCards(1);
		p.addActions(1);
		p.addCoins(1);
		int discardCount = p.getEmptySupplyPileCount();
		if (discardCount >= p.getHand().size()) {
			p.getHand().removeIf(c -> {
				p.discard(c);
				return true;
			});
		} else {
			for (int i = 0; i < discardCount; i++) {
				DominionCard card = p.chooseCard(p.getHand(),
						"Discard a card. **(" + (i + 1) + "/" + discardCount + ")**");
				p.removeFromHand(card);
				p.discard(card);
			}
		}
	}), //

	Remodel(Emojis.HOUSE_ABANDONED, 4, Action,
			"Trash a card from your hand. Gain a card costing up to 2C more than it.", p -> {
				if (!p.getHand().isEmpty()) {
					DominionCard trashCard = p.chooseCard(p.getHand(), "Trash a card.");
					p.removeFromHand(trashCard);
					p.trash(trashCard);
					int cost = trashCard.getCost() + 2;
					List<DominionCard> choices = p.getSupplyCardsAtMostCost(cost);
					if (!choices.isEmpty()) {
						DominionCard gainCard = p.chooseCard(choices, "Gain a card.");
						p.takeFromSupply(gainCard);
						p.gain(gainCard);
					}
				}
			}), //

	Throne_Room(Emojis.TOILET, 4, Action, "You may play an Action card from your hand twice.", p -> {
		List<DominionCard> choices = p.getHand().stream().filter(c -> c.getType() == Action)
				.collect(Collectors.toList());
		Optional<DominionCard> card = p.chooseCardOrSkip(choices, "Play an Action card twice, or skip.");
		if (card.isPresent()) {
			p.removeFromHand(card.get());
			p.play(card.get());
			p.play(card.get(), false);
		}
	}), //

	Bandit(Emojis.DAGGER, 5, Action, Attack,
			"Gain a Gold. Each other player reveals the top 2 cards of their deck, trashes a revealed Treasure other than Copper, and discards the rest.",
			p -> {
				if (p.takeFromSupply(Gold)) {
					p.gain(Gold);
				}
				for (Player other : p.getOtherPlayers()) {
					Optional<DominionCard> card1 = other.takeFromDeck();
					Optional<DominionCard> card2 = other.takeFromDeck();
					boolean card1Treasure = card1.map(c -> c.getType() == Treasure && c != Copper).orElse(false);
					boolean card2Treasure = card2.map(c -> c.getType() == Treasure && c != Copper).orElse(false);
					if (!card1.isPresent()) {
						continue;
					} else if (!card2.isPresent()) {
						if (card1Treasure) {
							other.trash(card1.get());
						} else {
							other.discard(card1.get());
						}
					} else if (card1.get() == card2.get()) {
						if (card1Treasure) {
							other.trash(card1.get());
						} else {
							other.discard(card1.get());
						}
						other.discard(card2.get());
					} else {
						if (card1Treasure && card2Treasure) {
							DominionCard card = other.chooseCard(Arrays.asList(card1.get(), card2.get()),
									"Trash a revealed Treasure card.");
							if (card1.get() == card) {
								other.trash(card1.get());
								other.discard(card2.get());
							} else {
								other.discard(card1.get());
								other.trash(card2.get());
							}
						} else if (card1Treasure) {
							other.trash(card1.get());
							other.discard(card2.get());
						} else {
							other.discard(card1.get());
							other.trash(card2.get());
						}
					}
				}
			}), //

	Library(Emojis.BOOKS, 5, Action,
			"Draw until you have 7 cards in hand, skipping any Actions cards you choose to; set those aside, discarding them afterwards.",
			p -> {
				List<DominionCard> skippedCards = new ArrayList<>();
				while (p.getHand().size() < 7) {
					Optional<DominionCard> card = p.takeFromDeck();
					if (card.isPresent()) {
						if (card.get().getType() == Action) {
							if (p.chooseCardOrSkip(card.get(), "Put action card in hand, or skip.")) {
								p.putInHand(card.get());
							} else {
								skippedCards.add(card.get());
							}
						} else {
							p.putInHand(card.get());
						}
					} else {
						break;
					}
				}
				for (DominionCard card : skippedCards) {
					p.discard(card);
				}
			}), //

	Mine(Emojis.PICK, 5, Action,
			"You may trash a Treasure from your hand. Gain a Treasure to your hand costing up to 3C more than it.",
			p -> {
				List<DominionCard> trashChoices = p.getHand().stream().filter(c -> c.getType() == Treasure)
						.collect(Collectors.toList());
				Optional<DominionCard> trashCard = p.chooseCardOrSkip(trashChoices, "Trash a Treasure card, or skip.");
				if (trashCard.isPresent()) {
					p.removeFromHand(trashCard.get());
					p.trash(trashCard.get());
					int cost = trashCard.get().getCost() + 2;
					List<DominionCard> gainChoices = p.getSupplyCardsAtMostCost(cost).stream()
							.filter(c -> c.getType() == Treasure).collect(Collectors.toList());
					if (!gainChoices.isEmpty()) {
						DominionCard gainCard = p.chooseCard(gainChoices, "Gain a card.");
						p.takeFromSupply(gainCard);
						p.gain(gainCard);
					}
				}
			}), //

	Sentry(Emojis.GUARDSMAN, 5, Action,
			"+1D|+1A|Look at the top 2 cards of your deck. Trash and/or discard any number of them. Put the rest back on top in any order.",
			p -> {
				p.drawCards(1);
				p.addActions(1);
				List<DominionCard> choices = new ArrayList<>(2);
				p.takeFromDeck().ifPresent(choices::add);
				p.takeFromDeck().ifPresent(choices::add);
				while (!choices.isEmpty()) {
					Optional<DominionCard> card = p.chooseCardOrSkip(choices, "Trash any cards, or skip.");
					if (card.isPresent()) {
						choices.remove(card.get());
						p.trash(card.get());
					} else {
						break;
					}
				}
				while (!choices.isEmpty()) {
					Optional<DominionCard> card = p.chooseCardOrSkip(choices, "Discard any cards, or skip.");
					if (card.isPresent()) {
						choices.remove(card.get());
						p.discard(card.get());
					} else {
						break;
					}
				}
				while (choices.size() > 1) {
					DominionCard card = p.chooseCard(choices, "Put card on top of deck.");
					choices.remove(card);
					p.putOnDeck(card);
				}
				if (choices.size() == 1) {
					p.putOnDeck(choices.get(0));
				}
			}), //

	Artisan(Emojis.PAINTBRUSH, 6, Action,
			"Gain a card to your hand costing up to 5C.|Put a card from your hand onto your deck.", p -> {
				List<DominionCard> gainChoices = p.getSupplyCardsAtMostCost(5);
				if (!gainChoices.isEmpty()) {
					DominionCard gainCard = p.chooseCard(gainChoices, "Gain a card.");
					p.takeFromSupply(gainCard);
					p.putInHand(gainCard);
					DominionCard putCard = p.chooseCard(p.getHand(), "Put card on top of deck.");
					p.removeFromHand(putCard);
					p.putOnDeck(putCard);
				}
			}), //

	Chancellor(Emojis.OLDER_MAN, 3, Action, "+2C|You may immediately put your deck into your discard pile.", p -> {
		p.addCoins(2);
		if (p.chooseActionOrSkip(Emojis.ARROW_HEADING_UP, "Put your deck into your discard.",
				"Choose action, or skip.")) {
			p.getDiscard().addAll(p.getDeck());
			p.getDeck().clear();
		}
	}), //

	Feast(Emojis.STEW, 4, Action, "Trash this card. Gain a card costing up to 5C.", p -> {
		DominionCard feast = valueOf("Feast");
		if (p.removeFromPlay(feast)) {
			p.trash(feast);
			List<DominionCard> choices = p.getSupplyCardsAtMostCost(5);
			if (!choices.isEmpty()) {
				DominionCard gainCard = p.chooseCard(choices, "Gain a card.");
				p.takeFromSupply(gainCard);
				p.gain(gainCard);
			}
		}
	}), //

	Spy(Emojis.SPY, 4, Action, Attack,
			"+1D|+1A|Each player (including you) reveals the top card of his deck and either discard it or puts it back, your choice.",
			p -> {
				p.drawCards(1);
				p.addActions(1);
				for (Player player : p.getAllPlayers()) {
					Optional<DominionCard> card = player.takeFromDeck();
					if (card.isPresent()) {
						if (p.chooseCardOrSkip(card.get(), "Discard " + player.getName() + "'s card, or skip it.")) {
							player.discard(card.get());
						} else {
							player.putOnDeck(card.get());
						}
					}
				}
			}), //

	Thief(Emojis.FOX, 4, Action, Attack,
			"Each other player reveals the top 2 cards of his deck. If they revealed any Treasure cards, they trash one of them that you choose. You may gain any or all of these trashed cards. They discard the other revealed cards.",
			p -> {
				for (Player other : p.getOtherPlayers()) {
					List<DominionCard> cards = new ArrayList<>(2);
					other.takeFromDeck().ifPresent(cards::add);
					other.takeFromDeck().ifPresent(cards::add);
					cards.removeIf(c -> {
						if (c.getType() != Treasure) {
							other.discard(c);
							return true;
						}
						return false;
					});
					if (!cards.isEmpty()) {
						DominionCard trashCard = p.chooseCard(cards, "Trash one of " + other.getName() + "'s cards.");
						if (p.chooseCardOrSkip(trashCard, "Gain the trashed card, or skip it.")) {
							p.gain(trashCard);
						} else {
							other.trash(trashCard);
						}
					}
					for (DominionCard card : cards) {
						other.discard(card);
					}
				}
			}), //

	Bureaucrat(Emojis.CLASSICAL_BUILDING, 4, Action, Attack,
			"Gain a Silver onto your deck. Each other player reveals a Victory card from their hand and puts it onto their deck (or reveals a hand with no Victory cards).",
			p -> {
				if (p.takeFromSupply(Silver)) {
					p.putOnDeck(Silver);
				}
				for (Player other : p.getOtherPlayers()) {
					List<DominionCard> victoryCards = other.getHand().stream().filter(c -> c.getType() == Victory)
							.collect(Collectors.toList());
					boolean revealHand;
					if (!victoryCards.isEmpty()) {
						Optional<DominionCard> card = other.hiddenChooseCardOrSkip(victoryCards,
								p.getName() + " played Bureaucrat. Reveal a Victory card, or skip it.");
						if (card.isPresent()) {
							revealHand = false;
							p.reveal(card.get(), other.getName() + "'s Victory card is revealed.");
						} else {
							revealHand = true;
						}
					} else {
						revealHand = true;
					}
					if (revealHand) {
						p.reveal(other.getHand(), other.getName() + "'s hand is revealed.");
					}
				}
			}), //

	Militia(Emojis.GUN, 4, Action, Attack, "+2C|Each other player discards down to 3 cards in hand.", p -> {
		p.addCoins(2);
		for (Player other : p.getOtherPlayers()) {
			while (other.getHand().size() > 3) {
				DominionCard card = other.hiddenChooseCard(other.getHand(),
						p.getName() + " played Militia. Discard down to 3 cards.");
				other.removeFromHand(card);
				other.discard(card);
			}
		}
	}),//

	// TODO Moat(Emojis.SHIELD, 2, Action, Reaction,
	// "+2D|When another player plays an Attack card, you may first reveal this from
	// your hand, to be unaffected by it."), //
	;

	public enum CardSubType {
		None, Reaction, Attack
	}

	public static class CardType<F> {
		public static final CardType<TypeActionInteraction> Action = new CardType<>("Action");
		public static final CardType<TypeTreasureInteraction> Treasure = new CardType<>("Treasure");
		public static final CardType<TypeVictoryInteraction> Victory = new CardType<>("Victory");

		private final String name;

		private CardType(String name) {
			this.name = name;
		}

		@SuppressWarnings("unchecked")
		public F getInteraction(DominionCard card) {
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

	public static final DominionCard[] kingdoms = new DominionCard[values().length - 7];
	static {
		for (int i = 7; i < values().length; i++) {
			kingdoms[i - 7] = values()[i];
		}
	}

	private static final LinkedHashMap<String, String> textReplace = new LinkedHashMap<>();
	static {
		for (CardType<?> cardType : Arrays.asList(Action, Treasure, Victory)) {
			textReplace.put(cardType.name, "**" + cardType.name + "**");
		}

		for (CardSubType cardSubType : CardSubType.values()) {
			textReplace.put(cardSubType.name(), "**" + cardSubType.name() + "**");
		}

		for (DominionCard card : values()) {
			textReplace.put(card.getTitle(), "**" + card.getEmoji() + card.getTitle() + "**");
		}

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

	private <F> DominionCard(String emoji, int cost, CardType<F> type, CardSubType subType, String text,
			F typeFunction) {
		this.typeFunction = typeFunction;
		this.title = name().replace('_', ' ');
		this.emoji = emoji;
		this.cost = cost;
		this.type = type;
		this.subType = subType;
		this.text = text;
	}

	private <F> DominionCard(String emoji, int cost, CardType<F> type, String text, F typeFunction) {
		this(emoji, cost, type, CardSubType.None, text, typeFunction);
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
