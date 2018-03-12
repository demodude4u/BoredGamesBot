package com.demod.discord.boredgames.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.demod.discord.boredgames.Emojis;
import com.demod.discord.boredgames.Game;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;

public class Connect4Game extends Game {
	public static enum Tile {
		NONE, P1, P2, P3, P4
	}

	private static final int IN_A_ROW = 4;

	private boolean hotseat = true;
	private String[] emojiPlayerSet;
	private int columns = 7;
	private int rows = 6;
	private Tile[/* slot */][/* height */] tiles;
	private final List<Member> players = new ArrayList<>(4);
	private int winner = -1;
	private int lastTurnColumn = -1;
	private int lastTurn = -1;
	private int turnCount = 0;

	public Connect4Game() {
		tiles = new Tile[columns][rows];
		for (Tile[] column : tiles) {
			Arrays.fill(column, Tile.NONE);
		}

		emojiPlayerSet = Emojis.getRandomPlayerSet();
	}

	private Connect4Game(Connect4Game copy) {
		this.hotseat = copy.hotseat;
		this.columns = copy.columns;
		this.rows = copy.rows;
		tiles = new Tile[columns][rows];
		for (int i = 0; i < tiles.length; i++) {
			System.arraycopy(copy.tiles[i], 0, tiles[i], 0, tiles[i].length);
		}
		this.emojiPlayerSet = copy.emojiPlayerSet;
		this.players.addAll(copy.players);
		this.winner = copy.winner;
		this.lastTurnColumn = copy.lastTurnColumn;
		this.lastTurn = copy.lastTurn;
		this.turnCount = copy.turnCount;
	}

	@Override
	public boolean allowUndo() {
		// TODO determine if undo should be allowed, or perhaps some penalty or
		// vote system

		// FIXME Disabled until new system in place
		return false;
	}

	public void applyMove(Member player, int column) {
		int turn = (lastTurn + 1) % players.size();
		Member playerTurn = players.get(turn);
		if (!player.equals(playerTurn)) {
			return;
		}

		Tile tile = Tile.values()[1 + turn];
		for (int i = rows - 1; i >= 0; i--) {
			final int height = i;
			if (height == 0 || tiles[column][height - 1] != Tile.NONE) {
				tiles[column][height] = tile;
				lastTurnColumn = column;
				turnCount++;
				lastTurn = turn;
				checkForWinner(column, height, tile);
				return;
			}
		}
		throw new InternalError("Illegal Game State!");
	}

	@Override
	public void buildDisplay(EmbedBuilder embed) {
		embed.setAuthor("Connect Four!");

		if (hotseat) {
			embed.setDescription(
					"This is a 2-4 player game. Try to create a 4 in a row chain of your player token to win! Press the "
							+ Emojis.HAND_SPLAYED + " to join, and press the " + Emojis.GAME_DIE
							+ " to begin the game!");

			if (!players.isEmpty()) {
				embed.addField("Players",
						IntStream.range(0, players.size())
								.mapToObj(i -> emojiPlayerSet[i] + " " + players.get(i).getEffectiveName())
								.collect(Collectors.joining("\n")),
						true);
			}

		} else if (isGameOver()) {
			if (emojiPlayerSet[0].equals(Emojis.QUESTION)) {
				emojiPlayerSet = Emojis.getRandomPlayerSet();
			}
			if (winner != -1) {
				embed.addField(players.get(winner).getEffectiveName() + " Wins!",
						IntStream.range(0, 4).mapToObj(i -> emojiPlayerSet[winner]).collect(Collectors.joining()),
						true);
			} else {
				embed.addField("It's a Draw!", IntStream.range(0, players.size()).mapToObj(i -> emojiPlayerSet[i])
						.collect(Collectors.joining()), true);
			}

			embed.setDescription(generateTilesEmoji());

			embed.setFooter("This game has ended.", null);

		} else {
			int turn = (lastTurn + 1) % players.size();
			String message = IntStream.range(0, players.size()).mapToObj(i -> emojiPlayerSet[i])
					.collect(Collectors.joining())
					+ "\n"
					+ IntStream.range(0, players.size())
							.mapToObj(i -> (turn == i) ? Emojis.ARROW_UP : Emojis.SMALL_BLACK_SQUARE)
							.collect(Collectors.joining());
			embed.addField(players.get(turn).getEffectiveName() + "'s Turn", message, true);

			embed.setDescription(generateTilesEmoji());

			embed.setFooter("Press the column number to place your piece.", null);
		}

	}

	private boolean canMove(int column) {
		return tiles[column][rows - 1] == Tile.NONE;
	}

	private void checkForWinner(int column, int height, Tile tile) {
		int inARow, startColumn, startHeight;

		// Check for horizontal
		inARow = 0;
		for (int i = 0; i < columns; i++) {
			if (tiles[i][height] == tile) {
				inARow++;
				if (inARow == IN_A_ROW) {
					winner = tile.ordinal() - 1;
					return;
				}
			} else {
				inARow = 0;
			}
		}

		// Check for vertical
		inARow = 0;
		for (int i = 0; i < rows; i++) {
			if (tiles[column][i] == tile) {
				inARow++;
				if (inARow == IN_A_ROW) {
					winner = tile.ordinal() - 1;
					return;
				}
			} else {
				inARow = 0;
			}
		}

		// Check for diagonal SE -> NW
		inARow = 0;
		startColumn = Math.max(0, column - height);
		startHeight = height - (column - startColumn);
		for (int i = 0; (startColumn + i < columns) && (startHeight + i < rows); i++) {
			if (tiles[startColumn + i][startHeight + i] == tile) {
				inARow++;
				if (inARow == IN_A_ROW) {
					winner = tile.ordinal() - 1;
					return;
				}
			} else {
				inARow = 0;
			}
		}

		// Check for diagonal SW -> NE
		inARow = 0;
		startColumn = Math.min(columns - 1, column + height);
		startHeight = height - (startColumn - column);
		for (int i = 0; (startColumn - i >= 0) && (startHeight + i < rows); i++) {
			if (tiles[startColumn - i][startHeight + i] == tile) {
				inARow++;
				if (inARow == IN_A_ROW) {
					winner = tile.ordinal() - 1;
					return;
				}
			} else {
				inARow = 0;
			}
		}
	}

	@Override
	public Game copy() {
		return new Connect4Game(this);
	}

	private String generateTilesEmoji() {
		StringBuilder sb = new StringBuilder();
		for (int height = rows - 1; height >= 0; height--) {
			for (int column = 0; column < columns; column++) {
				Tile tile = tiles[column][height];
				if (tile == Tile.NONE) {
					if (height == rows - 1 && column == lastTurnColumn) {
						sb.append(Emojis.SMALL_RED_DOWN_ARROW);
					} else {
						sb.append(Emojis.SMALL_BLACK_SQUARE);
					}
				} else {
					sb.append(emojiPlayerSet[tile.ordinal() - 1]);
				}
			}
			sb.append('\n');
		}
		for (int column = 0; column < columns; column++) {
			sb.append(Emojis.BLOCK_NUMBER[column + 1]);
		}
		return sb.toString();
	}

	public void hotseatGameStart(Member player) {
		if (!players.contains(player)) {
			return;
		}
		hotseat = false;
	}

	public void hotseatNewPlayer(Member player) {
		if (players.size() < 4 && !players.contains(player)) {
			players.add(player);

			if (players.size() >= 3) {
				rows++;
				columns++;
				tiles = Arrays.copyOf(tiles, columns);
				for (int i = 0; i < columns - 1; i++) {
					tiles[i] = Arrays.copyOf(tiles[i], rows);
					tiles[i][rows - 1] = Tile.NONE;
				}
				tiles[columns - 1] = new Tile[rows];
				Arrays.fill(tiles[columns - 1], Tile.NONE);
			}
		}
	}

	@Override
	public boolean isGameOver() {
		if (winner != -1) {
			return true;
		}
		for (int column = 0; column < columns; column++) {
			if (tiles[column][rows - 1] == Tile.NONE) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void registerActions(ActionRegistry registry) {
		if (hotseat) {
			if (players.size() < 4) {
				registry.addAction(Emojis.HAND_SPLAYED, this::hotseatNewPlayer);
			}
			if (players.size() >= 2) {
				registry.addAction(Emojis.GAME_DIE, this::hotseatGameStart);
			}

		} else {
			for (int i = 0; i < columns; i++) {
				final int column = i;
				if (canMove(column)) {
					registry.addAction(Emojis.BLOCK_NUMBER[column + 1], (player) -> applyMove(player, column));
				}
			}
		}
	}
}