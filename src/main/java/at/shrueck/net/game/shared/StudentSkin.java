package at.shrueck.net.game.shared;

public enum StudentSkin {
    FJP(
            0,
            "fjp",
            "assets/player/fjp/Meshy_AI_FJP_biped_Animation_Walking_withSkin.glb",
            "assets/player/fjp/Meshy_AI_FJP_biped_Animation_Running_withSkin.glb",
            "assets/player/fjp/Meshy_AI_FJP_biped_Animation_Cherish_Pop_Dance_withSkin.glb",
            "Armature|Cherish_Pop_Dance|baselayer"
    ),
    CH(
            1,
            "ch",
            "assets/player/ch/Meshy_AI_Open_Arms_Pose_biped_Animation_Walking_withSkin.glb",
            "assets/player/ch/Meshy_AI_Open_Arms_Pose_biped_Animation_Running_withSkin.glb",
            "assets/player/ch/Meshy_AI_Open_Arms_Pose_biped_Animation_Boom_Dance_withSkin.glb",
            "Armature|Boom_Dance|baselayer"
    ),
    MI(
            2,
            "mi",
            "assets/player/mi/Meshy_AI_Blindfolded_T_Pose_biped_Animation_Walking_withSkin.glb",
            "assets/player/mi/Meshy_AI_Blindfolded_T_Pose_biped_Animation_Running_withSkin.glb",
            "assets/player/mi/Meshy_AI_Blindfolded_T_Pose_biped_Animation_Bass_Beats_withSkin.glb",
            "Armature|Bass_Beats|baselayer"
    ),
    TJR(
            3,
            "tjr",
            "assets/player/tjr/Meshy_AI_Star_Pose_biped_Animation_Walking_withSkin.glb",
            "assets/player/tjr/Meshy_AI_Star_Pose_biped_Animation_Running_withSkin.glb",
            "assets/player/tjr/Meshy_AI_Star_Pose_biped_Animation_Breakdance_1990_withSkin.glb",
            "Armature|Breakdance_1990|baselayer"
    );

    private final int code;
    private final String label;
    private final String walkingAssetPath;
    private final String runningAssetPath;
    private final String specialAssetPath;
    private final String specialClipName;

    StudentSkin(
            int code,
            String label,
            String walkingAssetPath,
            String runningAssetPath,
            String specialAssetPath,
            String specialClipName
    ) {
        this.code = code;
        this.label = label;
        this.walkingAssetPath = walkingAssetPath;
        this.runningAssetPath = runningAssetPath;
        this.specialAssetPath = specialAssetPath;
        this.specialClipName = specialClipName;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    public String walkingAssetPath() {
        return walkingAssetPath;
    }

    public String runningAssetPath() {
        return runningAssetPath;
    }

    public String specialAssetPath() {
        return specialAssetPath;
    }

    public String specialClipName() {
        return specialClipName;
    }

    public StudentSkin next() {
        StudentSkin[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static StudentSkin fromCode(int code) {
        for (StudentSkin skin : values()) {
            if (skin.code == code) {
                return skin;
            }
        }
        return FJP;
    }
}
