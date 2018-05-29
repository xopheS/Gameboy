package ch.epfl.gameboj.gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.sound.sampled.LineUnavailableException;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.lcd.LcdImage;
import ch.epfl.gameboj.mvc.Model;
import ch.epfl.gameboj.mvc.View;
import ch.epfl.gameboj.mvc.controller.Controller;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.stage.Stage;

public class Main extends Application {
	private Locale currentLocale = Locale.getDefault();
	private ResourceBundle currentGuiBundle = ResourceBundle.getBundle("GUIBundle", currentLocale);
    boolean clientInit;
    
    private static final Model model = Model.getModel();
    private static final View view = View.getView();
    private static final Controller controller = Controller.getController();

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException, LineUnavailableException, InterruptedException {
    	view.setPrimaryStage(primaryStage);
    	view.setCurrentLocale(currentLocale);
    	view.setCurrentGuiBundle(currentGuiBundle);
    	controller.setModel(model);
    	model.setView(view);
    	
        List<String> cmdArgs = getParameters().getRaw();
        Preconditions.checkArgument(cmdArgs.size() <= 2, () -> System.exit(1));

        String fileName = cmdArgs.get(0); // TODO change this
        String saveFileName = cmdArgs.size() == 2 ? cmdArgs.get(1) : null;
        
        String preloadCartridgeName = view.getLoadedPreferences().get("PRELOAD_CARTRIDGE", null);
        
        if (preloadCartridgeName != null) {
        	controller.startEmulation(preloadCartridgeName);
        } else {
        	if (saveFileName == null) {
        		controller.startModelGameboy(fileName);
        	} else {
        		controller.startModelGameboy(fileName, saveFileName);
        	}
        }

        controller.setInput(view.getEmulationView(), model.getGameboj().getJoypad());       
        
        view.showPrimaryStage();
    }

    public static void screenshot() throws IOException {
        LcdImage screenshot = model.getGameboj().getLcdController().currentImage();
        Date d = new Date();
        String date = Long.toString(d.getTime());
        BufferedImage i = new BufferedImage(screenshot.getWidth(), screenshot.getHeight(), BufferedImage.TYPE_INT_RGB);
        ImageIO.write(SwingFXUtils.fromFXImage(ImageConverter.convert(screenshot), i), "png", new File("ScreenCaptures/" + date + ".png"));
    }
}
