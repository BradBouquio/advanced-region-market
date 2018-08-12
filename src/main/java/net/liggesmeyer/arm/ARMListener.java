package net.liggesmeyer.arm;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.liggesmeyer.arm.gui.Gui;
import net.liggesmeyer.arm.regions.Region;
import net.liggesmeyer.arm.regions.RegionKind;
import net.liggesmeyer.arm.regions.RentRegion;
import net.liggesmeyer.arm.regions.SellRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ARMListener implements Listener {


    @EventHandler
    public void addSign(SignChangeEvent sign) {
        if(sign.getLine(0).equalsIgnoreCase("[ARM-Sell]")){
            if(!sign.getPlayer().hasPermission(Permission.ADMIN_CREATE_SELL)){
                sign.getPlayer().sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
                return;
            }

            String worldname = sign.getLine(1);
            String regionname = sign.getLine(2);

            if (sign.getLine(1).equals("")){
                worldname = sign.getBlock().getLocation().getWorld().getName();
            } else {
                if (Bukkit.getWorld(worldname) == null) {
                    sign.getPlayer().sendMessage(Messages.PREFIX + Messages.WORLD_DOES_NOT_EXIST);
                    return;
                }
            }

            if (Main.getWorldguard().getRegionManager(Bukkit.getWorld(worldname)).getRegion(regionname) == null) {
                sign.getPlayer().sendMessage(Messages.PREFIX + Messages.REGION_DOES_NOT_EXIST);
                return;
            }
            ProtectedRegion region = Main.getWorldguard().getRegionManager(Bukkit.getWorld(worldname)).getRegion(regionname);
            Double price;


            try{
                price = Region.calculatePrice(region, sign.getLine(3));
            } catch (IllegalArgumentException e){
                sign.getPlayer().sendMessage(Messages.PREFIX + Messages.PLEASE_USE_A_NUMBER_AS_PRICE + " or a RegionType");
                return;
            }

            if(price < 0) {
                sign.getPlayer().sendMessage(Messages.PREFIX + ChatColor.DARK_RED + "Price must be positive!");
                return;
            }


            for(int i = 0; i < Region.getRegionList().size(); i++) {
                if (Region.getRegionList().get(i).getRegionworld().equals(worldname)) {
                    if (Region.getRegionList().get(i).getRegion().getId().equals(regionname)) {
                        if (Region.getRegionList().get(i) instanceof RentRegion) {
                            sign.getPlayer().sendMessage(Messages.PREFIX + "Region already registred as rentregion");
                            return;
                        }
                        Region.getRegionList().get(i).addSign(sign.getBlock().getLocation());
                        sign.setCancelled(true);
                        sign.getPlayer().sendMessage(Messages.PREFIX + Messages.SIGN_ADDED_TO_REGION);
                        return;

                    }
                }
            }
            LinkedList<Sign> sellsign = new LinkedList<Sign>();
            sellsign.add((Sign) sign.getBlock().getState());
            Material defaultlogo = Material.BED;
            Region.getRegionList().add(new SellRegion(region, worldname, sellsign, price, false, true, false, true, RegionKind.DEFAULT, null,1,true));
            sign.getPlayer().sendMessage(Messages.PREFIX + Messages.REGION_ADDED_TO_ARM);
            sign.setCancelled(true);
        }


        if(sign.getLine(0).equalsIgnoreCase("[ARM-Rent]")){
            if(!sign.getPlayer().hasPermission(Permission.ADMIN_CREATE_RENT)){
                sign.getPlayer().sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
                return;
            }
            String worldname = sign.getLine(1);
            String regionname = sign.getLine(2);

            if (sign.getLine(1).equals("")){
                worldname = sign.getBlock().getLocation().getWorld().getName();
            } else {
                if (Bukkit.getWorld(worldname) == null) {
                    sign.getPlayer().sendMessage(Messages.PREFIX + Messages.WORLD_DOES_NOT_EXIST);
                    return;
                }
            }
            if (Main.getWorldguard().getRegionManager(Bukkit.getWorld(worldname)).getRegion(regionname) == null) {
                sign.getPlayer().sendMessage(Messages.PREFIX + Messages.REGION_DOES_NOT_EXIST);
                return;
            }
            ProtectedRegion region = Main.getWorldguard().getRegionManager(Bukkit.getWorld(worldname)).getRegion(regionname);

            double price = 0;
            long extendPerClick = 0;
            long maxRentTime = 0;

            try{
                String[] priceline = sign.getLine(3).split(";", 3);
                String pricestring = priceline[0];
                String extendPerClickString = priceline[1];
                String maxRentTimeString = priceline[2];
                extendPerClick = RentRegion.stringToTime(extendPerClickString);
                maxRentTime = RentRegion.stringToTime(maxRentTimeString);
                price = Integer.parseInt(pricestring);
            } catch (ArrayIndexOutOfBoundsException e) {
                sign.getPlayer().sendMessage(Messages.PREFIX + "Please write your price in line 3 in the following pattern:");
                sign.getPlayer().sendMessage("<Price>;<Extend per Click (ex.: 5d)>;<Max rent Time (ex.: 10d)>");
                return;
            } catch (IllegalArgumentException e) {
                sign.getPlayer().sendMessage(Messages.PREFIX + "Please use d for days, h for hours, m for minutes and s for seconds");
                sign.getPlayer().sendMessage(Messages.PREFIX + "Please write you price in line 3 in the following pattern:");
                sign.getPlayer().sendMessage("<Price>;<Extend per Click (ex.: 5d)>;<Max rent Time (ex.: 10d)>");
                return;
            }


            for(int i = 0; i < Region.getRegionList().size(); i++) {
                if (Region.getRegionList().get(i).getRegionworld().equals(worldname)) {
                    if (Region.getRegionList().get(i).getRegion().getId().equals(regionname)) {
                        if (Region.getRegionList().get(i) instanceof SellRegion) {
                            sign.getPlayer().sendMessage(Messages.PREFIX + "Region already registred as sellregion");
                            return;
                        }
                        Region.getRegionList().get(i).addSign(sign.getBlock().getLocation());
                        sign.setCancelled(true);
                        sign.getPlayer().sendMessage(Messages.PREFIX + Messages.SIGN_ADDED_TO_REGION);
                        return;

                    }
                }
            }
            LinkedList<Sign> sellsign = new LinkedList<Sign>();
            sellsign.add((Sign) sign.getBlock().getState());
            Material defaultlogo = Material.BED;
            Region.getRegionList().add(new RentRegion(region, worldname, sellsign, price, false, true, false, true, RegionKind.DEFAULT, null,
                    1,1, maxRentTime, extendPerClick, true));
            sign.getPlayer().sendMessage(Messages.PREFIX + Messages.REGION_ADDED_TO_ARM);
            sign.setCancelled(true);
        }
    }

    @EventHandler
    public void removeSign(BlockBreakEvent block) {
        if ((block.getBlock().getType() != Material.SIGN_POST) && (block.getBlock().getType() != Material.WALL_SIGN)) {
            return;
        }
        if (!Main.getWorldguard().canBuild(block.getPlayer(), block.getBlock().getLocation())) {
            return;
        }

        if(!Region.checkIfSignExists((Sign) block.getBlock().getState())){
            return;
        }

        if(!block.getPlayer().hasPermission(Permission.ADMIN_REMOVE)) {
            block.getPlayer().sendMessage(Messages.PREFIX + Messages.NO_PERMISSION);
            block.setCancelled(true);
            return;
        }
        double loc_x = block.getBlock().getLocation().getX();
        double loc_y = block.getBlock().getLocation().getY();
        double loc_z = block.getBlock().getLocation().getZ();
        Location loc = new Location(block.getBlock().getWorld(), loc_x, loc_y, loc_z);

        for (int i = 0; i < Region.getRegionList().size(); i++) {
            if(Region.getRegionList().get(i).removeSign(loc, block.getPlayer())){
                return;
            }
        }
    }

    @EventHandler
    public void buyRegion(PlayerInteractEvent event){
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getClickedBlock().getType() == Material.SIGN_POST || event.getClickedBlock().getType() == Material.WALL_SIGN) {
                Sign sign = (Sign) event.getClickedBlock().getState();

                for(int i = 0; i < Region.getRegionList().size(); i++){
                    if(Region.getRegionList().get(i).hasSign(sign)){
                        if(Region.getRegionList().get(i) instanceof SellRegion){
                            SellRegion region = (SellRegion) Region.getRegionList().get(i);
                            region.buy(event.getPlayer());
                            return;
                        }
                        if(Region.getRegionList().get(i) instanceof RentRegion){
                            RentRegion region = (RentRegion) Region.getRegionList().get(i);
                            region.buy(event.getPlayer());
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void addBuiltBlock(BlockPlaceEvent event){
        for(int i = 0; i < Region.getRegionList().size(); i++){
            if(Main.getWorldguard().canBuild(event.getPlayer(), event.getBlock().getLocation())){
                int x = event.getBlock().getLocation().getBlockX();
                int y = event.getBlock().getLocation().getBlockY();
                int z = event.getBlock().getLocation().getBlockZ();
                if (Region.getRegionList().get(i).getRegion().contains(x, y, z) && Region.getRegionList().get(i).getRegionworld().equals(event.getPlayer().getLocation().getWorld().getName())) {
                    if(Region.getRegionList().get(i).isHotel()){
                        if(Region.getRegionList().get(i).isSold()){
                            if(Main.getWorldguard().canBuild(event.getPlayer(), event.getBlock().getLocation())) {
                                Region.getRegionList().get(i).addBuiltBlock(event.getBlock().getLocation());
                            }
                        }
                    }
                    return;
                }
            }
        }

    }

    @EventHandler
    public void breakBlock(BlockBreakEvent event) {
        if(!event.getPlayer().hasPermission(Permission.ADMIN_BUILDEVERYWHERE)){
            if(Main.getWorldguard().canBuild(event.getPlayer(), event.getBlock().getLocation())){
                for(int i = 0; i < Region.getRegionList().size(); i++) {
                    if(Region.getRegionList().get(i).isHotel()){
                        int x = event.getBlock().getLocation().getBlockX();
                        int y = event.getBlock().getLocation().getBlockY();
                        int z = event.getBlock().getLocation().getBlockZ();
                        if(Region.getRegionList().get(i).getRegion().contains(x, y, z) && Region.getRegionList().get(i).getRegionworld().equals(event.getPlayer().getLocation().getWorld().getName())) {
                            if(Region.getRegionList().get(i).allowBlockBreak(event.getBlock().getLocation())){
                                return;
                            } else {
                                event.setCancelled(true);
                                event.getPlayer().sendMessage(Messages.PREFIX + Messages.REGION_ERROR_CAN_NOT_BUILD_HERE);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void protectSigns(BlockPhysicsEvent sign) {
        if (sign.getBlock().getType() == Material.SIGN_POST || sign.getBlock().getType() == Material.WALL_SIGN){
            for (int i = 0; i < Region.getRegionList().size() ; i++){
                if(Region.getRegionList().get(i).hasSign((Sign) sign.getBlock().getState())){
                    sign.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void setLastLoginAndOpenOvertake(PlayerJoinEvent event) {
        if(Main.getEnableAutoReset() || Main.getEnableTakeOver()){
            try{
                ResultSet rs = Main.getStmt().executeQuery("SELECT * FROM `" + Main.getSqlPrefix() + "lastlogin` WHERE `uuid` = '" + event.getPlayer().getUniqueId().toString() + "'");

                if(rs.next()){
                    Main.getStmt().executeUpdate("UPDATE `" + Main.getSqlPrefix() + "lastlogin` SET `lastlogin` = CURRENT_TIMESTAMP WHERE `uuid` = '" + event.getPlayer().getUniqueId().toString() + "'");
                } else {
                    Main.getStmt().executeUpdate("INSERT INTO `" + Main.getSqlPrefix() + "lastlogin` (`uuid`, `lastlogin`) VALUES ('" + event.getPlayer().getUniqueId().toString() + "', CURRENT_TIMESTAMP)");
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if(Main.getEnableTakeOver()){
            Plugin plugin = Bukkit.getPluginManager().getPlugin("AdvancedRegionMarket");
            Player player = event.getPlayer();
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    ARMListener.doOvertakeCheck(player);
                }
            }, 40L);
        }

    }

    public static void doOvertakeCheck(Player player) {
        GregorianCalendar comparedate = new GregorianCalendar();
        comparedate.add(Calendar.DAY_OF_MONTH, (-1 * Main.getTakeoverAfter()));
        Date convertdate = new Date();
        convertdate.setTime(comparedate.getTimeInMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String compareTime = sdf.format(convertdate);


        try {
            ResultSet rs = Main.getStmt().executeQuery("SELECT * FROM `" + Main.getSqlPrefix() + "lastlogin` WHERE `lastlogin` < '" + compareTime + "'");

            List<Region> overtake = new LinkedList<>();
            while (rs.next()){
                List<Region> regions = Region.getRegionsByOwner(UUID.fromString(rs.getString("uuid")));

                for(int i = 0; i < regions.size(); i++){
                    if(regions.get(i).getAutoreset()){
                        if(regions.get(i).getRegion().getMembers().contains(player.getUniqueId())){
                            overtake.add(regions.get(i));
                        }
                    }
                }
            }
            if(overtake.size() != 0){
                Gui.openOvertakeGUI(player, overtake);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}