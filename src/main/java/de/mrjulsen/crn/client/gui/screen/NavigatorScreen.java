package de.mrjulsen.crn.client.gui.screen;

import java.util.List;
import java.util.UUID;

import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.ModMain;
import de.mrjulsen.crn.client.gui.ControlCollection;
import de.mrjulsen.mcdragonlib.DragonLibConstants;
import de.mrjulsen.mcdragonlib.client.gui.GuiAreaDefinition;
import de.mrjulsen.mcdragonlib.client.gui.GuiUtils;
import de.mrjulsen.mcdragonlib.client.gui.DragonLibTooltip;
import de.mrjulsen.mcdragonlib.client.gui.wrapper.CommonScreen;
import de.mrjulsen.crn.client.gui.ModGuiIcons;
import de.mrjulsen.crn.client.gui.widgets.ModDestinationSuggestions;
import de.mrjulsen.crn.client.gui.widgets.RouteEntryOverviewWidget;
import de.mrjulsen.crn.config.ModClientConfig;
import de.mrjulsen.crn.config.ModCommonConfig;
import de.mrjulsen.crn.data.ClientTrainStationSnapshot;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.SimpleRoute;
import de.mrjulsen.crn.data.TrainStationAlias;
import de.mrjulsen.crn.event.listeners.IJourneyListenerClient;
import de.mrjulsen.crn.event.listeners.JourneyListenerManager;
import de.mrjulsen.crn.network.InstanceManager;
import de.mrjulsen.crn.network.NetworkManager;
import de.mrjulsen.crn.network.packets.cts.NavigationRequestPacket;
import de.mrjulsen.crn.network.packets.cts.NearestStationRequestPacket;
import de.mrjulsen.crn.util.ModGuiUtils;
import de.mrjulsen.mcdragonlib.utils.TimeUtils;
import de.mrjulsen.mcdragonlib.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastIds;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class NavigatorScreen extends CommonScreen implements IJourneyListenerClient {

    private static final ResourceLocation GUI = new ResourceLocation(ModMain.MOD_ID, "textures/gui/navigator.png");
    private static final int GUI_WIDTH = 255;
    private static final int GUI_HEIGHT = 247;

    private static final int DEFAULT_ICON_BUTTON_WIDTH = 18;
    private static final int DEFAULT_ICON_BUTTON_HEIGHT = 18;

    private static final int ENTRIES_START_Y_OFFSET = 10;  
    private static final int ENTRY_SPACING = 4;
    
    private final int AREA_X = 16;
    private final int AREA_Y = 67;        
    private final int AREA_W = 220;
    private final int AREA_H = 143;

    private int guiLeft, guiTop;
    private int angle = 0;    
    
    // Controls
    private IconButton locationButton;
    private IconButton searchButton;   
    private IconButton goToTopButton;
    private IconButton globalSettingsButton;  
    private IconButton searchSettingsButton;  
	private EditBox fromBox;
	private EditBox toBox;    
	private LerpedFloat scroll = LerpedFloat.linear().startWithValue(0);   
	private ModDestinationSuggestions destinationSuggestions;
    private GuiAreaDefinition switchButtonsArea; 
    private final ControlCollection routesCollection = new ControlCollection(); 

    // Data
    private SimpleRoute[] routes;
    private String stationFrom;
    private String stationTo;
    private int lastRefreshedTime;
    private final NavigatorScreen instance;
    private final Level level;
    private final Font shadowlessFont;
    private final UUID clientId = UUID.randomUUID();

    // var
    private boolean isLoadingRoutes = false;
    private boolean generatingRouteEntries = false;

    // Tooltips
    private final MutableComponent searchingText = Utils.translate("gui." + ModMain.MOD_ID + ".navigator.searching");
    private final MutableComponent noConnectionsText = Utils.translate("gui." + ModMain.MOD_ID + ".navigator.no_connections");
    private final MutableComponent notSearchedText = Utils.translate("gui." + ModMain.MOD_ID + ".navigator.not_searched");
    private final MutableComponent errorTitle = Utils.translate("gui." + ModMain.MOD_ID + ".navigator.error_title");
    private final MutableComponent startEndEqualText = Utils.translate("gui." + ModMain.MOD_ID + ".navigator.start_end_equal");
    private final MutableComponent startEndNullText = Utils.translate("gui." + ModMain.MOD_ID + ".navigator.start_end_null");

    private final MutableComponent tooltipSearch = Utils.translate("gui." + ModMain.MOD_ID + ".navigator.search.tooltip");
    private final MutableComponent tooltipLocation = Utils.translate("gui." + ModMain.MOD_ID + ".navigator.location.tooltip");
    private final MutableComponent tooltipSwitch = Utils.translate("gui." + ModMain.MOD_ID + ".navigator.switch.tooltip");
    private final MutableComponent tooltipGlobalSettings = Utils.translate("gui." + ModMain.MOD_ID + ".navigator.global_settings.tooltip");
    private final MutableComponent tooltipSearchSettings = Utils.translate("gui." + ModMain.MOD_ID + ".navigator.search_settings.tooltip");


    @SuppressWarnings("resource")
    public NavigatorScreen(Level level) {
        super(Utils.translate("gui." + ModMain.MOD_ID + ".navigator.title"));
        this.instance = this;
        this.level = level;
        this.shadowlessFont = new NoShadowFontWrapper(Minecraft.getInstance().font); 
    }

    @Override
    public UUID getJourneyListenerClientId() {
        return clientId;
    }

    private void generateRouteEntries() {
        generatingRouteEntries = true;
        routesCollection.components.clear();

        if (routes != null && routes.length > 0) {
            for (int i = 0; i < routes.length; i++) {
                SimpleRoute route = routes[i];
                AbstractWidget w = new RouteEntryOverviewWidget(instance, level, lastRefreshedTime, guiLeft + 26, guiTop + 67 + ENTRIES_START_Y_OFFSET + (i * (RouteEntryOverviewWidget.HEIGHT + ENTRY_SPACING)), route, (btn) -> {});
                routesCollection.components.add(w);
            } 
        }         
        generatingRouteEntries = false;
    }   

    private void clearSuggestions() {
        if (destinationSuggestions != null) {
            destinationSuggestions.getEditBox().setSuggestion("");
        }
        destinationSuggestions = null;
    }

    private void setLastRefreshedTime() {
        lastRefreshedTime = (int)(level.getDayTime());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        JourneyListenerManager.getInstance().removeClientListenerForAll(this);
        super.onClose();
    }

    private void switchButtonClick() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        String fromInput = fromBox.getValue();
        String toInput = toBox.getValue();

        fromBox.setValue(toInput);
        toBox.setValue(fromInput);
    }

    @Override
    protected void init() {
        super.init();        
        guiLeft = this.width / 2 - GUI_WIDTH / 2;
        guiTop = this.height / 2 - GUI_HEIGHT / 2;

        switchButtonsArea = new GuiAreaDefinition(guiLeft + 190, guiTop + 34, 11, 12);
        addTooltip(DragonLibTooltip.of(tooltipSwitch).assignedTo(switchButtonsArea));

        locationButton = this.addRenderableWidget(new IconButton(guiLeft + 208, guiTop + 20, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.POSITION.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                long id = InstanceManager.registerClientNearestStationResponseAction((result) -> {
                    if (result.aliasName.isPresent()) {
                        fromBox.setValue(result.aliasName.get().getAliasName().get());
                    }
                });
                NetworkManager.getInstance().sendToServer(Minecraft.getInstance().getConnection().getConnection(), new NearestStationRequestPacket(id, minecraft.player.position()));
            }
        });
        addTooltip(DragonLibTooltip.of(tooltipLocation).assignedTo(locationButton));

        searchButton = this.addRenderableWidget(new IconButton(guiLeft + 208, guiTop + 42, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_MTD_SCAN) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                
                if (stationFrom == null || stationTo == null) {
                    Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToastIds.PERIODIC_NOTIFICATION, errorTitle, startEndNullText));
                    return;
                }

                if (stationFrom.equals(stationTo)) {
                    Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToastIds.PERIODIC_NOTIFICATION, errorTitle, startEndEqualText));
                    return;
                }

                isLoadingRoutes = true;

                long id = InstanceManager.registerClientNavigationResponseAction((routes, data) -> {
                    JourneyListenerManager.getInstance().removeClientListenerForAll(instance);

                    instance.routes = routes.toArray(SimpleRoute[]::new);
                    setLastRefreshedTime();
                    generateRouteEntries();
                    isLoadingRoutes = false;

                    for (SimpleRoute route : instance.routes) {
                        UUID listenerId = route.listen(instance);
                        JourneyListenerManager.getInstance().get(listenerId, instance).start();
                    }
                });
                scroll.chase(0, 0.7f, Chaser.EXP);
                NetworkManager.getInstance().sendToServer(Minecraft.getInstance().getConnection().getConnection(), new NavigationRequestPacket(id, stationFrom, stationTo));
               
            }
        });
        addTooltip(DragonLibTooltip.of(tooltipSearch).assignedTo(searchButton));

        fromBox = new EditBox(font, guiLeft + 50, guiTop + 25, 157, 12, Utils.emptyText());
		fromBox.setBordered(false);
		fromBox.setMaxLength(25);
		fromBox.setTextColor(0xFFFFFF);
        fromBox.setValue(stationFrom);
        fromBox.setResponder(x -> {
            stationFrom = x;
            updateEditorSubwidgets(fromBox);
        });
		addRenderableWidget(fromBox);

        toBox = new EditBox(font, guiLeft + 50, guiTop + 47, 157, 12, Utils.emptyText());
		toBox.setBordered(false);
		toBox.setMaxLength(25);
		toBox.setTextColor(0xFFFFFF);
        toBox.setValue(stationTo);
        toBox.setResponder(x -> {
            stationTo = x;
            updateEditorSubwidgets(toBox);
        });
		addRenderableWidget(toBox);

        goToTopButton = this.addRenderableWidget(new IconButton(guiLeft + GUI_WIDTH - 10, guiTop + AREA_Y, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_PRIORITY_VERY_HIGH) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                scroll.chase(0, 0.7f, Chaser.EXP);
            }
        });
        addTooltip(DragonLibTooltip.of(Constants.TOOLTIP_GO_TO_TOP).assignedTo(goToTopButton));

        // Global Options Button
        if (minecraft.player.hasPermissions(ModCommonConfig.GLOBAL_SETTINGS_PERMISSION_LEVEL.get())) {
            globalSettingsButton = this.addRenderableWidget(new IconButton(guiLeft + 43, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.SETTINGS.getAsCreateIcon()) {
                @Override
                public void onClick(double mouseX, double mouseY) {
                    super.onClick(mouseX, mouseY);
                    minecraft.setScreen(new GlobalSettingsScreen(level, instance));
                }
            });
            addTooltip(DragonLibTooltip.of(tooltipGlobalSettings).assignedTo(globalSettingsButton));
        }

        searchSettingsButton = this.addRenderableWidget(new IconButton(guiLeft + 21, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, ModGuiIcons.FILTER.getAsCreateIcon()) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                minecraft.setScreen(new SearchSettingsScreen(level, instance));
            }
        });
        addTooltip(DragonLibTooltip.of(tooltipSearchSettings).assignedTo(searchSettingsButton));

        this.addRenderableWidget(new IconButton(guiLeft + GUI_WIDTH - 42, guiTop + 222, DEFAULT_ICON_BUTTON_WIDTH, DEFAULT_ICON_BUTTON_HEIGHT, AllIcons.I_MTD_CLOSE) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                onClose();
            }
        });

        generateRouteEntries();
    }

    protected void updateEditorSubwidgets(EditBox field) {
        clearSuggestions();

		destinationSuggestions = new ModDestinationSuggestions(this.minecraft, this, field, this.font, getViableStations(field), field.getHeight() + 2 + field.getY());
        destinationSuggestions.setAllowSuggestions(true);
        destinationSuggestions.updateCommandInfo();
	}

    private List<TrainStationAlias> getViableStations(EditBox field) {
        return ClientTrainStationSnapshot.getInstance().getAllTrainStations().stream()
            .map(x -> GlobalSettingsManager.getInstance().getSettingsData().getAliasFor(x))
            .distinct()
            .filter(x -> !GlobalSettingsManager.getInstance().getSettingsData().isBlacklisted(x))
            .sorted((a, b) -> a.getAliasName().get().compareTo(b.getAliasName().get()))
            .toList();
	}

    @Override
    public void tick() {
        angle += 6;
        if (angle > 360) {
            angle = 0;
        }
        
		scroll.tickChaser();
        
		if (destinationSuggestions != null) {
            destinationSuggestions.tick();

            if (!toBox.canConsumeInput() && !fromBox.canConsumeInput()) {
                clearSuggestions();
            }
        }

        this.goToTopButton.visible = routes != null && scroll.getValue() > 0;

        searchButton.active = !isLoadingRoutes;

        super.tick();
    }

    @Override
    public void renderBg(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) { 
        
		float scrollOffset = -scroll.getValue(pPartialTick);

        renderBackground(graphics);
        GuiUtils.blit(GUI, graphics, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);
        for (Renderable widget : this.renderables)
            widget.render(graphics, pMouseX, pMouseY, pPartialTick);

        graphics.drawString(shadowlessFont, title, guiLeft + 19, guiTop + 4, 0x4F4F4F);
        String timeString = TimeUtils.parseTime((int)((level.getDayTime() + Constants.TIME_SHIFT) % DragonLibConstants.TICKS_PER_DAY), ModClientConfig.TIME_FORMAT.get());
        graphics.drawString(shadowlessFont, timeString, guiLeft + GUI_WIDTH - 22 - shadowlessFont.width(timeString), guiTop + 4, 0x4F4F4F);

        if (!isLoadingRoutes && !generatingRouteEntries) {
            if (routes == null) {
                graphics.drawCenteredString(font, notSearchedText, guiLeft + GUI_WIDTH / 2, guiTop + 32 + GUI_HEIGHT / 2, 0xFFFFFF);
                ModGuiIcons.INFO.render(graphics, (int)(guiLeft + GUI_WIDTH / 2 - 8), (int)(guiTop + GUI_HEIGHT / 2));
            } else if (routes.length <= 0) {
                graphics.drawCenteredString(font, noConnectionsText, guiLeft + GUI_WIDTH / 2, guiTop + 32 + GUI_HEIGHT / 2, 0xFFFFFF);
                AllIcons.I_ACTIVE.render(graphics, (int)(guiLeft + GUI_WIDTH / 2 - 8), (int)(guiTop + GUI_HEIGHT / 2));
            } else {
                UIRenderHelper.swapAndBlitColor(minecraft.getMainRenderTarget(), UIRenderHelper.framebuffer);
                ModGuiUtils.startStencil(graphics, guiLeft + AREA_X, guiTop + AREA_Y, AREA_W, AREA_H);
                graphics.pose().pushPose();
                graphics.pose().translate(0, scrollOffset, 0);

                int start = (int)(Math.abs(scrollOffset + ENTRIES_START_Y_OFFSET) / (ENTRY_SPACING + RouteEntryOverviewWidget.HEIGHT));
                int end = Math.min(routesCollection.components.size(), start + 2 + (int)(AREA_H / (ENTRY_SPACING + RouteEntryOverviewWidget.HEIGHT)));
                for (int i = start; i < end; i++) {
                    routesCollection.components.get(i).render(graphics, (int)(pMouseX), (int)(pMouseY - scrollOffset), pPartialTick);
                }

                graphics.pose().popPose();
                ModGuiUtils.endStencil();                
                graphics.fillGradient(guiLeft + AREA_X, guiTop + AREA_Y, guiLeft + AREA_X + AREA_W, guiTop + AREA_Y + 10, 0x77000000, 0x00000000);
                graphics.fillGradient(guiLeft + AREA_X, guiTop + AREA_Y + AREA_H - 10, guiLeft + AREA_X + AREA_W, guiTop + AREA_Y + AREA_H, 0x00000000, 0x77000000);
                UIRenderHelper.swapAndBlitColor(UIRenderHelper.framebuffer, minecraft.getMainRenderTarget());

                // Scrollbar
                double maxHeight = ENTRIES_START_Y_OFFSET + routes.length * (RouteEntryOverviewWidget.HEIGHT + 4) + ENTRIES_START_Y_OFFSET;
                double aH = AREA_H + 1;
                if (aH / maxHeight < 1) {
                    int scrollerHeight = Math.max(10, (int)(aH * (aH / maxHeight)));
                    int startY = guiTop + AREA_Y + (int)((AREA_H) * (Math.abs(scrollOffset) / maxHeight));

                    graphics.fill(guiLeft + AREA_X + AREA_W - 3, startY, guiLeft + AREA_X + AREA_W, startY + scrollerHeight, 0x7FFFFFFF);
                }
            }
        } else {            
            double offsetX = Math.sin(Math.toRadians(angle)) * 5;
            double offsetY = Math.cos(Math.toRadians(angle)) * 5; 
            
            graphics.drawCenteredString(font, searchingText, guiLeft + GUI_WIDTH / 2, guiTop + 32 + GUI_HEIGHT / 2, 0xFFFFFF);
            AllIcons.I_MTD_SCAN.render(graphics, (int)(guiLeft + GUI_WIDTH / 2 - 8 + offsetX), (int)(guiTop + GUI_HEIGHT / 2 + offsetY));
        }

        if (switchButtonsArea.isInBounds(pMouseX, pMouseY)) {
            graphics.fill(switchButtonsArea.getLeft(), switchButtonsArea.getTop(), switchButtonsArea.getRight(), switchButtonsArea.getBottom(), 0x3FFFFFFF);
        }
    }

    @Override
	public void renderFg(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		if (destinationSuggestions != null) {
			graphics.pose().pushPose();
			graphics.pose().translate(0, 0, 500);
			destinationSuggestions.render(graphics, mouseX, mouseY);
			graphics.pose().popPose();
		}
        super.renderFg(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (destinationSuggestions != null && destinationSuggestions.mouseClicked((int) pMouseX, (int) pMouseY, pButton))
			return true;

		float scrollOffset = scroll.getValue();

        if (switchButtonsArea.isInBounds(pMouseX, pMouseY)) {
            switchButtonClick();
        }

        if (pMouseX > guiLeft + AREA_X && pMouseX < guiLeft + AREA_X + AREA_W && pMouseY > guiTop + AREA_Y && pMouseY < guiTop + AREA_Y + AREA_H) {
            routesCollection.performForEach(x -> x.mouseClicked(pMouseX, pMouseY + scrollOffset, pButton));
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (destinationSuggestions != null && destinationSuggestions.keyPressed(pKeyCode, pScanCode, pModifiers))
			return true;

        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (destinationSuggestions != null && destinationSuggestions.mouseScrolled(pMouseX, pMouseY, Mth.clamp(pDelta, -1.0D, 1.0D)))
			return true;

		float chaseTarget = scroll.getChaseTarget();
		float max = -AREA_H;
        if (routes != null && routes.length > 0) {
            max += ENTRIES_START_Y_OFFSET + routes.length * (RouteEntryOverviewWidget.HEIGHT + 4) + ENTRIES_START_Y_OFFSET;
        }

		if (max > 0) {
			chaseTarget -= pDelta * 12;
			chaseTarget = Mth.clamp(chaseTarget, 0, max);
			scroll.chase((int) chaseTarget, 0.7f, Chaser.EXP);
		} else
			scroll.chase(0, 0.7f, Chaser.EXP);

		return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }
    
}
