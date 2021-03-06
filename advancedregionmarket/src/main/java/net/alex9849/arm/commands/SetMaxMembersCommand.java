package net.alex9849.arm.commands;

import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.Messages;
import net.alex9849.arm.Permission;
import net.alex9849.arm.regions.Region;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetMaxMembersCommand extends RegionOptionModifyCommand<Integer> {

    public SetMaxMembersCommand(AdvancedRegionMarket plugin) {
        super("setmaxmembers", plugin, Arrays.asList(Permission.ADMIN_SET_MAX_MEMBERS), true, "maxMembers",
                "([0-9]+|unlimited)", "[AMOUNT/UNLIMITED]", false,
                Messages.SUBREGION_MAX_MEMBERS_ERROR, "");
    }

    @Override
    protected void applySetting(Player sender, Region region, Integer setting) {
        region.setMaxMembers(setting);
    }

    @Override
    protected Integer getSettingFromString(Player player, String settingsString) {
        int newMaxMembers = -1;
        if(!settingsString.equalsIgnoreCase("unlimited")) {
            newMaxMembers = Integer.parseInt(settingsString);
        }
        return newMaxMembers;
    }

    @Override
    protected List<String> tabCompleteSettingsObject(Player player, String setting) {
        if("unlimited".startsWith(setting)) {
            return Arrays.asList("unlimited");
        }
        return new ArrayList<>();
    }
}
