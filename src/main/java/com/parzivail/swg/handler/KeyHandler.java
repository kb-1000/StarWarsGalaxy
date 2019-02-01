package com.parzivail.swg.handler;

import com.parzivail.swg.StarWarsGalaxy;
import com.parzivail.swg.proxy.Client;
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent;
import cpw.mods.fml.common.gameevent.InputEvent.MouseInputEvent;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

/**
 * Created by colby on 9/30/2016.
 */
public class KeyHandler
{
	public static void onInput(KeyInputEvent event)
	{
		onInput();
	}

	public static void onInput(MouseInputEvent event)
	{
		onInput();
	}

	private static void onInput()
	{
		StarWarsGalaxy.proxy.checkLeftClickPressed(false);

		if (Keyboard.isKeyDown(Keyboard.KEY_I))
		{
			Client.guiQuestNotification.show();
		}

		//		if (KeybindRegistry.keyDebug.getIsKeyPressed())
		//		{
		//		}
	}

	public static void handleVehicleMovement()
	{
		// TODO
		//		EntityShipParentTest ship = SwgEntityUtil.getShipRiding(Client.mc.thePlayer);
		//		if (Client.mc.thePlayer != null && ship != null)
		//		{
		//			if ($(Client.mc.gameSettings.keyBindLeft))
		//				ship.acceptInput(ShipInput.RollLeft);
		//
		//			if ($(Client.mc.gameSettings.keyBindRight))
		//				ship.acceptInput(ShipInput.RollRight);
		//
		//			if ($(Client.mc.gameSettings.keyBindForward))
		//				ship.acceptInput(ShipInput.PitchDown);
		//
		//			if ($(Client.mc.gameSettings.keyBindBack))
		//				ship.acceptInput(ShipInput.PitchUp);
		//
		//			if ($(Client.mc.gameSettings.keyBindJump))
		//				ship.acceptInput(ShipInput.ThrottleUp);
		//
		//			if ($(Client.mc.gameSettings.keyBindSprint))
		//				ship.acceptInput(ShipInput.ThrottleDown);
		//		}
	}

	private static boolean $(KeyBinding key)
	{
		return Keyboard.isKeyDown(key.getKeyCode());
	}
}
