package ch.epfl.gameboj.gui.screens;

import static ch.epfl.gameboj.GameBoy.CYCLES_PER_NANOSECOND;
import static ch.epfl.gameboj.component.lcd.LcdController.IMAGE_CYCLE_DURATION;
import static ch.epfl.gameboj.component.lcd.LcdController.LCD_HEIGHT;
import static ch.epfl.gameboj.component.lcd.LcdController.LCD_WIDTH;

import java.awt.Desktop;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.CartridgeDisassembler;
import ch.epfl.gameboj.ImageViewRecorder;
import ch.epfl.gameboj.component.lcd.LcdImage;
import ch.epfl.gameboj.gui.ImageConverter;
import ch.epfl.gameboj.gui.Main;
import ch.epfl.gameboj.gui.color.ColorTheme;
import ch.epfl.gameboj.mvc.Model;
import ch.epfl.gameboj.mvc.View;
import ch.epfl.gameboj.mvc.controller.Controller;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.effect.SepiaTone;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public final class DevelopmentModeScreen {
    static BooleanProperty isSafeModeOn = new SimpleBooleanProperty();
    
    public static boolean isMuted;
    
    static BooleanProperty isPaused = new SimpleBooleanProperty();
    
    static boolean isSpeedButtonPressed;
    
    static long timeOnPause;
    
    static boolean isScreenCapOn;
    
    static long pauseTime;
    
    static List<Image> screenCapFrames = new ArrayList<>();
	
	public static Scene getDevelopmentModeScreen(Stage primaryStage) throws UnknownHostException {
		Model model = Model.getModel();
		View view = View.getView();
		Controller controller = Controller.getController();
		
		Locale currentLocale = view.getCurrentLocale();
		ResourceBundle currentGuiBundle = view.getCurrentGuiBundle();
		Preferences loadedPreferences = view.getLoadedPreferences();
		
		BorderPane developmentBorderPane = new BorderPane();
        FlowPane leftViewPane = new FlowPane();

        VBox topBox = new VBox();
        
        FileChooser gamePakChooser = new FileChooser();
        gamePakChooser.setTitle(currentGuiBundle.getString("chooseGamePak"));

        Menu fileMenu = new Menu(currentGuiBundle.getString("file")); // file related functionality
        MenuItem saveCartridgeMenuItem = new MenuItem(currentGuiBundle.getString("save"));
        MenuItem saveAsCartridgeMenuItem = new MenuItem(currentGuiBundle.getString("saveAs"));
        MenuItem loadCartridgeMenuItem = new MenuItem(currentGuiBundle.getString("load"));
        loadCartridgeMenuItem.setOnAction(e -> {
        	controller.startModelGameboy(gamePakChooser.showOpenDialog(primaryStage)); 
        });
        MenuItem exitMenuItem = new MenuItem(currentGuiBundle.getString("exit"));
        exitMenuItem.setOnAction(e -> {
        	System.exit(0);
        });
        fileMenu.getItems().addAll(exitMenuItem, saveCartridgeMenuItem, saveAsCartridgeMenuItem, loadCartridgeMenuItem);

        Menu dumpMenu = new Menu(currentGuiBundle.getString("dump")); // dumping related functionality
        MenuItem dumpTileSourceMenuItem = new MenuItem("Dump tile source");
        MenuItem dumpBackgroundMenuItem = new MenuItem("Dump background");
        MenuItem dumpWindowMenuItem = new MenuItem("Dump window");
        MenuItem dumpSpritesMenuItem = new MenuItem("Dump sprites");
        dumpMenu.getItems().addAll(dumpTileSourceMenuItem, dumpBackgroundMenuItem, dumpWindowMenuItem,
                dumpSpritesMenuItem);

        Menu debugMenu = new Menu(currentGuiBundle.getString("debug")); // debugging related functionality
        MenuItem stepByStepMenuItem = new MenuItem(currentGuiBundle.getString("stepByStepExecution"));
        MenuItem decompileMenuItem = new MenuItem(currentGuiBundle.getString("disassembleCartridge"));
        
        Stage gameboyStateStage = new Stage();
        GridPane gameboyStatePane = new GridPane();
        Label aLabel = new Label("A");
        Label bLabel = new Label("B");
        Label cLabel = new Label("C");
        Label dLabel = new Label("D");
        Label eLabel = new Label("E");
        Label fLabel = new Label("F");
        Label hLabel = new Label("H");
        Label lLabel = new Label("L");
        
        Label pcLabel = new Label("PC");
        pcLabel.setTooltip(new Tooltip("Program counter"));
        Label pcValueLabel = new Label("0");
        pcValueLabel.textProperty().bind(model.getGameboj().getCpu().getPC().asString());
        Label spLabel = new Label("SP");
        spLabel.setTooltip(new Tooltip("Stack pointer"));
        Label spValueLabel = new Label("0");
        spValueLabel.textProperty().bind(model.getGameboj().getCpu().getSP().asString());
        Label imeLabel = new Label("IME");
        imeLabel.setTooltip(new Tooltip("Interrupt master enable"));
        Label imeValueLabel = new Label("0");
        imeValueLabel.textProperty().bind(model.getGameboj().getCpu().getIME().asString());
        Label ieLabel = new Label("IE");
        ieLabel.setTooltip(new Tooltip("Interrupt enable"));
        Label ieValueLabel = new Label("0");
        ieValueLabel.textProperty().bind(model.getGameboj().getCpu().getIE().asString());
        Label ifLabel = new Label("IF");
        ifLabel.setTooltip(new Tooltip("Interrupt flags"));
        Label ifValueLabel = new Label("0");
        ifValueLabel.textProperty().bind(model.getGameboj().getCpu().getIF().asString());
        gameboyStatePane.addRow(0, aLabel, bLabel, cLabel, dLabel, eLabel, fLabel, hLabel, lLabel);
        gameboyStatePane.addRow(1, pcLabel, spLabel, imeLabel, ieLabel, ifLabel);
        gameboyStatePane.addRow(2, pcValueLabel, spValueLabel, imeValueLabel, ieValueLabel, ifValueLabel);
        Scene gameboyStateScene = new Scene(gameboyStatePane);
        gameboyStateStage.setScene(gameboyStateScene);
        
        MenuItem showStateMenuItem = new MenuItem(currentGuiBundle.getString("showGameboyState"));
        showStateMenuItem.setOnAction(e -> {
        	gameboyStateStage.show();
        });
        MenuItem disassembleBootMenuItem = new MenuItem(currentGuiBundle.getString("disassembleBootRom"));
        disassembleBootMenuItem.setOnAction(e -> {
        	try {
				FileWriter writer = new FileWriter("bootrom.txt");
				writer.write(CartridgeDisassembler.decompileBootRom());
				writer.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        });
        MenuItem disassembleHeaderMenuItem = new MenuItem(currentGuiBundle.getString("disassembleCartridgeHeader"));
        disassembleHeaderMenuItem.setOnAction(e -> {
        	try {
				FileWriter writer = new FileWriter("Z80Assembly_header.txt");
				writer.write(CartridgeDisassembler.decompileHeader(Controller.fileName));
				writer.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        });
        debugMenu.getItems().addAll(stepByStepMenuItem, decompileMenuItem, disassembleBootMenuItem,
        		disassembleHeaderMenuItem, showStateMenuItem);

        Menu optionsMenu = new Menu(currentGuiBundle.getString("options")); // gameboy options
        MenuItem changeSpeedMenuItem = new MenuItem(currentGuiBundle.getString("changeSpeed"));
        MenuItem gameboyConfigurationMenuItem = new MenuItem(currentGuiBundle.getString("configuration"));
        
        Label volumeLabel = new Label(currentGuiBundle.getString("globalVolume"));
    	Label themeLabel = new Label(currentGuiBundle.getString("gameboyColorTheme"));
    	Slider volumeSlider = new Slider();
    	volumeSlider.setMin(0);
    	volumeSlider.setMax(100);
    	volumeSlider.setValue(50);
    	view.getCurrentVolume().bind(volumeSlider.valueProperty());
    	ChoiceBox<String> gbThemes = new ChoiceBox<>(FXCollections.observableArrayList(
            	    "Standard", "Creepy", "Beautiful Day", "Night")
        );
    	gbThemes.getSelectionModel().selectedItemProperty().addListener((f, o, n) -> {
    		switch (n) {
        		case "Standard":
        			ImageConverter.setColorTheme(ColorTheme.STANDARD_COLOR_THEME);
        			break;
        		case "Creepy":
        			ImageConverter.setColorTheme(ColorTheme.CREEPY_COLOR_THEME);
    			break;
        		case "Beautiful Day":
        			ImageConverter.setColorTheme(ColorTheme.DESERT_COLOR_THEME);
    			break;
        		case "Night":
        			ImageConverter.setColorTheme(ColorTheme.NIGHT_COLOR_THEME);
    			break;
    		}
    	});
    	
        ImageView gameboySkin = new ImageView(new Image("file:game-boy-vector-free-download-cartoon-gameboy.jpg"));
        Pane emulationPane = new Pane(gameboySkin);
        view.getEmulationView().setTranslateX(130);
        view.getEmulationView().setTranslateY(70);
        emulationPane.getChildren().add(view.getEmulationView());
        developmentBorderPane.setCenter(emulationPane);
    	
    	ChoiceBox<String> gbEffects = new ChoiceBox<>(FXCollections.observableArrayList("Sepia", "Lighting", "SepiaAndLighting")); //TODO check three other types of light
    	Light.Distant distantLightSource = new Light.Distant();
    	distantLightSource.setAzimuth(20);
    	Lighting lightingEffect = new Lighting(distantLightSource);
    	SepiaTone sepiaEffect = new SepiaTone();
		FadeTransition fadeEffectTransition = new FadeTransition();
    	gbEffects.getSelectionModel().selectedItemProperty().addListener((f, o, n) -> {
    		switch (n) {
    		case "Sepia":
    			emulationPane.setEffect(sepiaEffect);
    			break;
    		case "Lighting":
    			emulationPane.setEffect(lightingEffect);
    			break;
    		case "SepiaAndLighting":
    			sepiaEffect.setInput(lightingEffect);
    			emulationPane.setEffect(sepiaEffect);
    			break;
    		}
    	});
    	GridPane configPane = new GridPane();
    	configPane.add(volumeLabel, 0, 0);
    	configPane.add(volumeSlider, 0, 1);
    	configPane.add(themeLabel, 0, 2);
    	configPane.add(gbThemes, 0, 3);
    	configPane.add(gbEffects, 4, 0);
        Scene gbConfigScene = new Scene(configPane, 130, 75);
    	Stage gbConfigStage = new Stage();
        
        gameboyConfigurationMenuItem.setOnAction(e -> {
        	gbConfigStage.setScene(gbConfigScene);
        	gbConfigStage.show();
        });
        optionsMenu.getItems().addAll(changeSpeedMenuItem, gameboyConfigurationMenuItem);

        Menu windowMenu = new Menu(currentGuiBundle.getString("window")); // workspace visual control
        Menu perspectiveMenu = new Menu(currentGuiBundle.getString("perspective")); // switch to view layout presets
        Menu showViewMenu = new Menu(currentGuiBundle.getString("showView")); // show view in workspace
        MenuItem setMaximized = new MenuItem(currentGuiBundle.getString("maximizeWindow"));
        setMaximized.setOnAction(e -> primaryStage.setMaximized(true));
        MenuItem setFullscreen = new MenuItem(currentGuiBundle.getString("goFullscreen"));
        setFullscreen.setOnAction(e -> primaryStage.setFullScreen(true));
        setFullscreen.setAccelerator(new KeyCodeCombination(KeyCode.F11));

        ImageView backgroundView = new ImageView();
        ImageView windowView = new ImageView();
        ImageView bgSpriteView = new ImageView();
        ImageView fgSpriteView = new ImageView();

        MenuItem backgroundViewMenuItem = new MenuItem(currentGuiBundle.getString("background"));
        Rectangle viewportRectangle = new Rectangle(0, 0, LCD_WIDTH, LCD_HEIGHT);
        viewportRectangle.setFill(Color.TRANSPARENT);
        viewportRectangle.setStroke(Color.DARKGREEN);
        Pane backgroundViewPane = new Pane(backgroundView);
        backgroundViewPane.getChildren().add(viewportRectangle);
        backgroundViewMenuItem.setOnAction(e -> leftViewPane.getChildren().add(backgroundViewPane));
        MenuItem windowViewMenuItem = new MenuItem(currentGuiBundle.getString("window"));
        windowViewMenuItem.setOnAction(e -> leftViewPane.getChildren().add(windowView));
        MenuItem bgSpritesViewMenuItem = new MenuItem(currentGuiBundle.getString("backgroundSprites"));
        bgSpritesViewMenuItem.setOnAction(e -> leftViewPane.getChildren().add(bgSpriteView));
        MenuItem fgSpritesViewMenuItem = new MenuItem(currentGuiBundle.getString("foregroundSprites"));
        fgSpritesViewMenuItem.setOnAction(e -> leftViewPane.getChildren().add(fgSpriteView));
        showViewMenu.getItems().addAll(backgroundViewMenuItem, windowViewMenuItem, bgSpritesViewMenuItem, fgSpritesViewMenuItem);
        windowMenu.getItems().addAll(perspectiveMenu, showViewMenu, setMaximized, setFullscreen);

        Menu preferencesMenu = new Menu(currentGuiBundle.getString("preferences")); // program preferences
        MenuItem themeMenuItem = new MenuItem(currentGuiBundle.getString("colorTheme"));
        MenuItem skinsMenuItem = new MenuItem(currentGuiBundle.getString("skins"));
        MenuItem languageMenuItem = new MenuItem(currentGuiBundle.getString("language"));
        GridPane languageSelectionPane = new GridPane();
    	ToggleGroup languageSelectionGroup = new ToggleGroup();
    	RadioButton englishButton = new RadioButton(Locale.ENGLISH.getDisplayName());
    	englishButton.setOnAction(e -> {
    		loadedPreferences.put("PREFERRED_LOCALE", "en_US");		
    	});
    	englishButton.setSelected(true);
    	englishButton.setToggleGroup(languageSelectionGroup);
    	RadioButton frenchButton = new RadioButton(Locale.FRANCE.getDisplayName());
    	frenchButton.setOnAction(e -> {
    		loadedPreferences.put("PREFERRED_LOCALE", "fr_FR");
    	});
    	frenchButton.setToggleGroup(languageSelectionGroup);
    	RadioButton romanianButton = new RadioButton(new Locale("ro", "RO").getDisplayName());
    	romanianButton.setOnAction(e -> {
    		loadedPreferences.put("PREFERRED_LOCALE", "ro_RO");
    	});
    	romanianButton.setToggleGroup(languageSelectionGroup);
    	languageSelectionPane.add(englishButton, 0, 0);
    	languageSelectionPane.add(frenchButton, 0, 1);
    	languageSelectionPane.add(romanianButton, 0, 2);
    	Scene languageSelectionScene = new Scene(languageSelectionPane);
    	Stage languageSelectionStage = new Stage();
        languageMenuItem.setOnAction(e -> {
        	languageSelectionStage.setScene(languageSelectionScene);
        	languageSelectionStage.show();
        });
        MenuItem safeModeButton = new MenuItem("Enable/Disable safe mode");
        Thread safeModeShutdownThread = new Thread() {
			public void run() {
				model.getGameboj().getCartridge().toFile("currentSave.gb");
			}
        };
        safeModeButton.setOnAction(e -> {
        	isSafeModeOn.set(!isSafeModeOn.get());
        	if (isSafeModeOn.get()) {
        		Runtime.getRuntime().addShutdownHook(safeModeShutdownThread);
        	} else {
        		Runtime.getRuntime().removeShutdownHook(safeModeShutdownThread);
        	}
        });
        MenuItem configurePreloadButton = new MenuItem("Preload preferences");
        configurePreloadButton.setOnAction(e -> {
        	loadedPreferences.put("PRELOAD_CARTRIDGE", gamePakChooser.showOpenDialog(primaryStage).getAbsolutePath());
        });
        preferencesMenu.getItems().addAll(themeMenuItem, skinsMenuItem, languageMenuItem, safeModeButton, configurePreloadButton);

        Menu helpMenu = new Menu(currentGuiBundle.getString("help")); // help functionality
        MenuItem programmingManualMenuItem = new MenuItem(currentGuiBundle.getString("nintendoProgrammingManual"));
        programmingManualMenuItem.setOnAction(e -> {
        	try {
				Desktop.getDesktop().browse(new URI("http://chrisantonellis.com/files/gameboy/gb-programming-manual.pdf"));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (URISyntaxException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        });
        MenuItem aboutMenuItem = new MenuItem(currentGuiBundle.getString("about"));
        aboutMenuItem.setOnAction(e -> {
        	try {
				Desktop.getDesktop().browse(new URI("https://dave_and_chris.gitlab.io/gameboj/"));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (URISyntaxException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        });
        helpMenu.getItems().addAll(programmingManualMenuItem, aboutMenuItem);

        MenuBar mainMenuBar = new MenuBar();
        mainMenuBar.getMenus().addAll(fileMenu, dumpMenu, debugMenu, optionsMenu, windowMenu, preferencesMenu,
                helpMenu);
        
        InetAddress locIP = InetAddress.getLocalHost();
		System.out.println(locIP.getHostAddress());
        
        Button testServerButton = new Button("Open link");
        testServerButton.setOnAction(e -> {
        	if (model.getServerSocket() == null) {
                model.initiateServer(locIP);
        	}
        });
        
        Button testClientButton = new Button("Connect to link");
        testClientButton.setOnAction(e -> {
        	if (model.getClientSocket() == null) {
        		Stage connectDataStage = new Stage();
        		GridPane connectDataPane = new GridPane();
        		Label ipLabel = new Label("Enter the host IP");
        		TextField ipField = new TextField();
        		connectDataPane.add(ipLabel, 0, 0);
        		connectDataPane.add(ipField, 0, 1);
        		Scene connectDataScene = new Scene(connectDataPane);
        		connectDataScene.setOnKeyPressed(f -> {
        			if (f.getCode() == KeyCode.ENTER) {
                    	try {
							model.initiateClient(InetAddress.getByName(ipField.getText()));
						} catch (UnknownHostException e1) {
							e1.printStackTrace();
						}
        			}
        		});
        		connectDataStage.setScene(connectDataScene);
        		connectDataStage.show();
        	}
        });

        // TODO add button graphics (btn.setGraphic())
        Button tbResetButton = new Button(currentGuiBundle.getString("reset"));
        tbResetButton.setOnAction(e -> {
        	controller.startModelGameboy(Controller.fileName);
            
            model.getAnimationTimer().stop();
            
            model.getStart().set(System.nanoTime());
            
            model.setAnimationTimer(new AnimationTimer() {
                @Override
                public void handle(long now) {
                    long elapsed = now - model.getStart().get() - pauseTime;
                    long elapsedCycles = (long) (elapsed * CYCLES_PER_NANOSECOND);

                    model.getGameboj().runUntil(model.getCycleSpeed().get() * IMAGE_CYCLE_DURATION + model.getGameboj().getCycles());

                    view.getEmulationView().requestFocus();
                    view.getEmulationView().setImage(ImageConverter.convert(model.getGameboj().getLcdController().currentImage()));

                    viewportRectangle.setTranslateX(model.getGameboj().getBus().read(AddressMap.REG_SCX)); // FIXME
                    viewportRectangle.setTranslateY(model.getGameboj().getBus().read(AddressMap.REG_SCY));
                    backgroundView.setImage(ImageConverter.convert(model.getGameboj().getLcdController().getBackground()));
                    windowView.setImage(ImageConverter.convert(model.getGameboj().getLcdController().getWindow()));
                    
                    LcdImage[] spriteLayerImages = model.getGameboj().getLcdController().getSprites();
                    bgSpriteView.setImage(ImageConverter.convert(spriteLayerImages[0]));
                    fgSpriteView.setImage(ImageConverter.convert(spriteLayerImages[1]));
                }
            });
            
            model.getAnimationTimer().start();
        });
        Button tbPauseButton = new Button(currentGuiBundle.getString("pause"));
        tbPauseButton.setOnAction(e -> {
            if (isPaused.get()) {
                isPaused.set(false);
    			fadeEffectTransition.stop();
                model.getAnimationTimer().start();
                pauseTime += System.nanoTime() - timeOnPause;
            } else {
                isPaused.set(true);
                model.getAnimationTimer().stop();
                fadeEffectTransition.setFromValue(1.0);
    			fadeEffectTransition.setToValue(0.6);
    			fadeEffectTransition.setAutoReverse(true);
    			fadeEffectTransition.setNode(view.getEmulationView());
    			fadeEffectTransition.setCycleCount(FadeTransition.INDEFINITE);
    			fadeEffectTransition.playFromStart();
                timeOnPause = System.nanoTime();
            }
        });       
        Button screenshotButton = new Button(currentGuiBundle.getString("screenshot"));
        screenshotButton.setOnAction(e -> {
			try {
				Main.screenshot();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});       
        Button speedTimes5Button = new Button("x3");
        speedTimes5Button.setOnAction(e -> {
        	model.getCycleSpeed().set(isSpeedButtonPressed ? 3 : 1)   ;  	
        	isSpeedButtonPressed = !isSpeedButtonPressed;
        });
        Button saveButton = new Button(currentGuiBundle.getString("save"));
        saveButton.setOnAction(e -> {
        	model.getGameboj().getCartridge().toFile("currentSave.gb");
        });
        Button toggleScreenCapButton = new Button("Start/stop screen capture");
        ImageViewRecorder recorder = new ImageViewRecorder("screen_recording.avi", view.getEmulationView());
        toggleScreenCapButton.setOnAction(e -> {
        	if (isScreenCapOn) {
        		isScreenCapOn = false;
        		screenCapFrames = new ArrayList<>();
        		recorder.stop();
        	} else {
        		isScreenCapOn = true;
        		recorder.start();
        	}
        });
        Button muteButton = new Button("Mute/Unmute");
        muteButton.setOnAction(e -> {
        	if (!isMuted) {
            	model.getGameboj().getSoundController().getLine().stop();
        	} else {
        		model.getGameboj().getSoundController().getLine().start();
        	}
        	isMuted = !isMuted;
        });
        
        Button terminateConnectionButton = new Button("Terminate");
        terminateConnectionButton.setOnAction(e -> {
			try {
				model.getServerSocket().close();
				model.getClientSocket().close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        });
        
        Button startGameButton = new Button("Start");
        startGameButton.setOnAction(e -> {
            model.getAnimationTimer().start();
        });
        ToolBar toolBar = new ToolBar(tbResetButton, tbPauseButton, speedTimes5Button, screenshotButton, toggleScreenCapButton, muteButton, saveButton,
        		testServerButton, testClientButton, terminateConnectionButton, startGameButton); 

        topBox.getChildren().addAll(mainMenuBar, toolBar);
        
        developmentBorderPane.setTop(topBox);
        developmentBorderPane.setLeft(leftViewPane);
        
        return new Scene(developmentBorderPane);
	}
}
