package com.demod.discord.boredgames.game;

import com.demod.discord.boredgames.Emojis;
import com.demod.discord.boredgames.Game;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

public class TestGame extends Game {

	@Override
	protected String getTitle() {
		return "Test Game";
	}

	private User getVolunteer() {
		return this.<User>displayChannel(embed -> {
			embed.setDescription("Raise your hand for testing!");
		}).addResultAction(ButtonStyle.SUCCESS, Emojis.HAND_SPLAYED, "Join", p -> p).send();
	}

	private boolean privateHighFive(User player) {
		displayChannel(embed -> {
			embed.setDescription("Waiting for a response from **" + player.getEffectiveName() + "**...");
		}).send();
		boolean result = this.<Boolean>displayPrivate(player, embed -> {
			embed.setDescription("Give me a high five! " + Emojis.HAND_SPLAYED);
		})//
				.addResult(ButtonStyle.PRIMARY, Emojis.HAND_SPLAYED, "High Five", true)//
				.addResult(ButtonStyle.SECONDARY, Emojis.TRACK_NEXT, "Nah", false)//
				.send();
		displayPrivate(player, embed -> {
			embed.setDescription("Thank you for the response!");
		}).send();
		return result;
	}

	@Override
	public void run() {
		User player = getVolunteer();
		boolean highFived = privateHighFive(player);
		displayChannel(embed -> {
			if (highFived) {
				embed.setDescription("Righteous! " + Emojis.HAND_SPLAYED);
			} else {
				embed.setDescription("Aww man...");
			}
		}).send();
	}

}
