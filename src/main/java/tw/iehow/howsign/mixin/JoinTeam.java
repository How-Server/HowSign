package tw.iehow.howsign.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class JoinTeam {
    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void onPlayerJoin(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        Scoreboard scoreboard = server.getScoreboard();
        if (player.hasPermissionLevel(4)) {
            String adminName = "admin";
            Team adminTeam = scoreboard.getTeam(adminName);
            if (adminTeam == null) {
                adminTeam = scoreboard.addTeam(adminName);
                adminTeam.setDisplayName(Text.of("Admin"));
                adminTeam.setColor(Formatting.AQUA);
            }
            scoreboard.addScoreHolderToTeam(player.getName().getString(), adminTeam);
        } else {
            String playerName = "player";
            Team playerTeam = scoreboard.getTeam(playerName);
            if (playerTeam == null) {
                playerTeam = scoreboard.addTeam(playerName);
                playerTeam.setDisplayName(Text.literal("Player"));
            }
            scoreboard.addScoreHolderToTeam(player.getName().getString(), playerTeam);
        }
    }
}
