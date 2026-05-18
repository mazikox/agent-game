package com.agentgierka.mmo.config;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.player.Player;
import com.agentgierka.mmo.player.PlayerRepository;
import com.agentgierka.mmo.agent.repository.AgentWorldStateRepository;
import com.agentgierka.mmo.engine.EngineControl;
import com.agentgierka.mmo.world.Location;
import com.agentgierka.mmo.world.LocationRepository;
import com.agentgierka.mmo.world.LocationType;
import com.agentgierka.mmo.world.Portal;
import com.agentgierka.mmo.world.PortalRepository;
import com.agentgierka.mmo.creature.model.*;
import com.agentgierka.mmo.creature.repository.*;
import com.agentgierka.mmo.creature.service.SpawnService;
import com.agentgierka.mmo.inventory.infrastructure.db.InventoryRepository;
import com.agentgierka.mmo.inventory.infrastructure.db.ItemDefinitionRepository;
import com.agentgierka.mmo.inventory.infrastructure.db.ItemDefinitionEntity;
import com.agentgierka.mmo.inventory.infrastructure.db.ItemDefinitionMapper;
import com.agentgierka.mmo.inventory.infrastructure.db.ItemDefinitionDictionary;
import com.agentgierka.mmo.inventory.domain.Inventory;
import com.agentgierka.mmo.inventory.domain.ItemStack;
import com.agentgierka.mmo.inventory.domain.ItemDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;

/**
 * Seeds the database with initial game data during startup.
 * Focuses on providing a base state for development and testing.
 */
@Profile("!test")
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

        private final PlayerRepository playerRepository;
        private final AgentRepository agentRepository;
        private final AgentWorldStateRepository agentWorldStateRepository;
        private final LocationRepository locationRepository;
        private final PortalRepository portalRepository;
        private final CreatureTemplateRepository creatureTemplateRepository;
        private final SpawnPointRepository spawnPointRepository;
        private final LootTableRepository lootTableRepository;
        private final CreatureInstanceRepository creatureInstanceRepository;
        private final SpawnService spawnService;
        private final PasswordEncoder passwordEncoder;
        private final EngineControl engineControl;
        private final InventoryRepository inventoryRepository;
        private final ItemDefinitionRepository itemDefinitionRepository;
        private final ItemDefinitionMapper itemDefinitionMapper;
        private final ItemDefinitionDictionary itemDefinitionDictionary;

        @Override
        @Transactional
        public void run(String... args) {
                try {
                        clearRedisState();

                        if (locationRepository.count() > 0) {
                                log.info("Game data already initialized. Skipping...");
                                spawnService.spawnAllActivePoints();
                                return;
                        }

                        log.info("Initializing game data...");
                        initializeAllData();
                        log.info("Game data initialization complete (v1 + Monsters).");
                } finally {
                        engineControl.setReady(true);
                        log.info("MMO Agent Engine is now READY and processing ticks.");
                }
        }

        private void clearRedisState() {
                // Clear Redis state on every boot to avoid "ghosts" in terminal
                agentWorldStateRepository.deleteAll();
                creatureInstanceRepository.deleteAll();
        }

        private void initializeAllData() {
                initializeItemDefinitions();
                itemDefinitionDictionary.reload();
                List<Location> locations = initializeLocations();
                Location forest = locations.get(0);
                Location meadow = locations.get(1);
                Location mine = locations.get(2);

                initializePortals(forest, meadow, mine);
                initializePlayersAndAgents(forest);
                initializeMonsters(forest, mine);

                // Populate Redis
                spawnService.spawnAllActivePoints();
        }

        private List<Location> initializeLocations() {
                Location forest = Location.builder()
                                .name("Forest of Beginnings")
                                .description("A lush green forest where young agents learn basic survival skills.")
                                .type(LocationType.FOREST)
                                .width(400)
                                .height(300)
                                .build();

                Location meadow = Location.builder()
                                .name("Azure Meadow")
                                .description("A peaceful meadow filled with blue flowers and butterflies.")
                                .type(LocationType.FOREST)
                                .width(50)
                                .height(50)
                                .build();

                Location mine = Location.builder()
                                .name("Deep Iron Mine")
                                .description("A dark, echoing mine rich with iron ore and mysterious shadows.")
                                .type(LocationType.MINE)
                                .width(30)
                                .height(30)
                                .build();

                return locationRepository.saveAll(List.of(forest, meadow, mine));
        }

        private void initializePortals(Location forest, Location meadow, Location mine) {
                portalRepository.save(Portal.builder()
                                .sourceLocation(forest).sourceX(10).sourceY(10)
                                .targetLocation(meadow).targetX(6).targetY(6)
                                .build());
                portalRepository.save(Portal.builder()
                                .sourceLocation(meadow).sourceX(5).sourceY(5)
                                .targetLocation(forest).targetX(11).targetY(11)
                                .build());
                portalRepository.save(Portal.builder()
                                .sourceLocation(forest).sourceX(90).sourceY(90)
                                .targetLocation(mine).targetX(6).targetY(6)
                                .build());
                portalRepository.save(Portal.builder()
                                .sourceLocation(mine).sourceX(5).sourceY(5)
                                .targetLocation(forest).targetX(89).targetY(89)
                                .build());
        }

        private void initializePlayersAndAgents(Location startingLocation) {
                // First Account (MasterAdmin)
                Player master = Player.create("MasterAdmin", passwordEncoder.encode("admin123"));
                playerRepository.save(master);

                Agent scout = Agent.create("Shadow-01", master, startingLocation, 50, 50, 5);
                agentRepository.save(scout);

                // Initialize inventory for the agent
                Inventory inventory = Inventory.createDefault();

                ItemDefinitionEntity swordEntity = itemDefinitionRepository.findById("sword_01").orElseThrow();
                ItemDefinitionEntity potionEntity = itemDefinitionRepository.findById("potion_hp").orElseThrow();
                ItemDefinitionEntity goldEntity = itemDefinitionRepository.findById("gold_coin").orElseThrow();

                ItemDefinition swordDef = itemDefinitionMapper.toDomain(swordEntity);
                ItemDefinition potionDef = itemDefinitionMapper.toDomain(potionEntity);
                ItemDefinition goldDef = itemDefinitionMapper.toDomain(goldEntity);

                inventory.addItem(new ItemStack(UUID.randomUUID(), swordDef, 1));
                inventory.addItem(new ItemStack(UUID.randomUUID(), potionDef, 5));
                inventory.addItem(new ItemStack(UUID.randomUUID(), goldDef, 10));

                inventoryRepository.save(inventory, scout.getId());

                // Second Account (MasterAdmin2)
                Player master2 = Player.create("MasterAdmin2", passwordEncoder.encode("admin123"));
                playerRepository.save(master2);

                Agent scout2 = Agent.create("Ghost-02", master2, startingLocation, 60, 50, 5);
                agentRepository.save(scout2);

                // Initialize inventory for the second agent
                Inventory inventory2 = Inventory.createDefault();
                inventory2.addItem(new ItemStack(UUID.randomUUID(), swordDef, 1));
                inventory2.addItem(new ItemStack(UUID.randomUUID(), potionDef, 5));
                inventory2.addItem(new ItemStack(UUID.randomUUID(), goldDef, 10));

                inventoryRepository.save(inventory2, scout2.getId());
        }

        private void initializeMonsters(Location forest, Location mine) {
                // Templates
                CreatureTemplate wolf = CreatureTemplate.create("Forest Wolf", CreatureRank.NORMAL, 1, 50, 8, 25, 5,
                                "/creatures/wolf.png");
                CreatureTemplate spider = CreatureTemplate.create("Giant Spider", CreatureRank.ELITE, 3, 150, 15, 75, 8,
                                "/creatures/spider.png");
                CreatureTemplate dragon = CreatureTemplate.create("Shadowfang Dragon", CreatureRank.BOSS, 10, 2000, 50,
                                500, 15,
                                "/creatures/dragon.png");
                CreatureTemplate spruce = CreatureTemplate.create("Spruce Tree", CreatureRank.NORMAL, 1, 30, 0, 10, 1,
                                "/creatures/choinka.png");
                CreatureTemplate spruceAlt = CreatureTemplate.create("Spruce Tree (Alt)", CreatureRank.NORMAL, 1, 30, 0,
                                10, 1,
                                "/creatures/choinkaINT.png");
                creatureTemplateRepository.saveAll(List.of(wolf, spider, dragon, spruce, spruceAlt));

                // --- EXACT SPAWNS (Matched to User Provided JSON) ---

                // Wolves & Spiders
                spawnPointRepository.save(SpawnPoint.create(wolf, forest, 30, 44, 0, 60));
                spawnPointRepository.save(SpawnPoint.create(wolf, forest, 75, 21, 0, 60));
                spawnPointRepository.save(SpawnPoint.create(spider, forest, 47, 81, 0, 120));
                spawnPointRepository.save(SpawnPoint.create(dragon, mine, 15, 15, 0, 600));

                // Spruce Trees (Exact locations)
                List.of(
                                new int[] { 24, 71 }, new int[] { 41, 74 }, new int[] { 55, 73 }, new int[] { 80, 76 },
                                new int[] { 85, 69 }, new int[] { 88, 41 }, new int[] { 88, 21 }, new int[] { 77, 30 },
                                new int[] { 61, 24 }, new int[] { 52, 17 }, new int[] { 20, 23 }, new int[] { 31, 28 },
                                new int[] { 38, 14 }, new int[] { 16, 41 }, new int[] { 94, 2 }, new int[] { 95, 48 },
                                new int[] { 5, 24 }, new int[] { 3, 67 }, new int[] { 55, 100 }, new int[] { 72, 110 },
                                new int[] { 89, 112 }, new int[] { 118, 92 }, new int[] { 116, 43 },
                                new int[] { 108, 75 }).forEach(
                                                coords -> spawnPointRepository.save(SpawnPoint.create(spruce, forest,
                                                                coords[0], coords[1], 0, 999999)));

                // Spruce Trees Alt (Exact locations)
                List.of(
                                new int[] { 13, 32 }, new int[] { 35, 37 }, new int[] { 56, 37 }, new int[] { 74, 37 },
                                new int[] { 87, 36 }, new int[] { 81, 82 }, new int[] { 76, 87 }, new int[] { 38, 84 },
                                new int[] { 24, 88 }, new int[] { 28, 79 }, new int[] { 12, 65 }, new int[] { 27, 18 },
                                new int[] { 32, 3 }, new int[] { 63, 2 }, new int[] { 94, 11 }, new int[] { 100, 61 },
                                new int[] { 115, 68 }, new int[] { 98, 74 }, new int[] { 76, 103 },
                                new int[] { 67, 106 }, new int[] { 104, 85 }, new int[] { 44, 89 },
                                new int[] { 45, 110 }).forEach(
                                                coords -> spawnPointRepository.save(SpawnPoint.create(spruceAlt, forest,
                                                                coords[0], coords[1], 0, 999999)));

                // Loot Tables
                LootTable wolfLoot = LootTable.forCreature(wolf, "Wolf Drops");
                wolfLoot.addEntry(LootEntry.create(wolfLoot, "Wolf Pelt", 0.4, 1, 1, 0));
                wolfLoot.addEntry(LootEntry.create(wolfLoot, "Wolf Fang", 0.15, 1, 2, 0));
                lootTableRepository.save(wolfLoot);

                LootTable forestLoot = LootTable.forLocation(forest, "Forest Global Drops");
                forestLoot.addEntry(LootEntry.create(forestLoot, "Herb", 0.1, 1, 3, 0));
                forestLoot.addEntry(LootEntry.create(forestLoot, "Gold Coin", 0.5, 5, 20, 0));
                lootTableRepository.save(forestLoot);
        }

        private void initializeItemDefinitions() {
                if (itemDefinitionRepository.count() > 0)
                        return;

                itemDefinitionRepository.saveAll(List.of(
                                ItemDefinitionEntity.builder()
                                                .id("sword_01").name("Iron Sword")
                                                .width(1).height(3).maxStack(1)
                                                .iconUrl("/items/sword_01.png").build(),
                                ItemDefinitionEntity.builder()
                                                .id("potion_hp").name("Health Potion")
                                                .width(1).height(1).maxStack(20)
                                                .iconUrl("/items/potion_hp.png").build(),
                                ItemDefinitionEntity.builder()
                                                .id("wolf_pelt").name("Wolf Pelt")
                                                .width(1).height(1).maxStack(20)
                                                .iconUrl("/items/wolf_pelt.png").build(),
                                ItemDefinitionEntity.builder()
                                                .id("wolf_fang").name("Wolf Fang")
                                                .width(1).height(1).maxStack(10)
                                                .iconUrl("/items/wolf_fang.png").build(),
                                ItemDefinitionEntity.builder()
                                                .id("herb").name("Herb")
                                                .width(1).height(1).maxStack(50)
                                                .iconUrl("/items/herb.png").build(),
                                ItemDefinitionEntity.builder()
                                                .id("gold_coin").name("Gold Coin")
                                                .width(1).height(1).maxStack(999)
                                                .iconUrl("/items/gold_coin.png").build()));
        }
}
