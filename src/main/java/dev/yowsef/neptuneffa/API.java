package dev.yowsef.neptuneffa;

import dev.lrxh.api.NeptuneAPI;
import dev.lrxh.api.NeptuneAPIProvider;
import dev.lrxh.api.data.IKitData;
import dev.lrxh.api.kit.IKit;
import dev.lrxh.api.kit.IKitRule;
import dev.lrxh.api.profile.IProfile;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.ShieldMeta;
import org.bukkit.block.banner.Pattern;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class API {

    public static NeptuneAPI get() {
        return NeptuneAPIProvider.getAPI();
    }

    public static boolean isAvailable() {
        try {
            return get() != null && get().getKitService() != null && get().getKitService().getAllKits() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static IProfile getProfile(UUID uuid) {
        try {
            // Get profile from cache, fallback to null
            return get().getProfileService().getProfile(uuid).getNow(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static IKitData getKitData(UUID uuid, IKit kit) {
        IProfile profile = getProfile(uuid);
        if (profile == null) return null;
        return profile.getGameData().getKitData().get(kit);
    }

    public static boolean isInLobby(IProfile profile) {
        if (profile == null) return false;
        String state = profile.getProfileState();
        return state != null && (state.equalsIgnoreCase("IN_LOBBY") || state.equalsIgnoreCase("neptune:in_lobby"));
    }

    public static IKit getUpToDateKit(IKit kit) {
        if (kit == null) return null;
        if (isAvailable()) {
            for (IKit k : get().getKitService().getAllKits()) {
                if (k.getName().equalsIgnoreCase(kit.getName())) {
                    return k;
                }
            }
        }
        return kit;
    }

    public static boolean kitIs(IKit kit, String saveName) {
        if (kit == null) return false;
        try {
            Class<?> kitRuleClass = Class.forName("dev.lrxh.neptune.game.kit.impl.KitRule");
            Object targetRule = null;
            for (Object enumConstant : kitRuleClass.getEnumConstants()) {
                String ruleSaveName = (String) enumConstant.getClass().getMethod("getSaveName").invoke(enumConstant);
                if (ruleSaveName.equalsIgnoreCase(saveName)) {
                    targetRule = enumConstant;
                    break;
                }
            }
            if (targetRule != null) {
                java.lang.reflect.Method isMethod = kit.getClass().getMethod("is", kitRuleClass);
                return (boolean) isMethod.invoke(kit, targetRule);
            }
        } catch (Exception ignored) {
        }

        if (kit.getRule() == null) return false;
        for (Map.Entry<IKitRule, Boolean> entry : kit.getRule().entrySet()) {
            if (entry.getKey().getSaveName().equalsIgnoreCase(saveName)) {
                return entry.getValue();
            }
        }
        return false;
    }

    public static void applyShieldPatterns(IProfile profile, Player player) {
        if (profile == null || player == null) return;
        try {
            Object settingData = profile.getClass().getMethod("getSettingData").invoke(profile);
            Object shieldPackage = settingData.getClass().getMethod("getShieldPatternPackage").invoke(settingData);
            
            @SuppressWarnings("unchecked")
            List<Pattern> patterns = (List<Pattern>) shieldPackage.getClass().getMethod("getPatterns").invoke(shieldPackage);

            if (patterns == null || patterns.isEmpty()) return;

            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null) continue;
                ItemMeta meta = item.getItemMeta();
                if (meta instanceof ShieldMeta shield) {
                    shield.setPatterns(patterns);
                    item.setItemMeta(shield);
                }
            }
            player.updateInventory();
        } catch (Exception e) {
            NeptuneFFA.getInstance().getLogger()
                    .warning("Failed to apply shield patterns: " + e.getMessage());
        }
    }
}
