package at.shrueck.net.game.assets;

import at.shrueck.net.game.character.AnimatedActor;
import at.shrueck.net.game.character.CharacterMode;
import at.shrueck.net.game.shared.AvatarRole;
import com.jme3.asset.AssetManager;
import java.util.EnumMap;

public final class AssetCatalog {

    private static final float SHRUECK_IDLE_SCALE = 2.80f;
    private static final float SHRUECK_MOVE_SCALE = 3.08f;
    private static final float SHRUECK_FAST_SCALE = 3.08f;
    private static final float SHRUECK_SPECIAL_SCALE = 2.80f;
    private static final float STUDENT_SCALE = 2.44f;

    private AssetCatalog() {
    }

    public static AnimatedActor createAvatar(AssetManager assetManager, AvatarRole role) {
        return role == AvatarRole.SHRUECK ? createShrueck(assetManager) : createStudent(assetManager, true);
    }

    public static AnimatedActor createShrueck(AssetManager assetManager) {
        EnumMap<CharacterMode, AnimationVariant> variants = new EnumMap<>(CharacterMode.class);
        variants.put(
                CharacterMode.IDLE,
                new AnimationVariant(
                        "assets/shrueck/Meshy_AI_T_Pose_Ogre_biped_Animation_Idle_withSkin.glb",
                        "Armature|Idle|baselayer",
                        SHRUECK_IDLE_SCALE,
                        0f
                )
        );
        variants.put(
                CharacterMode.MOVE,
                new AnimationVariant(
                        "assets/shrueck/Meshy_AI_T_Pose_Ogre_biped_Animation_Walking_withSkin.glb",
                        "Armature|walking_man|baselayer",
                        SHRUECK_MOVE_SCALE,
                        0f
                )
        );
        variants.put(
                CharacterMode.FAST,
                new AnimationVariant(
                        "assets/shrueck/Meshy_AI_T_Pose_Ogre_biped_Animation_RunFast_withSkin.glb",
                        "Armature|RunFast|baselayer",
                        SHRUECK_FAST_SCALE,
                        0f
                )
        );
        variants.put(
                CharacterMode.SPECIAL,
                new AnimationVariant(
                        "assets/shrueck/Meshy_AI_T_Pose_Ogre_biped_Animation_All_Night_Dance_withSkin.glb",
                        "Armature|All_Night_Dance|baselayer",
                        SHRUECK_SPECIAL_SCALE,
                        0f
                )
        );
        return new AnimatedActor("shrueck", assetManager, variants, CharacterMode.IDLE);
    }

    public static AnimatedActor createStudent(AssetManager assetManager, boolean specialIdle) {
        EnumMap<CharacterMode, AnimationVariant> variants = new EnumMap<>(CharacterMode.class);
        variants.put(
                CharacterMode.MOVE,
                new AnimationVariant(
                        "assets/player/fjp/Meshy_AI_Tabletop_T_Pose_biped_Animation_Walking_withSkin.glb",
                        "Armature|walking_man|baselayer",
                        STUDENT_SCALE,
                        0f
                )
        );
        variants.put(
                CharacterMode.FAST,
                new AnimationVariant(
                        "assets/player/fjp/Meshy_AI_Tabletop_T_Pose_biped_Animation_Running_withSkin.glb",
                        "Armature|running|baselayer",
                        STUDENT_SCALE,
                        0f
                )
        );
        if (specialIdle) {
            variants.put(
                    CharacterMode.SPECIAL,
                    new AnimationVariant(
                            "assets/player/fjp/Meshy_AI_Tabletop_T_Pose_biped_Animation_Crystal_Beads_withSkin.glb",
                            "Armature|Crystal_Beads|baselayer",
                            STUDENT_SCALE,
                            0f
                    )
            );
        }
        return new AnimatedActor("student", assetManager, variants, CharacterMode.MOVE);
    }

    public record AnimationVariant(String assetPath, String clipName, float uniformScale, float baseYaw) {
    }
}