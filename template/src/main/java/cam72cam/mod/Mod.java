package cam72cam.#MODID#;

import cam72cam.mod.ModCore;

@cpw.mods.fml.common.Mod(modid = Mod.MODID, name = Mod.NAME, version = Mod.VERSION, dependencies = "required-before:universalmodcore", acceptedMinecraftVersions = "[1.10,1.11)")
public class Mod {
    public static final String MODID = "#MODID#";
    public static final String NAME = "#MODNAME#";
    public static final String VERSION = "#MODVERSION#";

    static {
        try {
            Class<ModCore.Mod> cls = (Class<ModCore.Mod>) Class.forName("#MODCLASS#");
            ModCore.register(() -> {
                try {
                    return cls.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException("Could not construct mod " + MODID, e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Could not load mod " + MODID, e);
        }
    }
}

