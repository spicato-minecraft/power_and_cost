package power_and_cost.clientgametest;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Records the strings, components, sprites, and tooltips a screen submits while extracting.
 */
final class RecordingGuiGraphics extends GuiGraphicsExtractor {

    final List<String> strings = new ArrayList<>();
    final List<Component> components = new ArrayList<>();
    final List<Identifier> sprites = new ArrayList<>();
    final List<List<Component>> tooltips = new ArrayList<>();

    RecordingGuiGraphics(Minecraft minecraft) {
        super(minecraft, new GuiRenderState(), minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
    }

    @Override
    public void text(Font font, String text, int x, int y, int color) {
        strings.add(text);
        super.text(font, text, x, y, color);
    }

    @Override
    public void text(Font font, String text, int x, int y, int color, boolean dropShadow) {
        strings.add(text);
        super.text(font, text, x, y, color, dropShadow);
    }

    @Override
    public void text(Font font, Component text, int x, int y, int color) {
        components.add(text);
        super.text(font, text, x, y, color);
    }

    @Override
    public void text(Font font, Component text, int x, int y, int color, boolean dropShadow) {
        components.add(text);
        super.text(font, text, x, y, color, dropShadow);
    }

    @Override
    public void blitSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
        sprites.add(sprite);
        super.blitSprite(pipeline, sprite, x, y, width, height);
    }

    @Override
    public void setComponentTooltipForNextFrame(Font font, List<Component> tooltip, int mouseX, int mouseY) {
        tooltips.add(List.copyOf(tooltip));
        super.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
    }
}
