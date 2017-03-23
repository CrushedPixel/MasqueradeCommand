# MasqueradeCommand
A simple disguise plugin for [Sponge](https://www.spongepowered.org/).

## Dependencies
To run this plugin, you need to have the [MasqueradePlugin] installed on your server.  
The exact behaviour of the masquerades depends on the version of [MasqueradePlugin] you're running.

## Commands
### `/mask`
#### Description
Masks the player as a different entity.

#### Usage
`/mask <entity> <option>`

##### Examples
* `/mask minecraft:creeper`
* `/mask minecraft:chicken`
* `/mask minecraft:fallingsand minecraft:dirt`

All entity types available can be retrieved using auto-complete when typing the command.

#### Permissions
To mask as a specific entity, the permission `masquerade.mask.[entity]` is required.  
For example, to mask as a creeper, 
you need the permission `masquerade.mask.minecraft:creeper`.

### `/mask option`
#### Description
Modifies the current masquerade's looks or behaviour by setting the value of a data key.

#### Usage
`/mask option <key> <value>`

##### Examples
* `/mask option sponge:is_aflame true`
* `/mask option sponge:display_name Dinnerbone`

All data keys applicable to the current masquerade can be retrieved using auto-complete when typing the command.

#### Permissions
To set a specific option, the permission `masquerade.mask.option.[key]` is required.  
For example, to change whether the masquerade is burning, 
you need the permission `masquerade.mask.option.sponge:is_aflame`.

### `/unmask`
#### Description
Unmasks the player if currently masked.

#### Permissions
No permissions are required for this command.

[MasqueradePlugin]: https://github.com/CrushedPixel/MasqueradePlugin/
