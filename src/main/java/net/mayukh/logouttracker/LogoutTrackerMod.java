package net.mayukh.logouttracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class LogoutTrackerMod implements ModInitializer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File DATA_FILE = new File("config/logout_coords.json");
    private Map<String, String> logoutLocations = new HashMap<>();

    @Override
    public void onInitialize() {
        loadData();

        // Record location on Disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            String name = handler.player.getName().getString().toLowerCase();
            String pos = String.format("X: %.1f, Y: %.1f, Z: %.1f in %s", 
                         handler.player.getX(), 
                         handler.player.getY(), 
                         handler.player.getZ(),
                         handler.player.getWorld().getRegistryKey().getValue().getPath());
            
            logoutLocations.put(name, pos);
            saveData();
        });

        // Register /lastseen command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("lastseen")
                .requires(source -> source.hasPermissionLevel(2)) 
                .then(CommandManager.argument("playername", StringArgumentType.string())
                .executes(context -> {
                    String name = StringArgumentType.getString(context, "playername").toLowerCase();
                    String loc = logoutLocations.get(name);
                    
                    if (loc != null) {
                        context.getSource().sendFeedback(() -> Text.literal("§6" + name + " §flogged out at: §a" + loc), false);
                    } else {
                        context.getSource().sendError(Text.literal("No recorded data for " + name));
                    }
                    return 1;
                })));
        });
    }

    private void saveData() {
        if (!DATA_FILE.getParentFile().exists()) DATA_FILE.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(DATA_FILE)) {
            GSON.toJson(logoutLocations, writer);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadData() {
        if (DATA_FILE.exists()) {
            try (Reader reader = new FileReader(DATA_FILE)) {
                logoutLocations = GSON.fromJson(reader, HashMap.class);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
}