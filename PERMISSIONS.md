# Permissions

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/lodestonetp reload` | `lodestonetp.admin` | Reload config and refresh effects |
| `/lodestonetp admin` | `lodestonetp.admin` | Open the in-game admin panel |
| `/lodestonetp networks` | `lodestonetp.manage_networks` | Manage teleporter networks |

## Permission Nodes

| Permission | Default | Description |
| --- | --- | --- |
| `lodestonetp.use` | true | Use teleporters to travel |
| `lodestonetp.create` | op | Create new teleporters |
| `lodestonetp.manage_cooldowns` | op | Set per-teleporter cooldown overrides |
| `lodestonetp.manage_networks` | op | Create and manage teleporter networks |
| `lodestonetp.network.bypass` | op | Bypass per-network permission-node restrictions |
| `lodestonetp.warmup.bypass` | op | Bypass teleport warmup delay |
| `lodestonetp.admin` | op | Full admin access - bypass cooldowns, costs, and manage any teleporter |

## Operator Behavior

Server operators bypass all major restrictions by default, including cooldowns, costs, access checks, and admin-only management flows.

If a permission plugin explicitly denies a node, verify the effective permissions in that plugin as well.
