#!/usr/bin/env python3

# Read original file
with open('src/main/java/io/github/marcel/loadstonetp/LodestoneTP.java', 'r') as f:
    content = f.read()

# Add HologramCommand import
content = content.replace(
    'import io.github.marcel.loadstonetp.commands.NetworkCommand;',
    'import io.github.marcel.loadstonetp.commands.NetworkCommand;\nimport io.github.marcel.loadstonetp.commands.HologramCommand;'
)

# Add lodestoneInteractListener field
content = content.replace(
    '    private WarmupManager warmupManager;',
    '    private WarmupManager warmupManager;\n    private LodestoneInteractListener lodestoneInteractListener;'
)

# Update command registration
content = content.replace(
    '                            .then(NetworkCommand.buildNode(this))\n                            .build(),',
    '                            .then(NetworkCommand.buildNode(this))\n                            .then(HologramCommand.buildNode(this))\n                            .build(),'
)

# Update listener registration
content = content.replace(
    '        getServer().getPluginManager().registerEvents(new LodestoneInteractListener(databaseManager, this), this);',
    '        lodestoneInteractListener = new LodestoneInteractListener(databaseManager, this);\n        getServer().getPluginManager().registerEvents(lodestoneInteractListener, this);'
)

# Add getter
content = content.replace(
    '    public WarmupManager getWarmupManager() {\n        return warmupManager;\n    }\n}',
    '    public WarmupManager getWarmupManager() {\n        return warmupManager;\n    }\n\n    public LodestoneInteractListener getLodestoneListener() {\n        return lodestoneInteractListener;\n    }\n}'
)

# Write back
with open('src/main/java/io/github/marcel/loadstonetp/LodestoneTP.java', 'w') as f:
    f.write(content)

print("File updated successfully")
