package com.demod.discord.boredgames;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class Emojis {
	public static final String BLOCK_0 = "0⃣";
	public static final String BLOCK_1 = "1⃣";
	public static final String BLOCK_2 = "2⃣";
	public static final String BLOCK_3 = "3⃣";
	public static final String BLOCK_4 = "4⃣";
	public static final String BLOCK_5 = "5⃣";
	public static final String BLOCK_6 = "6⃣";
	public static final String BLOCK_7 = "7⃣";
	public static final String BLOCK_8 = "8⃣";
	public static final String BLOCK_9 = "9⃣";
	public static final String BLOCK_10 = "\uD83D\uDD1F";
	public static final String BLOCK_1234 = "\uD83D\uDD22";
	public static final String[] BLOCK_NUMBER = { //
			BLOCK_0, //
			BLOCK_1, //
			BLOCK_2, //
			BLOCK_3, //
			BLOCK_4, //
			BLOCK_5, //
			BLOCK_6, //
			BLOCK_7, //
			BLOCK_8, //
			BLOCK_9, //
			BLOCK_10,//
	};

	public static final String HAND_SPLAYED = "\uD83D\uDD90";

	public static final String SMALL_BLACK_SQUARE = "\u25AA";

	public static final String WHITE_CIRCLE = "\u26AA";
	public static final String RED_CIRCLE = "\uD83D\uDD34";
	public static final String BLUE_CIRCLE = "\uD83D\uDD35";

	public static final String SMALL_RED_DOWN_ARROW = "\uD83D\uDD3B";

	public static final String ARROW_UP = "\u2B06";
	public static final String ARROW_FORWARD = "\u25B6";
	public static final String REWIND = "\u23EA";
	public static final String SHUFFLE = "\uD83D\uDD00";
	public static final String TRACK_NEXT = "\u23ED";

	public static final String QUESTION = "\u2753";
	public static final String BANGBANG = "\u203C";

	public static final String LOCK = "\uD83D\uDD12";

	public static final String RED_HEART = "\u2764";
	public static final String BLUE_HEART = "\uD83D\uDC99";
	public static final String GREEN_HEART = "\uD83D\uDC9A";
	public static final String YELLOW_HEART = "\uD83D\uDC9B";

	public static final String RED_BOOK = "\uD83D\uDCD5";
	public static final String BLUE_BOOK = "\uD83D\uDCD8";
	public static final String GREEN_BOOK = "\uD83D\uDCD7";
	public static final String ORANGE_BOOK = "\uD83D\uDCD9";

	public static final String RED_CAR = "\uD83D\uDE97";
	public static final String BLUE_CAR = "\uD83D\uDE99";
	public static final String TRACTOR = "\uD83D\uDE9C";
	public static final String TAXI = "\uD83D\uDE95";

	public static final String SOCCER = "\u26BD";
	public static final String BASKETBALL = "\uD83C\uDFC0";
	public static final String FOOTBALL = "\uD83C\uDFC8";
	public static final String BASEBALL = "\u26BE";

	public static final String APPLE = "\uD83C\uDF4E";
	public static final String TANGERINE = "\uD83C\uDF4A";
	public static final String PEAR = "\uD83C\uDF50";
	public static final String LEMON = "\uD83C\uDF4B";
	public static final String HAMBURGER = "\uD83C\uDF54";
	public static final String PIZZA = "\uD83C\uDF55";
	public static final String HOT_DOG = "\uD83C\uDF2D";
	public static final String FRIES = "\uD83C\uDF5F";
	public static final String ICE_CREAM = "\uD83C\uDF68";
	public static final String CAKE = "\uD83C\uDF70";
	public static final String COOKIE = "\uD83C\uDF6A";
	public static final String DOUGHNUT = "\uD83C\uDF69";

	public static final String FOX = "\uD83E\uDD8A";
	public static final String DOG = "\uD83D\uDC36";
	public static final String CAT = "\uD83D\uDC31";
	public static final String MOUSE = "\uD83D\uDC2D";

	public static final String FAMILY_MMB = "\uD83D\uDC68\u200D\uD83D\uDC68\u200D\uD83D\uDC66";
	public static final String FAMILY_MMBB = "\uD83D\uDC68\u200D\uD83D\uDC68\u200D\uD83D\uDC66\u200D\uD83D\uDC66";
	public static final String CONSTRUCTION_WORKER = "\uD83D\uDC77";

	public static final String SIGNAL_STRENGTH = "\uD83D\uDCF6";

	public static final String GAME_DIE = "\uD83C\uDFB2";
	public static final String DOLLAR = "\uD83D\uDCB5";
	public static final String MONEYBAG = "\uD83D\uDCB0";
	public static final String GEM = "\uD83D\uDC8E";
	public static final String SCALES = "\u2696";
	public static final String SKULL = "\uD83D\uDC80";
	public static final String MICROSCOPE = "\uD83D\uDD2C";
	public static final String TULIP = "\uD83C\uDF37";
	public static final String HAMMER_PICK = "\u2692";
	public static final String SHOPPING_BAGS = "\uD83D\uDECD";
	public static final String CRYSTAL_BALL = "\uD83D\uDD2E";
	public static final String CROSSED_SWORDS = "\u2694";
	public static final String TICKET = "\uD83C\uDFAB";
	public static final String SHIELD = "\uD83D\uDEE1";

	public static final String RICE_SCENE = "\uD83C\uDF91";
	public static final String FOUNTAIN = "\u26F2";
	public static final String PARK = "\uD83C\uDFDE";
	public static final String HOMES = "\uD83C\uDFD8";
	public static final String OFFICE = "\uD83C\uDFE2";
	public static final String HOUSE = "\uD83C\uDFE0";
	public static final String CIRCUS_TENT = "\uD83C\uDFAA";

	public static final String[][] PLAYER_SETS = { //
			{ RED_HEART, BLUE_HEART, GREEN_HEART, YELLOW_HEART }, //
			{ RED_BOOK, BLUE_BOOK, GREEN_BOOK, ORANGE_BOOK }, //
			{ RED_CAR, BLUE_CAR, TRACTOR, TAXI }, //
			{ SOCCER, BASKETBALL, FOOTBALL, BASEBALL }, //
			{ APPLE, TANGERINE, PEAR, LEMON }, //
			{ HAMBURGER, PIZZA, HOT_DOG, FRIES }, //
			{ ICE_CREAM, CAKE, COOKIE, DOUGHNUT }, //
			{ FOX, DOG, CAT, MOUSE }, //
			{ QUESTION, QUESTION, QUESTION, QUESTION },//
	};

	public static String[] getRandomPlayerSet() {
		List<String> shuffleList = Arrays.asList(PLAYER_SETS[new Random().nextInt(PLAYER_SETS.length)]);
		Collections.shuffle(shuffleList);
		return shuffleList.stream().toArray(String[]::new);
	}
}
