package cam72cam.mod.gui;

import com.google.common.base.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;

public class TextField {
    private final GuiTextField textfield;
    private Predicate<String> validator;

    public TextField(IScreenBuilder builder, int x, int y, int width, int height) {
        this.textfield = create(builder, x, y, width, height);
        builder.addTextField(this);
    }

    GuiTextField create(IScreenBuilder builder, int x, int y, int width, int height) {
        validator = str -> true;
        return new GuiTextField(Minecraft.getMinecraft().fontRendererObj, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height) {
            @Override
            public void setText(String text) {
                if (validator.apply(text)) {
                    super.setText(text);
                }
            }
        };
    }

    GuiTextField internal() {
        return textfield;
    }

    public void setValidator(Predicate<String> filter) {
        this.validator = filter;
    }

    public void setFocused(boolean b) {
        textfield.setFocused(b);
    }

    public String getText() {
        if (validator.apply(textfield.getText())) {
            return textfield.getText();
        }
        return "";
    }

    public void setText(String s) {
        textfield.setText(s);
    }

    public void setVisible(Boolean visible) {
        textfield.setVisible(visible);
    }
}
