package tw.iehow.howsign.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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
        String teamName = "player";
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.addTeam(teamName);
            team.setDisplayName(Text.literal("Player"));
            team.setNameTagVisibilityRule(AbstractTeam.VisibilityRule.NEVER);
        }
        scoreboard.addScoreHolderToTeam(player.getName().getString(), team);
    }
}
