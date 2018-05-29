package ch.epfl.gameboj.gui;

import static ch.epfl.gameboj.GameBoy.CYCLES_PER_NANOSECOND;
import static ch.epfl.gameboj.component.lcd.LcdController.IMAGE_CYCLE_DURATION;
import static ch.epfl.gameboj.component.lcd.LcdController.LCD_HEIGHT;
import static ch.epfl.gameboj.component.lcd.LcdController.LCD_WIDTH;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.sound.sampled.LineUnavailableException;

import ch.epfl.extended.ImageViewRecorder;
import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.CartridgeDisassembler;
import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.Joypad.Key;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.lcd.LcdImage;
import ch.epfl.gameboj.component.serial.SerialProtocol;
import ch.epfl.gameboj.gui.color.ColorTheme;
import ch.epfl.gameboj.gui.screens.LoginScreen;
import ch.epfl.gameboj.gui.screens.ModeChoiceScreen;
import ch.epfl.gameboj.gui.screens.SimpleModeScreen;
import ch.epfl.gameboj.gui.screens.SplashScreen;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
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

public class Main extends Application {
	private Locale currentLocale = Locale.getDefault();
	private ResourceBundle currentGuiBundle = ResourceBundle.getBundle("GUIBundle", currentLocale);
    private static GameBoy gameboj;
    BooleanProperty isPaused = new SimpleBooleanProperty();
    BooleanProperty isSafeModeOn = new SimpleBooleanProperty();
    boolean isSpeedButtonPressed;
    boolean isScreenCapOn;
    public static boolean isMuted;
    List<Image> screenCapFrames = new ArrayList<>();
    long start;
    long timeOnPause;
    long pauseTime;
    int cycleSpeed = 1;
    private AnimationTimer animationTimer;
    public static DoubleProperty currentVolume = new SimpleDoubleProperty();
    private static ServerSocket serverSocket;
    private static Socket clientSocket;
    public static boolean printCPU;
    boolean clientInit;

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException, LineUnavailableException, InterruptedException {
        List<String> cmdArgs = getParameters().getRaw();
        Preconditions.checkArgument(cmdArgs.size() <= 2, () -> System.exit(1));

        String fileName = cmdArgs.get(0); // TODO change this
        String saveFileName = cmdArgs.size() == 2 ? cmdArgs.get(1) : null;
        
        gameboj = saveFileName == null ? new GameBoy(Cartridge.ofFile(new File(fileName))) 
        		: new GameBoy(Cartridge.ofFile(new File(fileName)), saveFileName); // FIXME
        
        currentVolume.set(0);
        
		InetAddress locIP = InetAddress.getLocalHost();
		System.out.println(locIP.getHostAddress());
		
		Runnable r = new Runnable() {
        	@Override
			public void run() {
        		try {	
        			System.out.println("Bind ip server");
            		serverSocket = new ServerSocket(4444, 0, locIP);
        			Socket clientSocket = serverSocket.accept();
        			
        			SerialProtocol.setDataInput(new DataInputStream(clientSocket.getInputStream()));
        			SerialProtocol.setDataOutput(new DataOutputStream(clientSocket.getOutputStream()));
        			
                	SerialProtocol.startProtocol(true);
        		} catch (UnknownHostException e1) {
        			// TODO Auto-generated catch block
        			e1.printStackTrace();
        		} catch (IOException e1) {
        			// TODO Auto-generated catch block
        			e1.printStackTrace();
        		}
        	}
        };
        
        // TEST ---------
        Button testServerButton = new Button("Test server");
        testServerButton.setOnAction(e -> {
            new Thread(r).start();
        });
        
        Button testClientButton = new Button("Test client");
        testClientButton.setOnAction(e -> {
        	if (!clientInit) {
            	initiateClient(locIP);
            	clientInit = true;
        	}
        });
        // TEST ---------

        // TODO replace gameboj with clever name
        primaryStage.setTitle("gameboj: the GameBoy emulator");

        // Splash screen

        ImageView emulationView = new ImageView();
        emulationView.setFitWidth(1.1 * LCD_WIDTH);
        emulationView.setFitHeight(1.1 * LCD_HEIGHT);
        
        Scene simpleModeScreen = SimpleModeScreen.getSimpleModeScreen(emulationView);

        BorderPane extendedBorderPane = new BorderPane();

        extendedBorderPane.setCenter(emulationView);

        Scene extendedModeScreen = new Scene(extendedBorderPane);

        // Development mode screen
        BorderPane developmentBorderPane = new BorderPane();
        FlowPane leftViewPane = new FlowPane();

        VBox topBox = new VBox();

        Menu fileMenu = new Menu(currentGuiBundle.getString("file")); // file related functionality
        MenuItem saveCartridgeMenuItem = new MenuItem(currentGuiBundle.getString("save"));
        MenuItem saveAsCartridgeMenuItem = new MenuItem(currentGuiBundle.getString("saveAs"));
        MenuItem loadCartridgeMenuItem = new MenuItem(currentGuiBundle.getString("load"));
        loadCartridgeMenuItem.setOnAction(e -> {
            FileChooser gamePakChooser = new FileChooser();
            gamePakChooser.setTitle(currentGuiBundle.getString("chooseGamePak"));
            try {
                gameboj = new GameBoy(Cartridge.ofFile(gamePakChooser.showOpenDialog(primaryStage)));
            } catch (FileNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (LineUnavailableException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        });
        MenuItem exitMenuItem = new MenuItem(currentGuiBundle.getString("exit"));
        exitMenuItem.setOnAction(e -> {
        	try {
				serverSocket.close();
				clientSocket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
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
        MenuItem showStateMenuItem = new MenuItem(currentGuiBundle.getString("showGameboyState"));
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
				FileWriter writer = new FileWriter("Z80 Assembly " + fileName.concat("_header.txt"));
				writer.write(CartridgeDisassembler.decompileHeader(fileName));
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
    	currentVolume.bind(volumeSlider.valueProperty());
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
    	ChoiceBox<String> gbEffects = new ChoiceBox<>(FXCollections.observableArrayList("Normal", "Fade"));
		FadeTransition fadeEffectTransition = new FadeTransition();
    	gbEffects.getSelectionModel().selectedItemProperty().addListener((f, o, n) -> {
    		switch (n) {
    		case "Normal":
    			
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
    		currentLocale = Locale.ENGLISH;
    		currentGuiBundle = ResourceBundle.getBundle("GUIBundle", currentLocale);
    		
    	});
    	englishButton.setSelected(true);
    	englishButton.setToggleGroup(languageSelectionGroup);
    	RadioButton frenchButton = new RadioButton(Locale.FRANCE.getDisplayName());
    	frenchButton.setOnAction(e -> {
    		currentLocale = Locale.FRANCE;
    		currentGuiBundle = ResourceBundle.getBundle("GUIBundle", currentLocale);
    	});
    	frenchButton.setToggleGroup(languageSelectionGroup);
    	RadioButton romanianButton = new RadioButton(new Locale("ro", "RO").getDisplayName());
    	romanianButton.setOnAction(e -> {
    		currentLocale = Locale.forLanguageTag("ro_RO");
    		currentGuiBundle = ResourceBundle.getBundle("GUIBundle", currentLocale);
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
				gameboj.getCartridge().toFile("currentSave.gb");
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
        preferencesMenu.getItems().addAll(themeMenuItem, skinsMenuItem, languageMenuItem, safeModeButton);

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

        // TODO add button graphics (btn.setGraphic())
        Button tbResetButton = new Button(currentGuiBundle.getString("reset"));
        tbResetButton.setOnAction(e -> {
            try {
                gameboj = new GameBoy(Cartridge.ofFile(new File(fileName)));
                
                animationTimer.stop();
                
                start = System.nanoTime();
                
                animationTimer = new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        long elapsed = now - start - pauseTime;
                        long elapsedCycles = (long) (elapsed * CYCLES_PER_NANOSECOND);

                        gameboj.runUntil(cycleSpeed * IMAGE_CYCLE_DURATION + gameboj.getCycles());

                        emulationView.requestFocus();
                        emulationView.setImage(ImageConverter.convert(gameboj.getLcdController().currentImage()));

                        viewportRectangle.setTranslateX(gameboj.getBus().read(AddressMap.REG_SCX)); // FIXME
                        viewportRectangle.setTranslateY(gameboj.getBus().read(AddressMap.REG_SCY));
                        backgroundView.setImage(ImageConverter.convert(gameboj.getLcdController().getBackground()));
                        windowView.setImage(ImageConverter.convert(gameboj.getLcdController().getWindow()));
                        
                        LcdImage[] spriteLayerImages = gameboj.getLcdController().getSprites();
                        bgSpriteView.setImage(ImageConverter.convert(spriteLayerImages[0]));
                        fgSpriteView.setImage(ImageConverter.convert(spriteLayerImages[1]));
                    }
                };
                
                animationTimer.start();
            } catch (FileNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (LineUnavailableException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        });
        Button tbPauseButton = new Button(currentGuiBundle.getString("pause"));
        tbPauseButton.setOnAction(e -> {
            if (isPaused.get()) {
                isPaused.set(false);
    			fadeEffectTransition.stop();
                animationTimer.start();
                pauseTime += System.nanoTime() - timeOnPause;
            } else {
                isPaused.set(true);
                animationTimer.stop();
                fadeEffectTransition.setFromValue(1.0);
    			fadeEffectTransition.setToValue(0.6);
    			fadeEffectTransition.setAutoReverse(true);
    			fadeEffectTransition.setNode(emulationView);
    			fadeEffectTransition.setCycleCount(FadeTransition.INDEFINITE);
    			fadeEffectTransition.playFromStart();
                timeOnPause = System.nanoTime();
            }
        });       
        Button screenshotButton = new Button(currentGuiBundle.getString("screenshot"));
        screenshotButton.setOnAction(e -> {
			try {
				screenshot();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});       
        Button speedTimes5Button = new Button("x3");
        speedTimes5Button.setOnAction(e -> {
        	cycleSpeed = isSpeedButtonPressed ? 3 : 1;      	
        	isSpeedButtonPressed = !isSpeedButtonPressed;
        });
        Button saveButton = new Button(currentGuiBundle.getString("save"));
        saveButton.setOnAction(e -> {
        	gameboj.getCartridge().toFile("currentSave.gb");
        });
        Button toggleScreenCapButton = new Button("Start/stop screen capture");
        ImageViewRecorder recorder = new ImageViewRecorder("screen_recording.avi", emulationView);
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
        	isMuted = !isMuted;
        });
        
        Button terminateConnectionButton = new Button("Terminate");
        terminateConnectionButton.setOnAction(e -> {
			try {
				serverSocket.close();
				clientSocket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        });
        
        Button startGameButton = new Button("Start");
        startGameButton.setOnAction(e -> {
            animationTimer.start();
        });
        ToolBar toolBar = new ToolBar(tbResetButton, tbPauseButton, speedTimes5Button, screenshotButton, toggleScreenCapButton, muteButton, saveButton,
        		testServerButton, testClientButton, terminateConnectionButton, startGameButton); 

        topBox.getChildren().addAll(mainMenuBar, toolBar);
        
        developmentBorderPane.setTop(topBox);
        ImageView gameboySkin = new ImageView(new Image("file:game-boy-vector-free-download-cartoon-gameboy.jpg"));
        Pane emulationPane = new Pane(gameboySkin);
        emulationView.setTranslateX(130);
        emulationView.setTranslateY(70);
        emulationPane.getChildren().add(emulationView);
        developmentBorderPane.setCenter(emulationPane);
        developmentBorderPane.setLeft(leftViewPane);

        Scene developmentModeScreen = new Scene(developmentBorderPane);
        setInput(emulationView, gameboj.getJoypad());       

        Scene modeChoiceScreen = new ModeChoiceScreen(primaryStage,
        		List.of(simpleModeScreen, extendedModeScreen, developmentModeScreen), currentGuiBundle).getScreen();
        
        Scene loginScreen = LoginScreen.getLoginScreen(primaryStage, modeChoiceScreen, currentGuiBundle);
        
        Scene splashScreen = SplashScreen.getSplashScreen(primaryStage, loginScreen);
        
        primaryStage.getIcons().add(new Image("file:gb_icon.png"));
        primaryStage.setScene(splashScreen);
        primaryStage.show();

        start = System.nanoTime();

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsed = now - start - pauseTime;
                long elapsedCycles = (long) (elapsed * CYCLES_PER_NANOSECOND);

                gameboj.runUntil(cycleSpeed * IMAGE_CYCLE_DURATION + gameboj.getCycles());

                emulationView.requestFocus();
                Image screenImage = ImageConverter.convert(gameboj.getLcdController().currentImage());
                emulationView.setImage(screenImage);
                
                LoginScreen.setShearedGameboyView(screenImage);
                
//                screenCapFrames.add(screenImage); //TODO remove?
//                
//                recorder.write();

                viewportRectangle.setTranslateX(gameboj.getBus().read(AddressMap.REG_SCX));
                viewportRectangle.setTranslateY(gameboj.getBus().read(AddressMap.REG_SCY));
                backgroundView.setImage(ImageConverter.convert(gameboj.getLcdController().getBackground()));
                windowView.setImage(ImageConverter.convert(gameboj.getLcdController().getWindow()));
                
                LcdImage[] spriteLayerImages = gameboj.getLcdController().getSprites();
                bgSpriteView.setImage(ImageConverter.convert(spriteLayerImages[0]));
                fgSpriteView.setImage(ImageConverter.convert(spriteLayerImages[1]));
            }
        };
    }

    private static void setInput(Node node, Joypad jp) {
        // Set up keyboard input
        node.setOnKeyPressed(e -> {
            switch (e.getCode()) {
            case A:
                jp.keyPressed(Key.A);
                break;
            case B:
                jp.keyPressed(Key.B);
                break;
            case S:
                jp.keyPressed(Key.START);
                break;
            case SPACE:
                jp.keyPressed(Key.SELECT);
                break;
            case UP:
                jp.keyPressed(Key.UP);
                break;
            case RIGHT:
                jp.keyPressed(Key.RIGHT);
                break;
            case DOWN:
                jp.keyPressed(Key.DOWN);
                break;
            case LEFT:
                jp.keyPressed(Key.LEFT);
                break;
            case H:
            	printCPU = !printCPU;
            	break;
            }
        });

        node.setOnKeyReleased(e -> {
            switch (e.getCode()) {
            case A:
                jp.keyReleased(Key.A);
                break;
            case B:
                jp.keyReleased(Key.B);
                break;
            case S:
                jp.keyReleased(Key.START);
                break;
            case SPACE:
                jp.keyReleased(Key.SELECT);
                break;
            case UP:
                jp.keyReleased(Key.UP);
                break;
            case RIGHT:
                jp.keyReleased(Key.RIGHT);
                break;
            case DOWN:
                jp.keyReleased(Key.DOWN);
                break;
            case LEFT:
                jp.keyReleased(Key.LEFT);
                break;
            }
        });
    }
    
    public static void initiateClient(InetAddress locIP) {
    	try {
    		clientSocket = new Socket(locIP, 4444);
    		
    		SerialProtocol.setDataInput(new DataInputStream(clientSocket.getInputStream()));
    		SerialProtocol.setDataOutput(new DataOutputStream(clientSocket.getOutputStream()));
    		
        	SerialProtocol.startProtocol(false);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    }

    private void screenshot() throws IOException {
        LcdImage screenshot = gameboj.getLcdController().currentImage();
        Date d = new Date();
        String date = Long.toString(d.getTime());
        BufferedImage i = new BufferedImage(screenshot.getWidth(), screenshot.getHeight(), BufferedImage.TYPE_INT_RGB);
        ImageIO.write(SwingFXUtils.fromFXImage(ImageConverter.convert(screenshot), i), "png", new File("ScreenCaptures/" + date + ".png"));
    }
}
