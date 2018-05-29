package ch.epfl.gameboj.mvc;

import static ch.epfl.gameboj.component.lcd.LcdController.LCD_HEIGHT;
import static ch.epfl.gameboj.component.lcd.LcdController.LCD_WIDTH;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import ch.epfl.gameboj.gui.screens.DevelopmentModeScreen;
import ch.epfl.gameboj.gui.screens.ExtendedModeScreen;
import ch.epfl.gameboj.gui.screens.LoginScreen;
import ch.epfl.gameboj.gui.screens.ModeChoiceScreen;
import ch.epfl.gameboj.gui.screens.SimpleModeScreen;
import ch.epfl.gameboj.gui.screens.SplashScreen;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public final class View {
	private static final View view = new View();
	private Model model = Model.getModel();
	
	private Stage primaryStage;
	
	private final Scene splashScreen;
	private final Scene loginScreen;
	private final Scene simpleModeScreen;
	private final Scene extendedModeScreen;
	private Scene developmentModeScreen;
	private final Scene modeChoiceScreen;
	
	private Locale currentLocale = Locale.getDefault();
	private ResourceBundle currentGuiBundle;
	
    private static DoubleProperty currentVolume = new SimpleDoubleProperty(0);
    
    private ImageView emulationView = new ImageView();
    
    private Preferences loadedPreferences = Preferences.userNodeForPackage(this.getClass());
	
	private View() {
        emulationView.setFitWidth(1.1 * LCD_WIDTH);
        emulationView.setFitHeight(1.1 * LCD_HEIGHT);
        
        emulationView.requestFocus();
        
        emulationView.imageProperty().bind(model.getScreenImage());
        
        simpleModeScreen = SimpleModeScreen.getSimpleModeScreen(emulationView);
        try {
			developmentModeScreen = DevelopmentModeScreen.getDevelopmentModeScreen(primaryStage);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			developmentModeScreen = null;
		}
        extendedModeScreen = ExtendedModeScreen.getExtendedModeScreen(emulationView);
        
        modeChoiceScreen = new ModeChoiceScreen(primaryStage,
        		List.of(simpleModeScreen, extendedModeScreen, developmentModeScreen), currentGuiBundle).getScreen();
        
        loginScreen = LoginScreen.getLoginScreen(primaryStage, modeChoiceScreen, currentGuiBundle);
        
        splashScreen = SplashScreen.getSplashScreen(primaryStage, loginScreen);       
        
        LoginScreen.setShearedGameboyView(model.getScreenImage().get());
	}
	
	public static View getView() {
		return view;
	}
	
	public void setPrimaryStage(Stage primaryStage) {
		this.primaryStage = primaryStage;
        // TODO replace gameboj with clever name
        primaryStage.setTitle("gameboj: the GameBoy emulator");
        primaryStage.getIcons().add(new Image("file:gb_icon.png"));
        primaryStage.setScene(splashScreen);
	}
	
	public Locale getCurrentLocale() {
		return currentLocale;
	}
	
	public void setCurrentLocale(Locale currentLocale) {
		this.currentLocale = currentLocale;
	}
	
	public ResourceBundle getCurrentGuiBundle() {
		return currentGuiBundle;
	}
	
	public void setCurrentGuiBundle(ResourceBundle currentGuiBundle) {
		this.currentGuiBundle = currentGuiBundle;
	}
	
	public Preferences getLoadedPreferences() {
		return loadedPreferences;
	}
	
	public void showPrimaryStage() {
		primaryStage.show();
	}
	
	public ImageView getEmulationView() {
		return emulationView;
	}
	
	public DoubleProperty getCurrentVolume() {
		return currentVolume;
	}
}
