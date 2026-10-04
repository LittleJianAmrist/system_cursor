package com.amrist.systemcursor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiListWorldSelectionEntry;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import java.lang.reflect.Field;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Side.CLIENT)
public final class SystemCursorHandler {

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static long arrowCursor;
    private static long handCursor;
    private static long textCursor;
    private static long verticalResizeCursor;
    private static long currentCursor;
    private static boolean cursorsInitialized;
    private static boolean cursorErrorLogged;

    private SystemCursorHandler() {
    }

    @SubscribeEvent
    public static void onGuiInit(GuiScreenEvent.InitGuiEvent event) {
        if (event.getGui() != null && initializeCursors()) {
            setCursor(arrowCursor);
        }
    }

    @SubscribeEvent
    public static void onGuiDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event.getGui() != null && initializeCursors()) {
            setCursor(detectCursor(event.getGui(), event.getMouseX(), event.getMouseY()));
        }
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (Reference.MOD_ID.equals(event.getModID())) {
            SystemCursorConfig.sync();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!initializeCursors()) {
            return;
        }

        if (MC.currentScreen == null) {
            setCursor(arrowCursor);
            return;
        }

        GuiScreen gui = MC.currentScreen;
        int mouseX = Mouse.getX() * gui.width / MC.displayWidth;
        int mouseY = gui.height - Mouse.getY() * gui.height / MC.displayHeight - 1;
        setCursor(detectCursor(gui, mouseX, mouseY));
    }

    private static long detectCursor(GuiScreen gui, int mouseX, int mouseY) {
        if (!SystemCursorConfig.isAllowCursorChanges()) {
            return arrowCursor;
        }

        if (gui instanceof GuiContainerCreative) {
            GuiContainerCreative creativeGui = (GuiContainerCreative) gui;
            int localMouseX = mouseX - creativeGui.getGuiLeft();
            int localMouseY = mouseY - creativeGui.getGuiTop();
            if (creativeGui.needsScrollBars() && isHovered(175, 18, 12, 112, localMouseX, localMouseY)) {
                return verticalResizeCursor;
            }
            for (CreativeTabs tab : CreativeTabs.CREATIVE_TAB_ARRAY) {
                if (creativeGui.isMouseOverTab(tab, localMouseX, localMouseY)) {
                    return handCursor;
                }
            }
        }

        if (hasHoveredWidget(gui, gui, mouseX, mouseY, Collections.newSetFromMap(new IdentityHashMap<>()))) {
            return handCursor;
        }

        for (GuiTextField textField : findTextFields(gui)) {
            if (textField != null && isHovered(textField.x, textField.y, textField.width, textField.height, mouseX, mouseY)) {
                return textCursor;
            }
        }

        return arrowCursor;
    }

    private static boolean hasHoveredWidget(Object target, GuiScreen rootScreen, int mouseX, int mouseY, Set<Object> visited) {
        if (target instanceof GuiScreen && target != rootScreen) {
            return false;
        }
        if (target == null || !visited.add(target)) {
            return false;
        }

        if (target instanceof GuiButton) {
            GuiButton button = (GuiButton) target;
            return button.visible && button.enabled && isHovered(button.x, button.y, button.width, button.height, mouseX, mouseY);
        }

        if (target instanceof GuiListExtended && isHoveredListAction((GuiListExtended) target, mouseX, mouseY)) {
            return true;
        }

        if (!(target instanceof Gui) && !(target instanceof GuiSlot) && !(target instanceof GuiListExtended.IGuiListEntry)) {
            return false;
        }

        Class<?> type = target.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    Object value = field.get(target);
                    if (value instanceof Iterable<?>) {
                        for (Object element : (Iterable<?>) value) {
                            if (hasHoveredWidget(element, rootScreen, mouseX, mouseY, visited)) {
                                return true;
                            }
                        }
                    } else if (value != null && value.getClass().isArray()) {
                        for (int index = 0; index < Array.getLength(value); index++) {
                            if (hasHoveredWidget(Array.get(value, index), rootScreen, mouseX, mouseY, visited)) {
                                return true;
                            }
                        }
                    } else if (hasHoveredWidget(value, rootScreen, mouseX, mouseY, visited)) {
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
            type = type.getSuperclass();
        }

        return false;
    }

    private static boolean isHoveredListAction(GuiListExtended list, int mouseX, int mouseY) {
        int entryIndex = list.getSlotIndexFromScreenCoords(mouseX, mouseY);
        if (entryIndex < 0) {
            return false;
        }

        GuiListExtended.IGuiListEntry entry = list.getListEntry(entryIndex);
        int rowLeft = list.left + list.width / 2 - list.getListWidth() / 2 + 2;
        int relativeX = mouseX - rowLeft;
        int rowTop = list.top + 4 - list.getAmountScrolled() + entryIndex * list.slotHeight + list.headerPadding;
        int relativeY = mouseY - rowTop;
        boolean overIcon = relativeX >= 0 && relativeX < 32 && relativeY >= 0 && relativeY < 32;

        return overIcon && (entry instanceof GuiListWorldSelectionEntry || entry instanceof ServerListEntryNormal);
    }

    private static List<GuiTextField> findTextFields(GuiScreen gui) {
        List<GuiTextField> result = new ArrayList<>();
        if (gui == null) {
            return result;
        }

        collectTextFields(gui, result);
        return result;
    }

    private static void collectTextFields(Object target, List<GuiTextField> result) {
        if (target == null) {
            return;
        }

        Class<?> clazz = target.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = field.get(target);
                    if (value instanceof GuiTextField) {
                        result.add((GuiTextField) value);
                    } else if (value instanceof List<?>) {
                        for (Object obj : (List<?>) value) {
                            if (obj instanceof GuiTextField) {
                                result.add((GuiTextField) obj);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    private static boolean isHovered(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static boolean initializeCursors() {
        if (cursorsInitialized) {
            return true;
        }
        if (!Display.isCreated() || Display.getWindow() == 0L) {
            return false;
        }

        try {
            arrowCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);
            handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
            textCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR);
            verticalResizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR);
            if (arrowCursor == 0L || handCursor == 0L || textCursor == 0L || verticalResizeCursor == 0L) {
                throw new IllegalStateException("GLFW returned an empty standard cursor handle");
            }
            cursorsInitialized = true;
            return true;
        } catch (Exception | LinkageError error) {
            destroyCursors();
            logCursorError("Could not create GLFW cursors", error);
            return false;
        }
    }

    private static void setCursor(long nextCursor) {
        if (Mouse.isGrabbed()) {
            return;
        }

        if (nextCursor == 0L) {
            nextCursor = arrowCursor;
        }

        if (nextCursor == currentCursor) {
            return;
        }

        try {
            GLFW.glfwSetCursor(Display.getWindow(), nextCursor);
            currentCursor = nextCursor;
        } catch (Exception | LinkageError error) {
            logCursorError("Could not apply GLFW cursor", error);
        }
    }

    private static void destroyCursors() {
        if (arrowCursor != 0L) {
            GLFW.glfwDestroyCursor(arrowCursor);
            arrowCursor = 0L;
        }
        if (handCursor != 0L) {
            GLFW.glfwDestroyCursor(handCursor);
            handCursor = 0L;
        }
        if (textCursor != 0L) {
            GLFW.glfwDestroyCursor(textCursor);
            textCursor = 0L;
        }
        if (verticalResizeCursor != 0L) {
            GLFW.glfwDestroyCursor(verticalResizeCursor);
            verticalResizeCursor = 0L;
        }
    }

    private static void logCursorError(String message, Throwable error) {
        if (!cursorErrorLogged) {
            ExampleMod.LOGGER.error("{}; GUI cursor changes are unavailable", message, error);
            cursorErrorLogged = true;
        }
    }
}
