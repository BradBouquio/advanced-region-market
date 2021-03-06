package net.alex9849.arm.presets.commands;

import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.Messages;
import net.alex9849.arm.Permission;
import net.alex9849.arm.exceptions.InputException;
import net.alex9849.arm.presets.presets.Preset;
import net.alex9849.arm.presets.presets.PresetType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PriceCommand extends PresetOptionModifyCommand<Double> {

    public PriceCommand(PresetType presetType, AdvancedRegionMarket plugin) {
        super("price", plugin, Arrays.asList(Permission.ADMIN_PRESET_SET_PRICE),
                true, "(([0-9]+[.])?[0-9]+|(?i)remove)", "[PRICE]", "", presetType);
    }

    @Override
    protected Double getSettingsFromString(CommandSender sender, String setting) throws InputException {
        if(setting.equalsIgnoreCase("remove")) {
            return null;
        }
        return Double.parseDouble(setting);
    }

    @Override
    protected void applySetting(CommandSender sender, Preset object, Double setting) {
        object.setPrice(setting);
    }

    @Override
    protected void sendSuccessMessage(CommandSender sender, Preset obj, Double aDouble) {
        super.sendSuccessMessage(sender, obj, aDouble);
        if(obj.canPriceLineBeLetEmpty()) {
            sender.sendMessage(Messages.PREFIX + "You can leave the price-line on signs empty now");
        }
    }

    @Override
    protected List<String> tabCompleteSettingsObject(Player player, String settings) {
        if ("remove".startsWith(settings)) {
            return Arrays.asList("remove");
        }
        return new ArrayList<>();
    }
}
