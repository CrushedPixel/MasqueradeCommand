package eu.crushedpixel.sponge.masquerade.command;

import eu.crushedpixel.sponge.masquerade.api.Masquerade;
import eu.crushedpixel.sponge.masquerade.api.Masquerades;
import org.spongepowered.api.CatalogTypes;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.args.PatternMatchingCommandElement;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(id = "masqueradecommand", dependencies = { @Dependency(id = "masquerade") })
public class MasqueradeCommandPlugin {

    public final Map<UUID, Masquerade> masqueradeMap = new ConcurrentHashMap<>();
    private Masquerades masquerades;

    @Listener
    public void onInit(GameInitializationEvent event) {
        masquerades = Sponge.getServiceManager().provide(Masquerades.class).get();
        registerCommands();
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        masqueradeMap.forEach((uuid, masquerade) -> {
            if (uuid.equals(event.getTargetEntity().getUniqueId())) return;
            masquerade.maskTo(event.getTargetEntity());
        });
    }

    private void registerCommands() {
        Sponge.getCommandManager().register(this, createMaskCommand(), "mask");
        Sponge.getCommandManager().register(this, createUnmaskCommand(), "unmask");
    }

    private CommandSpec createMaskCommand() {
        return CommandSpec.builder()
                .permission("masquerade.mask")
                .arguments(
                        GenericArguments.onlyOne(GenericArguments.catalogedElement(Text.of("entity"), CatalogTypes.ENTITY_TYPE))
                )
                .child(createOptionSubcommand(), "option")
                .child(createFallingBlockSubCommand(), "minecraft:fallingsand")
                .executor((source, context) -> {
                    if (!(source instanceof Player)) {
                        throw new CommandException(Text.of("Only players can use this command."));
                    }

                    Player player = (Player) source;
                    EntityType entityType = context.<EntityType>getOne("entity").get();

                    if (!player.hasPermission(String.format("masquerade.mask.%s", entityType.getId()))) {
                        throw new CommandException(Text.of("You don't have permission to use this masquerade."));
                    }

                    Masquerade masquerade = masquerades.fromType(entityType, player);

                    activateMasquerade(player, masquerade);

                    return CommandResult.success();
                })
                .build();
    }

    private CommandSpec createFallingBlockSubCommand() {
        return CommandSpec.builder()
                .permission("masquerade.mask.minecraft:fallingsand")
                .arguments(
                        GenericArguments.onlyOne(GenericArguments.catalogedElement(Text.of("block"), CatalogTypes.BLOCK_TYPE))
                )
                .executor((source, context) -> {
                    if (!(source instanceof Player)) {
                        throw new CommandException(Text.of("Only players can use this command."));
                    }

                    Player player = (Player) source;
                    BlockType blockType = context.<BlockType>getOne("block").get();
                    BlockState blockState = BlockState.builder().blockType(blockType).build();

                    Masquerade masquerade = masquerades.fallingBlock(blockState, player);

                    activateMasquerade(player, masquerade);

                    return CommandResult.success();
                }).build();
    }

    private void activateMasquerade(Player player, Masquerade masquerade) {
        // if there's an existing masquerade, remove it before applying the new masquerade
        Masquerade oldMasquerade = masqueradeMap.get(player.getUniqueId());

        if (oldMasquerade != null) {
            oldMasquerade.unmask();
        }

        // activate the masquerade
        for (Player p : Sponge.getServer().getOnlinePlayers()) {
            if (p.getUniqueId().equals(player.getUniqueId())) continue;
            masquerade.maskTo(p);
        }

        masqueradeMap.put(player.getUniqueId(), masquerade);
    }

    private CommandSpec createOptionSubcommand() {
        return CommandSpec.builder()
                .permission("masquerade.mask.option")
                .arguments(
                        GenericArguments.onlyOne(new PatternMatchingCommandElement(Text.of("key")) {
                            @Override
                            protected Iterable<String> getChoices(CommandSource source) {
                                if (!(source instanceof Player)) {
                                    return Collections.emptyList();
                                }

                                Player player = (Player) source;
                                Masquerade masquerade = masqueradeMap.get(player.getUniqueId());

                                if (masquerade == null) {
                                    return Collections.emptyList();
                                }

                                List<String> choices = new ArrayList<>();
                                for (Key key : masquerade.getKeys()) {
                                    choices.add(key.getId());
                                }

                                return choices;
                            }

                            @Override
                            protected Object getValue(String s) throws IllegalArgumentException {
                                // can't return the actual key without having the sender/masquerade,
                                // so we convert it afterwards
                                return s;
                            }
                        }),
                        GenericArguments.onlyOne(GenericArguments.string(Text.of("value")))
                )
                .executor((source, context) -> {
                    if (!(source instanceof Player)) {
                        throw new CommandException(Text.of("Only players can use this command."));
                    }

                    Player player = (Player) source;
                    Masquerade masquerade = masqueradeMap.get(player.getUniqueId());

                    if (masquerade == null) {
                        throw new CommandException(Text.of("You are not currently masked"));
                    }

                    Optional<String> keyOptional = context.getOne("key");
                    Optional<String> valueOptional = context.getOne("value");
                    if (!keyOptional.isPresent() || !valueOptional.isPresent()) {
                        throw new CommandException(Text.of("Usage: /mask option <key> <value>"));
                    }

                    String keyName = keyOptional.get();
                    String value = valueOptional.get();

                    Key key = null;

                    for (Key k : masquerade.getKeys()) {
                        if (k.getId().equalsIgnoreCase(keyName)) {
                            key = k;
                            break;
                        }
                    }

                    if (key == null) {
                        throw new CommandException(Text.of(String.format("Unknown key %s", keyName)));
                    }

                    if (!player.hasPermission(String.format("masquerade.mask.option.%s", key.getId()))) {
                        throw new CommandException(Text.of("You don't have permission to modify this key."));
                    }

                    try {
                        Class valueClazz = key.getElementToken().getRawType();

                        if (valueClazz == Integer.class) {
                            masquerade.setData(key, Integer.valueOf(value));
                        } else if (valueClazz == Double.class) {
                            masquerade.setData(key, Double.valueOf(value));
                        } else if (valueClazz == Boolean.class) {
                            masquerade.setData(key, Boolean.valueOf(value));
                        } else if (valueClazz == Byte.class) {
                            masquerade.setData(key, Byte.valueOf(value));
                        } else if (valueClazz == String.class) {
                            masquerade.setData(key, value);
                        } else if (valueClazz == Text.class) {
                            masquerade.setData(key, Text.of(value));
                        } else {
                            throw new CommandException(Text.of(String.format("Unknown value type for key %s: %s", keyName, valueClazz.getName())));
                        }

                    } catch (Exception e) {
                        throw new CommandException(Text.of(String.format("Invalid value for key %s: %s", keyName, value)));
                    }

                    return CommandResult.success();
                })
                .build();
    }

    private CommandSpec createUnmaskCommand() {
        return CommandSpec.builder()
                .executor((source, context) -> {
                    if (!(source instanceof Player)) {
                        throw new CommandException(Text.of("Only players can use this command."));
                    }

                    Player player = (Player) source;
                    Masquerade masquerade = masqueradeMap.get(player.getUniqueId());

                    if (masquerade == null) {
                        throw new CommandException(Text.of("You are not currently masked"));
                    }

                    masquerade.unmask();
                    masqueradeMap.remove(player.getUniqueId());

                    return CommandResult.success();
                }).build();
    }

}
