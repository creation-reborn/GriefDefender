/*
 * This file is part of GriefDefender, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.griefdefender.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimBlockSystem;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageDataConfig;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.storage.BaseStorage;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_PLAYER_INFO_BASE)
public class CommandPlayerInfo extends BaseCommand {

    private MessageDataConfig MESSAGE_DATA = GriefDefenderPlugin.getInstance().messageData;

    @CommandCompletion("@gdplayers @gddummy")
    @CommandAlias("playerinfo")
    @Description("Gets information about a player.")
    @Syntax("[<player>|<player> <world>]")
    @Subcommand("player info")
    public void execute(CommandSender src, @Optional String[] args) throws InvalidCommandArgument {
        OfflinePlayer user = null;
        World world = null;
        if (args.length > 0) {
            GDPermissionUser holder = PermissionHolderCache.getInstance().getOrCreateUser(args[0]);
            if (holder == null) {
                TextAdapter.sendComponent(src, MESSAGE_DATA.getMessage(MessageStorage.COMMAND_PLAYER_NOT_FOUND,
                        ImmutableMap.of("player", args[0])));
                return;
            }
            if (args.length > 1) {
                world = Bukkit.getServer().getWorld(args[1]);
                if (world == null) {
                    TextAdapter.sendComponent(src, MESSAGE_DATA.getMessage(MessageStorage.COMMAND_WORLD_NOT_FOUND,
                            ImmutableMap.of("world", args[1])));
                    return;
                }
            }
            user = holder.getOfflinePlayer();
        }

        if (user == null) {
            if (!(src instanceof Player)) {
                GriefDefenderPlugin.sendMessage(src, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.COMMAND_INVALID_PLAYER));
                return;
            }

            user = (OfflinePlayer) src;
            if (world == null) {
                world = ((Player) src).getWorld();
            }
        }
        if (world == null) {
            world = Bukkit.getServer().getWorlds().get(0);
        }

        // otherwise if no permission to delve into another player's claims data or self
        if ((user != null && user != src && !src.hasPermission(GDPermissions.COMMAND_PLAYER_INFO_OTHERS))) {
           TextAdapter.sendComponent(src, MessageCache.getInstance().PERMISSION_PLAYER_VIEW_OTHERS);
        }


        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(world.getUID(), user.getUniqueId());
        boolean useGlobalData = BaseStorage.USE_GLOBAL_PLAYER_STORAGE;
        List<Claim> claimList = new ArrayList<>();
        for (Claim claim : playerData.getInternalClaims()) {
            if (useGlobalData) {
                claimList.add(claim);
            } else {
                if (claim.getWorldUniqueId().equals(world.getUID())) {
                    claimList.add(claim);
                }
            }
        }
        Component claimSizeLimit = TextComponent.of("none", TextColor.GRAY);
        if (playerData.getMaxClaimX(ClaimTypes.BASIC) != 0 || playerData.getMaxClaimY(ClaimTypes.BASIC) != 0 || playerData.getMaxClaimZ(ClaimTypes.BASIC) != 0) {
            claimSizeLimit = TextComponent.of(playerData.getMaxClaimX(ClaimTypes.BASIC) + "," + playerData.getMaxClaimY(ClaimTypes.BASIC) + "," + playerData.getMaxClaimZ(ClaimTypes.BASIC), TextColor.GRAY);
        }

        final double claimableChunks = GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME ? (playerData.getRemainingClaimBlocks() / 65536.0) : (playerData.getRemainingClaimBlocks() / 256.0);
        final Component uuidText = MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_UUID, 
                ImmutableMap.of("id", user.getUniqueId().toString()));
        final Component worldText = MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_WORLD, 
                ImmutableMap.of("world", world.getName()));
        final Component sizeLimitText = MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_CLAIM_SIZE_LIMIT, 
                ImmutableMap.of("limit", claimSizeLimit));
        final Component initialBlockText = MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_BLOCK_INITIAL, 
                ImmutableMap.of("amount", String.valueOf(playerData.getInitialClaimBlocks())));
        final Component accruedBlockText = MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_BLOCK_ACCRUED, 
                ImmutableMap.of(
                    "amount", String.valueOf(playerData.getAccruedClaimBlocks()),
                    "block_amount", String.valueOf(playerData.getBlocksAccruedPerHour())));
        final Component maxAccruedBlockText = MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_BLOCK_MAX_ACCRUED, 
                ImmutableMap.of("amount", String.valueOf(playerData.getMaxAccruedClaimBlocks())));
        final Component bonusBlockText = MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_BLOCK_BONUS, 
                ImmutableMap.of("amount", String.valueOf(playerData.getBonusClaimBlocks())));
        final Component remainingBlockText = MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_BLOCK_REMAINING, 
                ImmutableMap.of("amount", String.valueOf(playerData.getRemainingClaimBlocks())));
        final Component minMaxLevelText = MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_CLAIM_LEVEL, 
                ImmutableMap.of("level", String.valueOf(playerData.getMinClaimLevel() + "-" + playerData.getMaxClaimLevel())));
        final Component abandonRatioText = MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_ABANDON_RETURN_RATIO, 
                ImmutableMap.of("ratio", String.valueOf(playerData.getAbandonedReturnRatio(ClaimTypes.BASIC))));
        final Component totalTaxText = MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_TAX_TOTAL, 
                ImmutableMap.of("amount", String.valueOf(playerData.getInitialClaimBlocks())));
        final Component totalBlockText = MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_BLOCK_TOTAL, 
                ImmutableMap.of("amount", String.valueOf(playerData.getInitialClaimBlocks())));
        final Component totalClaimableChunkText = MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_CHUNK_TOTAL, 
                ImmutableMap.of("amount", String.valueOf(Math.round(claimableChunks * 100.0)/100.0)));
        final Component totalClaimText = MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_CLAIM_TOTAL, 
                ImmutableMap.of("amount", String.valueOf(claimList.size())));

        List<Component> claimsTextList = Lists.newArrayList();
        claimsTextList.add(uuidText);
        claimsTextList.add(worldText);
        claimsTextList.add(sizeLimitText);
        claimsTextList.add(initialBlockText);
        claimsTextList.add(accruedBlockText);
        claimsTextList.add(maxAccruedBlockText);
        claimsTextList.add(bonusBlockText);
        claimsTextList.add(remainingBlockText);
        claimsTextList.add(minMaxLevelText);
        claimsTextList.add(abandonRatioText);
        final int townLimit = playerData.getCreateClaimLimit(ClaimTypes.TOWN);
        final int basicLimit = playerData.getCreateClaimLimit(ClaimTypes.BASIC);
        final int subLimit = playerData.getCreateClaimLimit(ClaimTypes.SUBDIVISION);
        Component claimCreateLimits = TextComponent.builder("")
                .append("TOWN", TextColor.GRAY)
                .append(" : ")
                .append(String.valueOf(townLimit), TextColor.GREEN)
                .append(" BASIC", TextColor.GRAY)
                .append(" : ")
                .append(String.valueOf(basicLimit), TextColor.GREEN)
                .append(" SUB", TextColor.GRAY)
                .append(" : ")
                .append(String.valueOf(subLimit), TextColor.GREEN)
                .build();
        claimsTextList.add(claimCreateLimits);
        if (GriefDefenderPlugin.getGlobalConfig().getConfig().claim.bankTaxSystem) {
            Component townTaxRate = TextComponent.builder("")
                    .append("TOWN", TextColor.GRAY)
                    .append(" : ")
                    .append(String.valueOf(playerData.getTaxRate(ClaimTypes.TOWN)), TextColor.GREEN)
                    .build();
                    // TODO
                    //TextColors.GRAY, " BASIC", TextColors.WHITE, " : ", TextColors.GREEN, playerData.optionTaxRateTownBasic, 
                    //TextColors.GRAY, " SUB", TextColors.WHITE, " : ", TextColors.GREEN, playerData.getTaxRate(type));
            Component claimTaxRate = TextComponent.builder("")
                    .append("BASIC", TextColor.GRAY)
                    .append(" : ")
                    .append(String.valueOf(playerData.getTaxRate(ClaimTypes.BASIC)), TextColor.GREEN)
                    .append(" SUB", TextColor.GRAY)
                    .append(" : ")
                    .append(String.valueOf(playerData.getTaxRate(ClaimTypes.SUBDIVISION)), TextColor.GREEN)
                    .build();
            Component currentTaxRateText = TextComponent.builder("")
                    .append("Current Claim Tax Rate", TextColor.YELLOW)
                    .append(" : ")
                    .append("N/A", TextColor.RED)
                    .build();
            if (src instanceof Player) {
                Player player = (Player) src;
                if (player.getUniqueId().equals(user.getUniqueId())) {
                    final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(player.getLocation());
                    if (claim != null && !claim.isWilderness()) {
                        final double playerTaxRate = GDPermissionManager.getInstance().getInternalOptionValue(user, Options.TAX_RATE, claim, playerData);
                        currentTaxRateText = MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_TAX_CURRENT_RATE, 
                                        ImmutableMap.of("rate", String.valueOf(playerTaxRate)));
                    }
                }
            }
            final Component globalTownTaxText = MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_TAX_GLOBAL_TOWN_RATE, 
                    ImmutableMap.of("rate", townTaxRate));
            final Component globalClaimTaxText = MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_TAX_GLOBAL_CLAIM_RATE, 
                    ImmutableMap.of("rate", claimTaxRate));
            claimsTextList.add(currentTaxRateText);
            claimsTextList.add(globalTownTaxText);
            claimsTextList.add(globalClaimTaxText);
            claimsTextList.add(totalTaxText);
        }
        claimsTextList.add(totalBlockText);
        claimsTextList.add(totalClaimableChunkText);
        claimsTextList.add(totalClaimText);

        if (NMSUtil.getInstance().getLastLogin(user) != 0) {
            Date lastActive = null;
            try {
                lastActive = new Date(NMSUtil.getInstance().getLastLogin(user));
            } catch(DateTimeParseException ex) {
                // ignore
            }
            if (lastActive != null) {
                claimsTextList.add(MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_LAST_ACTIVE, 
                        ImmutableMap.of("date", lastActive)));
            }
        }

        PaginationList.Builder paginationBuilder = PaginationList.builder()
                .title(MessageCache.getInstance().PLAYERINFO_UI_TITLE).padding(TextComponent.of(" ").decoration(TextDecoration.STRIKETHROUGH, true)).contents(claimsTextList);
        paginationBuilder.sendTo(src);
    }
}
