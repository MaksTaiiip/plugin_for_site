package org.lor.azrisite;

import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;

public class QuestPlugin extends JavaPlugin {

    private String apiUrl;
    private String pluginSecret;
    private final OkHttpClient http = new OkHttpClient();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // ← Додай цей рядок
        getLogger().info("Config файл знаходиться: " + getDataFolder().getAbsolutePath());

        apiUrl = getConfig().getString("api-url");
        pluginSecret = getConfig().getString("plugin-secret");

        // ← І цей — щоб бачити що реально зчиталось
        getLogger().info("Зчитано plugin-secret: " + pluginSecret);
        getLogger().info("Зчитано api-url: " + apiUrl);

        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getCommand("questtest").setExecutor((sender, command, label, args) -> {
            sender.sendMessage("§aQuestPlugin працює!");
            return true;
        });

        getLogger().info("QuestPlugin увімкнено!");
    }

    @Override
    public void onDisable() {
        getLogger().info("QuestPlugin вимкнено.");
    }

    public void reportBlockBreak(String playerUUID, String blockType) {
        getLogger().info("Відправляємо запит: UUID=" + playerUUID + " блок=" + blockType);

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                String json = new JSONObject()
                        .put("minecraft_uuid", playerUUID)
                        .put("block_type", blockType)
                        .toString();

                Request request = new Request.Builder()
                        .url(apiUrl + "/api/quests/block-break")
                        .addHeader("x-plugin-secret", pluginSecret)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(json, MediaType.parse("application/json")))
                        .build();

                try (Response response = http.newCall(request).execute()) {
                    getLogger().info("Відповідь від API: " + response.code());

                    if (!response.isSuccessful()) return;

                    // Читаємо тіло ОДИН РАЗ і зберігаємо в змінну
                    String body = response.body().string();
                    getLogger().info("Тіло відповіді: " + body);

                    JSONObject result = new JSONObject(body);
                    JSONArray rewards = result.optJSONArray("rewards");

                    if (rewards != null && rewards.length() > 0) {
                        getServer().getScheduler().runTask(this, () -> {
                            for (int i = 0; i < rewards.length(); i++) {
                                String playerName = Bukkit.getOfflinePlayer(
                                        UUID.fromString(playerUUID)
                                ).getName();

                                String cmd = rewards.getString(i)
                                        .replace("%player%", playerName);

                                getLogger().info("Виконую команду: " + cmd);
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                            }
                        });
                    }
                }

            } catch (Exception e) {
                getLogger().warning("ПОМИЛКА: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}