package at.shrueck.net.game.world;

import com.jme3.asset.AssetManager;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

public final class SchoolSceneBuilder {

    private final AssetManager assetManager;

    public SchoolSceneBuilder(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public SchoolLayout build(Node rootNode) {
        Node schoolNode = new Node("school");
        rootNode.attachChild(schoolNode);

        addFloor(schoolNode);
        addOuterWalls(schoolNode);
        addReceptionArea(schoolNode);
        addCrossHallLockers(schoolNode);
        addClassroomCorners(schoolNode);
        addCommonArea(schoolNode);
        addInteriorLights(rootNode);

        return SchoolWorldFactory.createDefaultLayout();
    }

    private void addFloor(Node schoolNode) {
        addBlock(schoolNode, "floor-base", new Vector3f(0f, -0.18f, 0f), 32f, 0.18f, 28f, new ColorRGBA(0.43f, 0.45f, 0.47f, 1f));
        addBlock(schoolNode, "hallway-main", new Vector3f(0f, -0.14f, 0f), 8.5f, 0.02f, 25f, new ColorRGBA(0.26f, 0.34f, 0.43f, 1f));
        addBlock(schoolNode, "hallway-cross", new Vector3f(0f, -0.13f, 0f), 26f, 0.02f, 6f, new ColorRGBA(0.30f, 0.37f, 0.46f, 1f));
        addBlock(schoolNode, "entrance-mat", new Vector3f(0f, -0.12f, 23.5f), 7f, 0.02f, 3f, new ColorRGBA(0.21f, 0.12f, 0.10f, 1f));
    }

    private void addOuterWalls(Node schoolNode) {
        ColorRGBA wallColor = new ColorRGBA(0.58f, 0.60f, 0.66f, 1f);
        addBlock(schoolNode, "wall-north", new Vector3f(0f, 4.2f, -28.5f), 32.5f, 4.2f, 0.5f, wallColor);
        addBlock(schoolNode, "wall-south-left", new Vector3f(-18f, 4.2f, 28.5f), 14f, 4.2f, 0.5f, wallColor);
        addBlock(schoolNode, "wall-south-right", new Vector3f(18f, 4.2f, 28.5f), 14f, 4.2f, 0.5f, wallColor);
        addBlock(schoolNode, "wall-west", new Vector3f(-32.5f, 4.2f, 0f), 0.5f, 4.2f, 28.5f, wallColor);
        addBlock(schoolNode, "wall-east", new Vector3f(32.5f, 4.2f, 0f), 0.5f, 4.2f, 28.5f, wallColor);

        addBlock(schoolNode, "board-north-left", new Vector3f(-18f, 3.0f, -27.7f), 6f, 1.2f, 0.08f, new ColorRGBA(0.12f, 0.28f, 0.17f, 1f));
        addBlock(schoolNode, "board-north-right", new Vector3f(18f, 3.0f, -27.7f), 6f, 1.2f, 0.08f, new ColorRGBA(0.12f, 0.28f, 0.17f, 1f));
        addBlock(schoolNode, "board-west", new Vector3f(-31.7f, 2.9f, 16f), 0.08f, 1.1f, 5f, new ColorRGBA(0.08f, 0.16f, 0.10f, 1f));
        addBlock(schoolNode, "board-east", new Vector3f(31.7f, 2.9f, -16f), 0.08f, 1.1f, 5f, new ColorRGBA(0.08f, 0.16f, 0.10f, 1f));
    }

    private void addReceptionArea(Node schoolNode) {
        addBlock(schoolNode, "reception-desk", new Vector3f(0f, 1f, 19f), 5.5f, 1f, 1.4f, new ColorRGBA(0.54f, 0.34f, 0.20f, 1f));
        addBlock(schoolNode, "reception-bench-left", new Vector3f(-18f, 0.55f, 22f), 2.6f, 0.55f, 0.9f, new ColorRGBA(0.41f, 0.29f, 0.18f, 1f));
        addBlock(schoolNode, "reception-bench-right", new Vector3f(18f, 0.55f, 22f), 2.6f, 0.55f, 0.9f, new ColorRGBA(0.41f, 0.29f, 0.18f, 1f));
    }

    private void addCrossHallLockers(Node schoolNode) {
        ColorRGBA lockerColor = new ColorRGBA(0.33f, 0.52f, 0.72f, 1f);
        addBlock(schoolNode, "lockers-west-north", new Vector3f(-10.5f, 1.7f, -14f), 1.1f, 1.7f, 5f, lockerColor);
        addBlock(schoolNode, "lockers-west-south", new Vector3f(-10.5f, 1.7f, 14f), 1.1f, 1.7f, 5f, lockerColor);
        addBlock(schoolNode, "lockers-east-north", new Vector3f(10.5f, 1.7f, -14f), 1.1f, 1.7f, 5f, lockerColor);
        addBlock(schoolNode, "lockers-east-south", new Vector3f(10.5f, 1.7f, 14f), 1.1f, 1.7f, 5f, lockerColor);

        addBlock(schoolNode, "hall-kiosk-left", new Vector3f(-18f, 0.9f, 0f), 2.2f, 0.9f, 1.2f, new ColorRGBA(0.57f, 0.40f, 0.24f, 1f));
        addBlock(schoolNode, "hall-kiosk-right", new Vector3f(18f, 0.9f, 0f), 2.2f, 0.9f, 1.2f, new ColorRGBA(0.57f, 0.40f, 0.24f, 1f));
    }

    private void addClassroomCorners(Node schoolNode) {
        addClassroomCluster(schoolNode, "northwest", -19f, -16f, new ColorRGBA(0.49f, 0.39f, 0.26f, 1f));
        addClassroomCluster(schoolNode, "northeast", 19f, -16f, new ColorRGBA(0.32f, 0.41f, 0.50f, 1f));
        addClassroomCluster(schoolNode, "southwest", -19f, 16f, new ColorRGBA(0.41f, 0.35f, 0.47f, 1f));
        addClassroomCluster(schoolNode, "southeast", 19f, 16f, new ColorRGBA(0.34f, 0.45f, 0.33f, 1f));
    }

    private void addClassroomCluster(Node schoolNode, String id, float centerX, float centerZ, ColorRGBA carpetColor) {
        addBlock(schoolNode, id + "-carpet", new Vector3f(centerX, -0.11f, centerZ), 8.5f, 0.025f, 6.5f, carpetColor);
        addDesk(schoolNode, id + "-desk-1", centerX - 3.2f, centerZ - 2.5f);
        addDesk(schoolNode, id + "-desk-2", centerX + 3.2f, centerZ - 2.5f);
        addDesk(schoolNode, id + "-desk-3", centerX - 3.2f, centerZ + 1.2f);
        addDesk(schoolNode, id + "-desk-4", centerX + 3.2f, centerZ + 1.2f);
        addBlock(schoolNode, id + "-teacher-desk", new Vector3f(centerX, 0.85f, centerZ + 4.2f), 3.2f, 0.85f, 1.1f, new ColorRGBA(0.56f, 0.36f, 0.22f, 1f));
    }

    private void addDesk(Node schoolNode, String name, float x, float z) {
        addBlock(schoolNode, name, new Vector3f(x, 0.75f, z), 1.5f, 0.75f, 0.95f, new ColorRGBA(0.63f, 0.44f, 0.27f, 1f));
    }

    private void addCommonArea(Node schoolNode) {
        addBlock(schoolNode, "library-table-left", new Vector3f(-8f, 0.75f, -22f), 2.2f, 0.75f, 1.2f, new ColorRGBA(0.48f, 0.33f, 0.19f, 1f));
        addBlock(schoolNode, "library-table-right", new Vector3f(8f, 0.75f, -22f), 2.2f, 0.75f, 1.2f, new ColorRGBA(0.48f, 0.33f, 0.19f, 1f));
        addBlock(schoolNode, "display-island", new Vector3f(0f, 1.15f, -8f), 2f, 1.15f, 2f, new ColorRGBA(0.69f, 0.53f, 0.28f, 1f));
    }

    private void addInteriorLights(Node rootNode) {
        addPointLight(rootNode, new Vector3f(0f, 7f, 22f), new ColorRGBA(0.74f, 0.66f, 0.52f, 1f).mult(0.55f), 24f);
        addPointLight(rootNode, new Vector3f(0f, 7f, 0f), new ColorRGBA(0.58f, 0.66f, 0.78f, 1f).mult(0.60f), 28f);
        addPointLight(rootNode, new Vector3f(0f, 7f, -20f), new ColorRGBA(0.70f, 0.68f, 0.60f, 1f).mult(0.50f), 24f);
    }

    private void addPointLight(Node rootNode, Vector3f position, ColorRGBA color, float radius) {
        PointLight light = new PointLight();
        light.setPosition(position);
        light.setColor(color);
        light.setRadius(radius);
        rootNode.addLight(light);
    }

    private void addBlock(Node parent, String name, Vector3f center, float halfX, float halfY, float halfZ, ColorRGBA color) {
        Geometry geometry = new Geometry(name, new Box(halfX, halfY, halfZ));
        geometry.setMaterial(createLitMaterial(color));
        geometry.setLocalTranslation(center);
        parent.attachChild(geometry);
    }

    private Material createLitMaterial(ColorRGBA color) {
        Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Diffuse", color);
        material.setColor("Ambient", color.mult(0.18f));
        material.setColor("Specular", ColorRGBA.White.mult(0.015f));
        material.setFloat("Shininess", 3f);
        return material;
    }
}