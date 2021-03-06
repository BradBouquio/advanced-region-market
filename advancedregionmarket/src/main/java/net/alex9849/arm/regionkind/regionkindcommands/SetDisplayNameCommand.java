package net.alex9849.arm.regionkind.regionkindcommands;

import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.Permission;
import net.alex9849.arm.regionkind.RegionKind;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetDisplayNameCommand extends RegionKindOptionModifyCommand<String> {

    public SetDisplayNameCommand(AdvancedRegionMarket plugin) {
        super("setdisplayname",
                Arrays.asList(Permission.REGIONKIND_SET_DISPLAYNAME), "[^;\n]+",
                "[Displayname]", "", plugin);
    }

    @Override
    protected String getSettingsFromString(CommandSender sender, String setting) {
        return setting;
    }

    @Override
    protected void applySetting(CommandSender sender, RegionKind object, String setting) {
        object.setDisplayName(setting);
    }

    @Override
    protected List<String> tabCompleteSettingsObject(Player player, String setting) {
        return new ArrayList<>();
    }
}