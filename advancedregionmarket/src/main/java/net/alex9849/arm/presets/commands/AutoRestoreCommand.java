package net.alex9849.arm.presets.commands;

import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.Permission;
import net.alex9849.arm.presets.presets.Preset;
import net.alex9849.arm.presets.presets.PresetType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutoRestoreCommand extends PresetOptionModifyCommand<Boolean> {

    public AutoRestoreCommand(PresetType presetType, AdvancedRegionMarket plugin) {
        super("autorestore", plugin, Arrays.asList(Permission.ADMIN_PRESET_SET_AUTORESTORE),
                true, "(false|true|(?i)remove)", "(true/false/remove)", "", presetType);
    }

    @Override
    protected Boolean getSettingsFromString(CommandSender sender, String setting) {
        if(setting.equalsIgnoreCase("remove")) {
            return null;
        }
        return Boolean.parseBoolean(setting);
    }

    @Override
    protected void applySetting(CommandSender sender, Preset object, Boolean setting) {
        object.setAutoRestore(setting);
    }

    @Override
    protected List<String> tabCompleteSettingsObject(Player player, String settings) {
        List<String> returnme = new ArrayList<>();
        if ("true".startsWith(settings)) {
            returnme.add("true");
        }
        if ("false".startsWith(settings)) {
            returnme.add("false");
        }
        if ("remove".startsWith(settings)) {
            returnme.add("remove");
        }
        return returnme;
    }
}
