package ch.epfl.gameboj.gui;

import static ch.epfl.gameboj.GameBoy.CYCLES_PER_NANOSECOND;
import static ch.epfl.gameboj.component.lcd.LcdController.IMAGE_CYCLE_DURATION;
import static ch.epfl.gameboj.component.lcd.LcdController.LCD_HEIGHT;
import static ch.epfl.gameboj.component.lcd.LcdController.LCD_WIDTH;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.effect.PerspectiveTransform;
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
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {
	private Locale currentLocale = new Locale("ro", "RO");
	private ResourceBundle currentGuiBundle = ResourceBundle.getBundle("GUIBundle", currentLocale);
    private static GameBoy gameboj;
    boolean isPaused;
    boolean isSpeedButtonPressed;
    boolean isScreenCapOn;
    public static boolean isMuted;
    List<Image> screenCapFrames = new ArrayList<>();
    long start;
    long timeOnPause;
    long pauseTime;
    int cycleSpeed = 1;
    AnimationTimer animationTimer;
    public static DoubleProperty currentVolume = new SimpleDoubleProperty();

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

        // TODO replace gameboj with clever name
        primaryStage.setTitle("gameboj: the GameBoy emulator");

        // Splash screen
        ImageView splashView = new ImageView("file:EPFL-Logo.jpg");
        BorderPane splashPane = new BorderPane();
        splashPane.setCenter(splashView);
        splashView.fitWidthProperty().bind(splashPane.widthProperty());
        splashView.fitHeightProperty().bind(splashPane.heightProperty());
        Scene splashScreen = new Scene(splashPane, 200, 200);

        ImageView emulationView = new ImageView();
//        emulationView.setFitWidth(2 * LCD_WIDTH);
//        emulationView.setFitHeight(2 * LCD_HEIGHT);
        emulationView.setFitWidth(1.1 * LCD_WIDTH);
        emulationView.setFitHeight(1.1 * LCD_HEIGHT);

        BorderPane simpleBorderPane = new BorderPane(emulationView);

        Scene simpleModeScreen = new Scene(simpleBorderPane);

        BorderPane extendedBorderPane = new BorderPane();

        // extendedBorderPane.setTop(toolBar); TODO fix code duplication
        extendedBorderPane.setCenter(emulationView); // Copy the emulation view actually or it doesn't work TODO

        // TODO add gameboy overlay

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
            gamePakChooser.setTitle("Choose Game Pak");
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
        exitMenuItem.setOnAction(e -> System.exit(0));
        fileMenu.getItems().addAll(exitMenuItem, saveCartridgeMenuItem, saveAsCartridgeMenuItem, loadCartridgeMenuItem);

        Menu dumpMenu = new Menu("Dump"); // dumping related functionality
        MenuItem dumpTileSourceMenuItem = new MenuItem("Dump tile source");
        MenuItem dumpBackgroundMenuItem = new MenuItem("Dump background");
        MenuItem dumpWindowMenuItem = new MenuItem("Dump window");
        MenuItem dumpSpritesMenuItem = new MenuItem("Dump sprites");
        dumpMenu.getItems().addAll(dumpTileSourceMenuItem, dumpBackgroundMenuItem, dumpWindowMenuItem,
                dumpSpritesMenuItem);

        Menu debugMenu = new Menu(currentGuiBundle.getString("debug")); // debugging related functionality
        MenuItem stepByStepMenuItem = new MenuItem("Step by step execution");
        MenuItem decompileMenuItem = new MenuItem("Decompile");
        MenuItem showStateMenuItem = new MenuItem("Show Gameboy state");
        MenuItem disassembleBootMenuItem = new MenuItem("Dissassemble boot rom");
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
        MenuItem disassembleHeaderMenuItem = new MenuItem("Disassemble cartridge header");
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
        MenuItem changeSpeedMenuItem = new MenuItem("Change speed");
        MenuItem gameboyConfigurationMenuItem = new MenuItem("Configuration");
        
        Label volumeLabel = new Label("Global volume");
    	Label themeLabel = new Label("Gameboy theme");
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
        			ImageConverter.JavaFXColor.COLOR0.setARGB(0xFFFFFFFF);
        			ImageConverter.JavaFXColor.COLOR1.setARGB(0xFFD3D3D3);
        			ImageConverter.JavaFXColor.COLOR2.setARGB(0xFFA9A9A9);
        			ImageConverter.JavaFXColor.COLOR3.setARGB(0xFF000000);
        			break;
        		case "Creepy":
        			ImageConverter.JavaFXColor.COLOR0.setARGB(0xFFA10684);
        			ImageConverter.JavaFXColor.COLOR1.setARGB(0xFF00FF00);
        			ImageConverter.JavaFXColor.COLOR2.setARGB(0xFFFF0921);
        			ImageConverter.JavaFXColor.COLOR3.setARGB(0xFFB3B191);
    			break;
        		case "Beautiful Day":
        			ImageConverter.JavaFXColor.COLOR0.setARGB(0xFF25FDE9);
        			ImageConverter.JavaFXColor.COLOR1.setARGB(0xFFFCDC12);
        			ImageConverter.JavaFXColor.COLOR2.setARGB(0xFF3A9D23);
        			ImageConverter.JavaFXColor.COLOR3.setARGB(0xFF3F2204);
    			break;
        		case "Night":
        			ImageConverter.JavaFXColor.COLOR0.setARGB(0xFF1B019B);
        			ImageConverter.JavaFXColor.COLOR1.setARGB(0xFFAFAFAF);
        			ImageConverter.JavaFXColor.COLOR2.setARGB(0xFF303030);
        			ImageConverter.JavaFXColor.COLOR3.setARGB(0xFF000000);
    			break;
    		}
    	});
    	GridPane configPane = new GridPane();
    	configPane.add(volumeLabel, 0, 0);
    	configPane.add(volumeSlider, 0, 1);
    	configPane.add(themeLabel, 0, 2);
    	configPane.add(gbThemes, 0, 3);
        	Scene gbConfigScene = new Scene(configPane, 130, 75);
    	Stage gbConfigStage = new Stage();
        
        gameboyConfigurationMenuItem.setOnAction(e -> {
        	gbConfigStage.setScene(gbConfigScene);
        	gbConfigStage.show();
        });
        optionsMenu.getItems().addAll(changeSpeedMenuItem, gameboyConfigurationMenuItem);

        Menu windowMenu = new Menu("Window"); // workspace visual control
        Menu perspectiveMenu = new Menu("Perspective"); // switch to view layout presets
        Menu showViewMenu = new Menu("Show view"); // show view in workspace
        MenuItem setMaximized = new MenuItem("Maximize window");
        setMaximized.setOnAction(e -> primaryStage.setMaximized(true));
        MenuItem setFullscreen = new MenuItem("Go fullscreen");
        setFullscreen.setOnAction(e -> primaryStage.setFullScreen(true));
        setFullscreen.setAccelerator(new KeyCodeCombination(KeyCode.F11));

        ImageView backgroundView = new ImageView();
        ImageView windowView = new ImageView();
        ImageView bgSpriteView = new ImageView();
        ImageView fgSpriteView = new ImageView();

        MenuItem backgroundViewMenuItem = new MenuItem("Background");
        Rectangle viewportRectangle = new Rectangle(0, 0, LCD_WIDTH, LCD_HEIGHT);
        viewportRectangle.setFill(Color.TRANSPARENT);
        viewportRectangle.setStroke(Color.DARKGREEN);
        Pane backgroundViewPane = new Pane(backgroundView);
        backgroundViewPane.getChildren().add(viewportRectangle);
        backgroundViewMenuItem.setOnAction(e -> leftViewPane.getChildren().add(backgroundViewPane));
        MenuItem windowViewMenuItem = new MenuItem("Window");
        windowViewMenuItem.setOnAction(e -> leftViewPane.getChildren().add(windowView));
        MenuItem bgSpritesViewMenuItem = new MenuItem("Background sprites");
        bgSpritesViewMenuItem.setOnAction(e -> leftViewPane.getChildren().add(bgSpriteView));
        MenuItem fgSpritesViewMenuItem = new MenuItem("Foreground sprites");
        fgSpritesViewMenuItem.setOnAction(e -> leftViewPane.getChildren().add(fgSpriteView));
        showViewMenu.getItems().addAll(backgroundViewMenuItem, windowViewMenuItem, bgSpritesViewMenuItem, fgSpritesViewMenuItem);
        windowMenu.getItems().addAll(perspectiveMenu, showViewMenu, setMaximized, setFullscreen);

        Menu preferencesMenu = new Menu("Preferences"); // program preferences
        MenuItem themeMenuItem = new MenuItem("Theme");
        MenuItem skinsMenuItem = new MenuItem("Skins");
        MenuItem languageMenuItem = new MenuItem("Language");
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
        preferencesMenu.getItems().addAll(themeMenuItem, skinsMenuItem, languageMenuItem);

        Menu helpMenu = new Menu("Help"); // help functionality
        MenuItem programmingManualMenuItem = new MenuItem("Nintendo programming manual");
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
        MenuItem aboutMenuItem = new MenuItem("About");
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
        Button tbResetButton = new Button("Reset");
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
        Button tbPauseButton = new Button("Pause");
        tbPauseButton.setOnAction(e -> {
            if (isPaused) {
                isPaused = false;
                animationTimer.start();
                pauseTime += System.nanoTime() - timeOnPause;
            } else {
                isPaused = true;
                animationTimer.stop();
                timeOnPause = System.nanoTime();
            }
        });       
        Button screenshotButton = new Button("Screen");
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
        	if (isSpeedButtonPressed) {
                cycleSpeed = 1;
                isSpeedButtonPressed = false;
            } else {
                cycleSpeed = 3;
                isSpeedButtonPressed = true;
            }
        });
        Button saveButton = new Button("Save");
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
        	if (isMuted) {
        		isMuted = false;
        	} else {
        		isMuted = true;
        	}
        });
        ToolBar toolBar = new ToolBar(tbResetButton, tbPauseButton, speedTimes5Button, screenshotButton, toggleScreenCapButton, muteButton, saveButton); 

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

        // Mode choice screen
        Button simpleModeButton = new Button("Simple Mode");
        simpleModeButton.setOnAction(e -> primaryStage.setScene(simpleModeScreen));
        Button extendedModeButton = new Button("Extended Mode");
        extendedModeButton.setOnAction(e -> primaryStage.setScene(extendedModeScreen));
        Button developmentModeButton = new Button("Development Mode");
        developmentModeButton.setOnAction(e -> {
        	primaryStage.setScene(developmentModeScreen);
        	primaryStage.setMaximized(true);
        });
        VBox modeButtonsBox = new VBox(10);

        modeButtonsBox.getChildren().addAll(simpleModeButton, extendedModeButton, developmentModeButton);
        modeButtonsBox.setAlignment(Pos.CENTER);
        modeButtonsBox.setPadding(new Insets(25, 25, 25, 25));

        BorderPane modeChoicePane = new BorderPane(modeButtonsBox);

        Scene modeChoiceScreen = new Scene(modeChoicePane, 435, 275);
        
        // Login screen
        GridPane loginPane = new GridPane();
        loginPane.setAlignment(Pos.CENTER);
        loginPane.setHgap(10);
        loginPane.setVgap(10);
        loginPane.setPadding(new Insets(25, 25, 25, 25));
        
        Text loginText = new Text("Please log in");
        loginPane.add(loginText, 0, 0, 2, 1);
        
        Label usernameLabel = new Label("Username:");
        loginPane.add(usernameLabel, 0, 1);
        TextField usernameField = new TextField();
        loginPane.add(usernameField, 1, 1);
        
        Label passwordLabel = new Label("Password:");
        loginPane.add(passwordLabel, 0, 2);
        PasswordField passwordField = new PasswordField();
        loginPane.add(passwordField, 1, 2);
        
        Button noAccountButton = new Button("Continue without login");
        noAccountButton.setOnAction(e -> {
        	primaryStage.setScene(modeChoiceScreen);
        });
        
        Button login = new Button("Log in");
        Image icon = new Image("File:cartoon-gameboy-gameboy-sbstn723-on-deviantart.png", 150,150,true, true, true);
        
        loginPane.add(noAccountButton, 2, 5);
        loginPane.add(login, 1, 5);
        ImageView shearedGameboyView = new ImageView();
        PerspectiveTransform shear = new PerspectiveTransform();
        shear.setUlx(13);
        shear.setUly(-2);
        shear.setUrx(50);
        shear.setUry(0);
        shear.setLrx(37);
        shear.setLry(28);
        shear.setLlx(0);
        shear.setLly(25);
        shearedGameboyView.setEffect(shear);
        shearedGameboyView.setRotate(21);
        shearedGameboyView.setTranslateX(47);
        shearedGameboyView.setTranslateY(46);
        loginPane.add(new ImageView(icon), 2, 1, 1, 2);
        loginPane.add(shearedGameboyView, 2, 1, 1, 2);       
        
        Scene loginScreen = new Scene(loginPane);
        loginScreen.setOnKeyPressed(e -> {
        	if (e.getCode() == KeyCode.ENTER) {
        		try {
        			Connection connection = DriverManager.getConnection("jdbc:mysql://sql7.freemysqlhosting.net:3306/sql7239737", "sql7239737", "QTbGGaykPd");
        			PreparedStatement ps = 
        					connection.prepareStatement("SELECT `Username`, `Password` FROM `Gameboj Users` WHERE `Username` = ? AND `Password` = ?");
        			ps.setString(1, usernameField.getText());
        			ps.setString(2, passwordField.getText());
        			ResultSet result = ps.executeQuery();
        			if (result.next()) {
        				primaryStage.setScene(modeChoiceScreen);
        			} else {
        				Text loginResultText = new Text("Username/password wrong");
        				loginPane.add(loginResultText, 0, 3);
        			}
        		} catch (SQLException ex) {
        			ex.printStackTrace();
        		}
        	}
        });

        FadeTransition splashFade = new FadeTransition();
        splashFade.setNode(splashPane);
        splashFade.setDelay(new Duration(1000));
        splashFade.setDuration(new Duration(3000));
        splashFade.setFromValue(1.0);
        splashFade.setToValue(0.0);
        splashFade.setOnFinished(e -> {
        	primaryStage.setScene(loginScreen);
        });
        
        //TODO USE TRANSITIONS FOR THE EMULATION VIEW
        
        primaryStage.getIcons().add(new Image("file:gb_icon.png"));
        primaryStage.setScene(splashScreen);
        splashFade.play();
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
                shearedGameboyView.setImage(screenImage);
                
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
        
        animationTimer.start();
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

    private void screenshot() throws IOException {
        LcdImage screenshot = gameboj.getLcdController().currentImage();
        Date d = new Date();
        String date = Long.toString(d.getTime());
        BufferedImage i = new BufferedImage(screenshot.getWidth(), screenshot.getHeight(), BufferedImage.TYPE_INT_RGB);
        ImageIO.write(SwingFXUtils.fromFXImage(ImageConverter.convert(screenshot), i), "png", new File("ScreenCaptures/" + date + ".png"));
    }
}





